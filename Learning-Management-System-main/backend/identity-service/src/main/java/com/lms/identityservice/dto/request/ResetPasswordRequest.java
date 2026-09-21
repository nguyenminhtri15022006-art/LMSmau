package com.lms.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "EMAIL_BLANK")
    @Size(max = 100, message = "EMAIL_SIZE")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            message = "EMAIL_REGET"
    )
    String email;

    @NotBlank(message = "OTP_BLANK")
    @Size(min = 6, max = 6, message = "OTP_INVALID")
    String otp;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, message = "PASSWORD_SIZE")
    @Pattern(
            regexp = "^\\S{8,}$",
            message = "PASSWORD_REGET"
    )
    String newPassword;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, message = "PASSWORD_SIZE")
    String confirmPassword;
}
