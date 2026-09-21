package com.lms.assignmentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CreateTestCaseRequest {

    @NotNull(message = "VALIDATION_ERROR")
    String input;

    @NotBlank(message = "VALIDATION_ERROR")
    String expectedOutput;

    @Builder.Default
    Boolean hidden = true;

    @Builder.Default
    Short orderIndex = 0;

    @Builder.Default
    Float scoreWeight = 1.0f;

}
