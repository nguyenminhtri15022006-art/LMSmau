package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.gemini.GeminiRequest;
import com.lms.chatbotservice.dto.gemini.GeminiResponse;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.service.GeminiClientService;
import com.lms.chatbotservice.service.OpenCodeClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClientServiceImpl implements GeminiClientService {

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final OpenCodeClientService openCodeClientService;

    // ── UC1, UC2, UC3 — Streaming ─────────────────────────

    @Override
    public Flux<String> streamChat(List<GeminiMessage> messages) {
        if (useOpenCode()) {
            return openCodeClientService.streamChat(messages);
        }

        GeminiRequest request = buildChatRequest(messages);
        String url = buildStreamUrl();

        log.debug("Calling Gemini stream API: {}", url);

        return geminiWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::extractTextFromChunk)
                .filter(text -> !text.isBlank())
                .timeout(geminiProperties.getStreamTimeout())
                .retryWhen(Retry.backoff(geminiProperties.getMaxRetry(), geminiProperties.getRetryBackoff())
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> log.warn(
                                "Gemini stream retry #{} due to: {}",
                                signal.totalRetries() + 1,
                                signal.failure().getMessage()))
                        .onRetryExhaustedThrow((spec, signal) -> {
                            Throwable cause = signal.failure();
                            if (cause instanceof WebClientResponseException e) {
                                log.error("Gemini stream retry exhausted: status={}, body={}",
                                        e.getStatusCode(), e.getResponseBodyAsString());
                            } else {
                                log.error("Gemini stream retry exhausted", cause);
                            }
                            return new ChatbotException(ErrorCode.LLM_API_ERROR);
                        }))
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("Gemini API HTTP error: status={}, body={}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                    return new ChatbotException(ErrorCode.LLM_API_ERROR);
                })
                .onErrorMap(java.util.concurrent.TimeoutException.class, e -> {
                    log.error("Gemini API timed out after {}", geminiProperties.getStreamTimeout());
                    return new ChatbotException(ErrorCode.LLM_TIMEOUT);
                })
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof ChatbotException) return e;
                    log.error("Unexpected error calling Gemini", e);
                    return new ChatbotException(ErrorCode.LLM_API_ERROR);
                });
    }

    // ── UC4 — Structured JSON Output ──────────────────────

    @Override
    public String generateJson(List<GeminiMessage> messages) {
        if (useOpenCode()) {
            return openCodeClientService.generateJson(messages);
        }

        GeminiRequest request = buildGenerateRequest(messages);
        String url = buildGenerateUrl();

        log.debug("Calling Gemini generate API: {}", url);

        try {
            GeminiResponse response = geminiWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(geminiProperties.getGenerateTimeout())
                    .retryWhen(Retry.backoff(geminiProperties.getMaxRetry(), geminiProperties.getRetryBackoff())
                            .filter(this::isRetryable))
                    .block();

            if (response == null
                    || response.getCandidates() == null
                    || response.getCandidates().isEmpty()) {
                throw new ChatbotException(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
            }

            // Lấy text từ candidate đầu tiên
            logUsageMetadata("generateJson", response.getUsageMetadata());

            GeminiMessage content = response.getCandidates().get(0).getContent();
            if (content == null
                    || content.getParts() == null
                    || content.getParts().isEmpty()) {
                throw new ChatbotException(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
            }

            String jsonText = content.getParts().get(0).getText();
            log.debug("Gemini JSON response length: {}", jsonText.length());
            return jsonText;

        } catch (ChatbotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Gemini generate API", e);
            throw new ChatbotException(ErrorCode.LLM_API_ERROR);
        }
    }

    // ── Request builders ──────────────────────────────────

    private GeminiRequest buildChatRequest(List<GeminiMessage> messages) {
        return GeminiRequest.builder()
                .contents(messages)
                .generationConfig(GeminiRequest.GenerationConfig.builder()
                        .maxOutputTokens(geminiProperties.getMaxTokens())
                        .temperature(geminiProperties.getTemperatureChat())
                        .build())
                .build();
    }

    private GeminiRequest buildGenerateRequest(List<GeminiMessage> messages) {
        return GeminiRequest.builder()
                .contents(messages)
                .generationConfig(GeminiRequest.GenerationConfig.builder()
                        .maxOutputTokens(geminiProperties.getMaxTokens())
                        .temperature(geminiProperties.getTemperatureGenerate())
                        .responseMimeType("application/json")
                        .build())
                .build();
    }

    // ── URL builders ──────────────────────────────────────

    // Streaming: POST /gemini-2.0-flash:streamGenerateContent?key=...
    private String buildStreamUrl() {
        return "/" + geminiProperties.getModel()
                + ":streamGenerateContent?key="
                + geminiProperties.getApiKey()
                + "&alt=sse";
    }

    // Non-streaming: POST /gemini-2.0-flash:generateContent?key=...
    private String buildGenerateUrl() {
        return "/" + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();
    }

    // ── Response parsing ──────────────────────────────────

    // Mỗi chunk SSE từ Gemini có dạng:
    // data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}]}
    // Cần extract phần text ra
    private Flux<String> extractTextFromChunk(String chunk) {
        try {
            // Bỏ prefix "data: " nếu có
            String json = chunk.startsWith("data: ")
                    ? chunk.substring(6).trim()
                    : chunk.trim();

            if (json.isEmpty() || json.equals("[DONE]")) {
                return Flux.empty();
            }

            JsonNode root = objectMapper.readTree(json);
            logUsageMetadata("streamChat", root.path("usageMetadata"));
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                return Flux.empty();
            }

            JsonNode parts = candidates.get(0)
                    .path("content")
                    .path("parts");

            if (parts.isEmpty()) {
                return Flux.empty();
            }

            String text = parts.get(0).path("text").asText("");
            return Flux.just(text);

        } catch (Exception e) {
            log.warn("Failed to parse chunk: {}", chunk);
            return Flux.empty();
        }
    }

    // Chỉ retry khi lỗi có thể thử lại được
    // 429 Too Many Requests, 503 Service Unavailable
    // Không retry 400 Bad Request (sai request), 401 Unauthorized (sai key)
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            return status == 429 || status == 503;
        }
        return false;
    }

    private void logUsageMetadata(String operation, GeminiResponse.UsageMetadata usageMetadata) {
        if (usageMetadata == null) return;

        log.info(
                "Gemini usage: operation={}, model={}, promptTokens={}, candidateTokens={}, totalTokens={}",
                operation,
                geminiProperties.getModel(),
                usageMetadata.getPromptTokenCount(),
                usageMetadata.getCandidatesTokenCount(),
                usageMetadata.getTotalTokenCount()
        );
    }

    private void logUsageMetadata(String operation, JsonNode usageMetadata) {
        if (usageMetadata == null || usageMetadata.isMissingNode() || usageMetadata.isNull()) return;

        log.info(
                "Gemini usage: operation={}, model={}, promptTokens={}, candidateTokens={}, totalTokens={}",
                operation,
                geminiProperties.getModel(),
                tokenCount(usageMetadata, "promptTokenCount"),
                tokenCount(usageMetadata, "candidatesTokenCount"),
                tokenCount(usageMetadata, "totalTokenCount")
        );
    }

    private Integer tokenCount(JsonNode usageMetadata, String fieldName) {
        JsonNode value = usageMetadata.path(fieldName);
        return value.isNumber() ? value.asInt() : null;
    }

    private boolean useOpenCode() {
        String provider = geminiProperties.getProvider();
        if (provider != null && !provider.isBlank()) {
            return "opencode".equalsIgnoreCase(provider);
        }

        String model = geminiProperties.getModel() == null ? "" : geminiProperties.getModel().toLowerCase();
        String baseUrl = geminiProperties.getBaseUrl() == null ? "" : geminiProperties.getBaseUrl().toLowerCase();
        return !model.startsWith("gemini")
                && !baseUrl.contains("generativelanguage.googleapis.com");
    }
}
