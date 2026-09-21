package com.lms.courseservice.entity.mongo;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "course_structures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseStructure {

    @Id
    String id;

    String courseId;

    @Builder.Default
    List<StructureNode> nodes = new ArrayList<>();

    @LastModifiedDate
    LocalDateTime updatedAt;
}