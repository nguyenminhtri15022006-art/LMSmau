package com.lms.chatbotservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.SessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionResponse {
    String id;
    String title;
    ContextType contextType;
    String contextRefId;
    SessionStatus status;
    int totalMessages;
    LocalDateTime lastMessageAt;
    LocalDateTime createdAt;
}