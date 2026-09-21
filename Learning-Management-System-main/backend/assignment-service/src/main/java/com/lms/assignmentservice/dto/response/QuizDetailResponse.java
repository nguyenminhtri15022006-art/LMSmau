package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizDetailResponse {
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
    String showResult;
    Instant startTime;
    Instant endTime;
    boolean published;
    int questionCount;
    String createdBy;
    Instant createdAt;
}
