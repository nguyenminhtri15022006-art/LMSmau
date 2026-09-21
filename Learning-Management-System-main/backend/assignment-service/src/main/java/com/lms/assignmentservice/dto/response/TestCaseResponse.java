package com.lms.assignmentservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TestCaseResponse {
    Long testId;
    String input;
    String expectedOutput;
    Boolean hidden;
    Short orderIndex;
    Float scoreWeight;
    Instant createdAt;
}
