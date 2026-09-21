package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.response.ChatSessionResponse;
import com.lms.chatbotservice.entity.ChatSession;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.SessionStatus;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.mapper.ChatSessionMapper;
import com.lms.chatbotservice.repository.ChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest {

    @Mock
    ChatSessionRepository sessionRepository;
    @Mock
    ChatSessionMapper sessionMapper;

    @InjectMocks
    ChatSessionServiceImpl chatSessionService;

    @Test
    void createSessionTruncatesTitleAndInitializesActiveSession() {
        ReflectionTestUtils.setField(chatSessionService, "titleMaxLength", 10);
        when(sessionRepository.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession session = invocation.getArgument(0);
                    session.setId("session-1");
                    return session;
                });

        ChatSession session = chatSessionService.createSession(
                "student-1", ContextType.PROBLEM, "42", "abcdefghijklmnopqrstuvwxyz");

        assertThat(session.getTitle()).isEqualTo("abcdefghij...");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getTotalMessages()).isZero();
        assertThat(session.getLastMessageAt()).isNotNull();
        assertThat(session.getCreatedAt()).isNotNull();
    }

    @Test
    void getSessionByIdThrowsWhenMissing() {
        when(sessionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatSessionService.getSessionById("missing"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void archiveSessionByUserRejectsNonOwner() {
        when(sessionRepository.findById("session-1"))
                .thenReturn(Optional.of(session("session-1", "student-2", SessionStatus.ACTIVE)));

        assertThatThrownBy(() -> chatSessionService.archiveSessionByUser("session-1", "student-1"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_OWNER);
    }

    @Test
    void archiveSessionByUserMarksSessionArchived() {
        ChatSession session = session("session-1", "student-1", SessionStatus.ACTIVE);
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        chatSessionService.archiveSessionByUser("session-1", "student-1");

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.ARCHIVED);
    }

    @Test
    void archiveStaleSessionsArchivesOnlyRepositoryResult() {
        ReflectionTestUtils.setField(chatSessionService, "archiveAfterDays", 7);
        ChatSession stale = session("stale", "student-1", SessionStatus.ACTIVE);
        when(sessionRepository.findByStatusAndLastMessageAtBefore(eq(SessionStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(stale));

        chatSessionService.archiveStaleSessions();

        assertThat(stale.getStatus()).isEqualTo(SessionStatus.ARCHIVED);
        verify(sessionRepository).saveAll(List.of(stale));
    }

    @Test
    void getSessionsByUserMapsPage() {
        ChatSession session = session("session-1", "student-1", SessionStatus.ACTIVE);
        ChatSessionResponse response = ChatSessionResponse.builder().id("session-1").build();
        when(sessionRepository.findByUserIdOrderByLastMessageAtDesc("student-1", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(sessionMapper.toChatSessionResponse(session)).thenReturn(response);

        var page = chatSessionService.getSessionsByUser("student-1", PageRequest.of(0, 20));

        assertThat(page.getContent()).containsExactly(response);
    }

    private ChatSession session(String id, String userId, SessionStatus status) {
        return ChatSession.builder()
                .id(id)
                .userId(userId)
                .contextType(ContextType.GENERAL)
                .contextRefId(null)
                .status(status)
                .build();
    }
}
