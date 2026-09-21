package com.lms.chatbotservice.repository;

import com.lms.chatbotservice.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // Lấy N tin nhắn gần nhất của session, sort mới nhất trước
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(
            String sessionId, Pageable pageable);

    // Đếm số tin nhắn — dùng để sync total_messages nếu cần
    long countBySessionId(String sessionId);

    // Xóa toàn bộ tin nhắn theo user (GDPR)
    void deleteByUserId(String userId);
}