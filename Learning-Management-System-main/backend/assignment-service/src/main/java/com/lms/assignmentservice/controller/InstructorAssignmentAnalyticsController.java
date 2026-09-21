package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.InstructorAssignmentAnalyticsResponse;
import com.lms.assignmentservice.dto.response.InstructorSubmissionSummaryResponse;
import com.lms.assignmentservice.service.InstructorAssignmentAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/instructor/courses/{courseId}")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InstructorAssignmentAnalyticsController {

    InstructorAssignmentAnalyticsService instructorAssignmentAnalyticsService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<InstructorAssignmentAnalyticsResponse> getAnalytics(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "quiz") String type) {
        return ApiResponse.<InstructorAssignmentAnalyticsResponse>builder()
                .data(instructorAssignmentAnalyticsService.getAnalytics(courseId, type))
                .build();
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<Page<InstructorSubmissionSummaryResponse>> getSubmissions(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "quiz") String type,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.<Page<InstructorSubmissionSummaryResponse>>builder()
                .data(instructorAssignmentAnalyticsService.getSubmissions(courseId, type, pageable))
                .build();
    }
}
