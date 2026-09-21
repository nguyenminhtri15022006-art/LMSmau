package com.lms.identityservice.service;

import com.lms.identityservice.dto.response.UserFullResponse;

import java.util.List;
import java.util.Set;

public interface InternalUserService {
    UserFullResponse getUserById(String userId);
    List<UserFullResponse> getUsersByIds(Set<String> userIds);
    boolean existsById(String userId);
    UserFullResponse findByEmail(String email);
}

