package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.ImportBulkQuestionRequest;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface QuestionService {
    QuestionDetailResponse createQuestion(String courseId, CreateQuestionRequest request);

    Page<QuestionDetailResponse> importBatchQuestion(String courseId, ImportBulkQuestionRequest request, Pageable pageable);

    Page<QuestionDetailResponse> getQuestionsByCourse(String courseId, String topic, QuestionType type, Pageable pageable);

    List<String> getTopics(String courseId);

    QuestionDetailResponse getQuestion(Integer questionId);

    QuestionDetailResponse updateQuestion(Integer questionId, CreateQuestionRequest request);

    void deleteQuestion(Integer questionId);
}
