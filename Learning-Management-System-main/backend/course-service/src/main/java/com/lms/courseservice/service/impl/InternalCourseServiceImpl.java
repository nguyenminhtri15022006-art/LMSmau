package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.response.ChatbotCourseResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.dto.response.InternalLessonResponse;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.InternalCourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalCourseServiceImpl implements InternalCourseService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    EnrollmentRepository enrollmentRepository;

    @Override
    public InternalCourseResponse getCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        return InternalCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .instructorId(course.getInstructorId())
                .status(course.getStatus())
                .build();
    }

    @Override
    public InternalLessonResponse getLesson(String courseId, String lessonId) {
        // Kiểm tra course tồn tại
        courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        // Kiểm tra lesson tồn tại và thuộc course này
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CourseException(ErrorCode.LESSON_NOT_FOUND));

        if (!lesson.getCourseId().equals(courseId))
            throw new CourseException(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);

        return InternalLessonResponse.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourseId())
                .lessonType(lesson.getLessonType())
                .build();
    }



    @Override
    public boolean existsEnrollment(String courseId, String userId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public boolean existsCourse(String courseId) {
        return courseRepository.existsById(courseId);
    }

    @Override
    public boolean existsLesson(String courseId, String lessonId) {
        return lessonRepository.existsByCourseIdAndId(courseId, lessonId);
    }

    @Override
    public long countStudents(String courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatbotCourseResponse> getAllPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLIC, Pageable.unpaged())
                .stream()
                .map(this::toChatbotCourseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatbotCourseResponse> getEnrolledCourses(String userId) {
        return enrollmentRepository.findByUserId(userId)
                .stream()
                .map(Enrollment::getCourse)
                .map(this::toChatbotCourseResponse)
                .toList();
    }

    private ChatbotCourseResponse toChatbotCourseResponse(Course course) {
        List<String> categoryNames = course.getCategories()
                .stream()
                .map(c -> c.getName())
                .toList();

        return ChatbotCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorId(course.getInstructorId())
                .status(course.getStatus().name())
                .thumbnailUrl(course.getThumbnailUrl())
                .duration(course.getDuration())
                .level(course.getLevel() != null ? course.getLevel().name() : null)
                .categoryNames(categoryNames)
                .learningPoints(course.getLearningPoints())
                .requirements(course.getRequirements())
                .build();
    }
}
