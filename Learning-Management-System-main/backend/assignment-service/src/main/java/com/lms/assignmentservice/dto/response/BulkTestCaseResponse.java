package com.lms.assignmentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkTestCaseResponse {
    private String status;
    private String message;
    private int total;
    private int successCount;
    private int failedCount;
    private List<TestCaseErrorInfo> errors;

    @Data
    @Builder
    public static class TestCaseErrorInfo {
        private int index;
        private Integer rowNumber;
        private String field;
        private String inputData;
        private String reason;
    }
}
