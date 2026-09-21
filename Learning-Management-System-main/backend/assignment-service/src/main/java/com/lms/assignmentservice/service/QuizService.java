package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.AddBulkQuestionQuizRequest;
import com.lms.assignmentservice.dto.request.CreateImportedQuizRequest;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.request.SaveQuizDraftRequest;
import com.lms.assignmentservice.dto.response.CachedPage;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.dto.response.QuizDetailResponse;
import com.lms.assignmentservice.dto.response.QuizInstructorDetailResponse;
import com.lms.assignmentservice.dto.response.QuizStudentDetailResponse;
import com.lms.assignmentservice.entity.Quiz;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuizService {
    QuizDetailResponse createQuiz(String courseId, CreateQuizRequest request);

    QuizDetailResponse createQuizFromImportedQuestions(String courseId, CreateImportedQuizRequest request);

    CachedPage<QuizDetailResponse> getQuizzesByCourse(String courseId, String lessonId, Pageable pageable);

    CachedPage<QuizInstructorDetailResponse> getQuizzesForInstructor(String courseId, String lessonId, Pageable pageable);

    CachedPage<QuizStudentDetailResponse> getQuizzesForStudent(String courseId, String lessonId, Pageable pageable);

    QuizDetailResponse getQuiz(Integer quizId);

    List<QuestionDetailResponse> getQuizQuestions(Integer quizId);

    QuizDetailResponse updateQuiz(Integer quizId, CreateQuizRequest request);

    QuizDetailResponse syncQuiz(Integer quizId, SaveQuizDraftRequest request);

    void deleteQuiz(Integer quizId);

    void restoreQuiz(Integer quizId);

    void publishQuiz(Integer quizId);

    void unpublishQuiz(Integer quizId);

    void bulkAddQuestionToQuiz(Integer quizId, AddBulkQuestionQuizRequest request);

    Integer cloneQuiz(Integer quizId);

    void removeQuestionInQuiz(Integer quizId, Integer questionId);

    Quiz findQuizById(Integer quizId);
}
