package com.lms.identityservice.service;

import com.lms.identityservice.dto.request.UserRoleUpdateRequest;
import com.lms.identityservice.dto.request.UserUpdatePasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;

public interface UserService {
    UserFullResponse getMyinfo();

    UserFullResponse updateInfo(UserUpdateRequest request);

    UserFullResponse updatePassword(UserUpdatePasswordRequest request);

    UserFullResponse updateRole(UserRoleUpdateRequest request);

    UserPublicProfileResponse getPublicProfile(String userId);
}
