package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response cho mỗi item trong lịch sử làm bài (paginated list).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttemptHistoryResponse {
    Long attemptId;
    Byte attemptNumber; // Lần thứ mấy (1, 2, 3...)

    BigDecimal score;
    Short totalScore;
    Boolean passed;

    String status; // IN_PROGRESS, SUBMITTED, EXPIRED

    Instant startedAt;
    Instant submittedAt;
    Integer timeSpentSeconds;
}
