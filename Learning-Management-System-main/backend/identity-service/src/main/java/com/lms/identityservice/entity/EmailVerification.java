package com.lms.identityservice.entity;

import com.lms.identityservice.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private User user;

    @Column(name = "token", length = 255, nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "ENUM('VERIFY_EMAIL','RESET_PASSWORD')")
    private VerificationType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // NULL = chưa dùng, có giá trị = đã sử dụng
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "attempts", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    public boolean isMaxAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }
}
