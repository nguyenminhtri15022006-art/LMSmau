package com.lms.assignmentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveQuizDraftRequest {
    @NotNull(message = "Thong tin quiz khong duoc de trong")
    @Valid
    CreateQuizRequest quiz;

    @NotEmpty(message = "Danh sach cau hoi khong duoc de trong")
    List<QuestionDraftRequest> questions;

    boolean publish;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuestionDraftRequest extends CreateQuestionRequest {
        Integer questionId;
        boolean reuseExisting;
    }
}
