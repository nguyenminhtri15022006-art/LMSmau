package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateSubmissionRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.RunCodeResponse;
import com.lms.assignmentservice.dto.response.SubmissionResponse;
import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.judge.JudgeEngine;
import com.lms.assignmentservice.judge.JudgeRequest;
import com.lms.assignmentservice.judge.JudgeResult;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.ProblemService;
import com.lms.assignmentservice.service.SubmissionJudgingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    TestCaseRepository testCaseRepository;

    @Mock
    ProblemService problemService;

    @Mock
    CourseClient courseClient;

    @Mock
    AssignmentAuthorizationService authorizationService;

    @Mock
    JudgeEngine judgeEngine;

    @Mock
    SubmissionJudgingService submissionJudgingService;

    @InjectMocks
    SubmissionServiceImpl submissionService;

    Problem problem;

    @BeforeEach
    void setUp() {
        problem = Problem.builder()
                .problemId(10)
                .courseId("course-1")
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .allowedLangs(List.of("C", "PYTHON"))
                .build();

        lenient().when(authorizationService.currentUserId()).thenReturn("user-1");
        lenient().when(authorizationService.isAdminOrInstructor()).thenReturn(true);
        lenient().when(judgeEngine.supportedLanguages()).thenReturn(List.of("C", "PYTHON"));
        lenient().when(judgeEngine.engineName()).thenReturn("test-engine");
        lenient().when(problemService.findById(10)).thenReturn(problem);
    }

    @Test
    void runCodeReturnsCompileErrorWithoutCreatingSubmission() {
        RunCodeRequest request = new RunCodeRequest();
        request.setProblemId(10);
        request.setLanguage("c");
        request.setSourceCode("print('wrong language')");

        RunCodeRequest.TestCaseInput testCase = new RunCodeRequest.TestCaseInput();
        testCase.setInput("");
        testCase.setExpectedOutput("");
        request.setTestCases(List.of(testCase));

        when(judgeEngine.judge(any(JudgeRequest.class))).thenReturn(compileErrorResult());

        RunCodeResponse response = submissionService.runCode(request);

        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.CE.name());
        assertThat(response.getAllPassed()).isFalse();
        assertThat(response.getCompileError()).isEqualTo("expected ';' before string constant");
        verify(submissionRepository, never()).save(any());
        verify(problemService, never()).incrementTotalSubmit(any());
    }

    @Test
    void submitCompileErrorStopsBeforeSubmissionIsSaved() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setProblemId(10);
        request.setLanguage("c");
        request.setSourceCode("print('wrong language')");

        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(TestCase.builder()
                        .testId(100L)
                        .input("1 2")
                        .expectedOutput("3")
                        .build()));
        when(judgeEngine.judge(any(JudgeRequest.class))).thenReturn(compileErrorResult());

        assertThatThrownBy(() -> submissionService.submit(request))
                .isInstanceOfSatisfying(AssignmentException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMPILATION_ERROR);
                    assertThat(ex.getData()).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) ex.getData()).get("compileError"))
                            .isEqualTo("expected ';' before string constant");
                });

        verify(submissionRepository, never()).save(any());
        verify(problemService, never()).incrementTotalSubmit(any());
        verify(submissionJudgingService, never()).judgeAsync(any(), any(), any(), any());
    }

    @Test
    void submitValidCodeCreatesPendingSubmissionAfterPreflight() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setProblemId(10);
        request.setLanguage("c");
        request.setSourceCode("int main(void) { return 0; }");

        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(TestCase.builder()
                        .testId(100L)
                        .input("")
                        .expectedOutput("")
                        .build()));
        when(judgeEngine.judge(any(JudgeRequest.class))).thenReturn(acceptedResult());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId("submission-1");
            return submission;
        });
        SubmissionResponse response = submissionService.submit(request);

        assertThat(response.getSubmissionId()).isEqualTo("submission-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(problemService).incrementTotalSubmit(10);
        verify(submissionJudgingService).judgeAsync("submission-1", problem, request.getSourceCode(), "C");

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getLanguage()).isEqualTo("C");
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getSubmissionForStudentIncludesAllTestStatusesButHidesHiddenDetails() {
        Submission submission = Submission.builder()
                .id("submission-1")
                .problemId(10)
                .userId("user-1")
                .language("C")
                .sourceCode("int main(void) { return 0; }")
                .status(SubmissionStatus.WA.name())
                .score(50)
                .testCaseResults(List.of(
                        Submission.TestCaseResult.builder()
                                .testId(100L)
                                .status(SubmissionStatus.AC.name())
                                .timeMs(12)
                                .memoryKb(128)
                                .hidden(false)
                                .outputSnippet("3")
                                .build(),
                        Submission.TestCaseResult.builder()
                                .testId(101L)
                                .status(SubmissionStatus.WA.name())
                                .timeMs(15)
                                .memoryKb(256)
                                .hidden(true)
                                .outputSnippet("hidden output")
                                .build()))
                .build();

        when(authorizationService.isAdminOrInstructor()).thenReturn(false);
        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(
                        TestCase.builder()
                                .testId(100L)
                                .input("1 2")
                                .expectedOutput("3")
                                .hidden(false)
                                .build(),
                        TestCase.builder()
                                .testId(101L)
                                .input("secret input")
                                .expectedOutput("secret output")
                                .hidden(true)
                                .build()));

        SubmissionResponse response = submissionService.getSubmission("submission-1");

        assertThat(response.getSourceCode()).isEqualTo("int main(void) { return 0; }");
        assertThat(response.getTestCaseResults()).hasSize(2);
        assertThat(response.getTestCaseResults().get(0).getHidden()).isFalse();
        assertThat(response.getTestCaseResults().get(0).getInput()).isEqualTo("1 2");
        assertThat(response.getTestCaseResults().get(0).getExpectedOutput()).isEqualTo("3");
        assertThat(response.getTestCaseResults().get(0).getOutputSnippet()).isEqualTo("3");
        assertThat(response.getPublicTestResults()).isNull();

        assertThat(response.getTestCaseResults().get(1).getHidden()).isTrue();
        assertThat(response.getTestCaseResults().get(1).getStatus()).isEqualTo(SubmissionStatus.WA.name());
        assertThat(response.getTestCaseResults().get(1).getInput()).isNull();
        assertThat(response.getTestCaseResults().get(1).getExpectedOutput()).isNull();
        assertThat(response.getTestCaseResults().get(1).getOutputSnippet()).isNull();
    }

    @Test
    void getHistoryReturnsSummaryOnly() {
        Submission submission = Submission.builder()
                .id("submission-1")
                .problemId(10)
                .userId("user-1")
                .language("C")
                .sourceCode("int main(void) { return 0; }")
                .status(SubmissionStatus.AC.name())
                .score(100)
                .testCaseResults(List.of(Submission.TestCaseResult.builder()
                        .testId(100L)
                        .status(SubmissionStatus.AC.name())
                        .hidden(false)
                        .outputSnippet("3")
                        .build()))
                .build();

        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtDesc("user-1", 10, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(submission)));

        var response = submissionService.getHistory(10, null, null, PageRequest.of(0, 10));
        SubmissionResponse item = response.getContent().getFirst();

        assertThat(item.getSubmissionId()).isEqualTo("submission-1");
        assertThat(item.getStatus()).isEqualTo(SubmissionStatus.AC.name());
        assertThat(item.getSourceCode()).isNull();
        assertThat(item.getPublicTestResults()).isNull();
        assertThat(item.getTestCaseResults()).isNull();
        assertThat(item.getAllTestResults()).isNull();
    }

    @Test
    void getLatestAcceptedReturnsSourceCodeForCurrentUser() {
        Submission accepted = Submission.builder()
                .id("submission-ac")
                .problemId(10)
                .userId("user-1")
                .language("PYTHON")
                .sourceCode("print('accepted')")
                .status(SubmissionStatus.AC.name())
                .score(100)
                .build();

        when(authorizationService.isAdminOrInstructor()).thenReturn(false);
        when(courseClient.checkEnrollment("course-1", "user-1")).thenReturn(true);
        when(submissionRepository.findTopByUserIdAndProblemIdAndStatusOrderBySubmittedAtDesc(
                "user-1",
                10,
                SubmissionStatus.AC.name()))
                .thenReturn(Optional.of(accepted));

        SubmissionResponse response = submissionService.getLatestAccepted(10);

        assertThat(response.getSubmissionId()).isEqualTo("submission-ac");
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.AC.name());
        assertThat(response.getLanguage()).isEqualTo("PYTHON");
        assertThat(response.getSourceCode()).isEqualTo("print('accepted')");
    }

    private JudgeResult compileErrorResult() {
        return JudgeResult.builder()
                .overallStatus(SubmissionStatus.CE)
                .totalScore(0)
                .compileError("expected ';' before string constant")
                .testCaseResults(List.of(JudgeResult.TestCaseResult.builder()
                        .testCaseId(100L)
                        .status(SubmissionStatus.CE)
                        .build()))
                .build();
    }

    private JudgeResult acceptedResult() {
        return JudgeResult.builder()
                .overallStatus(SubmissionStatus.AC)
                .totalScore(100)
                .testCaseResults(List.of(JudgeResult.TestCaseResult.builder()
                        .testCaseId(100L)
                        .status(SubmissionStatus.AC)
                        .build()))
                .build();
    }
}
