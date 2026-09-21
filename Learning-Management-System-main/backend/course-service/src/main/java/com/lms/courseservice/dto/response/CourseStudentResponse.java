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
public class CourseStudentResponse {
    String userId;
    String username;
    String fullname;
    String email;
    String avatarUrl;
    LocalDateTime enrolledAt;
    String enrollmentStatus;
}
