package com.lms.assignmentservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalSubmissionResponse {
    String submissionId;
    Integer problemId;
    String userId;
    String language;
    String status;
    Integer score;
    Integer execTimeMs;
    Integer memoryUsedKb;
    Instant submittedAt;
    Instant judgedAt;
    String sourceCode;
    String compileError;
}