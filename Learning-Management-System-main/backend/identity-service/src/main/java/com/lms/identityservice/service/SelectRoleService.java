package com.lms.identityservice.service;

import com.lms.identityservice.dto.request.SelectRoleRequest;
import com.lms.identityservice.dto.response.SelectRoleResponse;

public interface SelectRoleService {
    SelectRoleResponse selectRole(String currentUserId, SelectRoleRequest request);
}
