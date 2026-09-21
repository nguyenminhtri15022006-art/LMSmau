package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorAssignmentAnalyticsResponse {
    int completionPercent;
    int completionTrendPercent;
    List<InstructorActivityPointResponse> activity;
    List<InstructorSubmissionSummaryResponse> recentSubmissions;
    long totalSubmissions;
}
