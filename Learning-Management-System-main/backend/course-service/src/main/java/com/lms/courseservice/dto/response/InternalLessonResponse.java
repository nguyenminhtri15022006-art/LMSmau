package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.courseservice.enums.LessonType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalLessonResponse {
    String id;
    String courseId;
    LessonType lessonType;
}
