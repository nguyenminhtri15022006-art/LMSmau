package com.lms.identityservice.service;

import com.lms.identityservice.dto.request.AdminCreateAdminRequest;
import com.lms.identityservice.dto.request.AdminResetPasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserDashboardResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<UserSummaryResponse> getAllUser(Pageable pageable, String role, String keyword);

    UserFullResponse getUserById(String userId);

    UserFullResponse toggleActive(String id);

    UserDashboardResponse getUserStats();

    UserFullResponse createAdmin(AdminCreateAdminRequest request);

    UserFullResponse updateUser(String id, UserUpdateRequest request);

    UserFullResponse resetPassword(String id, AdminResetPasswordRequest request);

    UserFullResponse toggleAdminActive(String id);
}
