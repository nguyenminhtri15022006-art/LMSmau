package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PracticeAnswerResponse {
    Integer questionId;
    Boolean correct;
    Boolean firstAnswerRecorded;
    String explanation;
}
