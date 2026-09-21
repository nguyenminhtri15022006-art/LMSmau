package com.lms.identityservice.dto.request;

import com.lms.identityservice.enums.RegisterableRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserRoleUpdateRequest {
    @NotNull(message = "Role không được để trống")
    RegisterableRole role;
}
