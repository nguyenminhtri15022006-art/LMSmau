package com.lms.chatbotservice.dto.gemini;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeminiMessage {
    String role;            // "user" hoặc "model"
    List<GeminiPart> parts;
}