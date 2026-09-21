package com.lms.courseservice.entity.mongo;


import com.lms.courseservice.enums.NodeType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StructureNode {
    String id;        // UUID
    NodeType type;      // "folder" | "lesson"
    String title;
    Integer order;
    String parentId;  // null nếu là root
    String lessonId;  // ObjectId của Lesson, chỉ có khi type = "lesson"
}
