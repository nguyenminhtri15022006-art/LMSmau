package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LockCourseRequest {

    @NotBlank(message = "LOCK_REASON_BLANK")
    @Size(max = 1000, message = "LOCK_REASON_SIZE")
    String reason;
}