package com.lms.assignmentservice.dto.response;

import com.lms.assignmentservice.enums.QuestionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QuestionDetailResponse {
    Integer questionId;
    String courseId;
    String topic;
    QuestionType type;
    String content;
    String explanation;
    String imageUrl;
    short score;
    long usedInQuizCount; // số quiz đang dùng câu này
    Instant createdAt;
    String createdBy;
    List<AnswerDetailResponse> answers;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerDetailResponse {
        Integer answerId;
        String content;
        boolean correct;
        Short orderIndex;
    }
}
