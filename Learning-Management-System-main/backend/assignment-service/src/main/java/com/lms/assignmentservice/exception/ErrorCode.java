package com.lms.assignmentservice.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // ===== Hệ thống =====
    INTERNAL_ERROR(3999, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_UNAVAILABLE(3998, "Dịch vụ ngoài tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_PATH(3997, "Đường dẫn không hợp lệ", HttpStatus.BAD_REQUEST),

    // ===== Auth / Access =====
    UNAUTHENTICATED(3001, "Bạn chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(3002, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    NOT_ENROLLED(3003, "Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN),
    COURSE_LOCKED(3004, "Khóa học đã bị khóa bởi Admin, không thể thực hiện thao tác này", HttpStatus.FORBIDDEN),

    // ===== Resource =====
    PROBLEM_NOT_FOUND(3101, "Không tìm thấy bài tập", HttpStatus.NOT_FOUND),
    TEST_CASE_NOT_FOUND(3102, "Không tìm thấy test case", HttpStatus.NOT_FOUND),
    QUIZ_NOT_FOUND(3103, "Không tìm thấy bài kiểm tra", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(3104, "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND),
    SUBMISSION_NOT_FOUND(3105, "Không tìm thấy bài nộp", HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_FOUND(3106, "Không tìm thấy lần làm bài", HttpStatus.NOT_FOUND),
    COURSE_NOT_FOUND(3107, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),
    QUESTION_IN_USE(3104, "Câu hỏi đang được dùng trong quiz. Hãy gỡ khỏi quiz trước khi xóa.", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_IN_QUIZ(3105, "Câu hỏi không nằm trong Quiz", HttpStatus.BAD_REQUEST),

    // ===== Business Logic =====
    QUIZ_NOT_AVAILABLE(3201, "Bài kiểm tra chưa mở hoặc đã đóng", HttpStatus.BAD_REQUEST),
    QUIZ_NOT_PUBLISHED(3202, "Bài kiểm tra chưa được công bố", HttpStatus.FORBIDDEN),
    ATTEMPT_IN_PROGRESS(3203, "Bạn đang có lần làm bài chưa hoàn thành", HttpStatus.CONFLICT),
    ALREADY_SUBMITTED(3204, "Bài kiểm tra này đã được nộp", HttpStatus.CONFLICT),
    MAX_ATTEMPTS_REACHED(3205, "Bạn đã đạt số lần làm bài tối đa", HttpStatus.FORBIDDEN),
    ATTEMPT_EXPIRED(3206, "Lần làm bài đã hết thời gian", HttpStatus.BAD_REQUEST),
    SHOW_RESULT_NOT_ALLOWED(3210, "Chưa đến lúc xem kết quả, vui lòng thử lại sau", HttpStatus.FORBIDDEN),
    QUIZ_ALREADY_PUBLISHED(3207, "Quiz đã được xuất bản, hãy tạo Quiz khác hoặc ngưng xuất bản", HttpStatus.BAD_REQUEST),
    QUIZ_EMPTY_CANNOT_PUBLISH(3208, "Không thể xuất bản bài kiểm tra chưa có câu hỏi nào", HttpStatus.BAD_REQUEST),
    QUIZ_HAS_ATTEMPTS_CANNOT_UNPUBLISH(3209, "Không thể ngưng xuất bản vì đã có sinh viên bắt đầu làm bài kiểm tra này", HttpStatus.BAD_REQUEST),
    QUIZ_HAS_ATTEMPTS_CANNOT_DELETE(3213, "Không thể xóa vì quiz đã có sinh viên làm bài", HttpStatus.BAD_REQUEST),
    QUIZ_PUBLISHED_WITH_ATTEMPTS_CANNOT_EDIT(3211, "Quiz đã được xuất bản và có sinh viên thi rồi, không thể sửa", HttpStatus.BAD_REQUEST),
    QUIZ_HAS_ATTEMPTS_CANNOT_EDIT(3214, "Quiz đã có sinh viên làm bài, hãy tạo bản sao để chỉnh sửa", HttpStatus.BAD_REQUEST),
    QUIZ_PUBLISHED_CANNOT_EDIT(3215, "Quiz đã được xuất bản, hãy ngưng xuất bản trước khi chỉnh sửa", HttpStatus.BAD_REQUEST),
    QUIZ_IS_DELETED(3217, "Bài kiểm tra đã bị xóa, vui lòng khôi phục trước khi thao tác", HttpStatus.BAD_REQUEST),
    PROBLEM_IS_DELETED(3218, "Bài tập code đã bị xóa, vui lòng khôi phục trước khi thao tác", HttpStatus.BAD_REQUEST),
    PROBLEM_HAS_SUBMISSIONS_CANNOT_UNPUBLISH(3219, "Không thể ngưng xuất bản vì bài tập code đã có bài nộp", HttpStatus.BAD_REQUEST),
    PROBLEM_HAS_SUBMISSIONS_CANNOT_EDIT_GRADING(3220, "Bài tập code đã có bài nộp. Hãy tạo bản sao để chỉnh sửa giới hạn, điểm hoặc test case", HttpStatus.BAD_REQUEST),
    PROBLEM_HAS_SUBMISSIONS_CANNOT_MUTATE_TEST_CASES(3221, "Bài tập code đã có bài nộp, không thể chỉnh sửa test case trực tiếp", HttpStatus.BAD_REQUEST),
    QUESTION_IN_LOCKED_QUIZ_CANNOT_EDIT(3216, "Câu hỏi thuộc quiz đã xuất bản hoặc đã có lượt làm, không thể sửa trực tiếp", HttpStatus.BAD_REQUEST),
    ATTEMPT_PROCESSING(3212, "Hệ thống đang xử lý bài thi, vui lòng tải lại trang", HttpStatus.CONFLICT),

    // ===== Submission / Judge =====
    INVALID_LANGUAGE(3301, "Ngôn ngữ lập trình không được hỗ trợ", HttpStatus.BAD_REQUEST),
    SOURCE_CODE_EMPTY(3302, "Source code không được để trống", HttpStatus.BAD_REQUEST),
    SOURCE_CODE_TOO_LARGE(3303, "Source code vượt quá giới hạn 64KB", HttpStatus.BAD_REQUEST),
    JUDGE0_ERROR(3304, "Lỗi hệ thống chấm bài, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    JUDGE0_TIMEOUT(3305, "Hệ thống chấm bài quá thời gian, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== Final Upsert Validation =====
    INVALID_ANSWER_FOR_QUESTION(3306, "Đáp án không hợp lệ cho câu hỏi này", HttpStatus.BAD_REQUEST),
    ANSWER_NOT_IN_QUESTION(3307, "Một hoặc nhiều đáp án không thuộc về câu hỏi này", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_IN_ATTEMPT_QUIZ(3308, "Câu hỏi không nằm trong bài kiểm tra đang làm", HttpStatus.BAD_REQUEST),
    SUSPICIOUS_SUBMIT_DETECTED(3309, "Phát hiện dữ liệu không hợp lệ khi nộp bài, vui lòng thử lại", HttpStatus.BAD_REQUEST),

    // ===== Abandoned Session =====
    ATTEMPT_AUTO_SUBMITTED(3310, "Lần làm bài đã hết hạn và được tự động nộp", HttpStatus.CONFLICT),

    // ===== Judge0 Fallback Sync =====
    JUDGE0_SYSTEM_ERROR(3311, "Lỗi hệ thống từ Judge0, vui lòng nộp lại", HttpStatus.INTERNAL_SERVER_ERROR),
    SUBMISSION_STATUS_SYNCED(3312, "Kết quả chấm bài đã được cập nhật", HttpStatus.OK),
    COMPILATION_ERROR(3313, "Biên dịch thất bại", HttpStatus.BAD_REQUEST),

    // ===== Validation =====
    VALIDATION_ERROR(3401, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    SLUG_ALREADY_EXISTS(3402, "Slug đã tồn tại, vui lòng chọn tên khác", HttpStatus.BAD_REQUEST),
    INVALID_ANSWER_COUNT(3403, "Số lượng đáp án không hợp lệ cho loại câu hỏi này", HttpStatus.BAD_REQUEST),
    INVALID_IMPORT_FORMAT(3405, "Định dạng import không hợp lệ", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_EMPTY(3406, "File import không được để trống", HttpStatus.BAD_REQUEST),
    IMPORT_NO_VALID_QUESTIONS(3407, "Không tìm thấy câu hỏi hợp lệ trong file", HttpStatus.BAD_REQUEST),
    AI_SOURCE_CONTENT_REQUIRED(3408, "Vui lòng nhập nội dung hoặc tải lên file để tạo quiz bằng AI", HttpStatus.BAD_REQUEST),
    AI_SOURCE_CONTENT_TOO_LONG(3409, "Nội dung tạo quiz bằng AI tối đa 10000 ký tự", HttpStatus.BAD_REQUEST),
    TEST_CASE_IMPORT_FILE_TOO_LARGE(3432, "File test case import vượt quá giới hạn 10MB", HttpStatus.BAD_REQUEST),
    TEST_CASE_IMPORT_READ_FAILED(3433, "Không thể đọc file test case import", HttpStatus.BAD_REQUEST),

    // ===== Quiz Validation =====
    QUIZ_TITLE_REQUIRED(3410, "Tiêu đề quiz không được để trống", HttpStatus.BAD_REQUEST),
    QUIZ_TITLE_TOO_LONG(3411, "Tiêu đề quiz tối đa 300 ký tự", HttpStatus.BAD_REQUEST),
    QUIZ_DURATION_INVALID(3412, "Thời gian làm bài phải từ 1-300 phút", HttpStatus.BAD_REQUEST),
    QUIZ_TOTAL_SCORE_INVALID(3413, "Tổng điểm phải từ 0-1000", HttpStatus.BAD_REQUEST),
    QUIZ_PASS_SCORE_INVALID(3414, "Điểm đạt phải từ 0-100", HttpStatus.BAD_REQUEST),
    QUIZ_MAX_ATTEMPTS_INVALID(3415, "Số lần thi tối đa phải >= 0", HttpStatus.BAD_REQUEST),
    QUIZ_TIME_WINDOW_INVALID(3416, "Thời gian kết thúc phải sau thời gian bắt đầu", HttpStatus.BAD_REQUEST),

    // ===== Question Validation =====
    QUESTION_CONTENT_REQUIRED(3420, "Nội dung câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    QUESTION_TYPE_REQUIRED(3421, "Loại câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    QUESTION_SCORE_INVALID(3422, "Điểm câu hỏi phải từ 1 trở lên", HttpStatus.BAD_REQUEST),
    QUESTION_ANSWERS_REQUIRED(3423, "Phải có ít nhất 2 đáp án", HttpStatus.BAD_REQUEST),
    QUESTION_ANSWER_CONTENT_REQUIRED(3424, "Nội dung đáp án không được để trống", HttpStatus.BAD_REQUEST),
    QUESTION_SINGLE_ANSWER_COUNT(3425, "Câu hỏi SINGLE phải có đúng 1 đáp án đúng", HttpStatus.BAD_REQUEST),
    QUESTION_TRUE_FALSE_ANSWER_COUNT(3426, "Câu hỏi TRUE_FALSE phải có đúng 2 đáp án và 1 đáp án đúng", HttpStatus.BAD_REQUEST),
    QUESTION_MULTIPLE_ANSWER_COUNT(3427, "Câu hỏi MULTIPLE phải có ít nhất 2 đáp án đúng", HttpStatus.BAD_REQUEST),
    AI_GENERATE_QUIZ_FAILED(3428, "Không thể tạo câu hỏi bằng AI", HttpStatus.BAD_GATEWAY),
    AI_GENERATE_TEST_CASES_FAILED(3429, "Không thể tạo test case bằng AI", HttpStatus.BAD_GATEWAY),

    // ===== Bulk Question Validation =====
    BULK_QUESTION_LIST_EMPTY(3430, "Danh sách câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    BULK_QUESTION_ID_REQUIRED(3431, "questionId không được để trống", HttpStatus.BAD_REQUEST),

    // ===== File Upload =====
    FILE_TYPE_NOT_SUPPORTED(3501, "Định dạng file không được hỗ trợ. Chỉ chấp nhận JPG, PNG, GIF, WebP, BMP, SVG", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(3502, "Upload file thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR);

    Integer code;
    String message;
    HttpStatusCode httpStatusCode;
}
