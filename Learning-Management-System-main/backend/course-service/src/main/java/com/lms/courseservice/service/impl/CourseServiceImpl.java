package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.configuration.IdentityClient;
import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.CourseStudentResponse;
import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.dto.response.UserInfoDto;
import com.lms.courseservice.dto.response.InstructorStatsResponse;
import com.lms.courseservice.dto.response.InviteResultResponse;
import com.lms.courseservice.dto.response.RecentEnrollmentResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.EnrollmentStatus;
import com.lms.courseservice.repository.mysql.CourseSpecifications;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CourseMapper;
import com.lms.courseservice.mapper.EnrollmentMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.CourseService;
import com.lms.courseservice.service.GoogleMeetService;
import com.lms.courseservice.service.MinioService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseRepository courseRepository;
    CategoryRepository categoryRepository;
    EnrollmentRepository enrollmentRepository;
    AnnouncementRepository announcementRepository;
    CourseStructureRepository courseStructureRepository;
    LessonRepository lessonRepository;
    CourseMapper courseMapper;
    EnrollmentMapper enrollmentMapper;
    MinioService minioService;
    IdentityClient identityClient;
    AssignmentClient assignmentClient;
    GoogleMeetService googleMeetService;


    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable, CourseStatus status, Long categoryId, String keyword, List<CourseLevel> levels) {
        var spec = CourseSpecifications.hasStatus(status)
                .and(CourseSpecifications.hasCategory(categoryId))
                .and(CourseSpecifications.hasLevelIn(levels))
                .and(CourseSpecifications.keywordMatches(keyword));

        Page<CourseResponse> page = courseRepository.findAll(spec, pageable).map(courseMapper::toCourseResponse);
        populateExerciseCounts(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(String id) {
        CourseResponse response = courseMapper.toCourseResponse(
                courseRepository.findById(id)
                        .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND)));
        try {
            Long count = assignmentClient.getExerciseCount(id).getData();
            response.setExerciseCount(count != null ? count : 0L);
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount cho course {}: {}", id, e.getMessage());
        }
        return response;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        // Lấy userId từ SecurityContext
        String instructorId = GatewayAuthentication.currentUserId();

        // Kiểm tra trùng tên
        if(courseRepository.existsByTitle(request.getTitle()))
            throw new CourseException(ErrorCode.COURSE_TITLE_EXISTED);

        // Map và set instructor
        Course course = courseMapper.toCourse(request);
        course.setInstructorId(instructorId);

        // Tạo link Google Meet trước - nếu fail thì không tốn MinIO storage
        MeetingInfo meetingInfo = googleMeetService.createMeeting(request.getTitle());
        course.setMeetingUrl(meetingInfo.getMeetingUrl());
        course.setGoogleEventId(meetingInfo.getEventId());  // lưu để xóa khi delete course

        // Gán thumbnail - Gọi MinioService để lưu và lấy url
        if(request.getThumbnail() != null && !request.getThumbnail().isEmpty()){
            String thumbnailUrl = minioService.uploadFile(request.getThumbnail(), "thumbnails");
            course.setThumbnailUrl(thumbnailUrl);
        }

        // Gán categories
        if(request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()){
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            course.setCategories(categories);
        }

        // Lưu course MySQL trước để có courseId
        // mongoStructureId sẽ được cập nhật ngay sau
        course.setMongoStructureId("pending");
        Course savedCourse = courseRepository.save(course);

        // Tạo CourseStructure trong MongoDB với courseId đã có
        CourseStructure structure = CourseStructure.builder()
                .courseId(savedCourse.getId())
                .nodes(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
        CourseStructure savedStructure = courseStructureRepository.save(structure);

        // Cập nhật mongoStructureId vào course
        savedCourse.setMongoStructureId(savedStructure.getId());
        courseRepository.save(savedCourse);

        return courseMapper.toCourseResponse(savedCourse);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(String id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        // Kiểm tra tên trùng
        if (!course.getTitle().equals(request.getTitle())
                && courseRepository.existsByTitle(request.getTitle()))
            throw new CourseException(ErrorCode.COURSE_TITLE_EXISTED);

        // Nếu có thumbnail mới → xóa ảnh cũ, upload ảnh mới
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            if (course.getThumbnailUrl() != null)
                minioService.deleteFile(course.getThumbnailUrl());  // ← xóa ảnh cũ

            String thumbnailUrl = minioService.uploadFile(
                    request.getThumbnail(), "thumbnails");
            course.setThumbnailUrl(thumbnailUrl);  // ← lưu URL mới
        }

        // Cập nhật categories
        if (request.getCategoryIds() != null) {
            List<Category> categories = categoryRepository
                    .findAllById(request.getCategoryIds());
            course.setCategories(categories);
        }

        courseMapper.updateCourse(course, request);
        return courseMapper.toCourseResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void deleteCourse(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        // Xóa Google Meet event — lỗi external service không nên block việc xóa course
        if (course.getGoogleEventId() != null) {
            try {
                googleMeetService.deleteMeeting(course.getGoogleEventId());
            } catch (Exception e) {
                log.warn("Không thể xóa Google Calendar event [{}]: {}", course.getGoogleEventId(), e.getMessage());
            }
        }

        // Xóa thumbnail trên MinIO
        if (course.getThumbnailUrl() != null)
            minioService.deleteFile(course.getThumbnailUrl());

        // Xóa tất cả Lesson documents trong MongoDB
        lessonRepository.deleteAllByCourseId(id);

        // Xóa CourseStructure trong MongoDB
        courseStructureRepository.findById(course.getMongoStructureId())
                .ifPresent(courseStructureRepository::delete);

        // Xóa enrollments trước để tránh FK constraint
        enrollmentRepository.deleteAllByCourse(course);

        courseRepository.delete(course);
    }

    @Override
    public EnrollmentResponse enrollCourse(String courseId) {
        String userId = GatewayAuthentication.currentUserId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if(course.getStatus() == CourseStatus.LOCKED)
            throw new CourseException(ErrorCode.COURSE_LOCKED);

        if(course.getStatus() != CourseStatus.PUBLIC)
            throw new CourseException(ErrorCode.COURSE_NOT_PUBLIC);

        if(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId))
            throw new CourseException(ErrorCode.ALREADY_ENROLLED);

        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        return enrollmentMapper.toEnrollmentResponse(
                enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getMyCourses(Pageable pageable) {
        String instructorId = GatewayAuthentication.currentUserId();
        Page<CourseResponse> page = courseRepository.findByInstructorId(instructorId, pageable)
                .map(courseMapper::toCourseResponse);
        populateExerciseCounts(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getEnrolledCourses() {
        String userId = GatewayAuthentication.currentUserId();
        List<CourseResponse> courses = enrollmentRepository.findByUserId(userId).stream()
                .map(enrollment -> courseMapper.toCourseResponse(enrollment.getCourse()))
                .collect(Collectors.toList());
        populateExerciseCounts(courses);
        return courses;
    }

    @Override
    @Transactional
    public CourseResponse lockCourse(String courseId, LockCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() == CourseStatus.LOCKED)
            throw new CourseException(ErrorCode.COURSE_ALREADY_LOCKED);

        course.setStatus(CourseStatus.LOCKED);
        courseRepository.save(course);

        announcementRepository.save(Announcement.builder()
                .course(course)
                .title("[Hệ thống] Khóa học đã bị khóa")
                .content("Khóa học này đã bị Admin khóa.\n\nLý do: " + request.getReason())
                .build());

        return courseMapper.toCourseResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse unlockCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.LOCKED)
            throw new CourseException(ErrorCode.COURSE_NOT_LOCKED);

        course.setStatus(CourseStatus.PRIVATE);
        return courseMapper.toCourseResponse(courseRepository.save(course));
    }

    // ===== Helper =====

    private void populateExerciseCounts(List<CourseResponse> courses) {
        if (courses.isEmpty()) return;
        try {
            List<String> ids = courses.stream().map(CourseResponse::getId).collect(Collectors.toList());
            Map<String, Long> counts = assignmentClient.getExerciseCountBatch(ids).getData();
            if (counts != null) courses.forEach(c -> c.setExerciseCount(counts.getOrDefault(c.getId(), 0L)));
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount batch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseStudentResponse> getCourseStudents(String courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));
        checkCourseOwner(course);

        return enrollmentRepository.findByCourseId(courseId, pageable)
                .map(enrollment -> {
                    CourseStudentResponse.CourseStudentResponseBuilder builder = CourseStudentResponse.builder()
                            .userId(enrollment.getUserId())
                            .enrolledAt(enrollment.getEnrolledAt())
                            .enrollmentStatus(enrollment.getStatus().name());
                    try {
                        var userInfo = identityClient.getUserById(enrollment.getUserId());
                        if (userInfo != null) {
                            builder.username(userInfo.getUsername());
                            builder.fullname(userInfo.getFullname());
                            builder.email(userInfo.getEmail());
                            builder.avatarUrl(userInfo.getAvatarUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Không lấy được thông tin user {}: {}", enrollment.getUserId(), e.getMessage());
                    }
                    return builder.build();
                });
    }

    @Override
    @Transactional
    public void removeStudentFromCourse(String courseId, String userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));
        checkCourseOwner(course);
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId))
            throw new CourseException(ErrorCode.NOT_ENROLLED);
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public UserInfoDto searchUserByEmail(String email) {
        try {
            return identityClient.findByEmail(email);
        } catch (Exception e) {
            log.warn("Không tìm thấy user với email {}: {}", email, e.getMessage());
            return null;
        }
    }

    private void checkCourseOwner(Course course){
        String currentUserId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();

        if("ROLE_ADMIN".equals(currentRole)) return;

        if(!course.getInstructorId().equals(currentUserId))
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
    }

    private void checkNotLocked(Course course) {
        if (course.getStatus() == CourseStatus.LOCKED)
            throw new CourseException(ErrorCode.COURSE_LOCKED);
    }

    @Override
    public InviteResultResponse inviteStudents(String courseId, InviteStudentRequest request) {
        String currentUserId = GatewayAuthentication.currentUserId();

        // Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        // Kiểm tra người gửi là chủ khóa học
        checkCourseOwner(course);
        checkNotLocked(course);

        List<String> successIds  = new ArrayList<>();
        List<String> alreadyIds  = new ArrayList<>();
        List<String> notFoundIds = new ArrayList<>();

        for (String userId : request.getUserIds()) {

            // Kiểm tra userId tồn tại trong Identity Service
            if (!identityClient.existsById(userId)) {
                notFoundIds.add(userId);
                continue;
            }

            // Kiểm tra đã enroll chưa
            if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                alreadyIds.add(userId);
                continue;
            }

            // Tạo enrollment
            Enrollment enrollment = Enrollment.builder()
                    .userId(userId)
                    .course(course)
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
            enrollmentRepository.save(enrollment);
            successIds.add(userId);
        }

        return InviteResultResponse.builder()
                .successIds(successIds)
                .alreadyIds(alreadyIds)
                .notFoundIds(notFoundIds)
                .build();
    }

    @Override
    public long countStudents(String courseId) {
        if (!courseRepository.existsById(courseId))
            throw new CourseException(ErrorCode.COURSE_NOT_FOUND);
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    public long countLessons(String courseId) {
        if (!courseRepository.existsById(courseId))
            throw new CourseException(ErrorCode.COURSE_NOT_FOUND);
        return lessonRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorStatsResponse getMyStats() {
        String instructorId = GatewayAuthentication.currentUserId();

        long totalCourses = courseRepository.countByInstructorId(instructorId);
        long totalStudents = enrollmentRepository.countTotalStudentsByInstructorId(instructorId);

        long totalExercises = 0;
        try {
            List<String> courseIds = courseRepository.findIdsByInstructorId(instructorId);
            if (!courseIds.isEmpty()) {
                Map<String, Long> counts = assignmentClient.getExerciseCountBatch(courseIds).getData();
                if (counts != null) {
                    totalExercises = counts.values().stream().mapToLong(Long::longValue).sum();
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount batch cho instructor {}: {}", instructorId, e.getMessage());
        }

        return InstructorStatsResponse.builder()
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .totalExercises(totalExercises)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentEnrollmentResponse> getRecentEnrollments(int limit) {
        String instructorId = GatewayAuthentication.currentUserId();
        return enrollmentRepository
                .findRecentByInstructorId(instructorId, PageRequest.of(0, limit))
                .stream()
                .map(e -> {
                    RecentEnrollmentResponse.RecentEnrollmentResponseBuilder builder =
                            RecentEnrollmentResponse.builder()
                                    .courseId(e.getCourse().getId())
                                    .courseTitle(e.getCourse().getTitle())
                                    .courseThumbnailUrl(e.getCourse().getThumbnailUrl())
                                    .userId(e.getUserId())
                                    .enrolledAt(e.getEnrolledAt());
                    try {
                        var userInfo = identityClient.getUserById(e.getUserId());
                        if (userInfo != null) {
                            builder.fullname(userInfo.getFullname());
                            builder.avatarUrl(userInfo.getAvatarUrl());
                        }
                    } catch (Exception ex) {
                        log.warn("Không lấy được thông tin user {}: {}", e.getUserId(), ex.getMessage());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }
}
