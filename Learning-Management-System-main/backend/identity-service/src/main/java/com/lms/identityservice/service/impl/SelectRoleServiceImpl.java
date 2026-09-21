package com.lms.identityservice.service.impl;

import com.lms.identityservice.dto.request.SelectRoleRequest;
import com.lms.identityservice.dto.response.SelectRoleResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.AuthenticationService;
import com.lms.identityservice.service.SelectRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SelectRoleServiceImpl implements SelectRoleService {

    private static final int ROLE_SELECTION_WINDOW_MINUTES = 30;

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    @Override
    public SelectRoleResponse selectRole(String currentUserId, SelectRoleRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        validateRoleSelectionEligibility(user);

        String roleName = switch (request.getRole()) {
            case STUDENT -> handleSelectStudent();
            case INSTRUCTOR -> handleSelectInstructor(user);
        };

        user.setRoleSelected(true);
        userRepository.save(user);

        var authResponse = authenticationService.issueTokensForUser(currentUserId);

        log.info("User {} đã chọn role {}", currentUserId, roleName);

        return SelectRoleResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .role(roleName)
                .message("Chào mừng! Bạn đã tham gia với vai trò " + roleName)
                .build();
    }

    private void validateRoleSelectionEligibility(User user) {
        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            throw new IdentityException(ErrorCode.ROLE_SELECTION_NOT_ALLOWED);
        }

        if (user.getRoleSelected()) {
            throw new IdentityException(ErrorCode.ROLE_ALREADY_SELECTED);
        }

        Instant deadline = user.getCreatedAt()
                .plusSeconds(ROLE_SELECTION_WINDOW_MINUTES * 60L);

        if (Instant.now().isAfter(deadline)) {
            throw new IdentityException(ErrorCode.ROLE_SELECTION_EXPIRED);
        }
    }

    private String handleSelectStudent() {
        return "STUDENT";
    }

    private String handleSelectInstructor(User user) {
        user.setRole(com.lms.identityservice.enums.RoleType.INSTRUCTOR);
        return "INSTRUCTOR";
    }
}
