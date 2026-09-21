package com.lms.chatbotservice.dto.request;

import com.lms.chatbotservice.enums.DifficultyType;
import com.lms.chatbotservice.enums.QuestionType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateQuizRequest {

    @NotBlank(message = "QUIZ_CONTENT_BLANK")
    @Size(max = 10000, message = "QUIZ_CONTENT_SIZE")
    String content;

    @NotNull(message = "QUIZ_COUNT_NULL")
    @Min(value = 1, message = "QUIZ_COUNT_MIN")
    @Max(value = 30, message = "QUIZ_COUNT_MAX")
    Integer questionCount;

    @NotNull(message = "QUIZ_DIFFICULTY_NULL")
    DifficultyType difficulty;

    @NotNull(message = "QUIZ_QUESTION_TYPE_NULL")
    QuestionType questionType;  // SINGLE, MULTIPLE, TRUE_FALSE
}
