package com.lms.assignmentservice.dto.request;

import com.lms.assignmentservice.enums.DifficultyType;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UpdateProblemRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 300, message = "Tiêu đề tối đa 300 ký tự")
    String title;

    @NotBlank(message = "Mô tả không được để trống")
    String description;

    @NotNull(message = "Độ khó không được để trống")
    DifficultyType difficulty;

    String lessonId;

    @Min(value = 50, message = "Giới hạn thời gian tối thiểu 50ms")
    @Max(value = 10000, message = "Giới hạn thời gian tối đa 10000ms")
    Integer timeLimitMs = 2000;

    @Min(value = 16, message = "Giới hạn bộ nhớ tối thiểu 16MB")
    @Max(value = 512, message = "Giới hạn bộ nhớ tối đa 512MB")
    Integer memoryLimitMb = 256;

    @NotEmpty(message = "Phải có ít nhất một ngôn ngữ được hỗ trợ")
    List<String> allowedLangs;

    Short score = 100;

    Boolean isPublic = false;
}
