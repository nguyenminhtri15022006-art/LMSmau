package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.CreateSubmissionRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.RunCodeResponse;
import com.lms.assignmentservice.dto.response.SubmissionResponse;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CodeJudgeController {

    SubmissionService submissionService;

    @PostMapping
    ApiResponse<SubmissionResponse> submitCode(@RequestBody @Valid CreateSubmissionRequest request) {
        return ApiResponse.<SubmissionResponse>builder()
                .data(submissionService.submit(request))
                .build();
    }

    @PostMapping("/run")
    ApiResponse<RunCodeResponse> runCode(@RequestBody @Valid RunCodeRequest request) {
        return ApiResponse.<RunCodeResponse>builder()
                .data(submissionService.runCode(request))
                .build();
    }

    @GetMapping("/latest-accepted")
    ApiResponse<SubmissionResponse> getLatestAcceptedSubmission(
            @RequestParam Integer problemId) {
        return ApiResponse.<SubmissionResponse>builder()
                .data(submissionService.getLatestAccepted(problemId))
                .build();
    }

    @GetMapping(value = "/{id}")
    ApiResponse<SubmissionResponse> getSubmission(@PathVariable("id") String submissionId) {
        return ApiResponse.<SubmissionResponse>builder()
                .data(submissionService.getSubmission(submissionId))
                .build();
    }
    
    @GetMapping
    ApiResponse<Page<SubmissionResponse>> getSubmissionHistory(
            @RequestParam Integer problemId,
            @RequestParam(value = "userId", required = false) String targetUserId,
            @RequestParam(value = "status", required = false) SubmissionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ApiResponse.<Page<SubmissionResponse>>builder()
                .data(submissionService.getHistory(problemId, targetUserId, status, pageable))
                .build();
    }
}
