package com.lms.apigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.apigateway.configuration.AuthFilter;
import com.lms.apigateway.configuration.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiGatewayFunctionalTest {

    @Test
    void authenticatedClientRequestIsOffloadedBeforeForwardingToDownstreamService() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("student-1");
        when(claims.get("scope", String.class)).thenReturn("ROLE_STUDENT");
        when(claims.get("email", String.class)).thenReturn("student@example.com");
        when(jwtUtil.extractToken("Bearer access-token")).thenReturn(Optional.of("access-token"));
        when(jwtUtil.verify("access-token")).thenReturn(Optional.of(claims));

        AuthFilter authFilter = new AuthFilter(jwtUtil, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/assignment/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .build());
        AtomicReference<ServerHttpRequest> downstreamRequest = new AtomicReference<>();
        GatewayFilterChain downstreamService = nextExchange -> {
            downstreamRequest.set(nextExchange.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(authFilter.apply(new AuthFilter.Config()).filter(exchange, downstreamService))
                .verifyComplete();

        HttpHeaders headers = downstreamRequest.get().getHeaders();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("student-1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("student@example.com");
        assertThat(headers.getFirst("X-Gateway-Authenticated")).isEqualTo("true");
    }
}
