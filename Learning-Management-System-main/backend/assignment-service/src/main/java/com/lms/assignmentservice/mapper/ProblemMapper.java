package com.lms.assignmentservice.mapper;

import com.lms.assignmentservice.dto.request.CreateProblemRequest;
import com.lms.assignmentservice.dto.request.UpdateProblemRequest;
import com.lms.assignmentservice.dto.response.InstructorProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemExampleResponse;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.dto.response.StudentProblemDetailResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import org.springframework.stereotype.Component;

@Component
public class ProblemMapper {
    public Problem toProblem(CreateProblemRequest request) {
        if (request == null) {
            return null;
        }

        return Problem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .timeLimitMs(request.getTimeLimitMs())
                .memoryLimitMb(request.getMemoryLimitMb())
                .allowedLangs(request.getAllowedLangs())
                .score(request.getScore())
                .isPublic(request.getIsPublic())
                .build();
    }

    public ProblemListResponse toProblemListResponse(Problem problem) {
        if (problem == null) {
            return null;
        }

        return ProblemListResponse.builder()
                .problemId(problem.getProblemId())
                .courseId(problem.getCourseId())
                .lessonId(problem.getLessonId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .allowedLangs(problem.getAllowedLangs())
                .score(problem.getScore())
                .isPublic(problem.getIsPublic())
                .deleted(problem.getDeleted())
                .deletedAt(problem.getDeletedAt())
                .acceptanceRate(calculateAcceptanceRate(problem))
                .totalSubmit(problem.getTotalSubmit())
                .createdAt(problem.getCreatedAt())
                .build();
    }

    public StudentProblemDetailResponse toStudentProblemDetailResponse(Problem problem) {
        if (problem == null) {
            return null;
        }

        return StudentProblemDetailResponse.builder()
                .problemId(problem.getProblemId())
                .courseId(problem.getCourseId())
                .lessonId(problem.getLessonId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .allowedLangs(problem.getAllowedLangs())
                .score(problem.getScore())
                .isPublic(problem.getIsPublic())
                .createdAt(problem.getCreatedAt())
                .build();
    }

    public InstructorProblemDetailResponse toInstructorProblemDetailResponse(Problem problem) {
        if (problem == null) {
            return null;
        }

        return InstructorProblemDetailResponse.builder()
                .problemId(problem.getProblemId())
                .courseId(problem.getCourseId())
                .lessonId(problem.getLessonId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .allowedLangs(problem.getAllowedLangs())
                .score(problem.getScore())
                .isPublic(problem.getIsPublic())
                .totalSubmit(problem.getTotalSubmit())
                .totalAccepted(problem.getTotalAccepted())
                .deleted(Boolean.TRUE.equals(problem.getDeleted()))
                .deletedAt(problem.getDeletedAt())
                .createdBy(problem.getCreatedBy())
                .createdAt(problem.getCreatedAt())
                .updatedAt(problem.getUpdatedAt())
                .build();
    }

    public ProblemExampleResponse toProblemExampleResponse(TestCase testCase) {
        if (testCase == null) {
            return null;
        }

        return ProblemExampleResponse.builder()
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .orderIndex(testCase.getOrderIndex())
                .build();
    }

    public void updateProblem(Problem problem, UpdateProblemRequest request) {
        if (problem == null || request == null) {
            return;
        }

        if (request.getTitle() != null) {
            problem.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            problem.setDescription(request.getDescription());
        }
        if (request.getDifficulty() != null) {
            problem.setDifficulty(request.getDifficulty());
        }
        if (request.getLessonId() != null) {
            problem.setLessonId(request.getLessonId());
        }
        if (request.getTimeLimitMs() != null) {
            problem.setTimeLimitMs(request.getTimeLimitMs());
        }
        if (request.getMemoryLimitMb() != null) {
            problem.setMemoryLimitMb(request.getMemoryLimitMb());
        }
        if (request.getAllowedLangs() != null) {
            problem.setAllowedLangs(request.getAllowedLangs());
        }
        if (request.getScore() != null) {
            problem.setScore(request.getScore());
        }
        if (request.getIsPublic() != null) {
            problem.setIsPublic(request.getIsPublic());
        }
    }

    public Integer calculateAcceptanceRate(Problem problem) {
        if (problem == null || problem.getTotalSubmit() == null || problem.getTotalSubmit() <= 0) {
            return 0;
        }
        int accepted = problem.getTotalAccepted() != null ? problem.getTotalAccepted() : 0;
        return Math.round((accepted * 100.0f) / problem.getTotalSubmit());
    }
}
