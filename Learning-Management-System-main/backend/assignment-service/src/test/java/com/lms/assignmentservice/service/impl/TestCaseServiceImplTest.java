package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateTestCaseRequest;
import com.lms.assignmentservice.dto.request.SyncTestCaseRequest;
import com.lms.assignmentservice.dto.response.BulkTestCaseResponse;
import com.lms.assignmentservice.dto.response.TestCaseResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.TestCaseMapper;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceImplTest {

    @Mock
    TestCaseRepository testCaseRepository;

    @Mock
    ProblemRepository problemRepository;

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    TestCaseMapper testCaseMapper;

    @Mock
    CourseResourceAuthorizationService courseResourceAuthorizationService;

    TestCaseServiceImpl testCaseService;
    Problem problem;
    CreateTestCaseRequest request;

    @BeforeEach
    void setUp() {
        testCaseService = new TestCaseServiceImpl(
                testCaseRepository,
                problemRepository,
                submissionRepository,
                testCaseMapper,
                courseResourceAuthorizationService);
        ReflectionTestUtils.setField(testCaseService, "maxImportFileSizeBytes", 10_485_760L);

        problem = Problem.builder()
                .problemId(10)
                .courseId("course-1")
                .createdBy("creator-1")
                .build();
        request = CreateTestCaseRequest.builder()
                .input("1 2")
                .expectedOutput("3")
                .scoreWeight(1.0f)
                .build();

        when(problemRepository.findById(10)).thenReturn(Optional.of(problem));
        when(submissionRepository.existsByProblemId(10)).thenReturn(true);
    }

    @Test
    void createTestCaseIsRejectedWhenProblemHasSubmissions() {
        assertMutationRejected(() -> testCaseService.createTestCase(request, 10));

        verify(testCaseRepository, never()).save(any());
    }

    @Test
    void createBulkTestCasesIsRejectedWhenProblemHasSubmissions() {
        assertMutationRejected(() -> testCaseService.createBulkTestCases(10, List.of(request)));

        verify(testCaseRepository, never()).saveAll(any());
    }

    @Test
    void updateTestCaseIsRejectedWhenProblemHasSubmissions() {
        assertMutationRejected(() -> testCaseService.updateTestCase(request, 10, 100L));

        verify(testCaseRepository, never()).findById(any());
        verify(testCaseRepository, never()).save(any());
    }

    @Test
    void deleteTestCaseIsRejectedWhenProblemHasSubmissions() {
        assertMutationRejected(() -> testCaseService.deleteTestCase(10, List.of(100L)));

        verify(testCaseRepository, never()).deleteByProblemIdAndTestCaseIds(any(), any());
    }

    @Test
    void syncTestCasesReconcilesInPlaceWhenProblemHasNoSubmissions() {
        TestCase kept = TestCase.builder()
                .testId(100L)
                .problem(problem)
                .input("old")
                .expectedOutput("old")
                .hidden(false)
                .orderIndex((short) 0)
                .scoreWeight(1.0f)
                .build();
        TestCase removed = TestCase.builder()
                .testId(101L)
                .problem(problem)
                .input("remove")
                .expectedOutput("remove")
                .hidden(true)
                .orderIndex((short) 1)
                .scoreWeight(1.0f)
                .build();
        List<SyncTestCaseRequest> syncRequest = List.of(
                SyncTestCaseRequest.builder()
                        .testId(100L)
                        .input("updated")
                        .expectedOutput("updated")
                        .hidden(true)
                        .orderIndex((short) 0)
                        .scoreWeight(2.0f)
                        .build(),
                SyncTestCaseRequest.builder()
                        .input("new")
                        .expectedOutput("new")
                        .hidden(false)
                        .orderIndex((short) 2)
                        .scoreWeight(1.0f)
                        .build()
        );
        List<TestCaseResponse> response = List.of(TestCaseResponse.builder().testId(100L).build());

        when(submissionRepository.existsByProblemId(10)).thenReturn(false);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(kept, removed))
                .thenReturn(List.of(kept));
        when(testCaseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(testCaseMapper.toListTestCaseResponse(List.of(kept))).thenReturn(response);

        assertThat(testCaseService.syncTestCases(10, syncRequest)).isSameAs(response);
        assertThat(kept.getInput()).isEqualTo("updated");
        assertThat(kept.getExpectedOutput()).isEqualTo("updated");
        assertThat(kept.getHidden()).isTrue();
        assertThat(kept.getScoreWeight()).isEqualTo(2.0f);
        verify(testCaseRepository).deleteAll(List.of(removed));
        verify(testCaseRepository).saveAll(anyList());
    }

    @Test
    void syncTestCasesIsRejectedWhenProblemHasSubmissions() {
        assertMutationRejected(() -> testCaseService.syncTestCases(10, List.of(
                SyncTestCaseRequest.builder()
                        .input("1 2")
                        .expectedOutput("3")
                        .build())));

        verify(testCaseRepository, never()).findByProblem_ProblemIdOrderByOrderIndexAsc(any());
        verify(testCaseRepository, never()).saveAll(any());
    }

    @Test
    void syncTestCasesRejectsUnknownTestId() {
        when(submissionRepository.existsByProblemId(10)).thenReturn(false);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of());

        assertThatThrownBy(() -> testCaseService.syncTestCases(10, List.of(
                SyncTestCaseRequest.builder()
                        .testId(999L)
                        .input("1")
                        .expectedOutput("1")
                        .build())))
                .isInstanceOf(AssignmentException.class)
                .extracting(ex -> ((AssignmentException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TEST_CASE_NOT_FOUND);

        verify(testCaseRepository, never()).saveAll(any());
        verify(testCaseRepository, never()).deleteAll(any());
    }

    @Test
    void importCsvRejectsInvalidRowsWithoutSavingAnything() {
        when(submissionRepository.existsByProblemId(10)).thenReturn(false);
        MockMultipartFile file = csvFile("""
                input,expectedOutput,isHidden,scoreWeight,orderIndex
                , ,true,1,0
                value,ok,true,0,1
                """);

        BulkTestCaseResponse response = testCaseService.importTestCases(10, file);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getSuccessCount()).isZero();
        assertThat(response.getFailedCount()).isEqualTo(2);
        assertThat(response.getErrors())
                .extracting(BulkTestCaseResponse.TestCaseErrorInfo::getRowNumber)
                .containsExactly(2, 3);
        verify(testCaseRepository, never()).findByProblem_ProblemIdOrderByOrderIndexAsc(any());
        verify(testCaseRepository, never()).saveAll(any());
        verify(testCaseRepository, never()).deleteAll(any());
    }

    @Test
    void importCsvReconcilesByOrderIndexAndAllowsEmptyInput() {
        TestCase kept = TestCase.builder()
                .testId(100L)
                .problem(problem)
                .input("old")
                .expectedOutput("old")
                .hidden(false)
                .orderIndex((short) 0)
                .scoreWeight(1.0f)
                .build();
        TestCase removed = TestCase.builder()
                .testId(101L)
                .problem(problem)
                .input("remove")
                .expectedOutput("remove")
                .hidden(true)
                .orderIndex((short) 5)
                .scoreWeight(1.0f)
                .build();
        MockMultipartFile file = csvFile("""
                input,expectedOutput,isHidden,scoreWeight,orderIndex
                ,empty input is valid,true,2,0
                value,ok,false,1,1
                """);
        when(submissionRepository.existsByProblemId(10)).thenReturn(false);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of(kept, removed));
        when(testCaseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        BulkTestCaseResponse response = testCaseService.importTestCases(10, file);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(kept.getInput()).isEmpty();
        assertThat(kept.getExpectedOutput()).isEqualTo("empty input is valid");
        assertThat(kept.getHidden()).isTrue();
        assertThat(kept.getScoreWeight()).isEqualTo(2.0f);
        verify(testCaseRepository).deleteAll(List.of(removed));

        ArgumentCaptor<List<TestCase>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(testCaseRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(2);
        assertThat(savedCaptor.getValue())
                .extracting(TestCase::getOrderIndex)
                .containsExactly((short) 0, (short) 1);
    }

    private void assertMutationRejected(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AssignmentException.class)
                .extracting(ex -> ((AssignmentException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_MUTATE_TEST_CASES);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "testcases.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
