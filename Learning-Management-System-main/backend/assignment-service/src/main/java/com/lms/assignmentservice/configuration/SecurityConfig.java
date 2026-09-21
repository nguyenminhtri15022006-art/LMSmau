package com.lms.assignmentservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.assignmentservice.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Security Config cho Assignment Service — Token Offloading pattern.
 * <p>
 * Assignment Service KHÔNG verify JWT nữa.
 * API Gateway đã verify và forward userId/role qua header.
 * Service này chỉ đọc header X-User-Id, X-User-Role.
 * <p>
 * Lưu ý: Assignment Service vẫn là nơi DUY NHẤT SIGN JWT (trong AuthenticationService).
 * Nhưng nó KHÔNG verify JWT của chính mình nữa — Gateway làm việc đó.
 */

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/internal/**",
            "/actuator/**",
            "/error"
    };

    private final ObjectMapper objectMapper;

    /*
     * kiểm tra các endpoint public thì cho phép đi tiếp -> controller
     * còn các endpoint còn lại kiểm tra jwt token hợp lệ -> controller
     * không hợp lệ, trả về json với code 401, chưa xác thực (unauthenticated)
     * */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.sessionManagement(
                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .requestMatchers("/internal/**").permitAll()
                                .anyRequest().authenticated());

        httpSecurity.addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, ErrorCode.INVALID_PATH))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, ErrorCode.ACCESS_DENIED)));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    /**
     * Filter đọc header X-User-Id và X-User-Role từ Gateway.
     * <p>
     * Với public endpoints (đã match ở trên), SecurityContext không cần set
     * vì permitAll() sẽ bỏ qua authentication check.
     * <p>
     * Với private endpoints, nếu không có X-User-Id → 401.
     * (Trường hợp này xảy ra khi có người gọi thẳng vào service, bypass Gateway)
     */
    @Bean
    public OncePerRequestFilter gatewayHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path = request.getServletPath();
                String method = request.getMethod();

                // bỏ qua cho public endpoint vs internal endpoint
                if (isPublicPath(path)) {
                    log.debug("[GatewayFilter] ✓ Public path, bypass filter: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }

                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                String email = request.getHeader("X-User-Email");

                if (userId == null || userId.isBlank()) {
                    log.warn("[GatewayFilter] X-User-Id header is NULL or BLANK for path: {} → 401", path);
                    log.debug("[GatewayFilter] All headers: {}",
                            java.util.Collections.list(request.getHeaderNames()));
                    writeError(response, ErrorCode.UNAUTHENTICATED);
                    return;
                }

                // Build Authentication from Header
                List<GrantedAuthority> authorities = role != null && !role.isBlank()
                        ? List.of(new SimpleGrantedAuthority(role))
                        : List.of();

                GatewayAuthentication auth = new GatewayAuthentication(userId, email, role, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("[GatewayFilter] ✓ Auth OK - userId: {}, role: {}, email: {}, path: {}",
                        userId, role, email, path);

                filterChain.doFilter(request, response);
            }

            private boolean isPublicPath(String path) {
                for (String pattern : PUBLIC_ENDPOINTS) {
                    String cleanPattern = pattern.replace("/**", "");
                    if (path.startsWith(cleanPattern) || path.equals(cleanPattern)) return true;
                }
                return false;
            }
        };
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", errorCode.getHttpStatusCode()
        );
        response.getOutputStream().write(objectMapper.writeValueAsBytes(body));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
