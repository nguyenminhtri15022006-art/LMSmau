package com.lms.chatbotservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== Hệ thống =====
    INTERNAL_ERROR(6999, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_UNAVAILABLE(6998, "Dịch vụ ngoài tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),

    // ===== Auth / Access =====
    UNAUTHENTICATED(6001, "Bạn chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(6002, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INSTRUCTOR_ONLY(6003, "Chức năng này chỉ dành cho giảng viên", HttpStatus.FORBIDDEN),

    // ===== Rate Limit =====
    RATE_LIMIT_EXCEEDED(6101, "Bạn đã vượt quá số lượt dùng chatbot trong ngày", HttpStatus.TOO_MANY_REQUESTS),

    // ===== Session =====
    SESSION_NOT_FOUND(6201, "Không tìm thấy phiên hội thoại", HttpStatus.NOT_FOUND),
    SESSION_ARCHIVED(6202, "Phiên hội thoại này đã bị lưu trữ", HttpStatus.GONE),
    SESSION_NOT_OWNER(6203, "Bạn không phải chủ sở hữu phiên hội thoại này", HttpStatus.FORBIDDEN),

    // ===== LLM =====
    LLM_API_ERROR(6301, "Lỗi khi gọi Gemini API", HttpStatus.BAD_GATEWAY),
    LLM_RESPONSE_PARSE_ERROR(6302, "Không thể phân tích phản hồi từ AI", HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_TIMEOUT(6303, "Gemini API không phản hồi, vui lòng thử lại", HttpStatus.GATEWAY_TIMEOUT),

    // ===== Context =====
    PROBLEM_NOT_FOUND(6401, "Không tìm thấy bài tập", HttpStatus.NOT_FOUND),
    COURSE_NOT_FOUND(6402, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),

    // ===== Generate =====
    GENERATE_QUIZ_FAILED(6501, "Không thể tạo câu hỏi trắc nghiệm", HttpStatus.INTERNAL_SERVER_ERROR),
    GENERATE_TESTCASE_FAILED(6502, "Không thể tạo test case", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== Validation — Chat =====
    CHAT_CONTEXT_TYPE_NULL(6601, "Loại ngữ cảnh không được để trống", HttpStatus.BAD_REQUEST),
    CHAT_MESSAGE_BLANK(6602, "Câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    CHAT_MESSAGE_SIZE(6603, "Câu hỏi tối đa 2000 ký tự", HttpStatus.BAD_REQUEST),

    // ===== Validation — Generate Quiz =====
    QUIZ_CONTENT_BLANK(6611, "Nội dung lý thuyết không được để trống", HttpStatus.BAD_REQUEST),
    QUIZ_CONTENT_SIZE(6612, "Nội dung lý thuyết tối đa 10000 ký tự", HttpStatus.BAD_REQUEST),
    QUIZ_COUNT_NULL(6613, "Số lượng câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    QUIZ_COUNT_MIN(6614, "Số lượng câu hỏi tối thiểu là 1", HttpStatus.BAD_REQUEST),
    QUIZ_COUNT_MAX(6615, "Số lượng câu hỏi tối đa là 30", HttpStatus.BAD_REQUEST),
    QUIZ_DIFFICULTY_NULL(6616, "Độ khó không được để trống", HttpStatus.BAD_REQUEST),
    QUIZ_QUESTION_TYPE_NULL(6617, "Loại câu hỏi không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — Generate Test Case =====
    TESTCASE_TITLE_BLANK(6621, "Tiêu đề bài toán không được để trống", HttpStatus.BAD_REQUEST),
    TESTCASE_TITLE_SIZE(6622, "Tiêu đề bài toán tối đa 300 ký tự", HttpStatus.BAD_REQUEST),
    TESTCASE_DESCRIPTION_BLANK(6623, "Mô tả bài toán không được để trống", HttpStatus.BAD_REQUEST),
    TESTCASE_DESCRIPTION_SIZE(6624, "Mô tả bài toán tối đa 5000 ký tự", HttpStatus.BAD_REQUEST),
    TESTCASE_CONSTRAINTS_SIZE(6625, "Ràng buộc tối đa 1000 ký tự", HttpStatus.BAD_REQUEST),
    TESTCASE_COUNT_NULL(6626, "Số lượng test case không được để trống", HttpStatus.BAD_REQUEST),
    TESTCASE_COUNT_MIN(6627, "Số lượng test case tối thiểu là 1", HttpStatus.BAD_REQUEST),
    TESTCASE_COUNT_MAX(6628, "Số lượng test case tối đa là 50", HttpStatus.BAD_REQUEST),
    TESTCASE_MISSING_INFO(6629, "Vui lòng cung cấp problemId hoặc nhập thủ công tiêu đề và mô tả", HttpStatus.BAD_REQUEST),
    // ===== Validation — General =====
    VALIDATION_ERROR(6699, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatusCode;
}
