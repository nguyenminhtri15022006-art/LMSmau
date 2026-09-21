package com.lms.chatbotservice.service;

import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;

import java.util.List;

public interface HistoryManagerService {

    ChatSession resolveSession(ContextType contextType, String contextRefId, String sessionId, String firstMessage);

    List<ChatMessage> getRecentMessages(String sessionId);

    void saveExchange(String sessionId, String userId, String userMessage, String modelResponse);

}
