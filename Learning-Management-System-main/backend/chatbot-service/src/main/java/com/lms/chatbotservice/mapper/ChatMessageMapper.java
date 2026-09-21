package com.lms.chatbotservice.mapper;

import com.lms.chatbotservice.dto.response.ChatMessageResponse;
import com.lms.chatbotservice.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ChatMessageMapper {
    ChatMessageResponse toChatMessageResponse(ChatMessage message);
}