package com.lms.assignmentservice.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request để bắt đầu làm bài.
 * Hiện tại đơn giản (chỉ validate basic info).
 * Nếu sau này cần ghi lại device info, IP, v.v., thêm vào đây.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class StartAttemptRequest {
    // Hiện tại có thể để trống, vì quizId đã có trong path
}

