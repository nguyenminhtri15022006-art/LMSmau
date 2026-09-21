package com.lms.assignmentservice.dto.request;

import com.lms.assignmentservice.enums.ShowResultType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateQuizRequest {
    @NotBlank(message = "Tiêu đề quiz không được để trống")
    @Size(max = 300, message = "Tiêu đề quiz tối đa 300 ký tự")
    String title;

    String lessonId;
    String description;

    /**
     * Thời gian làm bài (phút).
     * Nullable: null = "Không giới hạn (Vô hạn)"
     * Nếu có giá trị: phải từ 1-300 phút
     */
    @Min(value = 1, message = "Thời gian làm bài phải từ 1-300 phút")
    @Max(value = 300, message = "Thời gian làm bài phải từ 1-300 phút")
    Integer duration;

    @Min(value = 0, message = "Tổng điểm phải từ 0-1000")
    @Max(value = 1000, message = "Tổng điểm phải từ 0-1000")
    Short totalScore = 100;

    @Min(value = 0, message = "Điểm đạt phải từ 0-100")
    @Max(value = 100, message = "Điểm đạt phải từ 0-100")
    Byte passScore = 50;

    @Min(value = 0, message = "Số lần thi tối đa phải >= 0")
    Byte maxAttempts = 0;

    Boolean shuffleQuestions = true;
    Boolean shuffleAnswers = true;

    ShowResultType showResult = ShowResultType.AFTER_SUBMIT;

    /**
     * Thời gian mở bài (bắt đầu làm).
     * Nullable: null = "Không giới hạn, học viên có thể làm bất cứ lúc nào"
     */
    Instant startTime;

    /**
     * Thời gian đóng bài (kết thúc làm).
     * Nullable: null = "Không giới hạn, học viên có thể làm bất cứ lúc nào"
     */
    Instant endTime;
}
