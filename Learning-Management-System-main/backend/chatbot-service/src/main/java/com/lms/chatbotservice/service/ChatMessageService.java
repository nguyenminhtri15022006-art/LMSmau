package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.response.ChatMessageResponse;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.MessageRole;

import java.util.List;

public interface ChatMessageService {

    ChatMessage saveMessage(String sessionId, String userId, MessageRole role, String content);

    List<ChatMessage> getRecentMessages(String sessionId, int limit);

    void deleteByUserId(String userId);

    List<ChatMessageResponse> getMessageResponses(String sessionId);
}
