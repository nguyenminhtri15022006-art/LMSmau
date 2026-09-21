package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.CreateProblemRequest;
import com.lms.assignmentservice.dto.request.UpdateProblemRequest;
import com.lms.assignmentservice.dto.response.InstructorProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.service.ProblemService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProblemController {

    ProblemService problemService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    ApiResponse<InstructorProblemDetailResponse> createProblem(@RequestBody CreateProblemRequest request) {
        return ApiResponse.<InstructorProblemDetailResponse>builder()
                .data(problemService.createProblem(request))
                .build();
    }

    @GetMapping
    ApiResponse<Page<ProblemListResponse>> getProblems(
            @RequestParam String courseId,
            @RequestParam(required = false) String lessonId,
            @RequestParam(required = false) DifficultyType difficulty,
            @PageableDefault(size = 20) Pageable pageable) {

        return ApiResponse.<Page<ProblemListResponse>>builder()
                .data(problemService.getProblemList(courseId, lessonId, difficulty, pageable).toPage(pageable))
                .build();
    }

    @GetMapping(value = "/{problemId}")
    ApiResponse<ProblemDetailResponse> getProblemById(@PathVariable Integer problemId) {
        return ApiResponse.<ProblemDetailResponse>builder()
                .data(problemService.getProblemById(problemId))
                .build();
    }

    @GetMapping(value = "/slug/{slug}")
    ApiResponse<ProblemDetailResponse> getProblemBySlug(@PathVariable String slug) {
        return ApiResponse.<ProblemDetailResponse>builder()
                .data(problemService.getProblemBySlug(slug))
                .build();
    }

    @PutMapping(value = "/{problemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    ApiResponse<InstructorProblemDetailResponse> updateProblem(@RequestBody UpdateProblemRequest request, @PathVariable Integer problemId) {
        return ApiResponse.<InstructorProblemDetailResponse>builder()
                .data(problemService.updateProblem(request, problemId))
                .build();
    }

    @PatchMapping(value = "/{problemId}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    ApiResponse<Void> unpublishProblem(@PathVariable Integer problemId) {
        problemService.unpublishProblem(problemId);
        return ApiResponse.<Void>builder()
                .message("Problem has been unpublished!")
                .build();
    }

    @PostMapping(value = "/{problemId}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    ApiResponse<CloneProblemResponse> cloneProblem(@PathVariable Integer problemId) {
        return ApiResponse.<CloneProblemResponse>builder()
                .data(new CloneProblemResponse(problemService.cloneProblem(problemId)))
                .build();
    }

    @DeleteMapping(value = "/{problemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    ApiResponse<Void> deleteProblem(@PathVariable Integer problemId) {
        problemService.deleteProblem(problemId);
        return ApiResponse.<Void>builder()
                .message("Problem has beeen deleted!")
                .build();
    }

    @PatchMapping(value = "/{problemId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> restoreProblem(@PathVariable Integer problemId) {
        problemService.restoreProblem(problemId);
        return ApiResponse.<Void>builder()
                .message("Problem has been restored!")
                .build();
    }

}

record CloneProblemResponse(Integer newProblemId) {
}
