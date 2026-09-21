package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.assignmentservice.judge.JudgeResult;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunCodeResponse {
    String status;
    Boolean allPassed;
    Integer score;
    Integer execTimeMs;
    Integer memoryUsedKb;
    String compileError;
    List<TestCaseResultResponse> testCaseResults;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestCaseResultResponse {
        Long testCaseNumber;
        String status;
        Integer timeMs;
        Integer memoryKb;
        String outputSnippet;
    }

    public static RunCodeResponse from(JudgeResult result) {
        var response = new RunCodeResponse();
        response.status = result.getOverallStatus().name();
        response.allPassed = "AC".equals(response.status);
        response.score = result.getTotalScore();
        response.execTimeMs = result.getMaxExecTimeMs();
        response.memoryUsedKb = result.getMaxMemoryKb();
        response.compileError = result.getCompileError();

        if (result.getTestCaseResults() != null) {
            response.testCaseResults = result.getTestCaseResults().stream()
                    .map(RunCodeResponse::mapTestCase)
                    .toList();
        }

        return response;
    }

    private static TestCaseResultResponse mapTestCase(JudgeResult.TestCaseResult testCase) {
        var response = new TestCaseResultResponse();
        response.testCaseNumber = testCase.getTestCaseId();
        response.status = testCase.getStatus().name();
        response.timeMs = testCase.getTimeMs();
        response.memoryKb = testCase.getMemoryKb();
        response.outputSnippet = testCase.getOutputSnippet();
        return response;
    }
}
