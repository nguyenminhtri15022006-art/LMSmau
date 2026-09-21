package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.request.CreateProblemRequest;
import com.lms.assignmentservice.dto.request.UpdateProblemRequest;
import com.lms.assignmentservice.dto.response.CachedPage;
import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.dto.response.InstructorProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.dto.response.StudentProblemDetailResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.SubmissionStatus;
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
import com.lms.assignmentservice.service.ProblemService;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProblemServiceImpl implements ProblemService {

    private static final String UNCATEGORIZED_LESSON_ID = "other";

    ProblemRepository problemRepository;
    SubmissionRepository submissionRepository;
    ProblemMapper problemMapper;
    TestCaseRepository testCaseRepository;
    TestCaseMapper testCaseMapper;
    CourseClient courseClient;
    AssignmentAuthorizationService authorizationService;
    CourseResourceAuthorizationService courseResourceAuthorizationService;

    // ==================== Write Operations ====================

    @Transactional
    @Override
    public InstructorProblemDetailResponse createProblem(CreateProblemRequest request) {
        validateCourseExists(request.getCourseId());

        String userId = authorizationService.currentUserId();

        Problem problem = problemMapper.toProblem(request);
        problem.setLessonId(normalizeLessonId(problem.getLessonId()));
        problem.setCreatedBy(userId);
        problem.setSlug(generateUniqueSlug(request.getTitle()));

        problem = problemRepository.save(problem);
        log.info("Created problem [{}] '{}' by instructor [{}]",
                problem.getProblemId(), problem.getTitle(), userId);

        return toInstructorDetail(problem);
    }

    @Transactional
    @Override
    public InstructorProblemDetailResponse updateProblem(UpdateProblemRequest request, Integer problemId) {
        Problem problem = findById(problemId);
        String userId = authorizationService.currentUserId();

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

        if (problem.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_IS_DELETED);
        }

        if (submissionRepository.existsByProblemId(problemId) && hasGradingChange(problem, request)) {
            throw new AssignmentException(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_EDIT_GRADING);
        }

        problemMapper.updateProblem(problem, request);
        problem.setLessonId(normalizeLessonId(problem.getLessonId()));

        problem = problemRepository.save(problem);
        log.info("Updated problem [{}] by user [{}]", problemId, userId);

        return toInstructorDetail(problem);
    }

    @Transactional
    @Override
    public void unpublishProblem(Integer problemId) {
        Problem problem = findById(problemId);
        String userId = authorizationService.currentUserId();

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

        if (problem.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_IS_DELETED);
        }

        if (!Boolean.TRUE.equals(problem.getIsPublic())) {
            return;
        }

        if (submissionRepository.existsByProblemId(problemId)) {
            throw new AssignmentException(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_UNPUBLISH);
        }

        problem.setIsPublic(false);
        problemRepository.save(problem);
        log.info("Unpublished problem [{}] by user [{}]", problemId, userId);
    }

    @Transactional
    @Override
    public Integer cloneProblem(Integer problemId) {
        Problem source = findById(problemId);
        String userId = authorizationService.currentUserId();

        courseResourceAuthorizationService.checkManager(source.getCourseId(), source.getCreatedBy());

        if (source.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_IS_DELETED);
        }

        String cloneTitle = source.getTitle() + " (Copy)";
        Problem clone = Problem.builder()
                .courseId(source.getCourseId())
                .lessonId(source.getLessonId())
                .title(cloneTitle)
                .slug(generateUniqueSlug(cloneTitle))
                .description(source.getDescription())
                .difficulty(source.getDifficulty())
                .timeLimitMs(source.getTimeLimitMs())
                .memoryLimitMb(source.getMemoryLimitMb())
                .allowedLangs(source.getAllowedLangs())
                .score(source.getScore())
                .isPublic(false)
                .deleted(false)
                .totalSubmit(0)
                .totalAccepted(0)
                .createdBy(userId)
                .build();

        clone = problemRepository.save(clone);
        Problem target = clone;

        List<TestCase> clonedTestCases = testCaseRepository
                .findByProblem_ProblemIdOrderByOrderIndexAsc(problemId)
                .stream()
                .map(testCase -> cloneTestCase(testCase, target))
                .toList();
        testCaseRepository.saveAll(clonedTestCases);

        log.info("Cloned problem [{}] to new problem [{}] by user [{}]",
                problemId, clone.getProblemId(), userId);
        return clone.getProblemId();
    }

    @Transactional
    @Override
    public void deleteProblem(Integer problemId) {
        Problem problem = findById(problemId);
        String userId = authorizationService.currentUserId();

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

        boolean hasSubmissions = submissionRepository.existsByProblemId(problemId);

        if (hasSubmissions) {
            // Soft-delete: problem đã có submission, giữ nguyên testcases + submissions
            problem.setDeleted(true);
            problem.setDeletedAt(java.time.Instant.now());
            problem.setDeletedBy(userId);
            problemRepository.save(problem);
            log.info("Soft-delete problem [{}] by user [{}]", problemId, userId);
            return;
        }

        // Hard-delete: chưa có submission
        testCaseRepository.deleteByProblem_ProblemId(problemId);
        problemRepository.delete(problem);
        log.info("Hard-delete problem [{}] by user [{}]", problemId, userId);
    }

    @Transactional
    @Override
    public void restoreProblem(Integer problemId) {
        Problem problem = findById(problemId);
        String userId = authorizationService.currentUserId();

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

        if (!problem.getDeleted()) {
            throw new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        problem.setDeleted(false);
        problem.setDeletedAt(null);
        problem.setDeletedBy(null);
        problemRepository.save(problem);
        log.info("Restore problem [{}] by user [{}]", problemId, userId);
    }

    // ==================== Read Operations ====================

    @Override
    public CachedPage<ProblemListResponse> getProblemList(String courseId, String lessonId, DifficultyType difficulty, Pageable pageable) {
        boolean studentView = !authorizationService.isAdminOrInstructor();
        if (studentView) {
            checkStudentEnrollment(courseId);
        }

        Page<Problem> page;
        if (studentView) {
            if (hasText(lessonId)) {
                page = difficulty != null
                        ? problemRepository.findByCourseIdAndLessonIdAndDifficultyAndIsPublicTrueAndDeletedFalse(courseId, lessonId, difficulty, pageable)
                        : problemRepository.findByCourseIdAndLessonIdAndIsPublicTrueAndDeletedFalse(courseId, lessonId, pageable);
            } else {
                page = difficulty != null
                        ? problemRepository.findByCourseIdAndDifficultyAndIsPublicTrueAndDeletedFalse(courseId, difficulty, pageable)
                        : problemRepository.findByCourseIdAndIsPublicTrueAndDeletedFalse(courseId, pageable);
            }
        } else {
            if (hasText(lessonId)) {
                page = difficulty != null
                        ? problemRepository.findByCourseIdAndLessonIdAndDifficulty(courseId, lessonId, difficulty, pageable)
                        : problemRepository.findByCourseIdAndLessonId(courseId, lessonId, pageable);
            } else {
                page = difficulty != null
                        ? problemRepository.findByCourseIdAndDifficulty(courseId, difficulty, pageable)
                        : problemRepository.findByCourseId(courseId, pageable);
            }
        }

        var responsePage = page.map(problemMapper::toProblemListResponse);
        List<ProblemListResponse> content = responsePage.getContent();
        if (studentView) {
            markCompletedProblems(courseId, content);
        }

        return CachedPage.<ProblemListResponse>builder()
                .content(content)
                .totalElements(responsePage.getTotalElements())
                .build();
    }

    @Override
    public ProblemDetailResponse getProblemById(Integer problemId) {
        Problem problem = findById(problemId);
        return toDetailForCurrentUser(problem);
    }

    @Override
    public ProblemDetailResponse getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));
        return toDetailForCurrentUser(problem);
    }

    // ==================== Internal Helpers ====================

    @Override
    public Problem findById(Integer problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    private ProblemDetailResponse toDetailForCurrentUser(Problem problem) {
        if (authorizationService.isAdmin()) {
            return toInstructorDetail(problem);
        }

        if (authorizationService.isInstructor()) {
            courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
            return toInstructorDetail(problem);
        }

        // Student: chặn truy cập problem đã bị xóa mềm
        if (problem.getDeleted() || !problem.getIsPublic()) {
            throw new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        checkStudentEnrollment(problem.getCourseId());
        return toStudentDetail(problem);
    }

    private void checkStudentEnrollment(String courseId) {
        Boolean enrolled = courseClient.checkEnrollment(courseId, authorizationService.currentUserId());
        if (!Boolean.TRUE.equals(enrolled)) {
            throw new AssignmentException(ErrorCode.NOT_ENROLLED);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeLessonId(String lessonId) {
        return hasText(lessonId) ? lessonId.trim() : UNCATEGORIZED_LESSON_ID;
    }

    private StudentProblemDetailResponse toStudentDetail(Problem problem) {
        List<TestCase> publicExamples = testCaseRepository
                .findByProblem_ProblemIdAndHiddenFalseOrderByOrderIndexAsc(problem.getProblemId());

        StudentProblemDetailResponse response = problemMapper.toStudentProblemDetailResponse(problem);
        response.setExamples(publicExamples.stream()
                .map(problemMapper::toProblemExampleResponse)
                .toList());
        return response;
    }

    private void markCompletedProblems(String courseId, List<ProblemListResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }

        Set<Integer> completedProblemIds = submissionRepository
                .findCompletionByUserIdAndCourseIdAndStatus(
                        authorizationService.currentUserId(),
                        courseId,
                        SubmissionStatus.AC.name())
                .stream()
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        responses.forEach(response ->
                response.setCompleted(completedProblemIds.contains(response.getProblemId())));
    }

    private InstructorProblemDetailResponse toInstructorDetail(Problem problem) {
        List<TestCase> testCases = testCaseRepository
                .findByProblem_ProblemIdOrderByOrderIndexAsc(problem.getProblemId());

        InstructorProblemDetailResponse response = problemMapper.toInstructorProblemDetailResponse(problem);
        response.setTestCases(testCaseMapper.toListTestCaseResponse(testCases));
        return response;
    }

    private boolean hasGradingChange(Problem problem, UpdateProblemRequest request) {
        return request.getTimeLimitMs() != null && !Objects.equals(problem.getTimeLimitMs(), request.getTimeLimitMs())
                || request.getMemoryLimitMb() != null && !Objects.equals(problem.getMemoryLimitMb(), request.getMemoryLimitMb())
                || request.getScore() != null && !Objects.equals(problem.getScore(), request.getScore());
    }

    private TestCase cloneTestCase(TestCase source, Problem targetProblem) {
        return TestCase.builder()
                .problem(targetProblem)
                .input(source.getInput())
                .expectedOutput(source.getExpectedOutput())
                .hidden(source.getHidden())
                .orderIndex(source.getOrderIndex())
                .scoreWeight(source.getScoreWeight())
                .build();
    }

    private void validateCourseExists(String courseId) {
        try {
            InternalCourseResponse course = courseClient.getCourse(courseId);
            if (course != null && "LOCKED".equals(course.getStatus())) {
                throw new AssignmentException(ErrorCode.COURSE_LOCKED);
            }
        } catch (FeignException feignException) {
            if (feignException.status() == 404) {
                throw new AssignmentException(ErrorCode.COURSE_NOT_FOUND);
            }
            throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (AssignmentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cannot verify courseId [{}]: {}", courseId, e.getMessage());
            throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    private String generateUniqueSlug(String title) {
        String base = toSlug(title);
        String slug = base;
        int suffix = 1;
        while (problemRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String ascii = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized).replaceAll("");
        return ascii.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    @Transactional
    @Override
    public void incrementTotalSubmit(Integer problemId) {
        problemRepository.incrementTotalSubmit(problemId);
    }

    @Transactional
    @Override
    public void incrementTotalAccept(Integer problemId) {
        problemRepository.incrementTotalAccepted(problemId);
    }
}
