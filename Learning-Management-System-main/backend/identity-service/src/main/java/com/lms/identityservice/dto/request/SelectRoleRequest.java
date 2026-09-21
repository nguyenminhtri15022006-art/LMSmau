package com.lms.identityservice.dto.request;

import com.lms.identityservice.enums.RegisterableRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectRoleRequest {

    @NotNull(message = "Role không được để trống")
    private RegisterableRole role;
}