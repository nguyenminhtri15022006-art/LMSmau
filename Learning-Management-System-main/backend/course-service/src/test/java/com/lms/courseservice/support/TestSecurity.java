package com.lms.courseservice.support;

import com.lms.courseservice.configuration.GatewayAuthentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class TestSecurity {

    private TestSecurity() {
    }

    public static void authenticate(String userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(
                        userId,
                        userId + "@example.com",
                        role,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
