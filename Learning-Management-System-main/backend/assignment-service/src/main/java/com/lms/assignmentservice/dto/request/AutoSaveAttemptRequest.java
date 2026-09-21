package com.lms.assignmentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request để auto-save câu trả lời.
 * Gửi bulk: toàn bộ list câu hỏi + đáp án đã chọn.
 * <p>
 * Format:
 * {
 * "answers": [
 * { "questionId": 5, "selectedAnswerIds": [12, 13] },
 * { "questionId": 6, "selectedAnswerIds": [15] }
 * ]
 * }
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoSaveAttemptRequest {

    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    List<@Valid AnswerItem> answers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerItem {
        Integer questionId;
        List<Integer> selectedAnswerIds;
    }
}

