package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.entity.TestCase;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Response cho submission.
 * <p>
 * STUDENT history chỉ thấy thông tin tóm tắt.
 * STUDENT detail thấy tất cả test case, nhưng hidden case chỉ có testId/status/hidden.
 * <p>
 * STUDENT owner detail thấy sourceCode của chính bài nộp.
 * INSTRUCTOR/ADMIN thấy thêm: sourceCode, allTestResults.
 * <p>
 * Dùng @JsonInclude(NON_NULL) để các field null không xuất hiện trong JSON.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionResponse {

    private String submissionId;    // ObjectId string
    private Integer problemId;
    private String userId;
    private String language;
    private String status;
    private Integer score;
    private Integer execTimeMs;
    private Integer memoryUsedKb;
    private Instant submittedAt;
    private Instant judgedAt;

    // Chỉ có trong response STUDENT — public test results
    private List<TestCaseResultResponse> publicTestResults;
    private List<TestCaseResultResponse> testCaseResults;

    // Có trong response owner detail và INSTRUCTOR/ADMIN
    private String sourceCode;
    private List<TestCaseResultResponse> allTestResults;
    private String compileError;

    // ===== Nested =====

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestCaseResultResponse {
        private Long testId;
        private String status;
        private Integer timeMs;
        private Integer memoryKb;
        private Boolean hidden;
        private String input;
        private String expectedOutput;
        private String outputSnippet;
        // isHidden không trả về client — chỉ dùng để filter
    }

    // Factory methods để tách rõ view

    public static SubmissionResponse forStudent(Submission doc) {
        var r = new SubmissionResponse();
        r.submissionId = doc.getId();
        r.problemId = doc.getProblemId();
        r.language = doc.getLanguage();
        r.status = doc.getStatus();
        r.score = doc.getScore();
        r.execTimeMs = doc.getExecTimeMs();
        r.memoryUsedKb = doc.getMemoryUsedKb();
        r.submittedAt = doc.getSubmittedAt();
        r.judgedAt = doc.getJudgeAt();
        r.compileError = doc.getCompileError();

        // Chỉ trả về public test case — filter hidden=true
        if (doc.getTestCaseResults() != null) {
            r.publicTestResults = doc.getTestCaseResults().stream()
                    .filter(tc -> Boolean.FALSE.equals(tc.getHidden()))
                    .map(SubmissionResponse::mapTestCase)
                    .toList();
        }
        return r;
    }

    public static SubmissionResponse forStudent(Submission doc, List<TestCase> problemTestCases) {
        var r = forStudent(doc);
        r.sourceCode = doc.getSourceCode();
        Map<Long, TestCase> testCaseById = problemTestCases == null
                ? Map.of()
                : problemTestCases.stream()
                .collect(Collectors.toMap(TestCase::getTestId, Function.identity(), (left, right) -> left));

        if (doc.getTestCaseResults() != null) {
            r.testCaseResults = doc.getTestCaseResults().stream()
                    .map(tc -> mapStudentTestCase(tc, testCaseById.get(tc.getTestId())))
                    .toList();
        }
        r.publicTestResults = null;

        return r;
    }

    public static SubmissionResponse forStudentSource(Submission doc) {
        var r = forHistory(doc);
        r.sourceCode = doc.getSourceCode();
        r.memoryUsedKb = doc.getMemoryUsedKb();
        r.judgedAt = doc.getJudgeAt();
        r.compileError = doc.getCompileError();
        return r;
    }

    public static SubmissionResponse forInstructor(Submission doc) {
        var r = forStudent(doc);
        r.sourceCode = doc.getSourceCode();
        r.compileError = doc.getCompileError();
        // Instructor thấy tất cả test case kể cả hidden
        if (doc.getTestCaseResults() != null) {
            r.allTestResults = doc.getTestCaseResults().stream()
                    .map(SubmissionResponse::mapTestCase)
                    .toList();
            r.publicTestResults = null;  // Instructor dùng allTestResults
        }
        return r;
    }

    public static SubmissionResponse forHistory(Submission doc) {
        var r = new SubmissionResponse();
        r.submissionId = doc.getId();
        r.problemId = doc.getProblemId();
        r.language = doc.getLanguage();
        r.status = doc.getStatus();
        r.score = doc.getScore();
        r.execTimeMs = doc.getExecTimeMs();
        r.submittedAt = doc.getSubmittedAt();
        return r;
    }

    public static SubmissionResponse pending(String submissionId, Integer problemId, String language, String status) {
        var r = new SubmissionResponse();
        r.submissionId = submissionId;
        r.problemId = problemId;
        r.language = language;
        r.status = status;
        return r;
    }

    private static TestCaseResultResponse mapTestCase(
            Submission.TestCaseResult tc) {
        var t = new TestCaseResultResponse();
        t.testId = tc.getTestId();
        t.status = tc.getStatus();
        t.timeMs = tc.getTimeMs();
        t.memoryKb = tc.getMemoryKb();
        t.outputSnippet = tc.getOutputSnippet();
        return t;
    }

    private static TestCaseResultResponse mapStudentTestCase(
            Submission.TestCaseResult tc,
            TestCase source) {
        var t = new TestCaseResultResponse();
        boolean hidden = source != null
                ? Boolean.TRUE.equals(source.getHidden())
                : Boolean.TRUE.equals(tc.getHidden());

        t.testId = tc.getTestId();
        t.status = tc.getStatus();
        t.hidden = hidden;

        if (!hidden) {
            t.timeMs = tc.getTimeMs();
            t.memoryKb = tc.getMemoryKb();
            t.outputSnippet = tc.getOutputSnippet();
            if (source != null) {
                t.input = source.getInput();
                t.expectedOutput = source.getExpectedOutput();
            }
        }

        return t;
    }
}
