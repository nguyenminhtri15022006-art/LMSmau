package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.AssignmentLessonCountResponse;

import java.util.List;

public interface AssignmentLessonSummaryService {
    List<AssignmentLessonCountResponse> getLessonCounts(String courseId);
}
