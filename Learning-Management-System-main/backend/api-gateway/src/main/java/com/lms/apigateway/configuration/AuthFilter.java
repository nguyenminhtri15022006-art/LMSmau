package com.lms.apigateway.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.apigateway.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * AuthFilter — Global Gateway Filter thực hiện Token Offloading.
 * <p>
 * Nhiệm vụ:
 * 1. Đọc Authorization header từ request
 * 2. Verify JWT bằng JwtUtil
 * 3. Extract userId, role, email từ claims
 * 4. Gắn vào header mới: X-User-Id, X-User-Role, X-User-Email
 * 5. XÓA Authorization header gốc trước khi forward xuống service
 * <p>
 * Dùng dạng Named Filter để config trong application.yaml:
 * filters:
 * - AuthFilter
 */

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            //get header from request
            ServerHttpRequest request = exchange.getRequest();
            if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublicRequest(request)) {
                ServerHttpRequest publicRequest = request.mutate()
                        .headers(this::removeGatewayAuthHeaders)
                        .build();
                return chain.filter(exchange.mutate().request(publicRequest).build());
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // get token from headers
            Optional<String> tokenOpt = jwtUtil.extractToken(authHeader);
            if (tokenOpt.isEmpty()) {
                return writeError(exchange, ErrorCode.UNAUTHENTICATED);
            }

            //verify token
            Optional<Claims> claimsOpt = jwtUtil.verify(tokenOpt.get());
            if (claimsOpt.isEmpty()) {
                return writeError(exchange, ErrorCode.UNAUTHENTICATED);
            }

            Claims claims = claimsOpt.get();
            String userId = claims.getSubject();
            String role = claims.get("scope", String.class);
            String email = claims.get("email", String.class);

            log.debug("Gateway xác thực thành công: userId={} role={} path={}",
                    userId, role, request.getPath());
            /*
             * Tạo request mới với:
             * - Xóa Authorization header gốc (chứa JWT thô)
             * - Thêm X-User-Id, X-User-Role, X-User-Email (plain text)
             *
             * Các service downstream chỉ đọc 3 header này — không cần JWT.
             * Hacker không thể fake header này vì không thể đi qua Gateway.
             */
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        removeGatewayAuthHeaders(httpHeaders);
                        httpHeaders.set("X-User-Id", userId);
                        httpHeaders.set("X-User-Role", role != null ? role : "");
                        httpHeaders.set("X-User-Email", email != null ? email : "");
                        httpHeaders.set("X-Gateway-Authenticated", "true");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    /**
     * Viết error response dạng JSON — dùng Reactive API (không dùng HttpServletResponse).
     * Đây là điểm khác biệt chính giữa WebFlux và Servlet.
     */
    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getHttpStatusCode());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", errorCode.getHttpStatusCode(),
                "path", exchange.getRequest().getPath().value()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // Fallback nếu JSON serialization lỗi
            byte[] fallback = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(fallback);
            return response.writeWith(Mono.just(buffer));
        }
    }

    // Config class bắt buộc của AbstractGatewayFilterFactory
    public static class Config {
        // Có thể thêm config nếu cần (ví dụ: whitelist roles)
    }

    private void removeGatewayAuthHeaders(HttpHeaders httpHeaders) {
        httpHeaders.remove(HttpHeaders.AUTHORIZATION);
        httpHeaders.remove("X-User-Id");
        httpHeaders.remove("X-User-Role");
        httpHeaders.remove("X-User-Email");
        httpHeaders.remove("X-Gateway-Authenticated");
    }

    private boolean isPublicRequest(ServerHttpRequest request) {
        if (!HttpMethod.GET.equals(request.getMethod())) return false;

        String path = request.getPath().value();
        String normalizedPath = path.startsWith("/api/") ? path.substring(4) : path;

        return normalizedPath.equals("/blog/posts/public")
                || normalizedPath.startsWith("/blog/posts/public/")
                || normalizedPath.equals("/blog/tags")
                || normalizedPath.startsWith("/blog/tags/search")
                || normalizedPath.matches("^/blog/posts/[^/]+/comments$")
                || normalizedPath.equals("/course/categories")
                || normalizedPath.equals("/course/courses")
                || normalizedPath.matches("^/course/courses/(?!my$|enrolled$|users$)[^/]+$")
                || normalizedPath.matches("^/course/courses/[^/]+/(students|lessons)/count$")
                || normalizedPath.matches("^/course/courses/[^/]+/structure$")
                || normalizedPath.matches("^/course/courses/[^/]+/structure/lessons$");
    }
}
