package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.courseservice.enums.EnrollmentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnrollmentResponse {
    Long id;
    String userId;
    String courseId;
    EnrollmentStatus status;
    LocalDateTime enrolledAt;
}
