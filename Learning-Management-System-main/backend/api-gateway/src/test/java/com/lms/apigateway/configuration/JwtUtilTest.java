package com.lms.apigateway.configuration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SIGNER_KEY);

    @Test
    void extractTokenReturnsBearerValueOnlyWhenHeaderIsValid() {
        assertThat(jwtUtil.extractToken("Bearer access-token")).contains("access-token");
        assertThat(jwtUtil.extractToken("Bearer   ")).isEmpty();
        assertThat(jwtUtil.extractToken("Basic access-token")).isEmpty();
        assertThat(jwtUtil.extractToken(null)).isEmpty();
    }

    @Test
    void verifyAcceptsAccessTokenAndExposesGatewayClaims() {
        String token = token("ACCESS");

        assertThat(jwtUtil.verify(token))
                .isPresent()
                .get()
                .satisfies(claims -> {
                    assertThat(claims.getSubject()).isEqualTo("user-1");
                    assertThat(claims.get("scope", String.class)).isEqualTo("ROLE_STUDENT");
                    assertThat(claims.get("email", String.class)).isEqualTo("student@example.com");
                });
    }

    @Test
    void verifyRejectsRefreshTokenAndMalformedToken() {
        assertThat(jwtUtil.verify(token("REFRESH"))).isEmpty();
        assertThat(jwtUtil.verify("not-a-jwt")).isEmpty();
    }

    private String token(String type) {
        return Jwts.builder()
                .subject("user-1")
                .claim("scope", "ROLE_STUDENT")
                .claim("email", "student@example.com")
                .claim("type", type)
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
