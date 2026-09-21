package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.response.ChatSessionResponse;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatSessionService {

    ChatSession createSession(String userId, ContextType contextType, String contextRefId, String firstMessage);

    ChatSession getSessionById(String sessionId);

    ChatSession findActiveSession(String userId, ContextType contextType, String contextRefId);

    void incrementMessageCount(String sessionId, int count);

    void archiveSession(String sessionId);

    void archiveSessionByUser(String sessionId, String userId);

    void reactivateSession(String sessionId);

    void archiveStaleSessions();

    Page<ChatSessionResponse> getSessionsByUser(String userId, Pageable pageable);
}
