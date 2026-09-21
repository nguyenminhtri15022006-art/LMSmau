package com.lms.identityservice.controller;

import com.lms.identityservice.dto.ApiResponse;
import com.lms.identityservice.dto.request.ForgotPasswordRequest;
import com.lms.identityservice.dto.request.ResetPasswordRequest;
import com.lms.identityservice.dto.request.SendEmailVerificationRequest;
import com.lms.identityservice.dto.request.VerifyEmailRequest;
import com.lms.identityservice.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailVerificationController {

    EmailVerificationService emailVerificationService;

    @PostMapping("/verify-email/send")
    ApiResponse<Void> sendVerifyEmail(@RequestBody @Valid SendEmailVerificationRequest request) {
        emailVerificationService.sendVerifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Mã xác thực đã được gửi đến " + request.getEmail())
                .build();
    }

    @PostMapping("/verify-email")
    ApiResponse<Void> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Xác thực email thành công")
                .build();
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        emailVerificationService.sendResetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Nếu email tồn tại, mã xác thực đã được gửi đến hộp thư của bạn")
                .build();
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        emailVerificationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Đặt lại mật khẩu thành công")
                .build();
    }
}
