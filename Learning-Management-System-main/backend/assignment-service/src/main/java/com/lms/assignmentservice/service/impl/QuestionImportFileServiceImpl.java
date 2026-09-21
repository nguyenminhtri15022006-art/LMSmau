package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.dto.response.QuestionImportResultResponse.QuestionImportError;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.QuestionImportFileService;
import com.lms.assignmentservice.service.parser.CsvQuestionParser;
import com.lms.assignmentservice.service.parser.DocxQuestionParser;
import com.lms.assignmentservice.service.parser.ParsedQuestionItem;
import com.lms.assignmentservice.service.parser.TxtQuestionParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuestionImportFileServiceImpl implements QuestionImportFileService {

    AssignmentAuthorizationService authorizationService;
    Validator validator;
    TxtQuestionParser txtParser;
    DocxQuestionParser docxParser;
    CsvQuestionParser csvParser;

    @Override
    public QuestionImportResultResponse importFile(String courseId, MultipartFile file, String topic) {
        return previewFile(courseId, file, topic);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionImportResultResponse previewFile(String courseId, MultipartFile file, String topic) {
        if (file == null || file.isEmpty()) {
            throw new AssignmentException(ErrorCode.IMPORT_FILE_EMPTY);
        }

        String userId = authorizationService.currentUserId();
        String extension = getExtension(file.getOriginalFilename());

        List<ParsedQuestionItem> parsed;
        try {
            parsed = switch (extension) {
                case "txt" -> txtParser.parse(file);
                case "docx" -> docxParser.parse(file);
                case "csv" -> csvParser.parse(file);
                default -> throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
            };
        } catch (AssignmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing question import file: {}", e.getMessage(), e);
            throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
        }

        List<QuestionImportError> errors = new ArrayList<>();
        List<ParsedQuestionItem> validItems = new ArrayList<>();

        for (ParsedQuestionItem item : parsed) {
            if (item.hasError()) {
                errors.add(toImportError(item, item.getErrorMessage()));
                continue;
            }

            CreateQuestionRequest request = item.getQuestion();
            if (request == null) {
                errors.add(toImportError(item, ErrorCode.IMPORT_NO_VALID_QUESTIONS.getMessage()));
                continue;
            }

            Set<ConstraintViolation<CreateQuestionRequest>> violations;
            try {
                violations = validator.validate(request);
            } catch (AssignmentException e) {
                errors.add(toImportError(item, e.getErrorCode().getMessage()));
                continue;
            } catch (ValidationException e) {
                if (e.getCause() instanceof AssignmentException assignmentException) {
                    errors.add(toImportError(item, assignmentException.getErrorCode().getMessage()));
                    continue;
                }
                throw e;
            }

            if (!violations.isEmpty()) {
                String messages = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                errors.add(toImportError(item, messages));
                continue;
            }

            validItems.add(item);
        }

        int totalRows = countReadRows(parsed);
        int failedCount = errors.size();

        if (parsed.isEmpty() && failedCount == 0) {
            throw new AssignmentException(ErrorCode.IMPORT_NO_VALID_QUESTIONS);
        }

        List<CreateQuestionRequest> previewQuestions = validItems.stream()
                .map(item -> applyTopic(item.getQuestion(), topic))
                .toList();
        int validCount = previewQuestions.size();

        if (failedCount > 0) {
            log.info("Question import preview validation failed: {} rows, {} valid, {} failed - course [{}] user [{}]",
                    totalRows, validCount, failedCount, courseId, userId);
            return QuestionImportResultResponse.builder()
                    .totalRows(totalRows)
                    .validCount(validCount)
                    .importedCount(0)
                    .failedCount(failedCount)
                    .questions(previewQuestions)
                    .errors(errors)
                    .build();
        }

        log.info("Question import preview completed: {} rows, {} valid, {} failed - course [{}] user [{}]",
                totalRows, validCount, failedCount, courseId, userId);

        return QuestionImportResultResponse.builder()
                .totalRows(totalRows)
                .validCount(validCount)
                .importedCount(0)
                .failedCount(failedCount)
                .questions(previewQuestions)
                .errors(errors)
                .build();
    }

    private CreateQuestionRequest applyTopic(CreateQuestionRequest request, String topic) {
        if (topic != null && !topic.isBlank()) {
            request.setTopic(topic);
        }
        return request;
    }

    private int countReadRows(List<ParsedQuestionItem> items) {
        return items.stream()
                .mapToInt(item -> {
                    int rawLineCount = item.getRawText() == null || item.getRawText().isBlank()
                            ? 1
                            : item.getRawText().split("\\R", -1).length;
                    return item.getRowNumber() + rawLineCount - 1;
                })
                .max()
                .orElse(0);
    }

    private QuestionImportError toImportError(ParsedQuestionItem item, String message) {
        return QuestionImportError.builder()
                .rowNumber(item.getRowNumber())
                .questionNumber(item.getQuestionNumber())
                .message(message)
                .rawText(item.getRawText())
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
