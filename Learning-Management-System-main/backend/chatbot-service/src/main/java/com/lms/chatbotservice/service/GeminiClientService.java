package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface GeminiClientService {

    // UC1, UC2, UC3 — stream từng chunk text về Frontend
    Flux<String> streamChat(List<GeminiMessage> messages);

    // UC4 — nhận JSON đầy đủ sau khi Gemini hoàn thành
    String generateJson(List<GeminiMessage> messages);
}