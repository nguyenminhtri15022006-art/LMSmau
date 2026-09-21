package com.lms.assignmentservice.judge;

public class JudgeEngineException extends RuntimeException {
    private final ErrorType errorType;

    public JudgeEngineException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public JudgeEngineException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        TIMEOUT,           // Engine không trả kết quả trong thời gian cho phép
        CONNECTION_FAILED, // Không kết nối được engine
        INVALID_RESPONSE,  // Engine trả về response không đọc được
        RATE_LIMITED,      // Quá giới hạn API (Judge0 free tier)
        UNKNOWN
    }
}
