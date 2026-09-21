package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.configuration.GatewayAuthentication;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class AssignmentAuthorizationServiceImpl implements AssignmentAuthorizationService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_INSTRUCTOR = "ROLE_INSTRUCTOR";

    @Override
    public String currentUserId() {
        return GatewayAuthentication.requireCurrentUserId();
    }

    @Override
    public String currentRole() {
        String role = GatewayAuthentication.currentRole();
        if (role == null || role.isBlank()) {
            return ROLE_PREFIX + "STUDENT";
        }

        String normalized = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        if ((ROLE_PREFIX + "TEACHER").equals(normalized)) {
            return ROLE_INSTRUCTOR;
        }

        return normalized;
    }

    @Override
    public boolean hasRole(String role) {
        return role != null && role.equals(currentRole());
    }

    @Override
    public boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    @Override
    public boolean isInstructor() {
        return hasRole(ROLE_INSTRUCTOR);
    }

    @Override
    public boolean isAdminOrInstructor() {
        return isAdmin() || isInstructor();
    }

    @Override
    public void checkOwnerOrAdmin(String ownerId) {
        if (isAdmin()) {
            return;
        }

        if (!currentUserId().equals(ownerId)) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    public void checkCurrentUserOrAdminOrInstructor(String targetUserId) {
        if (currentUserId().equals(targetUserId) || isAdminOrInstructor()) {
            return;
        }

        throw new AssignmentException(ErrorCode.ACCESS_DENIED);
    }
}

