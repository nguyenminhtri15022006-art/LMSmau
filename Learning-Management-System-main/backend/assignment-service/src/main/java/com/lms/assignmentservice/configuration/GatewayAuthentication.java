package com.lms.assignmentservice.configuration;

import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

/**
 * Authentication object được build từ header Gateway forward xuống.
 * Dùng chung pattern với Assignment Service, Course Service, Blog Service.
 * <p>
 * principal  = userId (String UUID)
 * credentials = email
 * details    = role ("ADMIN" | "TEACHER" | "STUDENT")
 */
public class GatewayAuthentication implements Authentication {

    private final String userId;
    private final String email;
    private final String role;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated = true;

    public GatewayAuthentication(String userId, String email, String role,
                                 List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return email;
    }

    @Override
    public Object getDetails() {
        return role;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean b) {
        this.authenticated = b;
    }

    @Override
    public String getName() {
        return userId;
    }

    // ===== Static helpers dùng trong Service/Controller =====

    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof GatewayAuthentication ga) return ga.userId;
        if (auth != null && auth.getName() != null) return auth.getName();
        throw new IllegalStateException("Không tìm thấy userId trong SecurityContext");
    }

    public static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof GatewayAuthentication ga) return ga.role;
        return null;
    }

    public static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof GatewayAuthentication ga) return ga.email;
        return null;
    }

    /**
     * Safe version of currentUserId() that throws AssignmentException instead of IllegalStateException.
     * Use this in service methods where you need guaranteed authentication.
     *
     * @return userId if authenticated
     * @throws AssignmentException if user is not authenticated
     */
    public static String requireCurrentUserId() {
        try {
            String userId = currentUserId();
            if (userId == null || userId.isBlank()) {
                throw new AssignmentException(ErrorCode.UNAUTHENTICATED);
            }
            return userId;
        } catch (IllegalStateException e) {
            throw new AssignmentException(ErrorCode.UNAUTHENTICATED);
        }
    }

    /**
     * Safe version of currentRole() that never returns null.
     * Returns "STUDENT" as default if role is not found.
     */
    public static String requireCurrentRole() {
        String role = currentRole();
        return (role != null && !role.isBlank()) ? role : "STUDENT";
    }
}
