package com.lms.apigateway.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility class verify JWT tại API Gateway.
 * <p>
 * ĐÂY LÀ NƠI DUY NHẤT trong toàn hệ thống biết JWT secret key.
 * Các service khác (Identity, Course, Assignment, Blog) không có class này.
 */
@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtUtil {

    SecretKey secretKey;

    public JwtUtil(@Value("${jwt.signer-key}") String signerKey) {
        this.secretKey = Keys.hmacShaKeyFor(signerKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verify token và trả về Claims nếu hợp lệ.
     * Trả về Optional.empty() nếu token không hợp lệ hoặc hết hạn.
     */
    public Optional<Claims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Kiểm tra đây là ACCESS token, không phải REFRESH token
            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                log.warn("Token type không hợp lệ: {}", type);
                return Optional.empty();
            }

            return Optional.of(claims);

        } catch (JwtException e) {
            log.debug("JWT không hợp lệ: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Lỗi không xác định khi verify JWT", e);
            return Optional.empty();
        }
    }

    /**
     * Extract token from Authorization headers.
     * Format: "Bearer <token>"
     */
    public Optional<String> extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }
}
