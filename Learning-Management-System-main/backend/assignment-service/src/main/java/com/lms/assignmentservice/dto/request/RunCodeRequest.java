package com.lms.assignmentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RunCodeRequest {

    @NotNull(message = "problemId khong duoc de trong")
    Integer problemId;

    @NotBlank(message = "Ngon ngu khong duoc de trong")
    String language;

    @NotBlank(message = "Source code khong duoc de trong")
    @Size(max = 65536, message = "Source code toi da 64KB")
    String sourceCode;

    @Valid
    @NotEmpty(message = "Danh sach testcase khong duoc de trong")
    List<TestCaseInput> testCases;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TestCaseInput {
        @NotNull(message = "Input khong duoc null")
        String input;

        @NotNull(message = "Expected output khong duoc null")
        String expectedOutput;
    }
}
