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
public class SendEmailVerificationRequest {
    @NotBlank(message = "EMAIL_BLANK")
    @Size(max = 100, message = "EMAIL_SIZE")
    @Pattern(
            regexp = "^[A-Aa-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            message = "EMAIL_REGET"
    )
    String email;
}
