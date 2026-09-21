package com.lms.chatbotservice.dto.feign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionDTO {
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