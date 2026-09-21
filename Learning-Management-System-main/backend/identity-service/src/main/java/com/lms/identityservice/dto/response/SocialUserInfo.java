package com.lms.identityservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialUserInfo {
    String providerId;      // Google's unique user ID (sub)
    String email;           // Email từ Google
    String fullname;        // Tên người dùng
    String avatarUrl;       // URL avatar/picture URL
}
