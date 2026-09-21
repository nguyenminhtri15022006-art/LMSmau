package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.InstructorAssignmentAnalyticsResponse;
import com.lms.assignmentservice.dto.response.InstructorSubmissionSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstructorAssignmentAnalyticsService {
    InstructorAssignmentAnalyticsResponse getAnalytics(String courseId, String type);

    Page<InstructorSubmissionSummaryResponse> getSubmissions(String courseId, String type, Pageable pageable);
}
