package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.courseservice.enums.LessonType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {
    String id;
    String courseId;
    LessonType lessonType;
    Map<String, Object> content;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
