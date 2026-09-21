package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.service.OpenCodeClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenCodeClientServiceImpl implements OpenCodeClientService {

    private final WebClient.Builder webClientBuilder;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> streamChat(List<GeminiMessage> messages) {
        String url = buildChatCompletionsUrl(geminiProperties.getBaseUrl());
        Map<String, Object> request = buildRequest(messages, true, false);

        log.debug("Calling OpenCode stream API: model={}, url={}", geminiProperties.getModel(), url);

        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(geminiProperties.getApiKey()))
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::extractTextFromChunk)
                .filter(text -> !text.isEmpty())
                .timeout(geminiProperties.getStreamTimeout())
                .retryWhen(Retry.backoff(geminiProperties.getMaxRetry(), geminiProperties.getRetryBackoff())
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> log.warn(
                                "OpenCode stream retry #{} due to: {}",
                                signal.totalRetries() + 1,
                                signal.failure().getMessage()))
                        .onRetryExhaustedThrow((spec, signal) -> {
                            Throwable cause = signal.failure();
                            if (cause instanceof WebClientResponseException e) {
                                log.error("OpenCode stream retry exhausted: status={}, body={}",
                                        e.getStatusCode(), e.getResponseBodyAsString());
                            } else {
                                log.error("OpenCode stream retry exhausted", cause);
                            }
                            return new ChatbotException(ErrorCode.LLM_API_ERROR);
                        }))
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("OpenCode API HTTP error: status={}, body={}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                    return new ChatbotException(ErrorCode.LLM_API_ERROR);
                })
                .onErrorMap(java.util.concurrent.TimeoutException.class, e -> {
                    log.error("OpenCode API timed out after {}", geminiProperties.getStreamTimeout());
                    return new ChatbotException(ErrorCode.LLM_TIMEOUT);
                })
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof ChatbotException) return e;
                    log.error("Unexpected error calling OpenCode API", e);
                    return new ChatbotException(ErrorCode.LLM_API_ERROR);
                });
    }

    @Override
    public String generateJson(List<GeminiMessage> messages) {
        String url = buildChatCompletionsUrl(geminiProperties.getBaseUrl());
        Map<String, Object> request = buildRequest(messages, false, true);

        log.debug("Calling OpenCode generate API: model={}, url={}", geminiProperties.getModel(), url);

        try {
            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(geminiProperties.getApiKey()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(geminiProperties.getGenerateTimeout())
                    .retryWhen(Retry.backoff(geminiProperties.getMaxRetry(), geminiProperties.getRetryBackoff())
                            .filter(this::isRetryable))
                    .block();

            String text = response == null ? "" : response.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (text.isBlank()) {
                throw new ChatbotException(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
            }

            return text;
        } catch (ChatbotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling OpenCode generate API", e);
            throw new ChatbotException(ErrorCode.LLM_API_ERROR);
        }
    }

    private Map<String, Object> buildRequest(
            List<GeminiMessage> messages,
            boolean stream,
            boolean jsonMode) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", geminiProperties.getModel());
        request.put("messages", messages.stream()
                .map(this::toOpenAiMessage)
                .filter(Objects::nonNull)
                .toList());
        request.put("stream", stream);
        request.put("max_tokens", geminiProperties.getMaxTokens());
        request.put("temperature", jsonMode
                ? geminiProperties.getTemperatureGenerate()
                : geminiProperties.getTemperatureChat());

        if (geminiProperties.getThinking() != null
                && geminiProperties.getThinking().getType() != null
                && !geminiProperties.getThinking().getType().isBlank()) {
            request.put("thinking", Map.of("type", geminiProperties.getThinking().getType()));
        }

        if (jsonMode) {
            request.put("response_format", Map.of("type", "json_object"));
        }

        return request;
    }

    private Map<String, String> toOpenAiMessage(GeminiMessage message) {
        if (message == null || message.getParts() == null || message.getParts().isEmpty()) {
            return null;
        }

        String content = message.getParts().stream()
                .map(part -> part == null ? "" : part.getText())
                .filter(Objects::nonNull)
                .reduce("", (left, right) -> left + right);

        if (content.isBlank()) {
            return null;
        }

        String role = "model".equalsIgnoreCase(message.getRole()) ? "assistant" : "user";
        return Map.of("role", role, "content", content);
    }

    private String buildChatCompletionsUrl(String baseUrl) {
        String chatCompletionsPath = "/chat/completions";
        String trimmed = trimTrailingSlash(baseUrl);
        if (trimmed.endsWith(chatCompletionsPath)) {
            trimmed = trimmed.substring(0, trimmed.length() - chatCompletionsPath.length());
        }
        return trimmed + chatCompletionsPath;
    }

    private Flux<String> extractTextFromChunk(String chunk) {
        try {
            String trimmed = chunk.trim();
            if (trimmed.startsWith("{")) {
                String text = extractDeltaText(trimmed);
                return text.isEmpty() ? Flux.empty() : Flux.just(text);
            }

            return Flux.fromIterable(chunk.lines()
                    .filter(line -> line.startsWith("data:"))
                    .map(this::extractSseData)
                    .filter(data -> !data.isEmpty() && !"[DONE]".equals(data.trim()))
                    .map(this::extractDeltaText)
                    .filter(text -> !text.isEmpty())
                    .toList());
        } catch (Exception e) {
            log.warn("Failed to parse OpenCode chunk: {}", chunk);
            return Flux.empty();
        }
    }

    private String extractSseData(String line) {
        String data = line.substring(5);
        return data.startsWith(" ") ? data.substring(1) : data;
    }

    private String extractDeltaText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode delta = root.path("choices").path(0).path("delta");

            String content = delta.path("content").asText("");
            if (!content.isEmpty()) {
                return content;
            }

            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            return status == 429 || status == 503;
        }
        return false;
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
