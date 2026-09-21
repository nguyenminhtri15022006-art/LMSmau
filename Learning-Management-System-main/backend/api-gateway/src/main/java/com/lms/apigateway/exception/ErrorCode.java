package com.lms.apigateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Các lỗi liên quan đến xác thực (Authentication)
    UNAUTHENTICATED(1401, "Chưa xác thực. Token không hợp lệ hoặc đã hết hạn.", HttpStatus.UNAUTHORIZED),
    MISSING_AUTHORIZATION_HEADER(1402, "Thiếu header Authorization trong request.", HttpStatus.BAD_REQUEST),
    INVALID_HEADER_FORMAT(1403, "Định dạng header Authorization không hợp lệ. Vui lòng sử dụng 'Bearer <token>'.", HttpStatus.BAD_REQUEST),

    // Các lỗi liên quan đến phân quyền (Authorization)
    ACCESS_DENIED(1404, "Bạn không có quyền truy cập vào tài nguyên này.", HttpStatus.FORBIDDEN),

    // Các lỗi liên quan đến hệ thống / network của Gateway
    EXTERNAL_SERVICE_UNAVAILABLE(1405, "Dịch vụ đích hiện không khả dụng hoặc không thể kết nối.", HttpStatus.SERVICE_UNAVAILABLE),
    RESOURCE_NOT_FOUND(1406, "Endpoint được yêu cầu không tồn tại trên hệ thống.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(1407, "Lỗi máy chủ nội bộ trên API Gateway.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatusCode;

    ErrorCode(Integer code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = statusCode;
    }
}