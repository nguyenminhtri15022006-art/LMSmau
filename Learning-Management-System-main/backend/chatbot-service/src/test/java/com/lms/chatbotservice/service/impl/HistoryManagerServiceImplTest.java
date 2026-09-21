package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.enums.SessionStatus;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.service.ChatMessageService;
import com.lms.chatbotservice.service.ChatSessionService;
import com.lms.chatbotservice.service.RedisCacheService;
import com.lms.chatbotservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryManagerServiceImplTest {

    @Mock
    ChatSessionService sessionService;
    @Mock
    ChatMessageService messageService;
    @Mock
    RedisCacheService redisCacheService;

    @InjectMocks
    HistoryManagerServiceImpl historyManagerService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void resolveSessionCreatesNewWhenNoActiveSessionExists() {
        TestSecurity.authenticate("student-1", "STUDENT");
        ChatSession created = session("new-session", "student-1", SessionStatus.ACTIVE);
        when(sessionService.findActiveSession("student-1", ContextType.GENERAL, null)).thenReturn(null);
        when(sessionService.createSession("student-1", ContextType.GENERAL, null, "hello")).thenReturn(created);

        ChatSession resolved = historyManagerService.resolveSession(ContextType.GENERAL, null, null, "hello");

        assertThat(resolved.getId()).isEqualTo("new-session");
        verify(sessionService).createSession("student-1", ContextType.GENERAL, null, "hello");
    }

    @Test
    void resolveSessionReactivatesArchivedSessionAndArchivesCurrentActive() {
        TestSecurity.authenticate("student-1", "STUDENT");
        ChatSession archived = session("old-session", "student-1", SessionStatus.ARCHIVED);
        ChatSession active = session("active-session", "student-1", SessionStatus.ACTIVE);
        when(sessionService.getSessionById("old-session")).thenReturn(archived, archived);
        when(sessionService.findActiveSession("student-1", ContextType.GENERAL, null)).thenReturn(active);

        ChatSession resolved = historyManagerService.resolveSession(ContextType.GENERAL, null, "old-session", "hello");

        assertThat(resolved.getId()).isEqualTo("old-session");
        verify(sessionService).archiveSession("active-session");
        verify(sessionService).reactivateSession("old-session");
    }

    @Test
    void resolveSessionRejectsSessionOwnedByAnotherUser() {
        TestSecurity.authenticate("student-1", "STUDENT");
        when(sessionService.getSessionById("session-2"))
                .thenReturn(session("session-2", "student-2", SessionStatus.ACTIVE));

        assertThatThrownBy(() -> historyManagerService.resolveSession(ContextType.GENERAL, null, "session-2", "hello"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_OWNER);
    }

    @Test
    void getRecentMessagesReturnsRedisHitWithoutMongoLookup() {
        List<ChatMessage> cached = List.of(message("m1", MessageRole.USER), message("m2", MessageRole.MODEL));
        when(redisCacheService.getMessages("session-1")).thenReturn(cached);

        List<ChatMessage> messages = historyManagerService.getRecentMessages("session-1");

        assertThat(messages).containsExactlyElementsOf(cached);
        verify(messageService, never()).getRecentMessages(any(), eq(20));
    }

    @Test
    void getRecentMessagesLoadsMongoDescReversesAndWarmsRedisOnMiss() {
        ReflectionTestUtils.setField(historyManagerService, "historyLimit", 2);
        ChatMessage newest = message("newest", MessageRole.MODEL);
        ChatMessage oldest = message("oldest", MessageRole.USER);
        when(redisCacheService.getMessages("session-1")).thenReturn(null);
        when(messageService.getRecentMessages("session-1", 2)).thenReturn(List.of(newest, oldest));

        List<ChatMessage> messages = historyManagerService.getRecentMessages("session-1");

        assertThat(messages).extracting(ChatMessage::getId).containsExactly("oldest", "newest");
        verify(redisCacheService).saveMessages("session-1", messages);
    }

    @Test
    void saveExchangePersistsBothMessagesAppendsRedisAndIncrementsSession() {
        ChatMessage userMessage = message("u1", MessageRole.USER);
        ChatMessage modelMessage = message("m1", MessageRole.MODEL);
        when(messageService.saveMessage("session-1", "student-1", MessageRole.USER, "question")).thenReturn(userMessage);
        when(messageService.saveMessage("session-1", "student-1", MessageRole.MODEL, "answer")).thenReturn(modelMessage);

        historyManagerService.saveExchange("session-1", "student-1", "question", "answer");

        verify(redisCacheService).appendMessage("session-1", userMessage);
        verify(redisCacheService).appendMessage("session-1", modelMessage);
        verify(sessionService).incrementMessageCount("session-1", 2);
    }

    private ChatSession session(String id, String userId, SessionStatus status) {
        return ChatSession.builder()
                .id(id)
                .userId(userId)
                .contextType(ContextType.GENERAL)
                .status(status)
                .build();
    }

    private ChatMessage message(String id, MessageRole role) {
        return ChatMessage.builder()
                .id(id)
                .role(role)
                .content(id)
                .build();
    }
}
