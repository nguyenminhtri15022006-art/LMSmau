package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteStudentRequest {

    @NotEmpty(message = "INVITE_USER_IDS_EMPTY")
    @Size(max = 50, message = "INVITE_USER_IDS_SIZE")
    List<String> userIds;
}
