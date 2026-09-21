package com.lms.chatbotservice.entity;

import com.lms.chatbotservice.enums.MessageRole;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_session_created",
                def = "{'session_id': 1, 'created_at': -1}"
        )
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    String id;

    @Field("session_id")
    String sessionId;

    @Indexed
    @Field("user_id")
    String userId;

    MessageRole role;           // USER hoặc MODEL

    String content;             // Nội dung đầy đủ tin nhắn

    @Field("token_count")
    int tokenCount;             // Ước tính: content.length() / 4

    @Field("created_at")
    LocalDateTime createdAt;
}