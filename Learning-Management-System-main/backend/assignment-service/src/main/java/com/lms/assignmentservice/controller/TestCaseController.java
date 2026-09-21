package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.AiTestCasePreviewRequest;
import com.lms.assignmentservice.dto.request.CreateTestCaseRequest;
import com.lms.assignmentservice.dto.request.SyncTestCaseRequest;
import com.lms.assignmentservice.dto.response.AiTestCasePreviewResponse;
import com.lms.assignmentservice.dto.response.BulkTestCaseResponse;
import com.lms.assignmentservice.dto.response.TestCaseResponse;
import com.lms.assignmentservice.service.ProblemTestCaseAiPreviewService;
import com.lms.assignmentservice.service.TestCaseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/problems")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class TestCaseController {
    TestCaseService testCaseService;
    ProblemTestCaseAiPreviewService problemTestCaseAiPreviewService;

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PostMapping(value = "/{problemId}/testcases")
    ApiResponse<TestCaseResponse> createTestCase(
            @Valid @RequestBody CreateTestCaseRequest request,
            @PathVariable Integer problemId) {

        return ApiResponse.<TestCaseResponse>builder()
                .data(testCaseService.createTestCase(request, problemId))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PostMapping(value = "/{problemId}/testcases/bulk")
    ApiResponse<BulkTestCaseResponse> createBulkTestCases(
            @PathVariable Integer problemId,
            @RequestBody List<CreateTestCaseRequest> requestList) {

        return ApiResponse.<BulkTestCaseResponse>builder()
                .data(testCaseService.createBulkTestCases(problemId, requestList))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PostMapping(value = "/{problemId}/testcases/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<BulkTestCaseResponse> importTestCases(
            @PathVariable Integer problemId,
            @RequestPart("file") MultipartFile file) {

        return ApiResponse.<BulkTestCaseResponse>builder()
                .data(testCaseService.importTestCases(problemId, file))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PostMapping(value = "/testcases/ai-generate/preview")
    ApiResponse<AiTestCasePreviewResponse> previewAiGeneratedTestCases(
            @Valid @RequestBody AiTestCasePreviewRequest request) {

        return ApiResponse.<AiTestCasePreviewResponse>builder()
                .data(problemTestCaseAiPreviewService.preview(request))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PutMapping(value = "/{problemId}/testcases/sync")
    ApiResponse<List<TestCaseResponse>> syncTestCases(
            @PathVariable Integer problemId,
            @Valid @RequestBody List<SyncTestCaseRequest> requestList) {

        return ApiResponse.<List<TestCaseResponse>>builder()
                .data(testCaseService.syncTestCases(problemId, requestList))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @GetMapping(value = "/{problemId}/testcases")
    ApiResponse<List<TestCaseResponse>> getTestCases(@PathVariable Integer problemId) {
        return ApiResponse.<List<TestCaseResponse>>builder()
                .data(testCaseService.getTestCasesForApi(problemId))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @PutMapping("/{problemId}/testcases/{testId}")
    ApiResponse<TestCaseResponse> updateTestCase(
            @Valid @RequestBody CreateTestCaseRequest request,
            @PathVariable Integer problemId,
            @PathVariable Long testId) {

        return ApiResponse.<TestCaseResponse>builder()
                .data(testCaseService.updateTestCase(request, problemId, testId))
                .build();
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @DeleteMapping("/{id}/testcases")
    ApiResponse<Void> deleteTestCases(@PathVariable("id") Integer problemId, @RequestParam("ids") List<Long> testIds) {
        testCaseService.deleteTestCase(problemId, testIds);
        return ApiResponse.<Void>builder()
                .message("TestCases have been deleted!")
                .build();
    }
}
