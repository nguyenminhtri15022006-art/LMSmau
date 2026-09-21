package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.UserRoleUpdateRequest;
import com.lms.identityservice.dto.request.UserUpdatePasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.RegisterableRole;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.StorageService;
import com.lms.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("isAuthenticated()")
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RefreshTokenRepository refreshTokenRepository;
    StorageService storageService;

    @Override
    public UserFullResponse getMyinfo() {
        String userId = GatewayAuthentication.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse updateInfo(UserUpdateRequest request) {
        String userId = GatewayAuthentication.currentUserId();
        log.info("userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        String oldAvatarUrl = user.getAvatarUrl();
        userMapper.updateUser(user, request);
        if (request.getAvatarUrl() != null && request.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl(null);
        }
        userRepository.save(user);

        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()
                && !oldAvatarUrl.equals(user.getAvatarUrl())) {
            storageService.deleteFile(oldAvatarUrl);
        }
        if (request.getDiscardedAvatarUrls() != null) {
            request.getDiscardedAvatarUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .filter(url -> !url.equals(user.getAvatarUrl()))
                    .distinct()
                    .forEach(url -> storageService.deleteAvatarFileForUser(url, userId));
        }

        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse updatePassword(UserUpdatePasswordRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IdentityException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IdentityException(ErrorCode.PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        return userMapper.toUserFullResponse(user);
    }

    @Override
    @PreAuthorize("permitAll()")
    public UserPublicProfileResponse getPublicProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        return UserPublicProfileResponse.builder()
                .userId(user.getUserId())
                .fullname(user.getFullname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }

    @Override
    @Transactional
    public UserFullResponse updateRole(UserRoleUpdateRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        if (request.getRole() == null) {
            throw new IdentityException(ErrorCode.INVALID_ROLE_SELECTION);
        }

        RoleType roleType = request.getRole() == RegisterableRole.INSTRUCTOR
                ? RoleType.INSTRUCTOR
                : RoleType.STUDENT;

        user.setRole(roleType);
        user.setRoleSelected(true);
        userRepository.save(user);

        return userMapper.toUserFullResponse(user);
    }
}
