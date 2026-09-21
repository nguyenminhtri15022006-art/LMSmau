package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GatewayAuthentication;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.enums.SessionStatus;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.service.ChatMessageService;
import com.lms.chatbotservice.service.ChatSessionService;
import com.lms.chatbotservice.service.HistoryManagerService;
import com.lms.chatbotservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryManagerServiceImpl implements HistoryManagerService {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final RedisCacheService redisCacheService;

    @Value("${chatbot.history.recent-limit:20}")
    int historyLimit;

    @Override
    public ChatSession resolveSession(ContextType contextType,
                                      String contextRefId, String sessionId, String firstMessage) {

        String userId = GatewayAuthentication.currentUserId();

        // Trường hợp 1: Client gửi kèm sessionId → tiếp tục hội thoại cụ thể
        if (sessionId != null && !sessionId.isBlank()) {
            ChatSession session = sessionService.getSessionById(sessionId);

            if (!session.getUserId().equals(userId)) {
                throw new ChatbotException(ErrorCode.SESSION_NOT_OWNER);
            }

            // Nếu session đang ARCHIVED, reactivate để user tiếp tục từ lịch sử cũ.
            // Đồng thời archive session ACTIVE hiện tại (nếu có) của cùng context
            // để tránh có 2 session ACTIVE cùng lúc.
            if (session.getStatus() == SessionStatus.ARCHIVED) {
                ChatSession currentActive = sessionService.findActiveSession(
                        userId, session.getContextType(), session.getContextRefId());
                if (currentActive != null && !currentActive.getId().equals(sessionId)) {
                    sessionService.archiveSession(currentActive.getId());
                }
                sessionService.reactivateSession(sessionId);
                log.debug("Reactivated archived session {} for user {}", sessionId, userId);
            }

            return sessionService.getSessionById(sessionId);
        }

        // Trường hợp 2: Tìm session ACTIVE đang có
        ChatSession existing = sessionService.findActiveSession(userId, contextType, contextRefId);
        if (existing != null) {
            log.debug("Reusing existing session {} for user {}", existing.getId(), userId);
            return existing;
        }

        // Trường hợp 3: Tạo session mới
        log.debug("Creating new session for user {} contextType {}", userId, contextType);
        return sessionService.createSession(userId, contextType, contextRefId, firstMessage);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId) {

        // Thử Redis trước
        List<ChatMessage> cached = redisCacheService.getMessages(sessionId);
        if (cached != null) {
            log.debug("Redis hit — {} messages for session {}", cached.size(), sessionId);
            return cached;
        }

        // Cache miss → query MongoDB (trả về DESC, cần reverse về ASC cho Gemini)
        log.debug("Redis miss — loading from MongoDB for session {}", sessionId);
        List<ChatMessage> messages = messageService.getRecentMessages(sessionId, historyLimit);

        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // Wrap mutable để tránh UnsupportedOperationException trên immutable list
        List<ChatMessage> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);

        // Warm-up Redis với thứ tự chronological (ASC)
        redisCacheService.saveMessages(sessionId, ordered);

        return ordered;
    }

    @Override
    public void saveExchange(String sessionId, String userId, String userMessage, String modelResponse) {

        ChatMessage userMsg = messageService.saveMessage(sessionId, userId, MessageRole.USER, userMessage);
        ChatMessage modelMsg = messageService.saveMessage(sessionId, userId, MessageRole.MODEL, modelResponse);

        redisCacheService.appendMessage(sessionId, userMsg);
        redisCacheService.appendMessage(sessionId, modelMsg);

        sessionService.incrementMessageCount(sessionId, 2);

        log.debug("Saved exchange for session {} user {}", sessionId, userId);
    }
}
