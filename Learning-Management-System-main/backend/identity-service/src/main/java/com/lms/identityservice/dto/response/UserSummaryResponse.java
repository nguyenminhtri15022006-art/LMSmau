package com.lms.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryResponse {
    String userId;
    String username;
    String fullname;
    String email;
    String avatarUrl;
    String role;
    Boolean active;
    Instant createdAt;
    String authProvider;
}
