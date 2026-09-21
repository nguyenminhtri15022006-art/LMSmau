package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.response.AnnouncementResponse;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.AnnouncementMapper;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.AnnouncementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnnouncementServiceImpl implements AnnouncementService {

    CourseRepository courseRepository;
    AnnouncementRepository announcementRepository;
    EnrollmentRepository enrollmentRepository;
    AnnouncementMapper announcementMapper;

    @Override
    public Page<AnnouncementResponse> getAnnouncements(String courseId,
                                                       Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkAccessPermission(course);

        return announcementRepository.findByCourseId(courseId, pageable)
                .map(announcementMapper::toAnnouncementResponse);
    }

    @Override
    public AnnouncementResponse createAnnouncement(String courseId,
                                                   CreateAnnouncementRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        Announcement announcement = Announcement.builder()
                .course(course)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return announcementMapper.toAnnouncementResponse(
                announcementRepository.save(announcement));
    }

    @Override
    public void deleteAnnouncement(String courseId, Long announcementId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new CourseException(
                        ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcementRepository.delete(announcement);
    }

    // ===== Helper =====
    private void checkNotLocked(Course course) {
        if (CourseStatus.LOCKED.equals(course.getStatus()))
            throw new CourseException(ErrorCode.COURSE_LOCKED);
    }

    private void checkCourseOwner(Course course) {
        String currentUserId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();
        if ("ROLE_ADMIN".equals(currentRole)) return;
        if (!course.getInstructorId().equals(currentUserId))
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
    }

    private void checkAccessPermission(Course course) {
        String userId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();
        if ("ROLE_ADMIN".equals(currentRole) ||
                ("ROLE_INSTRUCTOR".equals(currentRole) && course.getInstructorId().equals(userId)))
            return;
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId()))
            throw new CourseException(ErrorCode.ACCESS_DENIED);
    }
}
