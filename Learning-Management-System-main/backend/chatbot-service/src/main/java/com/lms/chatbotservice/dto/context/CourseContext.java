package com.lms.chatbotservice.dto.context;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseContext {
    String courseId;
    String title;
    String description;
    String instructorId;
    String status;
    Integer duration;
    String level;
    List<String> categoryNames;
    List<String> learningPoints;
    List<String> requirements;
    boolean enrolled;
}