package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.GenerateQuizResponse;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.httpClient.ChatbotClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.QuestionFileContentExtractor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAiPreviewServiceImplTest {

    @Mock
    AssignmentAuthorizationService authorizationService;

    @Mock
    QuestionFileContentExtractor contentExtractor;

    @Mock
    ChatbotClient chatbotClient;

    QuizAiPreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new QuizAiPreviewServiceImpl(authorizationService, contentExtractor, chatbotClient, validator);
        ReflectionTestUtils.setField(service, "maxContentLength", 10_000);
    }

    @Test
    void previewReturnsImportResultForGeneratedQuestions() {
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(chatbotClient.generateQuiz(any())).thenReturn(ApiResponse.<GenerateQuizResponse>builder()
                .data(response(List.of(question("What is Java?", QuestionType.SINGLE, true))))
                .build());

        QuestionImportResultResponse result = service.preview(
                "course-1",
                null,
                "Java overview",
                10,
                DifficultyType.MEDIUM,
                QuestionType.SINGLE,
                "OOP");

        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().getFirst().getTopic()).isEqualTo("OOP");

        ArgumentCaptor<com.lms.assignmentservice.dto.request.GenerateQuizRequest> captor =
                ArgumentCaptor.forClass(com.lms.assignmentservice.dto.request.GenerateQuizRequest.class);
        verify(chatbotClient).generateQuiz(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Java overview");
        assertThat(captor.getValue().getDifficulty()).isEqualTo(DifficultyType.MEDIUM);
    }

    @Test
    void previewUsesUploadedFileContentAsAiSource() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.txt",
                "text/plain",
                "ignored raw file".getBytes(StandardCharsets.UTF_8));
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(contentExtractor.extract(file)).thenReturn("Extracted file content");
        when(chatbotClient.generateQuiz(any())).thenReturn(ApiResponse.<GenerateQuizResponse>builder()
                .data(response(List.of(question("What is Java?", QuestionType.SINGLE, true))))
                .build());

        service.preview(
                "course-1",
                file,
                "Manual content should be ignored",
                5,
                DifficultyType.EASY,
                QuestionType.SINGLE,
                null);

        ArgumentCaptor<com.lms.assignmentservice.dto.request.GenerateQuizRequest> captor =
                ArgumentCaptor.forClass(com.lms.assignmentservice.dto.request.GenerateQuizRequest.class);
        verify(chatbotClient).generateQuiz(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Extracted file content");
    }

    @Test
    void previewRejectsMissingSourceContent() {
        when(authorizationService.currentUserId()).thenReturn("user-1");

        assertThatThrownBy(() -> service.preview(
                "course-1",
                null,
                " ",
                10,
                DifficultyType.EASY,
                QuestionType.SINGLE,
                null))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_SOURCE_CONTENT_REQUIRED));
    }

    @Test
    void previewReportsInvalidGeneratedQuestionsAsImportErrors() {
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(chatbotClient.generateQuiz(any())).thenReturn(ApiResponse.<GenerateQuizResponse>builder()
                .data(response(List.of(question("Invalid", QuestionType.SINGLE, false))))
                .build());

        QuestionImportResultResponse result = service.preview(
                "course-1",
                null,
                "Java overview",
                10,
                DifficultyType.HARD,
                QuestionType.SINGLE,
                null);

        assertThat(result.getValidCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    private GenerateQuizResponse response(List<GenerateQuizResponse.QuizQuestionResponse> questions) {
        GenerateQuizResponse response = new GenerateQuizResponse();
        response.setTotalQuestions(questions.size());
        response.setQuestions(questions);
        return response;
    }

    private GenerateQuizResponse.QuizQuestionResponse question(String content, QuestionType type, boolean validCorrectAnswer) {
        GenerateQuizResponse.QuizQuestionResponse question = new GenerateQuizResponse.QuizQuestionResponse();
        question.setQuestion(content);
        question.setQuestionType(type);
        question.setExplanation("Because it is correct.");
        question.setAnswers(List.of(
                answer("A", validCorrectAnswer),
                answer("B", false)));
        return question;
    }

    private GenerateQuizResponse.AnswerResponse answer(String content, boolean correct) {
        GenerateQuizResponse.AnswerResponse answer = new GenerateQuizResponse.AnswerResponse();
        answer.setContent(content);
        answer.setCorrect(correct);
        return answer;
    }
}
