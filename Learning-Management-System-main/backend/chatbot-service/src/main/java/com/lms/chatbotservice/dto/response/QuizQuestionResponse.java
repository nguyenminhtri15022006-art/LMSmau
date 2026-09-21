package com.lms.chatbotservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.chatbotservice.enums.QuestionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionResponse {
    String question;
    QuestionType questionType;      // SINGLE, MULTIPLE, TRUE_FALSE
    List<AnswerResponse> answers;       // "A", "B", "C" hoặc "D"
    String explanation;         // Giải thích đáp án
}