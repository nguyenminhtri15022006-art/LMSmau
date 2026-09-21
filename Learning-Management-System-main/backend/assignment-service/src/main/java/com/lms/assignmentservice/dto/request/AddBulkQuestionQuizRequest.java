package com.lms.assignmentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddBulkQuestionQuizRequest {

    @NotEmpty(message = "Danh sách câu hỏi không được để trống")
    @Valid // Kích hoạt validate cho từng phần tử bên trong List
    List<AddQuestionToQuizRequest> questionItems;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AddQuestionToQuizRequest {
        @NotNull(message = "questionId không được để trống")
        Integer questionId;

        Short overrideScore;

        Short orderIndex;
    }
}
