package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface OpenCodeClientService {

    Flux<String> streamChat(List<GeminiMessage> messages);

    String generateJson(List<GeminiMessage> messages);
}
