package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.enums.ContextType;

public interface ContextBuilderService {

    // Gọi Feign Client tương ứng theo contextType:
    // GENERAL  → chỉ userId, không gọi thêm service nào
    // PROBLEM  → AssignmentClient: lấy đề bài + lần nộp gần nhất
    // COURSE   → CourseClient + AssignmentClient: gọi song song
    // GENERATE → không gọi thêm, context do caller tự truyền vào prompt
    DomainContext build(String contextRefId, ContextType contextType);

    DomainContext build(String contextRefId, ContextType contextType, String userId);
}