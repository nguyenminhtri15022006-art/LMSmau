package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.constant.SecutityConstants;
import com.lms.identityservice.dto.request.*;
import com.lms.identityservice.dto.response.AuthenticationResponse;
import com.lms.identityservice.dto.response.IntrospectResponse;
import com.lms.identityservice.dto.response.SocialUserInfo;
import com.lms.identityservice.dto.response.UserCreationResponse;
import com.lms.identityservice.entity.RefreshToken;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RegisterableRole;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.enums.TokenType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.AuthenticationService;
import com.lms.identityservice.service.GoogleOAuthService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    GoogleOAuthService googleOAuthService;

    @NonFinal
    @Value("${jwt.signer-key}")
    String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long refreshableDuration;


    @Override
    @Transactional
    public UserCreationResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IdentityException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IdentityException(ErrorCode.EMAIL_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRoleSelected(true);
        userRepository.save(user);

        return userMapper.toUserCreationResponse(user);
    }

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String login = request.getUsername();
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new IdentityException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.getActive()) {
            throw new IdentityException(ErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IdentityException(ErrorCode.INVALID_CREDENTIALS);
        }

        userRepository.updateLastLoginTime(user.getUsername(), Instant.now());

        String accessToken = generateToken(user, TokenType.ACCESS.name(), validDuration);
        String refreshToken = generateToken(user, TokenType.REFRESH.name(), refreshableDuration);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(refreshableDuration))
                .revoked(false)
                .ipAddress(request.getIpAddress())
                .deviceInfo(request.getDeviceInfo())
                .build());

        Date accessExpiryTime = new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(accessExpiryTime)
                .user(userMapper.toUserFullResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse socialLogin(SocialLoginRequest request, String provider) {
        SocialUserInfo socialUserInfo;

        if ("google".equalsIgnoreCase(provider)) {
            socialUserInfo = googleOAuthService.getUserInfo(request.getCode(), request.getRedirectUri());
        } else {
            throw new IdentityException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }

        String ipAddress = request.getIpAddress() != null ? request.getIpAddress() : getClientIpAddress();
        String deviceInfo = request.getDeviceInfo() != null ? request.getDeviceInfo() : getDeviceInfoFromRequest();

        AuthProvider authProvider = resolveAuthProvider(provider);
        String providerId = socialUserInfo.getProviderId();
        if (providerId == null || providerId.isBlank()) {
            throw new IdentityException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        User linkedUser = userRepository.findByProviderIdAndAuthProvider(providerId, authProvider)
                .orElse(null);
        if (linkedUser != null) {
            return buildAuthenticationResponse(linkedUser, false, ipAddress, deviceInfo);
        }

        String email = socialUserInfo.getEmail();
        if (email == null || email.isBlank()) {
            throw new IdentityException(ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED);
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (!existingUser.getActive()) {
                throw new IdentityException(ErrorCode.USER_INACTIVE);
            }

            if (existingUser.getProviderId() == null) {
                existingUser.setAuthProvider(authProvider);
                existingUser.setProviderId(providerId);
            }

            if ((existingUser.getFullname() == null || existingUser.getFullname().isBlank()) && socialUserInfo.getFullname() != null) {
                existingUser.setFullname(socialUserInfo.getFullname());
            }
            if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) && socialUserInfo.getAvatarUrl() != null) {
                existingUser.setAvatarUrl(socialUserInfo.getAvatarUrl());
            }
            userRepository.save(existingUser);
            return buildAuthenticationResponse(existingUser, false, ipAddress, deviceInfo);
        }

        User user = createSocialUser(socialUserInfo, authProvider, null);
        return buildAuthenticationResponse(user, true, ipAddress, deviceInfo);
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;
        try {
            verifyToken(request.getToken());
        } catch (IdentityException | ParseException | JOSEException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .isValid(isValid)
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        try {
            SignedJWT signedJWT = verifyToken(request.getRefreshToken());

            String type = signedJWT.getJWTClaimsSet().getStringClaim("type");
            if (!TokenType.REFRESH.name().equals(type)) {
                throw new IdentityException(ErrorCode.INVALID_TOKEN);
            }

            String tokenHash = hashToken(request.getRefreshToken());
            RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new IdentityException(ErrorCode.INVALID_TOKEN));

            if (oldToken.isRevoked()) {
                log.warn("Phat hien nguy co lo token! User ID: {}", oldToken.getUser().getUserId());
                throw new IdentityException(ErrorCode.TOKEN_REVOKED);
            }

            String userId = signedJWT.getJWTClaimsSet().getSubject();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);

            String newAccessToken = generateToken(user, TokenType.ACCESS.name(), validDuration);
            String newRefreshToken = generateToken(user, TokenType.REFRESH.name(), refreshableDuration);

            RefreshToken newTokenEntity = RefreshToken.builder()
                    .tokenHash(hashToken(newRefreshToken))
                    .expiresAt(Instant.now().plusSeconds(refreshableDuration))
                    .revoked(false)
                    .user(user)
                    .ipAddress(oldToken.getIpAddress())
                    .deviceInfo(oldToken.getDeviceInfo())
                    .build();
            refreshTokenRepository.save(newTokenEntity);

            return AuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .accessTokenExpiry(new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                    .build();

        } catch (ParseException | JOSEException e) {
            log.error("Failed to refresh token", e);
            throw new IdentityException(ErrorCode.INVALID_TOKEN);
        }
    }

    private AuthenticationResponse buildAuthenticationResponse(User user, boolean isNewUser, String ipAddress, String deviceInfo) {
        if (!user.getActive()) {
            throw new IdentityException(ErrorCode.USER_INACTIVE);
        }

        userRepository.updateLastLoginTime(user.getUsername(), Instant.now());

        String accessToken = generateToken(user, TokenType.ACCESS.name(), validDuration);
        String refreshToken = generateToken(user, TokenType.REFRESH.name(), refreshableDuration);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(refreshableDuration))
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build());

        Date accessExpiryTime = new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli());

        boolean requiresRoleSelection = Boolean.FALSE.equals(user.getRoleSelected());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(accessExpiryTime)
                .isNewUser(isNewUser)
                .requiresRoleSelection(requiresRoleSelection)
                .user(userMapper.toUserFullResponse(user))
                .build();
    }

    private AuthProvider resolveAuthProvider(String provider) {
        if ("google".equalsIgnoreCase(provider)) {
            return AuthProvider.GOOGLE;
        }
        throw new IdentityException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
    }

    private User createSocialUser(SocialUserInfo socialUserInfo, AuthProvider authProvider, RegisterableRole role) {
        return createSocialUser(
                socialUserInfo.getEmail(),
                socialUserInfo.getProviderId(),
                authProvider,
                socialUserInfo.getFullname(),
                socialUserInfo.getAvatarUrl(),
                role
        );
    }

    private User createSocialUser(
            String email,
            String providerId,
            AuthProvider authProvider,
            String fullname,
            String avatarUrl,
            RegisterableRole role) {

        String baseUsername = email.split("@")[0];
        String uniqueUsername = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8);
        RoleType roleType = role != null ? mapRole(role) : RoleType.STUDENT;

        User newUser = User.builder()
                .email(email)
                .username(uniqueUsername)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullname(fullname != null ? fullname : baseUsername)
                .avatarUrl(avatarUrl)
                .role(roleType)
                .roleSelected(role != null)
                .active(true)
                .verified(true)
                .authProvider(authProvider)
                .providerId(providerId)
                .build();
        return userRepository.save(newUser);
    }

    private User linkSocialAccount(User user, SocialUserInfo socialUserInfo, AuthProvider authProvider, RegisterableRole role) {
        return linkSocialAccount(
                user,
                socialUserInfo.getProviderId(),
                authProvider,
                socialUserInfo.getFullname(),
                socialUserInfo.getAvatarUrl(),
                role
        );
    }

    private User linkSocialAccount(
            User user,
            String providerId,
            AuthProvider authProvider,
            String fullname,
            String avatarUrl,
            RegisterableRole role) {

        if (!user.getActive()) {
            throw new IdentityException(ErrorCode.USER_INACTIVE);
        }

        if (user.getProviderId() != null) {
            if (user.getAuthProvider() != authProvider) {
                throw new IdentityException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
            if (!user.getProviderId().equals(providerId)) {
                throw new IdentityException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
        } else {
            if (user.getAuthProvider() != AuthProvider.LOCAL && user.getAuthProvider() != authProvider) {
                throw new IdentityException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
            user.setAuthProvider(authProvider);
            user.setProviderId(providerId);
        }

        if ((user.getFullname() == null || user.getFullname().isBlank()) && fullname != null) {
            user.setFullname(fullname);
        }
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (!user.getVerified()) {
            user.setVerified(true);
        }

        applyRoleIfNeeded(user, role);
        return userRepository.save(user);
    }

    private void applyRoleIfNeeded(User user, RegisterableRole role) {
        if (user.getRoleSelected()) {
            return;
        }
        RoleType roleType = mapRole(role);
        user.setRole(roleType);
        user.setRoleSelected(true);
    }

    private RoleType mapRole(RegisterableRole role) {
        if (role == null) {
            throw new IdentityException(ErrorCode.INVALID_ROLE_SELECTION);
        }
        return role == RegisterableRole.INSTRUCTOR ? RoleType.INSTRUCTOR : RoleType.STUDENT;
    }


    @Override
    @Transactional
    public AuthenticationResponse issueTokensForUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        String accessToken = generateToken(user, TokenType.ACCESS.name(), validDuration);
        String refreshToken = generateToken(user, TokenType.REFRESH.name(), refreshableDuration);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(refreshableDuration))
                .revoked(false)
                .build());

        Date accessExpiryTime = new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(accessExpiryTime)
                .user(userMapper.toUserFullResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        try {
            JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());

            if (!signedJWT.verify(jwsVerifier)) {
                throw new IdentityException(ErrorCode.UNAUTHENTICATED);
            }

            String tokenHash = hashToken(request.getToken());
            RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new IdentityException(ErrorCode.INVALID_TOKEN));
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);

            log.info("user: {} logout, tokenHash: {}, isRevoked: {}",
                    GatewayAuthentication.currentUserId(),
                    tokenHash,
                    stored.isRevoked());

        } catch (ParseException | JOSEException e) {
            log.error("Failed to logout", e);
            throw new IdentityException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String generateToken(User user, String type, long duration) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserId())
                .issuer(SecutityConstants.JWT_ISSUER)
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim("scope", SecutityConstants.ROLE_PREFIX + user.getRole())
                .claim("type", type.toUpperCase())
                .claim("email", user.getEmail())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Failed to generate token", e);
            throw new IdentityException(ErrorCode.INVALID_TOKEN);
        }
    }

    private SignedJWT verifyToken(String token) throws ParseException, JOSEException {
        JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        if (!(signedJWT.verify(jwsVerifier) && expiryTime.after(new Date()))) {
            throw new IdentityException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SecutityConstants.HASH_ALGORITHM);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(SecutityConstants.HASH_ALGORITHM + " not available", e);
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";

            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty()) {
                return ip.split(",")[0];
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Could not get client IP address", e);
            return "unknown";
        }
    }

    private String getDeviceInfoFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";

            HttpServletRequest request = attributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            String screenRes = request.getHeader("X-Screen-Resolution");

            String info = userAgent != null ? userAgent.substring(0, Math.min(200, userAgent.length())) : "unknown";
            if (screenRes != null) {
                info += " | " + screenRes;
            }
            return info;
        } catch (Exception e) {
            log.warn("Could not get device info from request", e);
            return "unknown";
        }
    }
}
