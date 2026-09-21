package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceImplTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;
    @Mock
    ListOperations<String, Object> listOperations;

    RedisCacheServiceImpl redisCacheService;

    @BeforeEach
    void setUp() {
        redisCacheService = new RedisCacheServiceImpl(redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(redisCacheService, "ttlHours", 24L);
        ReflectionTestUtils.setField(redisCacheService, "maxMessages", 20);
    }

    @Test
    void getMessagesReturnsNullWhenRedisTimesOut() {
        when(redisTemplate.hasKey(key())).thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThat(redisCacheService.getMessages("session-1")).isNull();
    }

    @Test
    void saveMessagesDoesNotThrowWhenRedisTimesOut() {
        when(redisTemplate.delete(key())).thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThatNoException()
                .isThrownBy(() -> redisCacheService.saveMessages("session-1", List.of(message("m1"))));
    }

    @Test
    void appendMessageDoesNotThrowWhenRedisTimesOut() {
        when(redisTemplate.hasKey(key())).thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThatNoException()
                .isThrownBy(() -> redisCacheService.appendMessage("session-1", message("m1")));
    }

    @Test
    void evictDoesNotThrowWhenRedisTimesOut() {
        when(redisTemplate.delete(key())).thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertThatNoException()
                .isThrownBy(() -> redisCacheService.evict("session-1"));
    }

    @Test
    void getMessagesReturnsCachedMessagesWhenRedisIsAvailable() {
        ChatMessage message = message("m1");
        when(redisTemplate.hasKey(key())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(key(), 0, -1)).thenReturn(List.of(message));

        assertThat(redisCacheService.getMessages("session-1"))
                .extracting(ChatMessage::getId)
                .containsExactly("m1");
    }

    private String key() {
        return "chat:session:session-1:messages";
    }

    private ChatMessage message(String id) {
        return ChatMessage.builder()
                .id(id)
                .role(MessageRole.USER)
                .content("hello")
                .build();
    }
}
