package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response khi học viên xem kết quả sau khi nộp bài.
 * Trả về: điểm số, đáp án học viên chọn vs đáp án đúng, lời giải thích.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizResultResponse {
    Long attemptId;
    Integer quizId;
    String quizTitle;

    Byte attemptNumber; // Lần thứ mấy (1, 2, 3...)

    BigDecimal score; // Điểm đạt được
    Short totalScore; // Tổng điểm
    Boolean passed; // Có vượt qua pass_score không

    Instant startedAt;
    Instant submittedAt;
    Integer timeSpentSeconds;
    String showResult; // IMMEDIATELY, AFTER_SUBMIT, AFTER_DEADLINE
    Boolean resultAvailable;
    Instant resultAvailableAt;
    Integer totalQuestions;

    // Chi tiết từng câu: điểm, đáp án chọn vs đúng, giải thích
    List<ResultQuestionItem> questions;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResultQuestionItem {
        Integer questionId;
        String content;
        String explanation;
        String type; // SINGLE, MULTIPLE, TRUE_FALSE
        Short score;
        BigDecimal earnedScore;

        // Danh sách đáp án: **LẦN NÀY CÓ** field `correct`
        List<ResultAnswerItem> answers;
        // Những đáp án học viên đã chọn
        List<Integer> selectedAnswerIds;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResultAnswerItem {
        Integer answerId;
        String content;
        Boolean correct;
        Short orderIndex;
    }
}


