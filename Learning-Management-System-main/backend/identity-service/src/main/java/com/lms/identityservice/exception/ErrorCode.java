package com.lms.identityservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // HỆ THỐNG
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9998, "Tham số không hợp lệ", HttpStatus.BAD_REQUEST),

    // AUTHENTICATION & AUTHORIZATION
    UNAUTHENTICATED(1001, "Bạn chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    USER_NOT_EXISTED(1003, "Tài khoản không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1004, "Tên đăng nhập đã được sử dụng", HttpStatus.CONFLICT),
    EMAIL_EXISTED(1005, "Email đã được sử dụng", HttpStatus.CONFLICT),
    USER_INACTIVE(1006, "Tài khoản đã bị khóa, vui lòng liên hệ admin", HttpStatus.FORBIDDEN),
    PASSWORD_INCORRECT(1007, "Mật khẩu đăng nhập không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1008, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    CANNOT_DEACTIVATE_ADMIN(1066, "Không thể khóa tài khoản Admin", HttpStatus.BAD_REQUEST),

    // TOKEN
    TOKEN_EXPIRED(1011, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(1012, "Token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSED(1013, "Phát hiện token bị tái sử dụng, tài khoản đã bị đăng xuất khỏi tất cả thiết bị", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1014, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),

    // EMAIL VERIFICATION
    EMAIL_ALREADY_VERIFIED(1021, "Email đã được xác thực trước đó", HttpStatus.BAD_REQUEST),
    VERIFICATION_EMAIL_ALREADY_SENT(1022, "Email xác thực đã được gửi, vui lòng kiểm tra hộp thư", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_VERIFICATION_TOKEN(1023, "OTP xác thực không hợp lệ", HttpStatus.BAD_REQUEST),
    TOKEN_ALREADY_USED(1024, "Đường dẫn xác thực đã được sử dụng", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(1025, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),

    // VALIDATION — User
    USERNAME_BLANK(1031, "Tên đăng nhập không được để trống", HttpStatus.BAD_REQUEST),
    USERNAME_SIZE(1032, "Tên đăng nhập phải có ít nhất 5 ký tự", HttpStatus.BAD_REQUEST),
    USERNAME_REGET(1033, "Tên đăng nhập không được chứa khoảng trắng", HttpStatus.BAD_REQUEST),
    PASSWORD_SIZE(1034, "Mật khẩu phải có ít nhất 8 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_BLANK(1035, "Mật khẩu không được để trống", HttpStatus.BAD_REQUEST),
    PASSWORD_REGET(1036, "Mật khẩu không được chứa khoảng trắng", HttpStatus.BAD_REQUEST),
    NAME_BLANK(1037, "Họ tên không được để trống", HttpStatus.BAD_REQUEST),
    NAME_SIZE(1038, "Họ tên phải từ 3 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    NAME_REGET(1039, "Họ tên chỉ được chứa chữ cái và khoảng trắng", HttpStatus.BAD_REQUEST),
    EMAIL_BLANK(1040, "Email không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_SIZE(1041, "Email không được vượt quá 100 ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_REGET(1042, "Email không đúng định dạng", HttpStatus.BAD_REQUEST),
    PHONE_INVALID(1043, "Số điện thoại không đúng định dạng (VD: 0912345678)", HttpStatus.BAD_REQUEST),
    DOB_INVALID(1044, "Tuổi phải từ 6 tuổi trở lên", HttpStatus.BAD_REQUEST),
    TOKEN_BLANK(1045, "Token không được để trống", HttpStatus.BAD_REQUEST),
    OTP_BLANK(1046, "Mã OTP không được để trống", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1047, "Mã OTP phải gồm 6 chữ số", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1048, "Mã OTP không đúng", HttpStatus.BAD_REQUEST),
    TOO_MANY_ATTEMPTS(1049, "Bạn đã nhập sai quá số lần cho phép, vui lòng gửi lại mã mới", HttpStatus.BAD_REQUEST),

    // SOCIAL LOGIN
    SOCIAL_LOGIN_FAILED(1051, "Đăng nhập mạng xã hội thất bại, vui lòng thử lại", HttpStatus.BAD_REQUEST),
    INVALID_SOCIAL_TOKEN(1052, "Token từ mạng xã hội không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    SOCIAL_EMAIL_NOT_PROVIDED(1053, "Tài khoản mạng xã hội chưa cấp quyền truy cập email", HttpStatus.BAD_REQUEST),
    SOCIAL_PROVIDER_NOT_SUPPORTED(1054, "Nhà cung cấp đăng nhập không được hỗ trợ", HttpStatus.BAD_REQUEST),
    SOCIAL_ACCOUNT_ALREADY_LINKED(1055, "Email này đã được liên kết với một tài khoản khác", HttpStatus.CONFLICT),
    EXTERNAL_SERVICE_UNAVAILABLE(1056, "Không thể kết nối tới dịch vụ bên ngoài, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    SOCIAL_EMAIL_NOT_VERIFIED(1057, "Tài khoản xã hội của bạn chưa xác minh email. Vui lòng xác minh hoặc đăng nhập bằng phương thức khác.", HttpStatus.BAD_REQUEST),

    // SELECT ROLE
    ROLE_ALREADY_SELECTED(1061, "Bạn đã chọn vai trò trước đó, vui lòng liên hệ Admin để thay đổi", HttpStatus.CONFLICT),
    ROLE_SELECTION_EXPIRED(1062, "Thời gian chọn vai trò đã hết, vui lòng liên hệ Admin", HttpStatus.GONE),
    ROLE_SELECTION_NOT_ALLOWED(1063, "Chức năng này chỉ dành cho tài khoản đăng ký qua mạng xã hội", HttpStatus.FORBIDDEN),
    INVALID_ROLE_SELECTION(1064, "Vai trò được chọn không hợp lệ", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1065, "Vai trò không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),

    // FILE UPLOAD
    FILE_TYPE_NOT_SUPPORTED(1071, "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận: JPG, PNG, GIF, WebP, BMP, SVG", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1072, "Tải tệp lên thất bại, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),

    ;

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatusCode;

    ErrorCode(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatus;
    }
}
