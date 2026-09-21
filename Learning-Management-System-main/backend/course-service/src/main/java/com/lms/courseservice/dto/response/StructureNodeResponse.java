package com.lms.courseservice.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.enums.NodeType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StructureNodeResponse {
    String id;
    NodeType type;
    String title;
    Integer order;
    String parentId;
    String lessonId;
    LessonType lessonType;
}
