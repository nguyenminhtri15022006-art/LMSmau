package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.service.ContextBuilderService;
import com.lms.chatbotservice.service.GeminiClientService;
import com.lms.chatbotservice.service.HistoryManagerService;
import com.lms.chatbotservice.service.PromptEngineService;
import com.lms.chatbotservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    HistoryManagerService historyManagerService;
    @Mock
    ContextBuilderService contextBuilderService;
    @Mock
    PromptEngineService promptEngineService;
    @Mock
    GeminiClientService geminiClientService;

    @InjectMocks
    ChatServiceImpl chatService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void chatEmitsSessionEventThenGeminiChunksAndSavesFullExchange() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        ChatRequest request = ChatRequest.builder()
                .contextType(ContextType.PROBLEM)
                .contextRefId("42")
                .message("Help me debug")
                .build();
        ChatSession session = ChatSession.builder().id("session-1").userId("student-1").build();
        List<ChatMessage> history = List.of(ChatMessage.builder()
                .role(MessageRole.USER)
                .content("old question")
                .build());
        DomainContext context = DomainContext.builder().userId("student-1").build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());

        when(historyManagerService.resolveSession(ContextType.PROBLEM, "42", null, "Help me debug"))
                .thenReturn(session);
        when(historyManagerService.getRecentMessages("session-1")).thenReturn(history);
        when(contextBuilderService.build("42", ContextType.PROBLEM, "student-1")).thenReturn(context);
        when(promptEngineService.buildChatPrompt("Help me debug", context, history, ContextType.PROBLEM))
                .thenReturn(prompt);
        when(geminiClientService.streamChat(prompt)).thenReturn(Flux.just("First ", "answer"));

        StepVerifier.create(chatService.chat(request))
                .expectNext("[SESSION:session-1]")
                .expectNext("First ")
                .expectNext("answer")
                .verifyComplete();

        verify(historyManagerService).saveExchange("session-1", "student-1", "Help me debug", "First answer");
    }

    @Test
    void chatDoesNotSaveExchangeWhenModelResponseIsBlank() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        ChatRequest request = ChatRequest.builder()
                .contextType(ContextType.GENERAL)
                .message("Hi")
                .build();
        ChatSession session = ChatSession.builder().id("session-1").userId("student-1").build();
        DomainContext context = DomainContext.builder().userId("student-1").build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());

        when(historyManagerService.resolveSession(ContextType.GENERAL, null, null, "Hi"))
                .thenReturn(session);
        when(historyManagerService.getRecentMessages("session-1")).thenReturn(List.of());
        when(contextBuilderService.build(null, ContextType.GENERAL, "student-1")).thenReturn(context);
        when(promptEngineService.buildChatPrompt("Hi", context, List.of(), ContextType.GENERAL))
                .thenReturn(prompt);
        when(geminiClientService.streamChat(prompt)).thenReturn(Flux.just("   "));

        StepVerifier.create(chatService.chat(request))
                .expectNext("[SESSION:session-1]")
                .expectNext("   ")
                .verifyComplete();

        verify(historyManagerService, never()).saveExchange(any(), any(), any(), any());
    }
}
