package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.AddBulkQuestionQuizRequest;
import com.lms.assignmentservice.dto.request.CreateImportedQuizRequest;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.request.SaveQuizDraftRequest;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.dto.response.QuizDetailResponse;
import com.lms.assignmentservice.dto.response.QuizInstructorDetailResponse;
import com.lms.assignmentservice.dto.response.QuizStudentDetailResponse;
import com.lms.assignmentservice.service.QuizService;
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

import java.util.List;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizController {
    QuizService quizService;

    @PostMapping(value = "/courses/{courseId}")
    ApiResponse<QuizDetailResponse> createQuiz(
            @PathVariable String courseId, @Valid @RequestBody CreateQuizRequest request) {
        return ApiResponse.<QuizDetailResponse>builder()
                .data(quizService.createQuiz(courseId, request))
                .build();
    }

    @PostMapping(value = "/courses/{courseId}/imported")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<QuizDetailResponse> createQuizFromImportedQuestions(
            @PathVariable String courseId, @Valid @RequestBody CreateImportedQuizRequest request) {
        return ApiResponse.<QuizDetailResponse>builder()
                .data(quizService.createQuizFromImportedQuestions(courseId, request))
                .build();
    }

    /**
     * Legacy endpoint: Get quizzes (role-based filtering).
     * @deprecated Use /instructor/courses/{courseId}/quizzes or /student/courses/{courseId}/quizzes instead.
     */
    @GetMapping(value = "/courses/{courseId}")
    @Deprecated(forRemoval = true)
    ApiResponse<Page<QuizDetailResponse>> getQuizzes(
            @PathVariable String courseId,
            @RequestParam(required = false) String lessonId,
            @PageableDefault(value = 20, size = 20) Pageable pageable) {

        return ApiResponse.<Page<QuizDetailResponse>>builder()
                .data(quizService.getQuizzesByCourse(courseId, lessonId, pageable).toPage(pageable))
                .build();
    }

    /**
     * Get quizzes for Instructor (all quizzes, regardless of publish status).
     * Requires: INSTRUCTOR or ADMIN role.
     */
    @GetMapping(value = "/instructor/courses/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<Page<QuizInstructorDetailResponse>> getQuizzesForInstructor(
            @PathVariable String courseId,
            @RequestParam(required = false) String lessonId,
            @PageableDefault(value = 20, size = 20) Pageable pageable) {
        return ApiResponse.<Page<QuizInstructorDetailResponse>>builder()
                .data(quizService.getQuizzesForInstructor(courseId, lessonId, pageable).toPage(pageable))
                .build();
    }

    /**
     * Get quizzes for Student (only published quizzes).
     * Requires: STUDENT role.
     */
    @GetMapping(value = "/student/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<Page<QuizStudentDetailResponse>> getQuizzesForStudent(
            @PathVariable String courseId,
            @RequestParam(required = false) String lessonId,
            @PageableDefault(value = 20, size = 20) Pageable pageable) {
        return ApiResponse.<Page<QuizStudentDetailResponse>>builder()
                .data(quizService.getQuizzesForStudent(courseId, lessonId, pageable).toPage(pageable))
                .build();
    }

    @GetMapping(value = "/{quizId}")
    ApiResponse<QuizDetailResponse> getQuiz(@PathVariable Integer quizId) {
        return ApiResponse.<QuizDetailResponse>builder()
                .data(quizService.getQuiz(quizId))
                .build();
    }

    @GetMapping(value = "/{quizId}/questions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<List<QuestionDetailResponse>> getQuizQuestions(@PathVariable Integer quizId) {
        return ApiResponse.<List<QuestionDetailResponse>>builder()
                .data(quizService.getQuizQuestions(quizId))
                .build();
    }

    @PutMapping(value = "/{quizId}")
    ApiResponse<QuizDetailResponse> updateQuiz(
            @PathVariable Integer quizId, @Valid @RequestBody CreateQuizRequest request) {

        return ApiResponse.<QuizDetailResponse>builder()
                .data(quizService.updateQuiz(quizId, request))
                .build();
    }

    @PutMapping(value = "/{quizId}/sync")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    ApiResponse<QuizDetailResponse> syncQuiz(
            @PathVariable Integer quizId, @Valid @RequestBody SaveQuizDraftRequest request) {

        return ApiResponse.<QuizDetailResponse>builder()
                .data(quizService.syncQuiz(quizId, request))
                .build();
    }

    @DeleteMapping(value = "/{quizId}")
    ApiResponse<Void> deleteQuiz(@PathVariable Integer quizId) {
        quizService.deleteQuiz(quizId);
        return ApiResponse.<Void>builder()
                .message("Quiz has been deleted")
                .build();
    }

    @PatchMapping(value = "/{quizId}/publish")
    ApiResponse<Void> publishQuiz(@PathVariable Integer quizId) {
        quizService.publishQuiz(quizId);
        return ApiResponse.<Void>builder()
                .message("Quiz has been published!")
                .build();
    }

    @PatchMapping(value = "/{quizId}/unpublish")
    ApiResponse<Void> unpublishQuiz(@PathVariable Integer quizId) {
        quizService.unpublishQuiz(quizId);
        return ApiResponse.<Void>builder()
                .message("Quiz has been unpublished!")
                .build();
    }

    @PatchMapping(value = "/{quizId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> restoreQuiz(@PathVariable Integer quizId) {
        quizService.restoreQuiz(quizId);
        return ApiResponse.<Void>builder()
                .message("Quiz has been restored!")
                .build();
    }

    // # QUIZ ↔ QUESTION (trong ngân hàng)

    @PostMapping(value = "/{quizId}/questions")
    ApiResponse<Void> addQuestionsToQuiz(
            @PathVariable Integer quizId, @Valid @RequestBody AddBulkQuestionQuizRequest request) {

        quizService.bulkAddQuestionToQuiz(quizId, request);
        return ApiResponse.<Void>builder()
                .message("Questions have been added to Quiz!")
                .build();
    }

    @PostMapping(value = "/{quizId}/clone")
    ApiResponse<CloneQuizResponse> cloneQuiz(@PathVariable Integer quizId) {
        return ApiResponse.<CloneQuizResponse>builder()
                .data(new CloneQuizResponse(quizService.cloneQuiz(quizId)))
                .build();
    }

    @DeleteMapping(value = "/{quizId}/questions/{questionId}")
    ApiResponse<Void> removeQuestionInQuiz(@PathVariable Integer quizId, @PathVariable Integer questionId) {
        quizService.removeQuestionInQuiz(quizId, questionId);
        return ApiResponse.<Void>builder()
                .message("Question has been removed from Quiz!")
                .build();
    }


}

record CloneQuizResponse(Integer newQuizId) {
}
