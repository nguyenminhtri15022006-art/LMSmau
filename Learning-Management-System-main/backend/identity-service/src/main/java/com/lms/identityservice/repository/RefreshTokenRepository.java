package com.lms.identityservice.repository;

import com.lms.identityservice.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    boolean existsByTokenHashAndRevokedTrue(String refreshToken);

    // 1. Dùng khóa bi quan để chống Race Condition
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.userId = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") String userId);

    @Modifying
    @Query("""
            DELETE FROM RefreshToken t
            WHERE t.expiresAt < :now OR t.revoked = true
            """)
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
