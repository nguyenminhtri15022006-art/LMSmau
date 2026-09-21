package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.UpdateLessonRequest;
import com.lms.courseservice.dto.response.LessonResponse;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.LessonMapper;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.LessonService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonServiceImpl implements LessonService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    EnrollmentRepository enrollmentRepository;
    LessonMapper lessonMapper;

    @Override
    public LessonResponse getLesson(String courseId, String lessonId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CourseException(ErrorCode.LESSON_NOT_FOUND));

        if(!lesson.getCourseId().equals(courseId))
            throw new CourseException(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);

        checkAccessPermission(course);

        return lessonMapper.toLessonResponse(lesson);
    }

    @Override
    public LessonResponse updateLesson(String courseId, String lessonId, UpdateLessonRequest request) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CourseException(ErrorCode.LESSON_NOT_FOUND));

        if(!lesson.getCourseId().equals(courseId))
            throw new CourseException(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);

        validateContent(request.getLessonType(), request.getContent());

        lesson.setLessonType(request.getLessonType());
        lesson.setContent(request.getContent());

        return lessonMapper.toLessonResponse(lessonRepository.save(lesson));
    }

    // =========== HELPER ===========
    private void checkNotLocked(Course course) {
        if (CourseStatus.LOCKED.equals(course.getStatus()))
            throw new CourseException(ErrorCode.COURSE_LOCKED);
    }

    private void checkAccessPermission(Course course) {
        String userId = GatewayAuthentication.currentUserId();
        String role = GatewayAuthentication.currentRole();

        if("ROLE_ADMIN".equals(role)
                || ("ROLE_TEACHER".equals(role) && course.getInstructorId().equals(userId)))
            return;

        if(!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId()))
            throw new CourseException(ErrorCode.ACCESS_DENIED);
    }

    private void checkCourseOwner(Course course) {
        String currentUserId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();

        if ("ROLE_ADMIN".equals(currentRole)) return;
        if (!course.getInstructorId().equals(currentUserId))
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
    }

    private void validateContent(LessonType lessonType, Map<String, Object> content) {
        switch (lessonType) {
            case VIDEO -> {
                if (!content.containsKey("video_url"))
                    throw new CourseException(ErrorCode.LESSON_CONTENT_NULL);
                if (!content.containsKey("video_source"))
                    throw new CourseException(ErrorCode.LESSON_CONTENT_NULL);
            }
            case DOCUMENT -> {
                if (!content.containsKey("file_url"))
                    throw new CourseException(ErrorCode.LESSON_CONTENT_NULL);
                if (!content.containsKey("file_type"))
                    throw new CourseException(ErrorCode.LESSON_CONTENT_NULL);
            }
        }
    }
}
