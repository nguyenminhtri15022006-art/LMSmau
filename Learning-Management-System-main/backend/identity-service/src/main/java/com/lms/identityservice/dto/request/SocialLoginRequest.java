package com.lms.identityservice.dto.request;

import com.lms.identityservice.enums.RegisterableRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SocialLoginRequest {

    @NotBlank(message = "Code không được để trống")
    String code;

    @NotBlank(message = "Redirect URI không được để trống")
    String redirectUri;

    RegisterableRole role;
    
    String ipAddress;
    String deviceInfo;
}
