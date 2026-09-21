package com.lms.apigateway.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.apigateway.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    JwtUtil jwtUtil;
    AuthFilter authFilter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        authFilter = new AuthFilter(jwtUtil, new ObjectMapper());
    }

    @Test
    void privateRequestWithValidTokenInjectsGatewayHeadersAndRemovesRawAuthorization() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("scope", String.class)).thenReturn("ROLE_STUDENT");
        when(claims.get("email", String.class)).thenReturn("student@example.com");
        when(jwtUtil.extractToken("Bearer access-token")).thenReturn(Optional.of("access-token"));
        when(jwtUtil.verify("access-token")).thenReturn(Optional.of(claims));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/assignment/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .header("X-User-Id", "spoofed-user")
                        .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        StepVerifier.create(filter(exchange, nextExchange -> {
                    forwardedRequest.set(nextExchange.getRequest());
                    return Mono.empty();
                }))
                .verifyComplete();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("student@example.com");
        assertThat(headers.getFirst("X-Gateway-Authenticated")).isEqualTo("true");
    }

    @Test
    void privateRequestWithoutBearerTokenReturnsUnauthenticatedJsonAndStopsChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/assignment/problems").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter(exchange, nextExchange -> {
                    chainCalled.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(ErrorCode.UNAUTHENTICATED.getHttpStatusCode());
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":1401")
                .contains("\"path\":\"/api/assignment/problems\"");
    }

    @Test
    void publicGetRequestRemovesSpoofedGatewayHeadersWithoutVerifyingJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/blog/posts/public")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer attacker-token")
                        .header("X-User-Id", "spoofed-user")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        StepVerifier.create(filter(exchange, nextExchange -> {
                    forwardedRequest.set(nextExchange.getRequest());
                    return Mono.empty();
                }))
                .verifyComplete();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.containsKey("X-User-Id")).isFalse();
        assertThat(headers.containsKey("X-User-Role")).isFalse();
        assertThat(headers.containsKey("X-Gateway-Authenticated")).isFalse();
        verifyNoInteractions(jwtUtil);
    }

    private Mono<Void> filter(MockServerWebExchange exchange, GatewayFilterChain chain) {
        return authFilter.apply(new AuthFilter.Config()).filter(exchange, chain);
    }
}
