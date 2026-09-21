package com.lms.assignmentservice.dto.request;

import com.lms.assignmentservice.enums.DifficultyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiTestCasePreviewRequest {
    Integer problemId;
    String courseId;
    String title;
    String description;
    String constraints;

    @NotNull(message = "Số lượng test case không được để trống")
    @Min(value = 1, message = "Số lượng test case tối thiểu là 1")
    @Max(value = 50, message = "Số lượng test case tối đa là 50")
    Integer count;

    DifficultyType difficulty;
    Integer timeLimitMs;
    Integer memoryLimitMb;
    Short score;
    List<String> allowedLangs;
    List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases;
}
