package com.lms.identityservice.dto.request;

import com.lms.identityservice.validator.DobConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @NotBlank(message = "NAME_BLANK")
    @Size(min = 3, max = 50, message = "NAME_SIZE")
    @Pattern(
            regexp = "^[A-Za-zÀ-ỹ\\s]+$",
            message = "NAME_REGET"
    )
    String fullname;

    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "PHONE_INVALID"
    )
    String phone;

    @DobConstraint(min = 6, message = "DOB_INVALID")
    LocalDate dob;

    String avatarUrl;
    List<String> discardedAvatarUrls;
    String bio;

}
