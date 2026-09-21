package com.lms.blogservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== Hệ thống =====
    INTERNAL_ERROR(4999, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_UNAVAILABLE(4998, "Dịch vụ ngoài tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),

    // ===== Auth / Access =====
    UNAUTHENTICATED(4001, "Bạn chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(4002, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    NOT_POST_OWNER(4003, "Bạn không phải tác giả của bài viết này", HttpStatus.FORBIDDEN),
    NOT_COMMENT_OWNER(4004, "Bạn không phải chủ bình luận này", HttpStatus.FORBIDDEN),

    // ===== Resource =====
    POST_NOT_FOUND(4101, "Không tìm thấy bài viết", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(4102, "Không tìm thấy bình luận", HttpStatus.NOT_FOUND),
    TAG_NOT_FOUND(4103, "Không tìm thấy tag", HttpStatus.NOT_FOUND),

    // ===== Business Logic =====
    POST_SLUG_EXISTED(4201, "Slug bài viết đã tồn tại", HttpStatus.CONFLICT),
    POST_TITLE_EXISTED(4202, "Tiêu đề bài viết đã tồn tại", HttpStatus.CONFLICT),
    TAG_NAME_EXISTED(4203, "Tên tag đã tồn tại", HttpStatus.CONFLICT),
    TAG_SLUG_EXISTED(4204, "Slug tag đã tồn tại", HttpStatus.CONFLICT),
    COMMENT_NOT_BELONG_TO_POST(4205, "Bình luận không thuộc bài viết này", HttpStatus.BAD_REQUEST),
    CANNOT_REPLY_TO_REPLY(4206, "Không thể reply một comment đã là reply", HttpStatus.BAD_REQUEST),
    POST_NOT_PUBLISHED(4207, "Bài viết chưa được công bố", HttpStatus.FORBIDDEN),
    CANNOT_DELETE_TAG(4208, "Không thể xóa tag đang có bài viết", HttpStatus.CONFLICT),
    CANNOT_VOTE_OWN_POST(4209, "Bạn không thể vote bài viết của chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_VOTE_OWN_COMMENT(4210, "Bạn không thể vote bình luận của chính mình", HttpStatus.BAD_REQUEST),

    // ===== Validation — Blog =====
    POST_TITLE_BLANK(4401, "Tiêu đề bài viết không được để trống", HttpStatus.BAD_REQUEST),
    POST_TITLE_SIZE(4402, "Tiêu đề bài viết tối đa 500 ký tự", HttpStatus.BAD_REQUEST),
    POST_CONTENT_BLANK(4403, "Nội dung bài viết không được để trống", HttpStatus.BAD_REQUEST),
    POST_STATUS_NULL(4404, "Trạng thái bài viết không được để trống", HttpStatus.BAD_REQUEST),
    POST_SUMMARY_SIZE(4405, "Tóm tắt bài viết tối đa 1000 ký tự", HttpStatus.BAD_REQUEST),

    // ===== Validation — Comment =====
    COMMENT_CONTENT_BLANK(4411, "Nội dung bình luận không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — Tag =====
    TAG_NAME_BLANK(4421, "Tên tag không được để trống", HttpStatus.BAD_REQUEST),
    TAG_NAME_SIZE(4422, "Tên tag tối đa 100 ký tự", HttpStatus.BAD_REQUEST),
    TAG_SLUG_BLANK(4423, "Slug tag không được để trống", HttpStatus.BAD_REQUEST),
    TAG_SLUG_SIZE(4424, "Slug tag tối đa 100 ký tự", HttpStatus.BAD_REQUEST),
    TAG_SLUG_INVALID(4425, "Slug tag chỉ được chứa chữ thường, số và dấu gạch ngang", HttpStatus.BAD_REQUEST),

    // Validation — Vote
    VOTE_TYPE_NULL(4431, "Loại vote không được để trống", HttpStatus.BAD_REQUEST),

    // ===== Validation — General =====
    VALIDATION_ERROR(4499, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);

    int code;
    String message;
    HttpStatus httpStatusCode;
}