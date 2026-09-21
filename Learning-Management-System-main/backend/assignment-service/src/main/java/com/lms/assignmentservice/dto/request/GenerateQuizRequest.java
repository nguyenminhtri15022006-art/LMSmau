package com.lms.assignmentservice.dto.request;

import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateQuizRequest {

    @NotBlank(message = "Nội dung lý thuyết không được để trống")
    @Size(max = 10000, message = "Nội dung lý thuyết tối đa 10000 ký tự")
    String content;

    @NotNull(message = "Số lượng câu hỏi không được để trống")
    @Min(value = 1, message = "Số lượng câu hỏi tối thiểu là 1")
    @Max(value = 30, message = "Số lượng câu hỏi tối đa là 30")
    Integer questionCount;

    @NotNull(message = "Độ khó không được để trống")
    DifficultyType difficulty;

    @NotNull(message = "Loại câu hỏi không được để trống")
    QuestionType questionType;
}
