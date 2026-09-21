package com.lms.assignmentservice.service;

import com.lms.assignmentservice.entity.QuizAttempt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface QuizGradingService {

    GradingResult gradeAttempt(QuizAttempt attempt, Map<Integer, List<Integer>> overrides);

    record GradingResult(BigDecimal totalScore, boolean passed) {}
}
