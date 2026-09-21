package com.lms.courseservice.dto.request;

import com.lms.courseservice.enums.LessonType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLessonRequest {

    @NotNull(message = "LESSON_TYPE_NULL")
    LessonType lessonType;

    @NotNull(message = "LESSON_CONTENT_NULL")
    Map<String, Object> content;

}