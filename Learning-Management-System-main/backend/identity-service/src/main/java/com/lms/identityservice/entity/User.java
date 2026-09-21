package com.lms.identityservice.entity;

import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @UuidGenerator
    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    String userId;

    @Column(name = "user_name", length = 55, nullable = false, unique = true)
    String username;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    String password;

    @Column(name = "full_name", length = 255, nullable = false)
    String fullname;

    @Column(name = "dob")
    LocalDate dob;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    RoleType role = RoleType.STUDENT;

    @Column(name = "role_selected", nullable = false)
    @Builder.Default
    Boolean roleSelected = false;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Column(name = "phone", length = 20)
    String phone;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    Boolean active = true;

    @Column(name = "is_verified", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 100)
    String providerId;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<RefreshToken> refreshTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<EmailVerification> emailVerifications = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
