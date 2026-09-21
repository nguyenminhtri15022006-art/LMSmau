package com.lms.chatbotservice.entity;

import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.SessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_sessions")
@CompoundIndexes({
        // findActiveSession: user + contextType + contextRefId + status
        @CompoundIndex(
                name = "idx_active_session_lookup",
                def = "{'user_id': 1, 'context_type': 1, 'context_ref_id': 1, 'status': 1}"
        ),
        // getSessionsByUser: sort theo thời gian
        @CompoundIndex(
                name = "idx_user_last_message",
                def = "{'user_id': 1, 'last_message_at': -1}"
        ),
        // archiveStaleSessions background job
        @CompoundIndex(
                name = "idx_status_last",
                def = "{'status': 1, 'last_message_at': 1}"
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSession {

    @Id
    String id;

    @Field("user_id")
    String userId;

    @Field("context_type")
    ContextType contextType;

    @Field("context_ref_id")
    String contextRefId;        // nullable — problem_id hoặc course_id

    String title;               // 50 ký tự đầu của tin nhắn đầu tiên

    SessionStatus status;

    @Field("total_messages")
    int totalMessages;

    @Field("last_message_at")
    LocalDateTime lastMessageAt;

    @Field("created_at")
    LocalDateTime createdAt;
}