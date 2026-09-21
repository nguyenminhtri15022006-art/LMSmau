package com.lms.assignmentservice.service;

import com.lms.assignmentservice.entity.Problem;

public interface SubmissionJudgingService {
    void judgeAsync(String submissionId, Problem problem, String sourceCode, String language);
}
