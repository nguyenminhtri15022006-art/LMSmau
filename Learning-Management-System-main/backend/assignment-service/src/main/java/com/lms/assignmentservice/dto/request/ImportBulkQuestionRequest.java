package com.lms.assignmentservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportBulkQuestionRequest {
    String topic;

    @NotEmpty(message = "LIST_QUESTION_EMPTY")
    List<CreateQuestionRequest> questions;
}
