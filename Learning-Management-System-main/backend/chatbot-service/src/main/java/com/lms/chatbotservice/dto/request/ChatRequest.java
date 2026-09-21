package com.lms.chatbotservice.dto.request;

import com.lms.chatbotservice.enums.ContextType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {

    @NotNull(message = "CHAT_CONTEXT_TYPE_NULL")
    ContextType contextType;

    String contextRefId;

    String sessionId;

    @NotBlank(message = "CHAT_MESSAGE_BLANK")
    @Size(max = 2000, message = "CHAT_MESSAGE_SIZE")
    String message;
}