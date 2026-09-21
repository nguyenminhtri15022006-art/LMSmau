package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.UpdateProblemRequest;
import com.lms.assignmentservice.dto.response.InstructorProblemDetailResponse;
import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.ProblemMapper;
import com.lms.assignmentservice.mapper.TestCaseMapper;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTest {

    @Mock
    ProblemRepository problemRepository;

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    ProblemMapper problemMapper;

    @Mock
    TestCaseRepository testCaseRepository;

    @Mock
    TestCaseMapper testCaseMapper;

    @Mock
    CourseClient courseClient;

    @Mock
    AssignmentAuthorizationService authorizationService;

    ProblemServiceImpl problemService;
    Problem problem;

    @BeforeEach
    void setUp() {
        CourseResourceAuthorizationService resourceAuthorizationService =
                new CourseResourceAuthorizationServiceImpl(authorizationService, courseClient);
        problemService = new ProblemServiceImpl(
                problemRepository,
                submissionRepository,
                problemMapper,
                testCaseRepository,
                testCaseMapper,
                courseClient,
                authorizationService,
                resourceAuthorizationService);

        problem = Problem.builder()
                .problemId(10)
                .courseId("course-1")
                .lessonId("lesson-1")
                .title("Two Sum")
                .createdBy("creator-1")
                .difficulty(DifficultyType.EASY)
                .build();

        when(authorizationService.isAdmin()).thenReturn(false);
        when(authorizationService.currentUserId()).thenReturn("course-instructor");

        InternalCourseResponse course = new InternalCourseResponse();
        course.setInstructorId("course-instructor");
        when(courseClient.getCourse("course-1")).thenReturn(course);
        when(problemRepository.findById(10)).thenReturn(Optional.of(problem));
    }

    @Test
    void courseInstructorCanLoadInstructorProblemDetailCreatedByAnotherUser() {
        InstructorProblemDetailResponse response = InstructorProblemDetailResponse.builder()
                .problemId(10)
                .build();
        when(authorizationService.isInstructor()).thenReturn(true);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of());
        when(testCaseMapper.toListTestCaseResponse(List.of())).thenReturn(List.of());
        when(problemMapper.toInstructorProblemDetailResponse(problem)).thenReturn(response);

        assertThat(problemService.getProblemById(10)).isSameAs(response);
    }

    @Test
    void courseInstructorCanUpdateProblemCreatedByAnotherUser() {
        UpdateProblemRequest request = UpdateProblemRequest.builder()
                .title("Updated")
                .description("Updated description")
                .difficulty(DifficultyType.MEDIUM)
                .allowedLangs(List.of("JAVA"))
                .build();
        InstructorProblemDetailResponse response = InstructorProblemDetailResponse.builder()
                .problemId(10)
                .build();

        when(problemRepository.save(problem)).thenReturn(problem);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of());
        when(testCaseMapper.toListTestCaseResponse(List.of())).thenReturn(List.of());
        when(problemMapper.toInstructorProblemDetailResponse(problem)).thenReturn(response);

        assertThat(problemService.updateProblem(request, 10)).isSameAs(response);
        verify(problemMapper).updateProblem(problem, request);
    }

    @Test
    void submittedProblemRejectsGradingFieldChanges() {
        UpdateProblemRequest request = UpdateProblemRequest.builder()
                .timeLimitMs(3000)
                .build();

        when(submissionRepository.existsByProblemId(10)).thenReturn(true);

        assertThatThrownBy(() -> problemService.updateProblem(request, 10))
                .isInstanceOf(AssignmentException.class)
                .extracting(ex -> ((AssignmentException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_EDIT_GRADING);

        verify(problemMapper, never()).updateProblem(any(), any());
        verify(problemRepository, never()).save(any());
    }

    @Test
    void submittedProblemAllowsSafeMetadataUpdates() {
        UpdateProblemRequest request = UpdateProblemRequest.builder()
                .title("Updated")
                .description("Updated description")
                .difficulty(DifficultyType.MEDIUM)
                .lessonId("lesson-2")
                .allowedLangs(List.of("JAVA", "PYTHON"))
                .build();
        InstructorProblemDetailResponse response = InstructorProblemDetailResponse.builder()
                .problemId(10)
                .build();

        when(submissionRepository.existsByProblemId(10)).thenReturn(true);
        when(problemRepository.save(problem)).thenReturn(problem);
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of());
        when(testCaseMapper.toListTestCaseResponse(List.of())).thenReturn(List.of());
        when(problemMapper.toInstructorProblemDetailResponse(problem)).thenReturn(response);

        assertThat(problemService.updateProblem(request, 10)).isSameAs(response);
        verify(problemMapper).updateProblem(problem, request);
    }

    @Test
    void unpublishProblemWithoutSubmissionsMakesProblemDraft() {
        problem.setIsPublic(true);

        problemService.unpublishProblem(10);

        assertThat(problem.getIsPublic()).isFalse();
        verify(problemRepository).save(problem);
    }

    @Test
    void unpublishProblemWithSubmissionsIsRejected() {
        problem.setIsPublic(true);
        when(submissionRepository.existsByProblemId(10)).thenReturn(true);

        assertThatThrownBy(() -> problemService.unpublishProblem(10))
                .isInstanceOf(AssignmentException.class)
                .extracting(ex -> ((AssignmentException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_UNPUBLISH);

        verify(problemRepository, never()).save(any());
    }

    @Test
    void cloneProblemCopiesProblemAndTestCasesAsDraft() {
        problem.setIsPublic(true);
        problem.setDeleted(false);
        problem.setTotalSubmit(7);
        problem.setTotalAccepted(3);
        problem.setAllowedLangs(List.of("JAVA"));
        TestCase testCase = TestCase.builder()
                .testId(100L)
                .input("1 2")
                .expectedOutput("3")
                .hidden(false)
                .orderIndex((short) 1)
                .scoreWeight(1.5f)
                .build();

        when(problemRepository.existsBySlug(anyString())).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem saved = invocation.getArgument(0);
            saved.setProblemId(99);
            return saved;
        });
        when(testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(10)).thenReturn(List.of(testCase));
        when(testCaseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(problemService.cloneProblem(10)).isEqualTo(99);

        verify(problemRepository).save(argThat(clone ->
                clone.getProblemId().equals(99)
                        && clone.getTitle().equals("Two Sum (Copy)")
                        && !clone.getIsPublic()
                        && !clone.getDeleted()
                        && clone.getTotalSubmit() == 0
                        && clone.getTotalAccepted() == 0));
        verify(testCaseRepository).saveAll(argThat(clones -> {
            List<TestCase> cloneList = (List<TestCase>) clones;
            return cloneList.size() == 1
                    && cloneList.get(0).getProblem().getProblemId().equals(99)
                    && cloneList.get(0).getInput().equals("1 2")
                    && cloneList.get(0).getExpectedOutput().equals("3")
                    && !cloneList.get(0).getHidden()
                    && cloneList.get(0).getOrderIndex().equals((short) 1)
                    && cloneList.get(0).getScoreWeight().equals(1.5f);
        }));
    }

    @Test
    void courseInstructorCanDeleteProblemCreatedByAnotherUser() {
        problemService.deleteProblem(10);

        verify(testCaseRepository).deleteByProblem_ProblemId(10);
        verify(problemRepository).delete(problem);
    }

    @Test
    void deleteProblemSoftDeletesWhenSubmissionsExist() {
        when(submissionRepository.existsByProblemId(10)).thenReturn(true);

        problemService.deleteProblem(10);

        verify(problemRepository, never()).delete(any());
        verify(problemRepository).save(problem);
        assertThat(problem.getDeleted()).isTrue();
        assertThat(problem.getDeletedAt()).isNotNull();
        assertThat(problem.getDeletedBy()).isEqualTo("course-instructor");
    }
}
