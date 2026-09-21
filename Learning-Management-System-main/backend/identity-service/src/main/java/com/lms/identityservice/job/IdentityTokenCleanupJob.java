package com.lms.identityservice.job;

import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.RefreshTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IdentityTokenCleanupJob {

    RefreshTokenRepository refreshTokenRepository;
    EmailVerificationRepository emailVerificationRepository;

    @Scheduled(
            cron = "${app.cleanup.cron:0 0 2 * * *}",
            zone = "${app.cleanup.zone:UTC}"
    )
    @Transactional
    public void cleanupExpiredIdentityTokens() {
        Instant now = Instant.now();
        int deletedRefreshTokens = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int deletedEmailVerifications = emailVerificationRepository.deleteExpiredOrUsed(now);

        if (deletedRefreshTokens == 0 && deletedEmailVerifications == 0) {
            log.debug("Identity cleanup job completed: no expired/used records found.");
            return;
        }

        log.info(
                "Identity cleanup job completed: removed {} refresh_tokens, {} email_verifications.",
                deletedRefreshTokens,
                deletedEmailVerifications
        );
    }
}
