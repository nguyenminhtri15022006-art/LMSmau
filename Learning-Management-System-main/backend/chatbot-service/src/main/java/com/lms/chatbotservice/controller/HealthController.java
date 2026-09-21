package com.lms.chatbotservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class HealthController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/health/redis")
    public String checkRedis() {
        try {
            redisTemplate.opsForValue().set("health:check", "ok");
            String value = redisTemplate.opsForValue().get("health:check");
            return "Redis OK: " + value;
        } catch (Exception e) {
            return "Redis ERROR: " + e.getMessage();
        }
    }
}
