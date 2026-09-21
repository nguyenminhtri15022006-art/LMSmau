package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.AdminCreateAdminRequest;
import com.lms.identityservice.dto.request.AdminResetPasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserDashboardResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserSummaryResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.AdminUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    public Page<UserSummaryResponse> getAllUser(Pageable pageable, String role, String keyword) {
        String normalizedRole = normalizeRole(role);
        RoleType roleType = normalizedRole != null ? RoleType.valueOf(normalizedRole) : null;
        String searchKeyword = normalizeKeyword(keyword);
        return userRepository.findUsersByFilters(roleType, searchKeyword, pageable)
                .map(userMapper::toUserSummaryResponse);
    }

    @Override
    public UserFullResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse toggleActive(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

        if (user.getRole() == RoleType.ADMIN) {
            throw new IdentityException(ErrorCode.CANNOT_DEACTIVATE_ADMIN);
        }

        boolean newStatus = !user.getActive();
        user.setActive(newStatus);
        userRepository.save(user);

        log.info("Admin {} {} user {}",
                GatewayAuthentication.currentUserId(),
                newStatus ? "activated" : "deactivated",
                id);

        if (!newStatus) {
            refreshTokenRepository.revokeAllByUserId(id);
        }

        return userMapper.toUserFullResponse(user);
    }

    @Override
    public UserDashboardResponse getUserStats() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfWeek = today
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> stats = userRepository.countUserDashboardStats(startOfWeek, startOfMonth);
        Object[] row = stats.isEmpty() ? new Object[5] : stats.get(0);

        return UserDashboardResponse.builder()
                .totalStudents(toLong(row[0]))
                .newStudentsThisWeek(toLong(row[1]))
                .totalInstructors(toLong(row[2]))
                .newInstructorsThisMonth(toLong(row[3]))
                .totalAdmins(toLong(row[4]))
                .build();
    }

    @Override
    @Transactional
    public UserFullResponse createAdmin(AdminCreateAdminRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IdentityException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IdentityException(ErrorCode.EMAIL_EXISTED);
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(RoleType.ADMIN)
                .active(true)
                .verified(true)
                .roleSelected(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);
        log.info("Admin {} created new admin account: {}", GatewayAuthentication.currentUserId(), request.getUsername());
        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        userRepository.save(user);
        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse resetPassword(String id, AdminResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(id);
        log.info("Admin {} reset password for user {}", GatewayAuthentication.currentUserId(), id);
        return userMapper.toUserFullResponse(user);
    }

    @Override
    @Transactional
    public UserFullResponse toggleAdminActive(String id) {
        String currentUserId = GatewayAuthentication.currentUserId();
        if (id.equals(currentUserId)) {
            throw new IdentityException(ErrorCode.CANNOT_DEACTIVATE_ADMIN);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        boolean newStatus = !user.getActive();
        user.setActive(newStatus);
        userRepository.save(user);
        if (!newStatus) {
            refreshTokenRepository.revokeAllByUserId(id);
        }
        log.info("Admin {} {} admin account {}", GatewayAuthentication.currentUserId(),
                newStatus ? "activated" : "deactivated", id);
        return userMapper.toUserFullResponse(user);
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private String normalizeRole(String role) {
        return StringUtils.hasText(role) ? role.trim().toUpperCase() : null;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
