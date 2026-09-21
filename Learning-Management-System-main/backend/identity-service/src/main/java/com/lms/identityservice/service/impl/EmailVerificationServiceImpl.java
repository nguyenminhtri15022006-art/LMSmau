package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.ForgotPasswordRequest;
import com.lms.identityservice.dto.request.ResetPasswordRequest;
import com.lms.identityservice.dto.request.SendEmailVerificationRequest;
import com.lms.identityservice.dto.request.VerifyEmailRequest;
import com.lms.identityservice.entity.EmailVerification;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.VerificationType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.EmailService;
import com.lms.identityservice.service.EmailVerificationService;
import com.lms.identityservice.service.OtpAttemptHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

	@NonFinal
	@Value("${app.otp-expiry-minutes}")
	long otpExpiryMinutes;

	@NonFinal
	@Value("${app.otp.max-attempts:5}")
	int otpMaxAttempts;

	EmailVerificationRepository emailVerificationRepository;
	UserRepository userRepository;
	EmailService emailService;
	OtpAttemptHandler otpAttemptHandler;
	PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void sendVerifyEmail(SendEmailVerificationRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

		if (user.getVerified()) {
			throw new IdentityException(ErrorCode.EMAIL_ALREADY_VERIFIED);
		}

		if (emailVerificationRepository.existsByValidOtp(
				user, VerificationType.VERIFY_EMAIL, Instant.now(), otpMaxAttempts)) {
			throw new IdentityException(ErrorCode.VERIFICATION_EMAIL_ALREADY_SENT);
		}

		String otp = generateOtp();
		saveOtp(user, otp, VerificationType.VERIFY_EMAIL);
		emailService.sendVerifyEmail(user.getEmail(), user.getFullname(), otp);
	}

	@Override
	@Transactional
	public void verifyEmail(VerifyEmailRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

		if (user.getVerified()) {
			throw new IdentityException(ErrorCode.EMAIL_ALREADY_VERIFIED);
		}

		EmailVerification emailVerification = emailVerificationRepository
				.findValidOtp(user, VerificationType.VERIFY_EMAIL, Instant.now(), otpMaxAttempts)
				.orElseThrow(() -> new IdentityException(ErrorCode.INVALID_VERIFICATION_TOKEN));

		validateOtp(emailVerification, request.getOtp());

		emailVerification.setUsedAt(Instant.now());
		emailVerificationRepository.save(emailVerification);

		user.setVerified(true);
		userRepository.save(user);

		log.info("user: {}, verify: {} with code: {}, attempt: {}",
				GatewayAuthentication.currentUserId(),
				user.getVerified(),
				emailVerification.getToken(),
				emailVerification.getAttempts());
	}

	@Override
	@Transactional
	public void sendResetPassword(ForgotPasswordRequest request) {
		userRepository.findByEmail(request.getEmail())
				.ifPresent(user -> {
					emailVerificationRepository.findValidOtp(
									user, VerificationType.RESET_PASSWORD, Instant.now(), otpMaxAttempts)
							.ifPresent(verification -> {
								verification.setUsedAt(Instant.now());
								emailVerificationRepository.save(verification);
							});

					String otp = generateOtp();
					saveOtp(user, otp, VerificationType.RESET_PASSWORD);
					emailService.sendResetPasswordEmail(user.getEmail(), user.getFullname(), otp);
				});
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new IdentityException(ErrorCode.PASSWORD_NOT_MATCH);
		}

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));

		EmailVerification emailVerification = emailVerificationRepository
				.findValidOtp(user, VerificationType.RESET_PASSWORD, Instant.now(), otpMaxAttempts)
				.orElseThrow(() -> new IdentityException(ErrorCode.INVALID_VERIFICATION_TOKEN));

		validateOtp(emailVerification, request.getOtp());

		emailVerification.setUsedAt(Instant.now());
		emailVerificationRepository.save(emailVerification);

		String passwordHash = passwordEncoder.encode(request.getNewPassword());
		user.setPassword(passwordHash);
		userRepository.save(user);
	}

	private String generateOtp() {
		return String.valueOf(100000 + new SecureRandom().nextInt(900000));
	}

	private void saveOtp(User user, String otp, VerificationType type) {
		emailVerificationRepository.save(EmailVerification.builder()
				.user(user)
				.token(otp)
				.type(type)
				.expiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60))
				.attempts(0)
				.build());
	}

	private void validateOtp(EmailVerification verification, String inputOtp) {
		if (verification.isMaxAttempts(otpMaxAttempts)) {
			throw new IdentityException(ErrorCode.TOO_MANY_ATTEMPTS);
		}

		if (!verification.getToken().equals(inputOtp)) {
			otpAttemptHandler.increment(verification.getId());
			int currentAttempts = verification.getAttempts() + 1;
			log.info("attempts: {}", verification.getAttempts());

			int remaining = otpMaxAttempts - currentAttempts;
			throw new IdentityException(remaining > 0 ? ErrorCode.INVALID_OTP : ErrorCode.TOO_MANY_ATTEMPTS);
		}
	}
}
