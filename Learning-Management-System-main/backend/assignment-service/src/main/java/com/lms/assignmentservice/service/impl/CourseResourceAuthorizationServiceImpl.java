package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseResourceAuthorizationServiceImpl implements CourseResourceAuthorizationService {

    AssignmentAuthorizationService authorizationService;
    CourseClient courseClient;

    @Override
    public void checkManager(String courseId, String createdBy) {
        if (authorizationService.isAdmin()) {
            return;
        }

        String userId = authorizationService.currentUserId();
        InternalCourseResponse course = courseClient.getCourse(courseId);

        if (!userId.equals(createdBy) && (course == null || !userId.equals(course.getInstructorId()))) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        if (course != null && "LOCKED".equals(course.getStatus())) {
            throw new AssignmentException(ErrorCode.COURSE_LOCKED);
        }
    }

    @Override
    public void checkCourseNotLocked(String courseId) {
        InternalCourseResponse course = courseClient.getCourse(courseId);
        if (course != null && "LOCKED".equals(course.getStatus())) {
            throw new AssignmentException(ErrorCode.COURSE_LOCKED);
        }
    }
}
