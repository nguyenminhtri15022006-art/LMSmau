package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.InternalProblemResponse;
import com.lms.assignmentservice.dto.response.InternalSubmissionResponse;

import java.util.List;
import java.util.Map;

public interface InternalAssignmentService {
    InternalProblemResponse getProblem(Integer problemId);
    InternalSubmissionResponse getLatestSubmission(Integer problemId, String userId);
    long getExerciseCount(String courseId);
    Map<String, Long> getExerciseCountBatch(List<String> courseIds);
}