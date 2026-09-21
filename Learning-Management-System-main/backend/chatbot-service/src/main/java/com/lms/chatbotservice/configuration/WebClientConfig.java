package com.lms.chatbotservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${chatbot.web-client.max-in-memory-size-bytes:2097152}")
    int maxInMemorySizeBytes;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(maxInMemorySizeBytes));
    }

    @Bean
    public WebClient geminiWebClient(GeminiProperties geminiProperties, WebClient.Builder webClientBuilder) {
        return webClientBuilder.clone()
                .baseUrl(geminiProperties.getBaseUrl())
                .build();
    }
}
