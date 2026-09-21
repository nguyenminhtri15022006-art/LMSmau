package com.lms.chatbotservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
    private Thinking thinking;
    private int maxTokens;
    private double temperatureChat;
    private double temperatureGenerate;
    private int maxRetry;
    private Duration retryBackoff;
    private Duration streamTimeout;
    private Duration generateTimeout;
    private RateLimit rateLimit;

    @Data
    public static class RateLimit {
        private int student;
        private int instructor;
    }

    @Data
    public static class Thinking {
        private String type;
    }
}
