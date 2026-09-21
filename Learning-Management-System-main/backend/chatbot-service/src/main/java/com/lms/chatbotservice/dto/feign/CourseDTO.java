package com.lms.chatbotservice.dto.feign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDTO {
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
    boolean enrolled;
}