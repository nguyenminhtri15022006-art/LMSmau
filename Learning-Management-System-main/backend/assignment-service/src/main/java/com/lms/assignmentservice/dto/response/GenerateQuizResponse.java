package com.lms.assignmentservice.dto.response;

import com.lms.assignmentservice.enums.QuestionType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateQuizResponse {
    int totalQuestions;
    List<QuizQuestionResponse> questions;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuizQuestionResponse {
        String question;
        QuestionType questionType;
        List<AnswerResponse> answers;
        String explanation;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerResponse {
        String content;
        Boolean correct;
    }
}
