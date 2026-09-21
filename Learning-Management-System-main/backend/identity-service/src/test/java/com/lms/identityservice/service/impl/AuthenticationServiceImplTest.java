package com.lms.identityservice.service.impl;

import com.lms.identityservice.dto.request.AuthenticationRequest;
import com.lms.identityservice.dto.request.RefreshTokenRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.entity.RefreshToken;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.GoogleOAuthService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final String SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserMapper userMapper;

    @Mock
    GoogleOAuthService googleOAuthService;

    AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                userMapper,
                googleOAuthService);
        ReflectionTestUtils.setField(authenticationService, "signerKey", SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "validDuration", 900L);
        ReflectionTestUtils.setField(authenticationService, "refreshableDuration", 3600L);
    }

    @Test
    void authenticateReturnsTokensAndStoresHashedRefreshToken() {
        User user = activeStudent();
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(userMapper.toUserFullResponse(user)).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .username("student")
                .role("STUDENT")
                .build());

        var response = authenticationService.authenticate(AuthenticationRequest.builder()
                .username("student")
                .password("secret123")
                .ipAddress("127.0.0.1")
                .deviceInfo("JUnit")
                .build());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getUserId()).isEqualTo("user-1");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken storedToken = tokenCaptor.getValue();
        assertThat(storedToken.getUser()).isSameAs(user);
        assertThat(storedToken.getTokenHash()).isNotBlank();
        assertThat(storedToken.getTokenHash()).isNotEqualTo(response.getRefreshToken());
        assertThat(storedToken.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(storedToken.getDeviceInfo()).isEqualTo("JUnit");
        verify(userRepository).updateLastLoginTime(anyString(), any(Instant.class));
    }

    @Test
    void authenticateRejectsInactiveUserBeforeCheckingPassword() {
        User user = activeStudent();
        user.setActive(false);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(user));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> authenticationService.authenticate(AuthenticationRequest.builder()
                        .username("student")
                        .password("secret123")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshTokenRejectsAccessTokenType() throws JOSEException {
        String accessToken = signedToken("user-1", "ACCESS");

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> authenticationService.refreshToken(
                        RefreshTokenRequest.builder().refreshToken(accessToken).build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    private User activeStudent() {
        return User.builder()
                .userId("user-1")
                .username("student")
                .email("student@example.com")
                .password("encoded-password")
                .fullname("Student One")
                .active(true)
                .verified(true)
                .role(RoleType.STUDENT)
                .roleSelected(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    private String signedToken(String userId, String type) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .claim("type", type)
                .build();
        JWSObject jwsObject = new JWSObject(
                new JWSHeader(JWSAlgorithm.HS512),
                new Payload(claimsSet.toJSONObject()));
        jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)));
        return jwsObject.serialize();
    }
}
