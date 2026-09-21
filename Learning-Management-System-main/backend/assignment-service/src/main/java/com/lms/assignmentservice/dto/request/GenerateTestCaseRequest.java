package com.lms.assignmentservice.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateTestCaseRequest {
    Integer problemId;
    String title;
    String description;
    String constraints;
    Integer count;
    List<String> allowedLangs;
    List<ExistingTestCase> existingTestCases;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExistingTestCase {
        String input;
        String expectedOutput;
        Boolean hidden;
        Short orderIndex;
        Float scoreWeight;
    }
}
