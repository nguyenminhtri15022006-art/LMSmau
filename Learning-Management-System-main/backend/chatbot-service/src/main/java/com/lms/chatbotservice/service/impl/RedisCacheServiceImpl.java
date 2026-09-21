package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String KEY_PREFIX = "chat:session:";
    private static final String KEY_SUFFIX = ":messages";

    @Value("${chatbot.cache.redis.ttl-hours:24}")
    long ttlHours;

    @Value("${chatbot.cache.redis.max-messages:20}")
    int maxMessages;

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId + KEY_SUFFIX;
    }

    @Override
    public void saveMessages(String sessionId, List<ChatMessage> messages) {
        String key = buildKey(sessionId);

        try {
            List<ChatMessage> toCache = messages.size() > maxMessages
                    ? messages.subList(messages.size() - maxMessages, messages.size())
                    : messages;

            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, new ArrayList<Object>(toCache));
            redisTemplate.expire(key, Duration.ofHours(ttlHours));

            log.debug("Cached {} messages for session {}", toCache.size(), sessionId);
        } catch (DataAccessException e) {
            log.warn("Redis cache save skipped for session {}: {} - {}",
                    sessionId, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        String key = buildKey(sessionId);

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(exists)) {
                log.debug("Cache miss for session {}", sessionId);
                return null;
            }

            List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null || raw.isEmpty()) {
                return null;
            }

            List<ChatMessage> messages = new ArrayList<>();
            for (Object item : raw) {
                ChatMessage message = redisObjectMapper.convertValue(item, ChatMessage.class);
                messages.add(message);
            }

            log.debug("Cache hit for session {}, {} messages", sessionId, messages.size());
            return messages;
        } catch (DataAccessException e) {
            log.warn("Redis cache read failed for session {}, falling back to MongoDB: {} - {}",
                    sessionId, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    @Override
    public void appendMessage(String sessionId, ChatMessage message) {
        String key = buildKey(sessionId);

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(exists)) {
                log.debug("Skip append; cache expired for session {}", sessionId);
                return;
            }

            redisTemplate.opsForList().rightPush(key, message);

            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > maxMessages) {
                redisTemplate.opsForList().leftPop(key);
            }

            redisTemplate.expire(key, Duration.ofHours(ttlHours));

            log.debug("Appended message to session {}, new size ~{}", sessionId, size);
        } catch (DataAccessException e) {
            log.warn("Redis cache append skipped for session {}: {} - {}",
                    sessionId, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void evict(String sessionId) {
        String key = buildKey(sessionId);
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache for session {}", sessionId);
        } catch (DataAccessException e) {
            log.warn("Redis cache eviction skipped for session {}: {} - {}",
                    sessionId, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
