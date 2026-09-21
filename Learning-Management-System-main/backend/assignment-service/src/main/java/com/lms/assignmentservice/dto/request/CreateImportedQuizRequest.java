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
public class CreateImportedQuizRequest {
    @NotNull(message = "ThÃ´ng tin quiz khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Valid
    CreateQuizRequest quiz;

    @NotEmpty(message = "Danh sÃ¡ch cÃ¢u há»i khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    List<SaveQuizDraftRequest.QuestionDraftRequest> questions;

    boolean publish;
}
