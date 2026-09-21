package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.response.ChatMessageResponse;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.mapper.ChatMessageMapper;
import com.lms.chatbotservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    ChatMessageRepository messageRepository;
    @Mock
    ChatMessageMapper messageMapper;

    @InjectMocks
    ChatMessageServiceImpl chatMessageService;

    @Test
    void saveMessageEstimatesTokenCountAndPersistsMessage() {
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage saved = chatMessageService.saveMessage(
                "session-1", "student-1", MessageRole.USER, "abcdefghijkl");

        assertThat(saved.getTokenCount()).isEqualTo(3);
        assertThat(saved.getCreatedAt()).isNotNull();
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo("session-1");
    }

    @Test
    void saveMessageUsesMinimumOneToken() {
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage saved = chatMessageService.saveMessage(
                "session-1", "student-1", MessageRole.USER, "abc");

        assertThat(saved.getTokenCount()).isEqualTo(1);
    }

    @Test
    void getRecentMessagesDelegatesToRepositoryWithRequestedLimit() {
        ChatMessage message = message("m1", MessageRole.USER);
        when(messageRepository.findBySessionIdOrderByCreatedAtDesc("session-1", PageRequest.of(0, 5)))
                .thenReturn(List.of(message));

        List<ChatMessage> messages = chatMessageService.getRecentMessages("session-1", 5);

        assertThat(messages).containsExactly(message);
    }

    @Test
    void getMessageResponsesReversesRepositoryDescOrderForUi() {
        ReflectionTestUtils.setField(chatMessageService, "responseHistoryLimit", 2);
        ChatMessage newest = message("newest", MessageRole.MODEL);
        ChatMessage oldest = message("oldest", MessageRole.USER);
        ChatMessageResponse newestResponse = ChatMessageResponse.builder().id("newest").build();
        ChatMessageResponse oldestResponse = ChatMessageResponse.builder().id("oldest").build();
        when(messageRepository.findBySessionIdOrderByCreatedAtDesc("session-1", PageRequest.of(0, 2)))
                .thenReturn(List.of(newest, oldest));
        when(messageMapper.toChatMessageResponse(oldest)).thenReturn(oldestResponse);
        when(messageMapper.toChatMessageResponse(newest)).thenReturn(newestResponse);

        List<ChatMessageResponse> responses = chatMessageService.getMessageResponses("session-1");

        assertThat(responses).extracting(ChatMessageResponse::getId).containsExactly("oldest", "newest");
    }

    @Test
    void deleteByUserIdDelegatesToRepository() {
        chatMessageService.deleteByUserId("student-1");

        verify(messageRepository).deleteByUserId("student-1");
    }

    private ChatMessage message(String id, MessageRole role) {
        return ChatMessage.builder()
                .id(id)
                .role(role)
                .content(id)
                .build();
    }
}
