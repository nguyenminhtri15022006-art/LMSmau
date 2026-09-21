package com.lms.chatbotservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/error",
            "/internal/**"
    };

    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.sessionManagement(
                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .anyRequest().authenticated());

        httpSecurity.addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, ErrorCode.ACCESS_DENIED)));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public OncePerRequestFilter gatewayHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path = normalizedPath(request);

                if (isPublicPath(path)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                String email = request.getHeader("X-User-Email");

                if (userId == null || userId.isBlank()) {
                    log.warn("Request đến {} không có X-User-Id header — có thể bypass Gateway", path);
                    writeError(response, ErrorCode.UNAUTHENTICATED);
                    return;
                }

                List<GrantedAuthority> authorities = role != null && !role.isBlank()
                        ? List.of(new SimpleGrantedAuthority(role))
                        : List.of();

                GatewayAuthentication auth = new GatewayAuthentication(userId, email, role, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Chatbot xác minh header: userId: {}, Role: {}, Email: {}, Authorities: {}",
                        GatewayAuthentication.currentUserId(),
                        GatewayAuthentication.currentRole(),
                        GatewayAuthentication.currentEmail(),
                        authorities);

                filterChain.doFilter(request, response);
            }

            private boolean isPublicPath(String path) {
                for (String pattern : PUBLIC_ENDPOINTS) {
                    String cleanPattern = pattern.replace("/**", "");
                    if (path.startsWith(cleanPattern) || path.equals(cleanPattern)) return true;
                }
                return false;
            }

            private String normalizedPath(HttpServletRequest request) {
                String contextPath = request.getContextPath();
                String requestUri = request.getRequestURI();
                if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
                    requestUri = requestUri.substring(contextPath.length());
                }
                return requestUri.isBlank() ? "/" : requestUri;
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
                "status", errorCode.getHttpStatusCode().value()
        );
        response.getOutputStream().write(objectMapper.writeValueAsBytes(body));
    }
}
