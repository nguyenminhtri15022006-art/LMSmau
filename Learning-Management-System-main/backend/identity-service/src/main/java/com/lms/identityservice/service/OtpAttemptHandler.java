package com.lms.identityservice.service;

import com.lms.identityservice.repository.EmailVerificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpAttemptHandler {

    EmailVerificationRepository emailVerificationRepository;

    // REQUIRES_NEW → tạo transaction mới, độc lập với transaction của caller
    // → dù caller throw exception và rollback, transaction này vẫn commit
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increment(Long verificationId) {
        emailVerificationRepository.incrementAttempts(verificationId);
    }
}