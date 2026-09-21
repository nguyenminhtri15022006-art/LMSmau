package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QuestionResultResponse {
    Integer questionId;
    String content;
    Double questionScore; // Điểm tối đa của câu
    Double earnedScore;   // Điểm thực tế đạt được
    String explanation;   // Chỉ hiện sau khi nộp bài [cite: 60]

    List<Integer> selectedAnswerIds; // Các ID học viên đã chọn
    List<Integer> correctAnswerIds; // Các ID đáp án đúng thực tế

    boolean isCorrect; // Trạng thái đúng/sai tổng thể của câu

    List<QuestionDetailResponse.AnswerDetailResponse> answers;
}
