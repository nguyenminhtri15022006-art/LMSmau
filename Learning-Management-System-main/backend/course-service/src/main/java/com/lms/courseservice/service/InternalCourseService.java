package com.lms.courseservice.service;

import com.lms.courseservice.dto.response.ChatbotCourseResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.dto.response.InternalLessonResponse;

import java.util.List;

public interface InternalCourseService {
    InternalCourseResponse getCourse(String courseId);
    InternalLessonResponse getLesson(String courseId, String lessonId);
    boolean existsEnrollment(String courseId, String userId);
    boolean existsCourse(String courseId);
    boolean existsLesson(String courseId, String lessonId);
    long countStudents(String courseId);
    List<ChatbotCourseResponse> getAllPublishedCourses();
    List<ChatbotCourseResponse> getEnrolledCourses(String userId);
}
