package com.lms.chatbotservice.mapper;

import com.lms.chatbotservice.dto.response.ChatSessionResponse;
import com.lms.chatbotservice.entity.ChatSession;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ChatSessionMapper {
    ChatSessionResponse toChatSessionResponse(ChatSession session);
}