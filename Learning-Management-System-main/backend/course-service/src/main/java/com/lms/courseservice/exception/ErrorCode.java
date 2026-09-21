package com.lms.courseservice.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== Hệ thống =====
    INTERNAL_ERROR(2999, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_UNAVAILABLE(2998, "Dịch vụ ngoài tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),

    // ===== Auth / Access =====
    UNAUTHENTICATED(2001, "Bạn chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(2002, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    NOT_COURSE_OWNER(2003, "Bạn không phải giảng viên của khóa học này", HttpStatus.FORBIDDEN),

    // ===== Resource =====
    COURSE_NOT_FOUND(2101, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),
    LESSON_NOT_FOUND(2102, "Không tìm thấy bài học", HttpStatus.NOT_FOUND),
    ENROLLMENT_NOT_FOUND(2103, "Không tìm thấy thông tin đăng ký", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(2104, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    SCHEDULE_NOT_FOUND(2105, "Không tìm thấy lịch học", HttpStatus.NOT_FOUND),
    ANNOUNCEMENT_NOT_FOUND(2106, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    STRUCTURE_NODE_NOT_FOUND(2107, "Không tìm thấy node trong cấu trúc bài học", HttpStatus.NOT_FOUND),
    PARENT_NODE_NOT_FOUND(2108, "Node cha không tồn tại trong cấu trúc khóa học này", HttpStatus.BAD_REQUEST),

    // ===== Business Logic =====
    ALREADY_ENROLLED(2201, "Bạn đã đăng ký khóa học này", HttpStatus.CONFLICT),
    COURSE_NOT_PUBLIC(2202, "Khóa học chưa được công bố", HttpStatus.FORBIDDEN),
    COURSE_LOCKED(2210, "Khóa học đã bị khóa bởi Admin", HttpStatus.FORBIDDEN),
    COURSE_ALREADY_LOCKED(2211, "Khóa học đã ở trạng thái bị khóa", HttpStatus.CONFLICT),
    COURSE_NOT_LOCKED(2212, "Khóa học không ở trạng thái bị khóa", HttpStatus.CONFLICT),
    CATEGORY_NAME_EXISTED(2203, "Tên danh mục đã tồn tại", HttpStatus.CONFLICT),
    COURSE_TITLE_EXISTED(2204, "Tên khóa học đã tồn tại", HttpStatus.CONFLICT),
    CANNOT_DELETE_CATEGORY(2205, "Không thể xóa danh mục đang có khóa học", HttpStatus.CONFLICT),
    COURSE_STRUCTURE_ERROR(2206, "Lỗi khi thao tác cấu trúc bài học", HttpStatus.INTERNAL_SERVER_ERROR),
    SLUG_ALREADY_EXISTS(2207, "Slug đã tồn tại, vui lòng chọn tên khác", HttpStatus.CONFLICT),
    LESSON_NOT_BELONG_TO_COURSE(2208, "Bài học không thuộc khóa học này", HttpStatus.BAD_REQUEST),
    NOT_ENROLLED(2209, "Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN),

    // ===== Validation - CATEGORY =====
    CATEGORY_NAME_BLANK(2401, "Tên danh mục không được để trống", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_SIZE(2402, "Tên danh mục tối đa 200 ký tự", HttpStatus.BAD_REQUEST),
    CATEGORY_SLUG_SIZE(2403, "Slug tối đa 200 ký tự", HttpStatus.BAD_REQUEST),

    // ===== Validation — Course =====
    COURSE_TITLE_BLANK(2411, "Tên khóa học không được để trống", HttpStatus.BAD_REQUEST),
    COURSE_TITLE_SIZE(2412, "Tên khóa học tối đa 500 ký tự", HttpStatus.BAD_REQUEST),
    COURSE_STATUS_NULL(2413, "Trạng thái khóa học không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — Schedule =====
    SCHEDULE_TITLE_BLANK(2421, "Tên buổi học không được để trống", HttpStatus.BAD_REQUEST),
    SCHEDULE_TITLE_SIZE(2422, "Tên buổi học tối đa 500 ký tự", HttpStatus.BAD_REQUEST),
    SCHEDULE_START_TIME_NULL(2423, "Thời gian bắt đầu không được để trống", HttpStatus.BAD_REQUEST),
    SCHEDULE_END_TIME_NULL(2424, "Thời gian kết thúc không được để trống", HttpStatus.BAD_REQUEST),
    SCHEDULE_TIME_INVALID(2425, "Thời gian lịch học không hợp lệ", HttpStatus.BAD_REQUEST),

    // ===== Validation — Announcement =====
    ANNOUNCEMENT_TITLE_BLANK(2431, "Tiêu đề thông báo không được để trống", HttpStatus.BAD_REQUEST),
    ANNOUNCEMENT_TITLE_SIZE(2432, "Tiêu đề thông báo tối đa 500 ký tự", HttpStatus.BAD_REQUEST),
    ANNOUNCEMENT_CONTENT_BLANK(2433, "Nội dung thông báo không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — Lesson =====
    LESSON_CONTENT_NULL(2443, "Nội dung bài học không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — Structure Node =====
    NODE_TYPE_BLANK(2451, "Loại node không được để trống", HttpStatus.BAD_REQUEST),
    NODE_TITLE_BLANK(2452, "Tên node không được để trống", HttpStatus.BAD_REQUEST),
    NODE_TITLE_SIZE(2453, "Tên node tối đa 500 ký tự", HttpStatus.BAD_REQUEST),
    NODE_LIST_EMPTY(2454, "Danh sách node không được để trống", HttpStatus.BAD_REQUEST),
    NODE_TYPE_INVALID(2455, "Loại node không hợp lệ, chỉ chấp nhận 'folder' hoặc 'lesson'", HttpStatus.BAD_REQUEST),

    // ===== Validation — Invite Student =====
    INVITE_USER_IDS_EMPTY(2461, "Danh sách người dùng không được để trống", HttpStatus.BAD_REQUEST),
    INVITE_USER_IDS_SIZE(2462, "Chỉ được mời tối đa 50 học viên một lần", HttpStatus.BAD_REQUEST),
    INVITE_USER_ID_BLANK(2463, "ID người dùng không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — General =====
    VALIDATION_ERROR(2499, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);

    int code;
    String message;
    HttpStatus httpStatusCode;
}
