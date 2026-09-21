package com.lms.chatbotservice.exception;

import lombok.Getter;

@Getter
public class ChatbotException extends RuntimeException {
    private final ErrorCode errorCode;

    public ChatbotException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
