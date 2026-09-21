package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceImplTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;

    RateLimiterServiceImpl rateLimiterService;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties();
        GeminiProperties.RateLimit rateLimit = new GeminiProperties.RateLimit();
        rateLimit.setStudent(3);
        rateLimit.setInstructor(10);
        properties.setRateLimit(rateLimit);
        rateLimiterService = new RateLimiterServiceImpl(redisTemplate, properties);
    }

    @Test
    void getLimitByRoleNormalizesRolePrefix() {
        assertThat(rateLimiterService.getLimitByRole("ROLE_INSTRUCTOR")).isEqualTo(10);
        assertThat(rateLimiterService.getLimitByRole("student")).isEqualTo(3);
        assertThat(rateLimiterService.getLimitByRole(null)).isEqualTo(3);
    }

    @Test
    void checkAndIncrementPassesRoleLimitToRedisScript() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), eq("10"), any(String.class)))
                .thenReturn(1L);

        rateLimiterService.checkAndIncrement("instructor-1", "ROLE_INSTRUCTOR");

        verify(redisTemplate).execute(any(RedisScript.class), any(List.class), eq("10"), any(String.class));
    }

    @Test
    void checkAndIncrementThrowsWhenRedisReturnsLimitExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), eq("3"), any(String.class)))
                .thenReturn(-1L);

        assertThatThrownBy(() -> rateLimiterService.checkAndIncrement("student-1", "STUDENT"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    void checkAndIncrementFailsOpenWhenRedisTimesOut() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), eq("3"), any(String.class)))
                .thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThatNoException()
                .isThrownBy(() -> rateLimiterService.checkAndIncrement("student-1", "STUDENT"));
    }

    @Test
    void getUsageTodayReturnsZeroWhenRedisKeyMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class))).thenReturn(null);

        assertThat(rateLimiterService.getUsageToday("student-1")).isZero();
    }

    @Test
    void getUsageTodayParsesRedisValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class))).thenReturn("2");

        assertThat(rateLimiterService.getUsageToday("student-1")).isEqualTo(2);
    }

    @Test
    void getUsageTodayReturnsZeroWhenRedisTimesOut() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class)))
                .thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThat(rateLimiterService.getUsageToday("student-1")).isZero();
    }
}
