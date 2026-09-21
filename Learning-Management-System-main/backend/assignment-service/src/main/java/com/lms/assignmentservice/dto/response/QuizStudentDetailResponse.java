package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response cho Students khi xem danh sách quiz hoặc chi tiết quiz.
 * Chứa chỉ những thông tin cần thiết cho học viên (không có metadata, status publish, etc).
 *
 * Fields:
 * - quizId, courseId, lessonId: Định danh
 * - title, description: Thông tin cơ bản
 * - totalScore, passScore, maxAttempts: Điểm và quy tắc tham gia
 * - duration, startTime, endTime: Thời gian làm bài
 * - questionCount: Số lượng câu hỏi
 *
 * NOT included:
 * - createdBy, createdAt, updatedAt (metadata)
 * - published (Students chỉ thấy published quizzes)
 * - shuffleQuestions, shuffleAnswers (Implementation detail)
 * - showResult (Client không cần biết, server sẽ enforce khi gọi result API)
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizStudentDetailResponse {
    Integer quizId;
    String courseId;
    String lessonId;
    String title;
    String description;
    Short totalScore;
    Byte passScore;
    Byte maxAttempts;
    Integer duration; // Nullable: null = unlimited
    Instant startTime; // Nullable: null = unlimited
    Instant endTime; // Nullable: null = unlimited
    Boolean completed;
    int questionCount;
}
