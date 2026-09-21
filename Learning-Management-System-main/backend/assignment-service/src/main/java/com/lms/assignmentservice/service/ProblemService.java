package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.CreateProblemRequest;
import com.lms.assignmentservice.dto.request.UpdateProblemRequest;
import com.lms.assignmentservice.dto.response.CachedPage;
import com.lms.assignmentservice.dto.response.InstructorProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemDetailResponse;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.enums.DifficultyType;
import org.springframework.data.domain.Pageable;
public interface ProblemService {
    InstructorProblemDetailResponse createProblem(CreateProblemRequest request);

    InstructorProblemDetailResponse updateProblem(UpdateProblemRequest request, Integer problemId);

    void unpublishProblem(Integer problemId);

    Integer cloneProblem(Integer problemId);

    void deleteProblem(Integer problemId);

    void restoreProblem(Integer problemId);

    CachedPage<ProblemListResponse> getProblemList(String courseId, String lessonId, DifficultyType difficulty, Pageable pageable);

    ProblemDetailResponse getProblemById(Integer problemId);

    ProblemDetailResponse getProblemBySlug(String slug);

    Problem findById(Integer problemId);

    void incrementTotalSubmit(Integer problemId);

    void incrementTotalAccept(Integer problemId);
}
