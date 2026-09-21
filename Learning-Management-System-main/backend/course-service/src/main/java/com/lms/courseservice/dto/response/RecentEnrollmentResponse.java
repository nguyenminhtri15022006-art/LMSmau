package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentEnrollmentResponse {
    String courseId;
    String courseTitle;
    String courseThumbnailUrl;
    String userId;
    String fullname;
    String avatarUrl;
    LocalDateTime enrolledAt;
}
