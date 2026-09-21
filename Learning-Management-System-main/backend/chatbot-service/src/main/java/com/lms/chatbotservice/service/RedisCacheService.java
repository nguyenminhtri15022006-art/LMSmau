package com.lms.chatbotservice.service;

import com.lms.chatbotservice.entity.ChatMessage;

import java.util.List;

public interface RedisCacheService {

    // Lưu toàn bộ danh sách tin nhắn vào Redis, TTL 24h
    // Dùng khi load lần đầu từ MongoDB (cache warm-up)
    void saveMessages(String sessionId, List<ChatMessage> messages);

    // Lấy danh sách tin nhắn từ Redis
    // Trả về null nếu cache miss (key không tồn tại hoặc hết TTL)
    List<ChatMessage> getMessages(String sessionId);

    // Thêm một tin nhắn mới vào cuối list trong Redis
    // Dùng sau mỗi lần saveExchange để giữ cache đồng bộ
    void appendMessage(String sessionId, ChatMessage message);

    // Xóa cache của session
    // Dùng khi session bị archive
    void evict(String sessionId);
}