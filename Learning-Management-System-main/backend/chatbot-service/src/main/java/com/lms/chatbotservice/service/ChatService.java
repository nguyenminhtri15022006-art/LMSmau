package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.request.ChatRequest;
import reactor.core.publisher.Flux;

public interface ChatService {

    // UC1, UC2, UC3 — trả về stream text
    Flux<String> chat(ChatRequest request);
}