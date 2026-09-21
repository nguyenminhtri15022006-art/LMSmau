package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final GeminiProperties geminiProperties;

    private static final String KEY_PREFIX = "rate:chatbot:";

    // Atomic check-and-increment via Lua script.
    // Returns the new counter value, or -1 if the limit is already reached.
    // Args: KEYS[1]=key, ARGV[1]=limit, ARGV[2]=ttlMillis
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    local count = current and tonumber(current) or 0
                    if count >= tonumber(ARGV[1]) then return -1 end
                    if current then
                        redis.call('INCR', KEYS[1])
                    else
                        redis.call('SET', KEYS[1], '1', 'PX', tonumber(ARGV[2]))
                    end
                    return count + 1
                    """, Long.class);

    // Key: rate:chatbot:{userId}:{yyyyMMdd}
    private String buildKey(String userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now();
    }

    // TTL còn lại đến hết ngày (0h hôm sau)
    private Duration ttlUntilMidnight() {
        LocalDateTime midnight = LocalDate.now()
                .plusDays(1)
                .atTime(LocalTime.MIDNIGHT);
        return Duration.between(LocalDateTime.now(), midnight);
    }

    @Override
    public void checkAndIncrement(String userId, String role) {
        String key = buildKey(userId);
        int limit = getLimitByRole(role);
        long ttlMs = ttlUntilMidnight().toMillis();

        Long result;
        try {
            result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(ttlMs));
        } catch (DataAccessException e) {
            log.warn("Redis rate limit unavailable, fail-open for user {} (role={}): {} - {}",
                    userId, role, e.getClass().getSimpleName(), e.getMessage());
            return;
        }

        if (result == null || result == -1L) {
            log.warn("Rate limit exceeded for user {} (role={}, limit={})",
                    userId, role, limit);
            throw new ChatbotException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        log.debug("Rate limit check passed for user {} ({}/{})", userId, result, limit);
    }

    @Override
    public int getUsageToday(String userId) {
        try {
            String value = redisTemplate.opsForValue().get(buildKey(userId));
            return value == null ? 0 : Integer.parseInt(value);
        } catch (DataAccessException e) {
            log.warn("Redis usage lookup unavailable for user {}, returning 0: {} - {}",
                    userId, e.getClass().getSimpleName(), e.getMessage());
            return 0;
        }
    }

    @Override
    public int getLimitByRole(String role) {
        if (role == null) return geminiProperties.getRateLimit().getStudent();
        // JWT scope có prefix "ROLE_" — so sánh không phân biệt hoa thường và bỏ prefix
        String normalized = role.toUpperCase().replace("ROLE_", "");
        return "INSTRUCTOR".equals(normalized)
                ? geminiProperties.getRateLimit().getInstructor()
                : geminiProperties.getRateLimit().getStudent();
    }
}
