package com.lms.identityservice.dto.request;

import com.lms.identityservice.enums.RegisterableRole;
import com.lms.identityservice.validator.DobConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank(message = "USERNAME_BLANK") // enumkey
    @Size(min = 5, message = "USERNAME_SIZE")
    @Pattern(
            regexp = "^[a-zA-z0-9_]+$",
            message = "USERNAME_REGET"
    )
    String username;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, message = "PASSWORD_SIZE")
    @Pattern(
            regexp = "^\\S{8,}$",
            message = "PASSWORD_REGET"
    )
    String password;


    @NotBlank(message = "NAME_BLANK")
    @Size(min = 3, max = 50, message = "NAME_SIZE")
    @Pattern(
            regexp = "^[A-Za-zÀ-ỹ\\s]+$",
            message = "NAME_REGET"
    )
    String fullname;

    @NotBlank(message = "EMAIL_BLANK")
    @Size(max = 100, message = "EMAIL_SIZE")
    @Pattern(
            regexp = "^[A-Aa-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            message = "EMAIL_REGET"
    )
    String email;

    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "PHONE_INVALID"
    )
    String phone;

    @DobConstraint(min = 6, message = "DOB_INVALID")
    LocalDate dob;

    RegisterableRole role;
}
