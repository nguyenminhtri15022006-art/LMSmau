package com.lms.assignmentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSubmissionRequest {

    @NotNull(message = "problemId không được để trống")
    Integer problemId;

    @NotBlank(message = "Ngôn ngữ không được để trống")
    String language;   // "JAVA" | "PYTHON" | "CPP" | "JAVASCRIPT"

    @NotBlank(message = "Source code không được để trống")
    @Size(max = 65536, message = "Source code tối đa 64KB")
    String sourceCode;
}