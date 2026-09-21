package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response cho Instructors khi xem danh sách quiz hoặc chi tiết quiz.
 * Chứa tất cả các thông tin quản lý: metadata, thông tin tạo, trạng thái publish, etc.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizInstructorDetailResponse {
    Integer quizId;
    String courseId;
    String lessonId;
    String title;
    String description;
    Integer duration; // Nullable: null = unlimited
    Short totalScore;
    Byte passScore;
    Byte maxAttempts;
    boolean shuffleQuestions;
    boolean shuffleAnswers;
    String showResult; // IMMEDIATELY, AFTER_SUBMIT, AFTER_DEADLINE
    Instant startTime; // Nullable: null = unlimited
    Instant endTime; // Nullable: null = unlimited
    boolean published;
    int questionCount;
    long attemptCount;
    boolean deleted;
    Instant deletedAt;
    String createdBy;
    Instant createdAt;
    Instant updatedAt;
}

