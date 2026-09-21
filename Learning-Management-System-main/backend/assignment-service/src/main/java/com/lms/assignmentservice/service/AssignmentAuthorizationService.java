package com.lms.assignmentservice.service;

public interface AssignmentAuthorizationService {
    String currentUserId();

    String currentRole();

    boolean hasRole(String role);

    boolean isAdmin();

    boolean isInstructor();

    boolean isAdminOrInstructor();

    void checkOwnerOrAdmin(String ownerId);

    void checkCurrentUserOrAdminOrInstructor(String targetUserId);
}

