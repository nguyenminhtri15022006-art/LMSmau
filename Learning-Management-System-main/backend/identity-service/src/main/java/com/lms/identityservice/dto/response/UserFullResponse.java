package com.lms.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserFullResponse {
    String userId;
    String providerId;
    String username;
    String fullname;
    String email;
    String phone;
    Date dob;
    String role;
    String avatarUrl;
    String bio;
    Boolean verified;
    Boolean active;
    Boolean roleSelected;
    String authProvider;
    Instant createdAt;
    Instant updatedAt;
    Instant lastLoginAt;
}
