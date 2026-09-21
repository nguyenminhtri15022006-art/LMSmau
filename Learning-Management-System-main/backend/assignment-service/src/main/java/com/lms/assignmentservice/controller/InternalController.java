package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.InternalProblemResponse;
import com.lms.assignmentservice.dto.response.InternalSubmissionResponse;
import com.lms.assignmentservice.service.InternalAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final InternalAssignmentService internalAssignmentService;

    @GetMapping("/problems/{problemId}")
    public ApiResponse<InternalProblemResponse> getProblem(@PathVariable Integer problemId) {
        return ApiResponse.<InternalProblemResponse>builder()
                .data(internalAssignmentService.getProblem(problemId)).build();
    }

    @GetMapping("/submissions/latest")
    public ApiResponse<InternalSubmissionResponse> getLatestSubmission(
            @RequestParam Integer problemId,
            @RequestParam String userId) {
        return ApiResponse.<InternalSubmissionResponse>builder()
                .data(internalAssignmentService.getLatestSubmission(problemId, userId))
                .build();
    }

    @GetMapping("/courses/{courseId}/exercise-count")
    public ApiResponse<Long> getExerciseCount(@PathVariable String courseId) {
        return ApiResponse.<Long>builder()
                .data(internalAssignmentService.getExerciseCount(courseId))
                .build();
    }

    @PostMapping("/courses/exercise-counts")
    public ApiResponse<Map<String, Long>> getExerciseCountBatch(@RequestBody List<String> courseIds) {
        return ApiResponse.<Map<String, Long>>builder()
                .data(internalAssignmentService.getExerciseCountBatch(courseIds))
                .build();
    }
}