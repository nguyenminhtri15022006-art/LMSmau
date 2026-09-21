package com.lms.chatbotservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateTestCaseRequest {

    // Hướng 1: truyền problemId → tự fetch từ Assignment Service
    Integer problemId;

    @Size(max = 300, message = "TESTCASE_TITLE_SIZE")
    String title;

    @Size(max = 5000, message = "TESTCASE_DESCRIPTION_SIZE")
    String description;

    @Size(max = 1000, message = "TESTCASE_CONSTRAINTS_SIZE")
    String constraints;

    @NotNull(message = "TESTCASE_COUNT_NULL")
    @Min(value = 1, message = "TESTCASE_COUNT_MIN")
    @Max(value = 50, message = "TESTCASE_COUNT_MAX")
    Integer count;

    List<String> allowedLangs;
    List<ExistingTestCase> existingTestCases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExistingTestCase {
        String input;
        String expectedOutput;
        Boolean hidden;
        Short orderIndex;
        Float scoreWeight;
    }
}
