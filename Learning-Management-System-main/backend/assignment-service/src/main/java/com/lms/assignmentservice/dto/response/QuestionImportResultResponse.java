package com.lms.assignmentservice.dto.response;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QuestionImportResultResponse {
    int totalRows;
    int validCount;
    int importedCount;
    int failedCount;
    @Builder.Default
    List<CreateQuestionRequest> questions = new ArrayList<>();
    @Builder.Default
    List<QuestionImportError> errors = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    public static class QuestionImportError {
        int rowNumber;
        Integer questionNumber;
        String message;
        String rawText;
    }
}
