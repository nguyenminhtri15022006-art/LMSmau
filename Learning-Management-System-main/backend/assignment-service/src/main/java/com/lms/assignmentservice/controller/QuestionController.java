package com.lms.assignmentservice.controller;


import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.ImportBulkQuestionRequest;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.service.QuestionImportFileService;
import com.lms.assignmentservice.service.QuestionService;
import com.lms.assignmentservice.service.QuizAiPreviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/questions")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuestionController {

    QuestionService questionService;
    QuestionImportFileService questionImportFileService;
    QuizAiPreviewService quizAiPreviewService;

    @PostMapping(value = "/courses/{courseId}")
    ApiResponse<QuestionDetailResponse> createQuestion(
            @PathVariable String courseId,
            @RequestBody @Valid CreateQuestionRequest request
    ) {
        return ApiResponse.<QuestionDetailResponse>builder()
                .data(questionService.createQuestion(courseId, request))
                .build();
    }

    @PostMapping(value = "/courses/{courseId}/import-file", consumes = "multipart/form-data")
    @Deprecated(forRemoval = false)
    ApiResponse<QuestionImportResultResponse> importQuestionFile(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topic", required = false) String topic
    ) {
        return ApiResponse.<QuestionImportResultResponse>builder()
                .data(questionImportFileService.importFile(courseId, file, topic))
                .build();
    }

    @PostMapping(value = "/courses/{courseId}/import-file/preview", consumes = "multipart/form-data")
    ApiResponse<QuestionImportResultResponse> previewQuestionFile(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topic", required = false) String topic
    ) {
        return ApiResponse.<QuestionImportResultResponse>builder()
                .data(questionImportFileService.previewFile(courseId, file, topic))
                .build();
    }

    @PostMapping(value = "/courses/{courseId}/ai-generate/preview", consumes = "multipart/form-data")
    ApiResponse<QuestionImportResultResponse> previewAiGeneratedQuestions(
            @PathVariable String courseId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam Integer questionCount,
            @RequestParam DifficultyType difficulty,
            @RequestParam QuestionType questionType,
            @RequestParam(value = "topic", required = false) String topic
    ) {
        return ApiResponse.<QuestionImportResultResponse>builder()
                .data(quizAiPreviewService.preview(courseId, file, content, questionCount, difficulty, questionType, topic))
                .build();
    }

    @PostMapping(value = "/courses/{courseId}/batch")
    ApiResponse<Page<QuestionDetailResponse>> importBatchQuestion(
            @PathVariable String courseId,
            @Valid @RequestBody ImportBulkQuestionRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<Page<QuestionDetailResponse>>builder()
                .data(questionService.importBatchQuestion(courseId, request, pageable))
                .build();
    }

    @GetMapping(value = "/courses/{courseId}")
    ApiResponse<Page<QuestionDetailResponse>> getMyQuestions(
            @PathVariable String courseId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) QuestionType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<Page<QuestionDetailResponse>>builder()
                .data(questionService.getQuestionsByCourse(courseId, topic, type, pageable))
                .build();
    }

    @GetMapping(value = "/courses/{courseId}/topics")
    ApiResponse<List<String>> getTopics(@PathVariable String courseId) {
        return ApiResponse.<List<String>>builder()
                .data(questionService.getTopics(courseId))
                .build();
    }

    @GetMapping(value = "/{questionId}")
    ApiResponse<QuestionDetailResponse> getQuestions(@PathVariable Integer questionId) {
        return ApiResponse.<QuestionDetailResponse>builder()
                .data(questionService.getQuestion(questionId))
                .build();
    }

    @PutMapping(value = "/{questionId}")
    ApiResponse<QuestionDetailResponse> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        return ApiResponse.<QuestionDetailResponse>builder()
                .data(questionService.updateQuestion(questionId, request))
                .build();
    }

    @DeleteMapping(value = "/{questionId}")
    ApiResponse<Void> deleteQuestion(@PathVariable Integer questionId) {
        questionService.deleteQuestion(questionId);
        return ApiResponse.<Void>builder()
                .message("Question has been deleted successfully!")
                .build();
    }
}
