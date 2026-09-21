package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.InternalProblemResponse;
import com.lms.assignmentservice.dto.response.InternalSubmissionResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.service.InternalAssignmentService;
import com.lms.assignmentservice.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternalAssignmentServiceImpl implements InternalAssignmentService {

    private final ProblemService problemService;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final ProblemRepository problemRepository;

    @Override
    public InternalProblemResponse getProblem(Integer problemId) {
        Problem p = problemService.findById(problemId);

        return InternalProblemResponse.builder()
                .problemId(p.getProblemId())
                .courseId(p.getCourseId())
                .lessonId(p.getLessonId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .description(p.getDescription())
                .difficulty(p.getDifficulty() != null ? p.getDifficulty().name() : null)
                .timeLimitMs(p.getTimeLimitMs())
                .memoryLimitMb(p.getMemoryLimitMb())
                .allowedLangs(p.getAllowedLangs())
                .score(p.getScore())
                .isPublic(p.getIsPublic())
                .totalSubmit(p.getTotalSubmit())
                .totalAccepted(p.getTotalAccepted())
                .build();
    }

    @Override
    public long getExerciseCount(String courseId) {
        return quizRepository.countByCourseIdAndPublishedTrueAndDeletedFalse(courseId)
                + problemRepository.countByCourseId(courseId);
    }

    @Override
    public Map<String, Long> getExerciseCountBatch(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return Map.of();

        Map<String, Long> result = new HashMap<>();
        courseIds.forEach(id -> result.put(id, 0L));

        quizRepository.countPublishedGroupByCourseIds(courseIds)
                .forEach(row -> result.merge((String) row[0], (Long) row[1], Long::sum));

        problemRepository.countGroupByCourseIds(courseIds)
                .forEach(row -> result.merge((String) row[0], (Long) row[1], Long::sum));

        return result;
    }

    @Override
    public InternalSubmissionResponse getLatestSubmission(Integer problemId, String userId) {
        return submissionRepository
                .findTopByUserIdAndProblemIdOrderBySubmittedAtDesc(userId, problemId)
                .map(s -> InternalSubmissionResponse.builder()
                        .submissionId(s.getId())
                        .problemId(s.getProblemId())
                        .userId(s.getUserId())
                        .language(s.getLanguage())
                        .status(s.getStatus())
                        .score(s.getScore())
                        .execTimeMs(s.getExecTimeMs())
                        .memoryUsedKb(s.getMemoryUsedKb())
                        .submittedAt(s.getSubmittedAt())
                        .judgedAt(s.getJudgeAt())
                        .sourceCode(s.getSourceCode())
                        .compileError(s.getCompileError())
                        .build())
                .orElse(null);
    }
}