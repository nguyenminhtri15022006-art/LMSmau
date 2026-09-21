package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.judge.JudgeEngine;
import com.lms.assignmentservice.judge.JudgeRequest;
import com.lms.assignmentservice.judge.JudgeResult;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.service.ProblemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionJudgingServiceImplTest {

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    TestCaseRepository testCaseRepository;

    @Mock
    ProblemService problemService;

    @Mock
    JudgeEngine judgeEngine;

    @InjectMocks
    SubmissionJudgingServiceImpl judgingService;

    Problem problem;

    @BeforeEach
    void setUp() {
        problem = Problem.builder()
                .problemId(10)
                .courseId("course-1")
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .build();
    }

    @Test
    void judgeAsyncIncrementsAcceptOnACResult() {
        Submission submission = Submission.builder()
                .id("submission-1")
                .problemId(10)
                .userId("user-1")
                .status(SubmissionStatus.PENDING.name())
                .build();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(TestCase.builder()
                        .testId(100L)
                        .input("")
                        .expectedOutput("")
                        .scoreWeight(1.0f)
                        .hidden(false)
                        .build()));
        when(judgeEngine.judge(any(JudgeRequest.class))).thenReturn(JudgeResult.builder()
                .overallStatus(SubmissionStatus.AC)
                .totalScore(100)
                .testCaseResults(List.of(JudgeResult.TestCaseResult.builder()
                        .testCaseId(100L)
                        .status(SubmissionStatus.AC)
                        .build()))
                .build());
        when(judgeEngine.engineName()).thenReturn("test-engine");

        judgingService.judgeAsync("submission-1", problem, "int main(void) { return 0; }", "C");

        verify(problemService).incrementTotalAccept(10);
    }
}
