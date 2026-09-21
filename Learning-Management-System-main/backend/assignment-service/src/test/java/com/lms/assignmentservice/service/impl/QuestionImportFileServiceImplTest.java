package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.parser.CsvQuestionParser;
import com.lms.assignmentservice.service.parser.DocxQuestionParser;
import com.lms.assignmentservice.service.parser.ParsedQuestionItem;
import com.lms.assignmentservice.service.parser.TxtQuestionParser;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionImportFileServiceImplTest {

    @Mock
    AssignmentAuthorizationService authorizationService;

    @Mock
    TxtQuestionParser txtParser;

    @Mock
    DocxQuestionParser docxParser;

    @Mock
    CsvQuestionParser csvParser;

    QuestionImportFileServiceImpl service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new QuestionImportFileServiceImpl(
                authorizationService,
                validator,
                txtParser,
                docxParser,
                csvParser);
    }

    @Test
    void previewFileReturnsErrorsWithoutSavingWhenAnyParsedQuestionIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.txt", "text/plain", "content".getBytes());
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(txtParser.parse(file)).thenReturn(List.of(
                item(1, validQuestion("Valid question", true)),
                item(5, validQuestion("Missing correct answer", false))));

        QuestionImportResultResponse result = service.previewFile("course-1", file, "Topic");

        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst().getRowNumber()).isEqualTo(5);
    }

    @Test
    void previewFileReturnsValidQuestionsWithoutSaving() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.txt", "text/plain", "content".getBytes());
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(txtParser.parse(file)).thenReturn(List.of(
                item(1, validQuestion("First valid question", true)),
                item(5, validQuestion("Second valid question", true))));

        QuestionImportResultResponse result = service.previewFile("course-1", file, "Topic");

        assertThat(result.getValidCount()).isEqualTo(2);
        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getQuestions())
                .extracting(CreateQuestionRequest::getTopic)
                .containsExactly("Topic", "Topic");
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void importFileReturnsErrorsWhenEveryParsedQuestionIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.txt", "text/plain", "content".getBytes());
        when(authorizationService.currentUserId()).thenReturn("user-1");
        when(txtParser.parse(file)).thenReturn(List.of(item(1, validQuestion("Invalid question", false))));

        QuestionImportResultResponse result = service.previewFile("course-1", file, null);

        assertThat(result.getValidCount()).isZero();
        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void importFileRejectsEmptyFileAtRequestLevel() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.importFile("course-1", file, null))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMPORT_FILE_EMPTY));
    }

    @Test
    void importFileRejectsUnsupportedExtensionAtRequestLevel() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.pdf", "application/pdf", "content".getBytes());
        when(authorizationService.currentUserId()).thenReturn("user-1");

        assertThatThrownBy(() -> service.importFile("course-1", file, null))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_IMPORT_FORMAT));
    }

    private ParsedQuestionItem item(int rowNumber, CreateQuestionRequest question) {
        return ParsedQuestionItem.builder()
                .rowNumber(rowNumber)
                .questionNumber(rowNumber)
                .rawText(question.getContent())
                .question(question)
                .build();
    }

    private CreateQuestionRequest validQuestion(String content, boolean hasCorrectAnswer) {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setContent(content);
        request.setType(QuestionType.SINGLE);
        request.setScore((byte) 10);
        request.setAnswers(List.of(
                answer("A", hasCorrectAnswer, (short) 0),
                answer("B", false, (short) 1)));
        return request;
    }

    private CreateQuestionRequest.AnswerRequest answer(String content, boolean correct, short orderIndex) {
        CreateQuestionRequest.AnswerRequest answer = new CreateQuestionRequest.AnswerRequest();
        answer.setContent(content);
        answer.setCorrect(correct);
        answer.setOrderIndex(orderIndex);
        return answer;
    }
}
