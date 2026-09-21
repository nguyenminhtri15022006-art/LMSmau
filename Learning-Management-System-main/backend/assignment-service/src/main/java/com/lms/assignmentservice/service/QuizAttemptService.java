package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.AutoSaveAttemptRequest;
import com.lms.assignmentservice.dto.request.PracticeAnswerRequest;
import com.lms.assignmentservice.dto.request.SubmitAttemptRequest;
import com.lms.assignmentservice.dto.response.AttemptHistoryResponse;
import com.lms.assignmentservice.dto.response.PracticeAnswerResponse;
import com.lms.assignmentservice.dto.response.QuizAttemptResponse;
import com.lms.assignmentservice.dto.response.QuizResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface QuizAttemptService {
    QuizAttemptResponse startOrResumeAttempt(Integer quizId);

    QuizAttemptResponse getInProgressAttempt(Integer quizId, Long attemptId);

    void autoSaveAttempt(Integer quizId, Long attemptId, AutoSaveAttemptRequest request);

    PracticeAnswerResponse checkPracticeAnswer(Integer quizId, Long attemptId, PracticeAnswerRequest request);

    QuizResultResponse submitAttempt(Integer quizId, Long attemptId, SubmitAttemptRequest request);

    QuizResultResponse getQuizResult(Integer quizId, Long attemptId);

    Page<AttemptHistoryResponse> getAttemptHistory(Integer quizId, String userId, Pageable pageable);
}
