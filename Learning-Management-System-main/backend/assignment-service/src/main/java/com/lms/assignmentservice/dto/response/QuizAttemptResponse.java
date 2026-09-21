package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 * Response khi học viên bắt đầu làm bài hoặc lấy in-progress attempt.
 * CRITICAL: Không chứa trường `isCorrect` của answers để tránh gian lận.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizAttemptResponse {
    Long attemptId;
    Integer quizId;
    String quizTitle;
    Integer duration; // Nullable: null = unlimited
    Short totalScore;

    Instant startedAt;
    Instant expiresAt;
    Instant serverTime;

    String status; // IN_PROGRESS, SUBMITTED, EXPIRED
    String showResult; // IMMEDIATELY, AFTER_SUBMIT, AFTER_DEADLINE

    // Câu hỏi + đáp án (không include `correct` field)
    List<AttemptQuestionItem> questions;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttemptQuestionItem {
        Integer questionId;
        String content;
        String explanation;
        String topic;
        String imageUrl;
        String type; // SINGLE, MULTIPLE, TRUE_FALSE
        Short score;

        // Danh sách đáp án: KHÔNG có field `correct`
        List<AttemptAnswerItem> answers;
        // Những đáp án đã chọn trước đó (nếu lấy lại in-progress attempt)
        List<Integer> selectedAnswerIds;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttemptAnswerItem {
        Integer answerId;
        String content;
        Short orderIndex;
        // KHÔNG có field `correct` ở đây!
    }
}


