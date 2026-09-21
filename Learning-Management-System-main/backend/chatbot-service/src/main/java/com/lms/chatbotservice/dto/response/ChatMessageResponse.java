package com.lms.chatbotservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.chatbotservice.enums.MessageRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    String id;
    MessageRole role;
    String content;
    int tokenCount;
    LocalDateTime createdAt;
}