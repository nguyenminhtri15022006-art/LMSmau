package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.VerifyEmailRequest;
import com.lms.identityservice.entity.EmailVerification;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.VerificationType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.EmailService;
import com.lms.identityservice.service.OtpAttemptHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    EmailVerificationRepository emailVerificationRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    EmailService emailService;

    @Mock
    OtpAttemptHandler otpAttemptHandler;

    @Mock
    PasswordEncoder passwordEncoder;

    EmailVerificationServiceImpl emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationServiceImpl(
                emailVerificationRepository,
                userRepository,
                emailService,
                otpAttemptHandler,
                passwordEncoder);
        ReflectionTestUtils.setField(emailVerificationService, "otpExpiryMinutes", 10L);
        ReflectionTestUtils.setField(emailVerificationService, "otpMaxAttempts", 5);
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication("user-1", "student@example.com", "ROLE_STUDENT", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifyEmailMarksOtpUsedAndUserVerified() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .verified(false)
                .build();
        EmailVerification otp = EmailVerification.builder()
                .id(10L)
                .user(user)
                .token("123456")
                .type(VerificationType.VERIFY_EMAIL)
                .attempts(0)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.VERIFY_EMAIL), any(), eq(5)))
                .thenReturn(Optional.of(otp));

        emailVerificationService.verifyEmail(VerifyEmailRequest.builder()
                .email("student@example.com")
                .otp("123456")
                .build());

        assertThat(user.getVerified()).isTrue();
        assertThat(otp.getUsedAt()).isNotNull();
        verify(emailVerificationRepository).save(otp);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailIncrementsAttemptAndThrowsInvalidOtpForWrongCode() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .verified(false)
                .build();
        EmailVerification otp = EmailVerification.builder()
                .id(10L)
                .user(user)
                .token("123456")
                .type(VerificationType.VERIFY_EMAIL)
                .attempts(1)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.VERIFY_EMAIL), any(), eq(5)))
                .thenReturn(Optional.of(otp));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> emailVerificationService.verifyEmail(VerifyEmailRequest.builder()
                        .email("student@example.com")
                        .otp("000000")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_OTP));

        verify(otpAttemptHandler).increment(10L);
    }
}
