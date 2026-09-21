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
public class AdminCreateAdminRequest {
    @NotBlank(message = "USERNAME_BLANK")
    @Size(min = 5, message = "USERNAME_SIZE")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "USERNAME_REGET")
    String username;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, message = "PASSWORD_SIZE")
    @Pattern(regexp = "^\\S{8,}$", message = "PASSWORD_REGET")
    String password;

    @NotBlank(message = "NAME_BLANK")
    @Size(min = 3, max = 50, message = "NAME_SIZE")
    @Pattern(regexp = "^[A-Za-zÀ-ỹ\\s]+$", message = "NAME_REGET")
    String fullname;

    @NotBlank(message = "EMAIL_BLANK")
    @Size(max = 100, message = "EMAIL_SIZE")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "EMAIL_REGET")
    String email;

    @Pattern(regexp = "^0[0-9]{9}$", message = "PHONE_INVALID")
    String phone;
}
