package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.response.ChatSessionResponse;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.SessionStatus;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.mapper.ChatSessionMapper;
import com.lms.chatbotservice.repository.ChatSessionRepository;
import com.lms.chatbotservice.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatSessionMapper sessionMapper;

    @Value("${chatbot.session.archive-after-days:30}")
    int archiveAfterDays;

    @Value("${chatbot.session.title-max-length:50}")
    int titleMaxLength;

    @Override
    public ChatSession createSession(String userId, ContextType contextType,
                                     String contextRefId, String firstMessage) {

        // Tự động sinh title từ 50 ký tự đầu của tin nhắn đầu tiên
        String title = firstMessage.length() > titleMaxLength
                ? firstMessage.substring(0, titleMaxLength) + "..."
                : firstMessage;

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .contextType(contextType)
                .contextRefId(contextRefId)
                .title(title)
                .status(SessionStatus.ACTIVE)
                .totalMessages(0)
                .lastMessageAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        ChatSession saved = sessionRepository.save(session);
        log.debug("Created session {} for user {} contextType {}",
                saved.getId(), userId, contextType);
        return saved;
    }

    @Override
    public ChatSession getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatbotException(ErrorCode.SESSION_NOT_FOUND));
    }

    @Override
    public ChatSession findActiveSession(String userId, ContextType contextType, String contextRefId) {
        return sessionRepository
                .findByUserIdAndContextTypeAndContextRefIdAndStatus(
                        userId, contextType, contextRefId, SessionStatus.ACTIVE)
                .orElse(null);  // null = không tìm thấy → tạo mới
    }

    @Override
    public void incrementMessageCount(String sessionId, int count) {
        sessionRepository.incrementMessageCount(sessionId, count, LocalDateTime.now());
    }

    @Override
    public void archiveSession(String sessionId) {
        ChatSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.ARCHIVED);
        sessionRepository.save(session);
        log.debug("Archived session {}", sessionId);
    }

    @Override
    public void reactivateSession(String sessionId) {
        ChatSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(session);
        log.debug("Reactivated session {}", sessionId);
    }

    @Override
    public void archiveSessionByUser(String sessionId, String userId) {
        ChatSession session = getSessionById(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new ChatbotException(ErrorCode.SESSION_NOT_OWNER);
        }
        session.setStatus(SessionStatus.ARCHIVED);
        sessionRepository.save(session);
        log.debug("User {} archived session {}", userId, sessionId);
    }

    @Override
    @Scheduled(cron = "${chatbot.session.archive-cron:0 0 0 * * *}")
    public void archiveStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(archiveAfterDays);

        List<ChatSession> staleSessions = sessionRepository
                .findByStatusAndLastMessageAtBefore(SessionStatus.ACTIVE, cutoff);

        staleSessions.forEach(session -> {
            session.setStatus(SessionStatus.ARCHIVED);
            log.debug("Auto-archiving stale session {}", session.getId());
        });

        sessionRepository.saveAll(staleSessions);
        log.info("Auto-archived {} stale sessions", staleSessions.size());
    }

    @Override
    public Page<ChatSessionResponse> getSessionsByUser(String userId, Pageable pageable) {
        return sessionRepository
                .findByUserIdOrderByLastMessageAtDesc(userId, pageable)
                .map(sessionMapper::toChatSessionResponse);
    }
}
