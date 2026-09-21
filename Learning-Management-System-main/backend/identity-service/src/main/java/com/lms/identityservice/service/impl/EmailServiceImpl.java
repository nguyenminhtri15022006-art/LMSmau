package com.lms.identityservice.service.impl;

import com.lms.identityservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {

	JavaMailSender mailSender;

	@NonFinal
	@Value("${app.otp-expiry-minutes}")
	long otpExpiryMinutes;

	@NonFinal
	@Value("${app.otp.max-attempts:5}")
	int otpMaxAttempts;

	@Override
	@Async
	public void sendVerifyEmail(String to, String fullName, String otp) {
		String subject = "[LMS] Mã xác thực email của bạn";
		String content = """
				<div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
					<h2>Xin chào %s,</h2>
					<p>Cảm ơn bạn đã đăng ký tài khoản LMS.</p>
					<p>Mã xác thực email của bạn là:</p>
					<div style="
						font-size: 32px;
						font-weight: bold;
						letter-spacing: 8px;
						text-align: center;
						padding: 16px;
						background: #F3F4F6;
						border-radius: 8px;
						margin: 16px 0;">
						%s
					</div>
					<p>Mã có hiệu lực trong <strong>%d phút</strong>.</p>
					<p>Nếu bạn nhập sai quá %d lần, mã sẽ bị vô hiệu và cần gửi lại.</p>
					<p style="color: #6B7280; font-size: 13px;">
						Nếu bạn không đăng ký tài khoản, vui lòng bỏ qua email này.
					</p>
				</div>
				""".formatted(fullName, otp, otpExpiryMinutes, otpMaxAttempts);

		sendHtmlEmail(to, subject, content);
	}

	@Override
	@Async
	public void sendResetPasswordEmail(String to, String fullName, String otp) {
		String subject = "[LMS] Mã đặt lại mật khẩu";
		String content = """
				<div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
					<h2>Xin chào %s,</h2>
					<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
					<p>Mã đặt lại mật khẩu của bạn là:</p>
					<div style="
						font-size: 32px;
						font-weight: bold;
						letter-spacing: 8px;
						text-align: center;
						padding: 16px;
						background: #F3F4F6;
						border-radius: 8px;
						margin: 16px 0;">
						%s
					</div>
					<p>Mã có hiệu lực trong <strong>%d phút</strong>.</p>
					<p>Nếu bạn nhập sai quá %d lần, mã sẽ bị vô hiệu và cần gửi lại.</p>
					<p style="color: #6B7280; font-size: 13px;">
						Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
					</p>
				</div>
				""".formatted(fullName, otp, otpExpiryMinutes, otpMaxAttempts);

		sendHtmlEmail(to, subject, content);
	}

	private void sendHtmlEmail(String to, String subject, String content) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(content, true);
			mailSender.send(message);
		} catch (MessagingException e) {
			log.error("Failed to send email to {}: {}", to, e.getMessage());
		}
	}
}



