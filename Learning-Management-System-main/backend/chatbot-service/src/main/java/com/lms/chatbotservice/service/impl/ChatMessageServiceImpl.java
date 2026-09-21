package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.response.ChatMessageResponse;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.mapper.ChatMessageMapper;
import com.lms.chatbotservice.repository.ChatMessageRepository;
import com.lms.chatbotservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatMessageMapper messageMapper;

    @Value("${chatbot.history.response-limit:20}")
    int responseHistoryLimit;

    @Override
    public ChatMessage saveMessage(String sessionId, String userId,
                                   MessageRole role, String content) {

        // Ước tính token: content.length() / 4
        int tokenCount = Math.max(1, content.length() / 4);

        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .role(role)
                .content(content)
                .tokenCount(tokenCount)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = messageRepository.save(message);
        log.debug("Saved message {} role={} session={}",
                saved.getId(), role, sessionId);
        return saved;
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        return messageRepository.findBySessionIdOrderByCreatedAtDesc(
                sessionId, PageRequest.of(0, limit));
    }

    @Override
    public void deleteByUserId(String userId) {
        messageRepository.deleteByUserId(userId);
        log.info("Deleted all messages for user {}", userId);
    }

    @Override
    public List<ChatMessageResponse> getMessageResponses(String sessionId) {
        List<ChatMessage> messages = getRecentMessages(sessionId, responseHistoryLimit);
        // getRecentMessages returns DESC; reverse to chronological order for the UI
        List<ChatMessage> chronological = new ArrayList<>(messages);
        java.util.Collections.reverse(chronological);
        return chronological.stream()
                .map(messageMapper::toChatMessageResponse)
                .toList();
    }
}
