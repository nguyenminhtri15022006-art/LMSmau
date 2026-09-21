package com.lms.identityservice.service;

public interface EmailService {
    void sendVerifyEmail(String to, String fullName, String otp);

    void sendResetPasswordEmail(String to, String fullName, String otp);
}