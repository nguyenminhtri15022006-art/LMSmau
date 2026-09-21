package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.AssignmentLessonCountResponse;
import com.lms.assignmentservice.service.AssignmentLessonSummaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssignmentLessonSummaryController {

    AssignmentLessonSummaryService assignmentLessonSummaryService;

    @GetMapping("/{courseId}/lesson-counts")
    ApiResponse<List<AssignmentLessonCountResponse>> getLessonCounts(@PathVariable String courseId) {
        return ApiResponse.<List<AssignmentLessonCountResponse>>builder()
                .data(assignmentLessonSummaryService.getLessonCounts(courseId))
                .build();
    }
}
