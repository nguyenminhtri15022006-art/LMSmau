package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.SubmissionService;
import com.lms.assignmentservice.service.ProblemService;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.SubmissionJudgingService;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.request.CreateSubmissionRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.RunCodeResponse;
import com.lms.assignmentservice.dto.response.SubmissionResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.judge.JudgeEngine;
import com.lms.assignmentservice.judge.JudgeEngineException;
import com.lms.assignmentservice.judge.JudgeRequest;
import com.lms.assignmentservice.judge.JudgeResult;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * SubmissionService — hoàn toàn không biết Judge0 hay bất kỳ engine cụ thể nào.
 * Chỉ biết JudgeEngine interface.
 * <p>
 * Khi đổi engine: chỉ đổi config judge.engine=custom
 * Class này không thay đổi một chữ.
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    SubmissionRepository submissionRepository;
    TestCaseRepository testCaseRepository;
    ProblemService problemService;
    CourseClient courseClient;
    AssignmentAuthorizationService authorizationService;
    SubmissionJudgingService submissionJudgingService;

    JudgeEngine judgeEngine;

    // ==================== Submit ====================

    /**
     * Nhận submission → validate → lưu PENDING → async chấm → trả 202 ngay.
     */

    @Transactional
    @Override
    public SubmissionResponse submit(CreateSubmissionRequest request) {
        String userId = authorizationService.currentUserId();

        String language = request.getLanguage().toUpperCase();

        // validate language
        if (!judgeEngine.supportedLanguages().contains(language)) {
            throw new AssignmentException(ErrorCode.INVALID_LANGUAGE);
        }

        if (request.getSourceCode() == null || request.getSourceCode().isBlank()) {
            throw new AssignmentException(ErrorCode.SOURCE_CODE_EMPTY);
        }

        if (request.getSourceCode().length() > 65536) {
            throw new AssignmentException(ErrorCode.SOURCE_CODE_TOO_LARGE);
        }

        Problem problem = problemService.findById(request.getProblemId());

        if (problem.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_IS_DELETED);
        }

        if (!authorizationService.isAdminOrInstructor()) {
            Boolean enrolled = courseClient.checkEnrollment(problem.getCourseId(), userId);
            if (!Boolean.TRUE.equals(enrolled)) {
                throw new AssignmentException(ErrorCode.NOT_ENROLLED);
            }
        }

        boolean languageAllowedForProblem = problem.getAllowedLangs() != null
                && problem.getAllowedLangs().stream()
                .map(String::toUpperCase)
                .anyMatch(language::equals);
        if (!languageAllowedForProblem) {
            throw new AssignmentException(ErrorCode.INVALID_LANGUAGE);
        }

        runCompilePreflight(problem, request.getSourceCode(), language);

        // tao submission
        Submission submission = Submission.builder()
                .problemId(problem.getProblemId())
                .userId(userId)
                .courseId(problem.getCourseId())
                .language(language)
                .sourceCode(request.getSourceCode())
                .status(SubmissionStatus.PENDING.name())
                .score(0)
                .submittedAt(Instant.now())
                .engineUsed(judgeEngine.engineName())
                .build();

        submission = submissionRepository.save(submission);

        log.info("Tạo submission [{}] problem=[{}] user=[{}] lang=[{}] engine=[{}]",
                submission.getId(), problem.getProblemId(), userId, language, judgeEngine.engineName());

        problemService.incrementTotalSubmit(problem.getProblemId());

        submissionJudgingService.judgeAsync(submission.getId(), problem, request.getSourceCode(), language);

        return SubmissionResponse.forStudent(submission);
    }

    @Override
    public RunCodeResponse runCode(RunCodeRequest request) {
        String userId = authorizationService.currentUserId();

        String language = request.getLanguage().toUpperCase();

        if (!judgeEngine.supportedLanguages().contains(language)) {
            throw new AssignmentException(ErrorCode.INVALID_LANGUAGE);
        }

        if (request.getSourceCode() == null || request.getSourceCode().isBlank()) {
            throw new AssignmentException(ErrorCode.SOURCE_CODE_EMPTY);
        }

        if (request.getSourceCode().length() > 65536) {
            throw new AssignmentException(ErrorCode.SOURCE_CODE_TOO_LARGE);
        }

        Problem problem = problemService.findById(request.getProblemId());

        if (problem.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_IS_DELETED);
        }

        if (!authorizationService.isAdminOrInstructor()) {
            Boolean enrolled = courseClient.checkEnrollment(problem.getCourseId(), userId);
            if (!Boolean.TRUE.equals(enrolled)) {
                throw new AssignmentException(ErrorCode.NOT_ENROLLED);
            }
        }

        boolean languageAllowedForProblem = problem.getAllowedLangs() != null
                && problem.getAllowedLangs().stream()
                .map(String::toUpperCase)
                .anyMatch(language::equals);
        if (!languageAllowedForProblem) {
            throw new AssignmentException(ErrorCode.INVALID_LANGUAGE);
        }

        JudgeRequest judgeRequest = JudgeRequest.builder()
                .language(language)
                .sourceCode(request.getSourceCode())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .testCases(IntStream.range(0, request.getTestCases().size())
                        .mapToObj(index -> {
                            RunCodeRequest.TestCaseInput testCase = request.getTestCases().get(index);
                            return JudgeRequest.TestCaseInput.builder()
                                    .testCaseId((long) index + 1)
                                    .input(testCase.getInput())
                                    .expectedOutput(testCase.getExpectedOutput())
                                    .scoreWeight(1.0f)
                                    .hidden(false)
                                    .build();
                        })
                        .toList())
                .build();

        try {
            JudgeResult judgeResult = judgeEngine.judge(judgeRequest);
            return RunCodeResponse.from(judgeResult);
        } catch (JudgeEngineException e) {
            log.error("Engine [{}] loi [{}] khi chay thu problem=[{}]: {}",
                    judgeEngine.engineName(), e.getErrorType(), problem.getProblemId(), e.getMessage());

            if (e.getErrorType() == JudgeEngineException.ErrorType.TIMEOUT) {
                throw new AssignmentException(ErrorCode.JUDGE0_TIMEOUT);
            }
            throw new AssignmentException(ErrorCode.JUDGE0_ERROR);
        }
    }

    // ==================== Query ====================

    @Override
    public SubmissionResponse getSubmission(String submissionId) {
        String userId = authorizationService.currentUserId();

        Submission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.SUBMISSION_NOT_FOUND));

        boolean isOwner = sub.getUserId().equals(userId);
        boolean isPrivileged = authorizationService.isAdminOrInstructor();

        if (!isOwner && !isPrivileged) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        if (isPrivileged) {
            return SubmissionResponse.forInstructor(sub);
        }

        List<TestCase> testCases = testCaseRepository
                .findByProblem_ProblemIdOrderByOrderIndexAsc(sub.getProblemId());
        return SubmissionResponse.forStudent(sub, testCases);
    }

    @Override
    public SubmissionResponse getLatestAccepted(Integer problemId) {
        String userId = authorizationService.currentUserId();

        Problem problem = problemService.findById(problemId);
        if (!authorizationService.isAdminOrInstructor()) {
            Boolean enrolled = courseClient.checkEnrollment(problem.getCourseId(), userId);
            if (!Boolean.TRUE.equals(enrolled)) {
                throw new AssignmentException(ErrorCode.NOT_ENROLLED);
            }
        }

        return submissionRepository
                .findTopByUserIdAndProblemIdAndStatusOrderBySubmittedAtDesc(
                        userId,
                        problemId,
                        SubmissionStatus.AC.name())
                .map(SubmissionResponse::forStudentSource)
                .orElse(null);
    }

    @Override
    public Page<SubmissionResponse> getHistory(Integer problemId, String targetUserId, SubmissionStatus status, Pageable pageable) {
        // 1. Lấy thông tin người đang thao tác từ JWT
        String currentUserId = authorizationService.currentUserId();

        // 2. Xác định quyền
        boolean isPrivileged = authorizationService.isAdminOrInstructor();

        // 3. Quyết định lấy data của ai
        // Nếu có đặc quyền (Admin/Teacher) VÀ có truyền lên ID học viên muốn xem -> Lấy của học viên đó
        // Ngược lại (Là học viên, hoặc Giảng viên không truyền ID) -> Lấy của chính mình
        String queryUserId = (isPrivileged && targetUserId != null) ? targetUserId : currentUserId;

        // 4. Truy vấn và mapping an toàn
        Page<Submission> submissions = status == null
                ? submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtDesc(queryUserId, problemId, pageable)
                : submissionRepository.findByUserIdAndProblemIdAndStatusOrderBySubmittedAtDesc(
                        queryUserId,
                        problemId,
                        status.name(),
                        pageable);

        return submissions
                .map(SubmissionResponse::forHistory);
    }

    // ==================== Private Helpers ====================

    private void runCompilePreflight(Problem problem, String sourceCode, String language) {
        JudgeRequest judgeRequest = JudgeRequest.builder()
                .language(language)
                .sourceCode(sourceCode)
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .testCases(List.of(buildPreflightTestCase(problem)))
                .build();

        try {
            JudgeResult judgeResult = judgeEngine.judge(judgeRequest);
            if (judgeResult.getOverallStatus() == SubmissionStatus.CE) {
                String compileError = firstNonBlank(
                        judgeResult.getCompileError(),
                        ErrorCode.COMPILATION_ERROR.getMessage()
                );
                throw new AssignmentException(
                        ErrorCode.COMPILATION_ERROR,
                        Map.of("compileError", compileError)
                );
            }
        } catch (JudgeEngineException e) {
            log.error("Engine [{}] loi [{}] khi preflight compile problem=[{}]: {}",
                    judgeEngine.engineName(), e.getErrorType(), problem.getProblemId(), e.getMessage());

            if (e.getErrorType() == JudgeEngineException.ErrorType.TIMEOUT) {
                throw new AssignmentException(ErrorCode.JUDGE0_TIMEOUT);
            }
            throw new AssignmentException(ErrorCode.JUDGE0_ERROR);
        }
    }

    private JudgeRequest.TestCaseInput buildPreflightTestCase(Problem problem) {
        return testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(problem.getProblemId())
                .stream()
                .findFirst()
                .map(testCase -> JudgeRequest.TestCaseInput.builder()
                        .testCaseId(testCase.getTestId())
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .scoreWeight(1.0f)
                        .hidden(false)
                        .build())
                .orElseGet(() -> JudgeRequest.TestCaseInput.builder()
                        .testCaseId(0L)
                        .input("")
                        .expectedOutput("")
                        .scoreWeight(1.0f)
                        .hidden(false)
                        .build());
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
