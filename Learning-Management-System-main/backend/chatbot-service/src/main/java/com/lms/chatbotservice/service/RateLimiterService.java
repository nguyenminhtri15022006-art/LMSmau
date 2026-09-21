package com.lms.chatbotservice.service;

public interface RateLimiterService {

    // Kiểm tra còn lượt không, nếu còn thì tăng counter luôn
    // Ném RATE_LIMIT_EXCEEDED nếu hết
    void checkAndIncrement(String userId, String role);

    // Lấy số lượt đã dùng hôm nay
    int getUsageToday(String userId);

    // Lấy giới hạn theo role
    int getLimitByRole(String role);
}