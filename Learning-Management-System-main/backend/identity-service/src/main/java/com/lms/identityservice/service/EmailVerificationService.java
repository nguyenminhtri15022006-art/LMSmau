package com.lms.identityservice.service;

import com.lms.identityservice.dto.request.ForgotPasswordRequest;
import com.lms.identityservice.dto.request.ResetPasswordRequest;
import com.lms.identityservice.dto.request.SendEmailVerificationRequest;
import com.lms.identityservice.dto.request.VerifyEmailRequest;
public interface EmailVerificationService {
    void sendVerifyEmail(SendEmailVerificationRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void sendResetPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
