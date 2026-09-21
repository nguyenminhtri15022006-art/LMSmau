package com.lms.assignmentservice.dto.response;

import com.lms.assignmentservice.enums.QuestionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QuestionAttemptResponse {
    Integer questionId;
    QuestionType type;
    String content;
    String imageUrl;
    short score;// số quiz đang dùng câu này
    List<AnswerForAttemptResponse> answers;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerForAttemptResponse {
        Integer answerId;
        String content;
        Short orderIndex;
    }
}
