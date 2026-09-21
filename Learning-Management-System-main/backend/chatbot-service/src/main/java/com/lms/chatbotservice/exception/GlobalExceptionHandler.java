package com.lms.chatbotservice.exception;

import com.lms.chatbotservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<ApiResponse<Void>> handleChatbotException(ChatbotException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e) {

        // Lấy message của field lỗi đầu tiên (là key ErrorCode)
        String key = e.getBindingResult().getFieldErrors()
                .stream().findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("VALIDATION_ERROR");

        // Resolve key sang ErrorCode, fallback về VALIDATION_ERROR nếu không tìm thấy
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(key);
        } catch (IllegalArgumentException ex) {
            errorCode = ErrorCode.VALIDATION_ERROR;
        }

        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.error("AccessDeniedException caught in GlobalExceptionHandler", e);
        ErrorCode code = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(code.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException e) {
        ErrorCode code = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity.status(code.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.<Void>builder()
                        .code(500)
                        .message("Lỗi hệ thống, vui lòng thử lại sau")
                        .build());
    }
}