package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.GenerateQuizRequest;
import com.lms.assignmentservice.dto.response.GenerateQuizResponse;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse.QuestionImportError;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.httpClient.ChatbotClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.QuestionFileContentExtractor;
import com.lms.assignmentservice.service.QuizAiPreviewService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizAiPreviewServiceImpl implements QuizAiPreviewService {

    AssignmentAuthorizationService authorizationService;
    QuestionFileContentExtractor contentExtractor;
    ChatbotClient chatbotClient;
    Validator validator;

    @Value("${quiz.ai.source-content-max-length:10000}")
    @NonFinal
    int maxContentLength;

    @Override
    public QuestionImportResultResponse preview(
            String courseId,
            MultipartFile file,
            String content,
            Integer questionCount,
            DifficultyType difficulty,
            QuestionType questionType,
            String topic) {
        String userId = authorizationService.currentUserId();
        String sourceContent = resolveContent(file, content);

        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setContent(sourceContent);
        request.setQuestionCount(questionCount);
        request.setDifficulty(difficulty);
        request.setQuestionType(questionType);

        ApiResponse<GenerateQuizResponse> response = chatbotClient.generateQuiz(request);
        GenerateQuizResponse generated = response == null ? null : response.getData();
        if (generated == null || generated.getQuestions() == null || generated.getQuestions().isEmpty()) {
            throw new AssignmentException(ErrorCode.AI_GENERATE_QUIZ_FAILED);
        }

        List<QuestionImportError> errors = new ArrayList<>();
        List<CreateQuestionRequest> validQuestions = new ArrayList<>();
        List<GenerateQuizResponse.QuizQuestionResponse> generatedQuestions = generated.getQuestions();

        for (int i = 0; i < generatedQuestions.size(); i++) {
            CreateQuestionRequest question = mapGeneratedQuestion(generatedQuestions.get(i), topic);
            Set<ConstraintViolation<CreateQuestionRequest>> violations;
            try {
                violations = validator.validate(question);
            } catch (ValidationException e) {
                errors.add(QuestionImportError.builder()
                        .rowNumber(i + 1)
                        .questionNumber(i + 1)
                        .message(resolveValidationExceptionMessage(e))
                        .rawText(generatedQuestions.get(i).getQuestion())
                        .build());
                continue;
            }
            if (violations.isEmpty()) {
                validQuestions.add(question);
            } else {
                errors.add(QuestionImportError.builder()
                        .rowNumber(i + 1)
                        .questionNumber(i + 1)
                        .message(violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; ")))
                        .rawText(generatedQuestions.get(i).getQuestion())
                        .build());
            }
        }

        log.info("AI quiz preview completed: {} generated, {} valid, {} failed - course [{}] user [{}]",
                generatedQuestions.size(), validQuestions.size(), errors.size(), courseId, userId);

        return QuestionImportResultResponse.builder()
                .totalRows(generatedQuestions.size())
                .validCount(validQuestions.size())
                .importedCount(0)
                .failedCount(errors.size())
                .questions(validQuestions)
                .errors(errors)
                .build();
    }

    private String resolveContent(MultipartFile file, String content) {
        String source = file != null && !file.isEmpty()
                ? contentExtractor.extract(file)
                : content;

        if (source == null || source.isBlank()) {
            throw new AssignmentException(ErrorCode.AI_SOURCE_CONTENT_REQUIRED);
        }

        String trimmed = source.trim();
        if (trimmed.length() > maxContentLength) {
            throw new AssignmentException(ErrorCode.AI_SOURCE_CONTENT_TOO_LONG);
        }

        return trimmed;
    }

    private CreateQuestionRequest mapGeneratedQuestion(GenerateQuizResponse.QuizQuestionResponse generated, String topic) {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setContent(generated.getQuestion());
        request.setType(generated.getQuestionType() == null ? QuestionType.SINGLE : generated.getQuestionType());
        request.setTopic(topic == null || topic.isBlank() ? "AI Generated" : topic);
        request.setExplanation(generated.getExplanation());
        request.setScore((byte) 10);

        List<CreateQuestionRequest.AnswerRequest> answers = new ArrayList<>();
        List<GenerateQuizResponse.AnswerResponse> generatedAnswers = generated.getAnswers() == null
                ? List.of()
                : generated.getAnswers();
        for (int i = 0; i < generatedAnswers.size(); i++) {
            GenerateQuizResponse.AnswerResponse generatedAnswer = generatedAnswers.get(i);
            CreateQuestionRequest.AnswerRequest answer = new CreateQuestionRequest.AnswerRequest();
            answer.setContent(generatedAnswer.getContent());
            answer.setCorrect(Boolean.TRUE.equals(generatedAnswer.getCorrect()));
            answer.setOrderIndex((short) i);
            answers.add(answer);
        }
        request.setAnswers(answers);
        return request;
    }

    private String resolveValidationExceptionMessage(ValidationException e) {
        if (e.getCause() instanceof AssignmentException assignmentException) {
            return assignmentException.getErrorCode().getMessage();
        }
        return ErrorCode.VALIDATION_ERROR.getMessage();
    }
}
