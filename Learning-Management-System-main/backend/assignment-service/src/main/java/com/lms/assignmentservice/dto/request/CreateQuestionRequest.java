package com.lms.assignmentservice.dto.request;

import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.validator.ValidAnswerCount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidAnswerCount
public class CreateQuestionRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String content;

    @NotNull(message = "Loại câu hỏi không được để trống")
    QuestionType type;

    String topic;
    String explanation;
    String imageUrl;

    @Min(value = 1, message = "Điểm câu hỏi phải từ 1 trở lên")
    Byte score = 10;

    @NotEmpty(message = "Phải có ít nhất 2 đáp án")
    @Size(min = 2, message = "Phải có ít nhất 2 đáp án")
    @Valid
    List<AnswerRequest> answers;

    @Data
    public static class AnswerRequest {
        Integer answerId;

        @NotBlank(message = "Nội dung đáp án không được để trống")
        String content;

        boolean correct = false;

        Short orderIndex = 0;
    }
}
