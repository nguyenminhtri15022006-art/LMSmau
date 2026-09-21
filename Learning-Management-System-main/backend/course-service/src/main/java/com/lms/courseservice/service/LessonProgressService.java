package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.response.CourseProgressResponse;

public interface LessonProgressService {
    void completeLesson(String courseId, String lessonId, CompleteLessonRequest req);
    CourseProgressResponse getCourseProgress(String courseId);
    double getAverageProgress(String courseId);
}