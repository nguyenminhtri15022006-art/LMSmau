package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateTestCaseResponse {
    int totalTestCases;
    List<TestCaseItem> testCases;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TestCaseItem {
        String input;
        String expectedOutput;

        @JsonAlias({"isHidden", "hidden"})
        Boolean hidden;

        Float scoreWeight;
        Short orderIndex;
        String description;
    }
}
