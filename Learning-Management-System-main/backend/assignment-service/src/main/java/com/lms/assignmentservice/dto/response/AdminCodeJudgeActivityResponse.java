package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCodeJudgeActivityResponse {
    String language;
    long total;
    long accepted;
    long wrongAnswer;
    long runtimeError;
    int acceptanceRate;
    Double avgTimeMs;
}
