package com.lms.assignmentservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request để nộp bài (submit).
 * <p>
 * **Quan trọng:** Client PHẢI gửi kèm toàn bộ final answers của học viên.
 * Backend sẽ:
 * 1. **Final Upsert:** Xóa records cũ + Insert records mới (y hệt auto-save)
 * 2. **Validate:** Kiểm tra câu hỏi và đáp án có hợp lệ không
 * 3. **Chấm điểm:** So sánh với đáp án đúng
 * 4. **Cập nhật trạng thái:** status = SUBMITTED, lưu score
 * <p>
 * **Tại sao cần Final Upsert?**
 * Giả sử auto-save chạy mỗi 30s. Nếu học viên chọn đáp án câu cuối ở giây 12,
 * rồi bấm nộp ở giây 12, auto-save chưa kịp chạy. Nếu backend dùng data cũ
 * thì câu cuối sẽ bị mất, học viên mất điểm oan.
 * <p>
 * **Kế thừa từ AutoSaveAttemptRequest:**
 * Structure của SubmitAttemptRequest giống hệt AutoSaveAttemptRequest.
 * Nếu muốn tránh code lặp (DRY), có thể tạo BaseAttemptAnswersRequest
 * rồi cho cả hai kế thừa. Tạm để riêng rẽ để rõ ràng.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptRequest {
    /**
     * Danh sách toàn bộ đáp án cuối cùng từ học viên.
     * Có thể là NULL hoặc empty (học viên không chọn gì).
     */
    List<AnswerItem> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    public static class AnswerItem {
        /**
         * ID của câu hỏi trong bài quiz
         */
        Integer questionId;

        /**
         * Danh sách ID đáp án được chọn.
         * - Câu SINGLE_CHOICE: 0 hoặc 1 element
         * - Câu MULTIPLE_CHOICE: 0 đến N elements
         * - Câu TRUE_FALSE: 0 hoặc 1 element
         */
        List<Integer> selectedAnswerIds;
    }
}
