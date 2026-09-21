package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.UserUpdatePasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    StorageService storageService;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder,
                refreshTokenRepository,
                storageService);
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication("user-1", "student@example.com", "ROLE_STUDENT", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatePasswordChangesPasswordAndRevokesExistingRefreshTokens() {
        User user = User.builder()
                .userId("user-1")
                .password("old-hash")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword1", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .build());

        UserFullResponse response = userService.updatePassword(UserUpdatePasswordRequest.builder()
                .oldPassword("oldPassword1")
                .newPassword("newPassword1")
                .confirmPassword("newPassword1")
                .build());

        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId("user-1");
    }

    @Test
    void updatePasswordRejectsMismatchedConfirmationBeforeEncoding() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(User.builder().userId("user-1").build()));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> userService.updatePassword(UserUpdatePasswordRequest.builder()
                        .oldPassword("oldPassword1")
                        .newPassword("newPassword1")
                        .confirmPassword("different1")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_NOT_MATCH));

        verify(passwordEncoder, never()).encode(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void updateInfoDeletesReplacedAndDiscardedAvatarFiles() {
        User user = User.builder()
                .userId("user-1")
                .fullname("Old Name")
                .avatarUrl("old-avatar.png")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            User target = invocation.getArgument(0);
            UserUpdateRequest request = invocation.getArgument(1);
            target.setFullname(request.getFullname());
            target.setAvatarUrl(request.getAvatarUrl());
            return null;
        }).when(userMapper).updateUser(eq(user), any(UserUpdateRequest.class));
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .fullname("New Name")
                .avatarUrl("new-avatar.png")
                .build());

        UserFullResponse response = userService.updateInfo(UserUpdateRequest.builder()
                .fullname("New Name")
                .avatarUrl("new-avatar.png")
                .discardedAvatarUrls(List.of("discarded.png", "new-avatar.png", "discarded.png"))
                .build());

        assertThat(response.getFullname()).isEqualTo("New Name");
        verify(storageService).deleteFile("old-avatar.png");
        verify(storageService).deleteAvatarFileForUser("discarded.png", "user-1");
        verify(storageService, never()).deleteAvatarFileForUser("new-avatar.png", "user-1");
    }
}
