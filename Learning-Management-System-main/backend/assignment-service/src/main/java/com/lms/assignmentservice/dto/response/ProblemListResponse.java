package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.assignmentservice.enums.DifficultyType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemListResponse {
    Integer problemId;
    String courseId;
    String lessonId;
    String title;
    String slug;
    String description;
    DifficultyType difficulty;
    Integer timeLimitMs;
    Integer memoryLimitMb;
    List<String> allowedLangs;
    Short score;
    Boolean isPublic;
    Boolean completed;
    Boolean deleted;
    Instant deletedAt;
    Integer acceptanceRate;
    Integer totalSubmit;
    Instant createdAt;
}
