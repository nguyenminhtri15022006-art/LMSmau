package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.DifficultyType;
import com.lms.chatbotservice.enums.QuestionType;

import java.util.List;

public interface PromptEngineService {

    // UC1, UC2, UC3 — chat thông thường
    List<GeminiMessage> buildChatPrompt(
            String userQuery,
            DomainContext context,
            List<ChatMessage> history,
            ContextType contextType);

    // UC4 — generate quiz
    List<GeminiMessage> buildGenerateQuizPrompt(
            String content,
            int questionCount,
            DifficultyType difficulty,
            QuestionType questionType);

    // UC4 — generate test case
    List<GeminiMessage> buildGenerateTestCasePrompt(
            String title,
            String description,
            String constraints,
            int count,
            List<String> allowedLangs,
            List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases);
}
