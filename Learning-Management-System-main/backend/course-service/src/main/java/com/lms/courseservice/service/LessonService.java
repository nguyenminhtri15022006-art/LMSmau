package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.UpdateLessonRequest;
import com.lms.courseservice.dto.response.LessonResponse;

public interface LessonService {
    LessonResponse getLesson(String courseId, String lessonId);
    LessonResponse updateLesson(String courseId, String lessonId,
                                UpdateLessonRequest request);
}
