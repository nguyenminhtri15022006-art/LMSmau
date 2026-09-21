package com.lms.chatbotservice.repository;

import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    // Lấy danh sách session của user, lọc theo status, sort mới nhất
    Page<ChatSession> findByUserIdAndStatusOrderByLastMessageAtDesc(
            String userId, SessionStatus status, Pageable pageable);

    // Lấy toàn bộ session (ACTIVE + ARCHIVED) để sinh viên xem lại lịch sử
    Page<ChatSession> findByUserIdOrderByLastMessageAtDesc(
            String userId, Pageable pageable);

    // Tìm session active theo user + contextType + contextRefId
    // Dùng để tiếp tục hội thoại cũ thay vì tạo session mới
    Optional<ChatSession> findByUserIdAndContextTypeAndContextRefIdAndStatus(
            String userId, ContextType contextType,
            String contextRefId, SessionStatus status);

    // Background job tìm session cần archive
    List<ChatSession> findByStatusAndLastMessageAtBefore(
            SessionStatus status, LocalDateTime cutoff);

    // Atomic increment totalMessages + update lastMessageAt — tránh read-modify-write
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'totalMessages': ?1 }, '$set': { 'lastMessageAt': ?2 } }")
    void incrementMessageCount(String sessionId, int count, LocalDateTime now);
}