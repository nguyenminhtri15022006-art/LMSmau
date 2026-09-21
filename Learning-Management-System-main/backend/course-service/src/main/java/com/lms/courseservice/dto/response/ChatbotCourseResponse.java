package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotCourseResponse {
    String id;
    String title;
    String description;
    String instructorId;
    String status;
    String thumbnailUrl;
    Integer duration;
    String level;
    List<String> categoryNames;
    List<String> learningPoints;
    List<String> requirements;
}