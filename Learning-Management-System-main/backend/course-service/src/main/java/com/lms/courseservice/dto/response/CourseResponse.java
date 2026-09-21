package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {
    String id;
    String title;
    String description;
    String instructorId;
    String thumbnailUrl;
    CourseStatus status;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Integer duration;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    CourseLevel level;
    String meetingUrl;
    List<CategoryResponse> categories;
    Long exerciseCount;
    List<String> learningPoints;
    List<String> requirements;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
