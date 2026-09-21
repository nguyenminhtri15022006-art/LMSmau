package com.lms.chatbotservice.dto.feign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemDTO {
    Integer problemId;
    String courseId;
    String lessonId;
    String title;
    String slug;
    String description;
    String difficulty;
    Integer timeLimitMs;
    Integer memoryLimitMb;
    List<String> allowedLangs;
    Short score;
    Boolean isPublic;
    Integer totalSubmit;
    Integer totalAccepted;
}