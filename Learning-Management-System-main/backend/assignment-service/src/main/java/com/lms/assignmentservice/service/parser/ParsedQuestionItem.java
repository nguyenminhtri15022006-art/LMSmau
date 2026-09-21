package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParsedQuestionItem {
    int rowNumber;
    Integer questionNumber;
    String rawText;
    CreateQuestionRequest question;
    String errorMessage;

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
