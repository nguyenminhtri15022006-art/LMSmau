package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.QuestionType;
import org.springframework.web.multipart.MultipartFile;

public interface QuizAiPreviewService {
    QuestionImportResultResponse preview(
            String courseId,
            MultipartFile file,
            String content,
            Integer questionCount,
            DifficultyType difficulty,
            QuestionType questionType,
            String topic);
}
