package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GatewayAuthentication;
import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final HistoryManagerService historyManagerService;
    private final ContextBuilderService contextBuilderService;
    private final PromptEngineService promptEngineService;
    private final GeminiClientService geminiClientService;

    @Override
    public Flux<String> chat(ChatRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        // Bước 1: Resolve session
        ChatSession session = historyManagerService.resolveSession(
                request.getContextType(),
                request.getContextRefId(),
                request.getSessionId(),
                request.getMessage());

        String sessionId = session.getId();

        // Bước 2: Lấy history từ Redis/MongoDB
        List<ChatMessage> history = historyManagerService
                .getRecentMessages(sessionId);

        // Bước 3: Build domain context từ Feign Client
        DomainContext context = contextBuilderService.build(
                request.getContextRefId(),
                request.getContextType(),
                userId);

        // Bước 4: Build prompt 4 lớp
        List<GeminiMessage> messages = promptEngineService.buildChatPrompt(
                request.getMessage(),
                context,
                history,
                request.getContextType());

        // Bước 5: Gửi sessionId trước như event đầu tiên,
        // sau đó stream text chunks từ Gemini
        StringBuilder responseBuilder = new StringBuilder();

        Flux<String> textStream = geminiClientService.streamChat(messages)
                .doOnNext(chunk -> responseBuilder.append(chunk))
                .doOnComplete(() -> {
                    String fullResponse = responseBuilder.toString();
                    if (!fullResponse.isBlank()) {
                        historyManagerService.saveExchange(
                                sessionId,
                                userId,
                                request.getMessage(),
                                fullResponse);
                        log.debug("Saved exchange for session {}", sessionId);
                    }
                })
                .doOnError(e ->
                        log.error("Stream error for session {}", sessionId, e));

        // Event đầu tiên mang sessionId để client biết session đang dùng
        // Format: "[SESSION:sessionId]" — client parse prefix này rồi bỏ qua khi render
        Flux<String> sessionEvent = Flux.just("[SESSION:" + sessionId + "]");

        return Flux.concat(sessionEvent, textStream);
    }
}