package com.lms.assignmentservice.judge;

import com.lms.assignmentservice.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kết quả chấm bài — chuẩn hóa từ mọi engine.
 * <p>
 * Dù Judge0 hay engine tự build, đều phải convert output của mình
 * về JudgeResult này trước khi trả về cho SubmissionService.
 */
@Data
@Builder
public class JudgeResult {

    /**
     * Trạng thái tổng hợp của toàn bộ submission.
     * Được tính từ kết quả các test case.
     */
    private SubmissionStatus overallStatus;

    private int totalScore;        // điểm tính được (0–100)
    private int maxExecTimeMs;     // thời gian chạy lâu nhất trong các test case
    private int maxMemoryKb;       // bộ nhớ lớn nhất trong các test case
    private String compileError;      // null nếu compile thành công

    private List<TestCaseResult> testCaseResults;

    // ===== Nested =====

    @Data
    @Builder
    public static class TestCaseResult {
        private Long testCaseId;
        private SubmissionStatus status;
        private int timeMs;
        private int memoryKb;
        private boolean hidden;
        private String outputSnippet;  // 200 ký tự đầu, không lưu full output
    }
}
