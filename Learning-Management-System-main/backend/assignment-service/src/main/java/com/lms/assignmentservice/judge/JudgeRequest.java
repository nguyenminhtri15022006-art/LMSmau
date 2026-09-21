package com.lms.assignmentservice.judge;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Input cho máy chấm — độc lập với engine cụ thể.
 * Judge0 hay engine tự build đều nhận model này, rồi tự convert sang format của mình.
 */
@Data
@Builder
public class JudgeRequest {

    private String language;       // "JAVA", "PYTHON", "CPP", "JAVASCRIPT"
    private String sourceCode;
    private List<TestCaseInput> testCases;

    // Giới hạn tài nguyên
    private int timeLimitMs;    // milliseconds
    private int memoryLimitMb;  // megabytes

    @Data
    @Builder
    public static class TestCaseInput {
        private Long testCaseId;       // để map kết quả về đúng test case
        private String input;
        private String expectedOutput;
        private float scoreWeight;
        private boolean hidden;
    }
}