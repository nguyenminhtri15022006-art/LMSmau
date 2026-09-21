package com.lms.assignmentservice.job;

import com.lms.assignmentservice.entity.QuizAttempt;
import com.lms.assignmentservice.enums.AttemptStatusType;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.service.QuizGradingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizAttemptCleanupJob {

    QuizAttemptRepository quizAttemptRepository;
    QuizGradingService quizGradingService;
    PlatformTransactionManager transactionManager;

    @Value("${quiz.abandoned-threshold-days}")
    @NonFinal
    Integer abandonedThresholdDays;

    @Value("${quiz.cleanup.page-size:100}")
    @NonFinal
    int pageSize;

    /**
     * Job 1: Dọn dẹp attempts quá hạn (có expiresAt < now).
     * Chạy mỗi phút. Dùng Pageable để tránh OOM.
     */
    @Scheduled(cron = "${quiz.cleanup.expired-cron:0 * * * * *}")
    public void cleanupExpiredAttempts() {
        Instant now = Instant.now();
        int page = 0;
        int totalProcessed = 0;

        Page<QuizAttempt> expiredPage;
        do {
            expiredPage = quizAttemptRepository.findByStatusAndExpiresAtBefore(
                    now, PageRequest.of(page, pageSize));

            for (QuizAttempt attempt : expiredPage.getContent()) {
                if (processExpiredAttempt(attempt.getAttemptId(), "TIMEOUT")) {
                    totalProcessed++;
                }
            }
            page++;
        } while (expiredPage.hasNext());

        if (totalProcessed > 0) {
            log.info("Cleanup expired attempts: {} completed", totalProcessed);
        }
    }

    /**
     * Job 2: Dọn dẹp attempts bỏ hoang (startedAt < now - 7 days).
     * Chạy mỗi giờ. Dùng Pageable để tránh OOM.
     */
    @Scheduled(cron = "${quiz.cleanup.abandoned-cron:0 0 * * * *}")
    public void cleanupAbandonedAttempts() {
        Instant threshold = Instant.now().minus(abandonedThresholdDays, ChronoUnit.DAYS);
        int page = 0;
        int totalProcessed = 0;

        Page<QuizAttempt> abandonedPage;
        do {
            abandonedPage = quizAttemptRepository.findByStatusAndStartedAtBefore(
                    threshold, PageRequest.of(page, pageSize));

            for (QuizAttempt attempt : abandonedPage.getContent()) {
                if (processExpiredAttempt(attempt.getAttemptId(), "AUTO_SUBMITTED")) {
                    totalProcessed++;
                }
            }
            page++;
        } while (abandonedPage.hasNext());

        if (totalProcessed > 0) {
            log.info("Cleanup abandoned attempts: {} completed (threshold: {} days)",
                    totalProcessed, abandonedThresholdDays);
        }
    }

    private boolean processExpiredAttempt(Long attemptId, String reason) {
        try {
            Boolean processed = newCleanupTransactionTemplate()
                    .execute(status -> processExpiredAttemptInTransaction(attemptId, reason));
            return Boolean.TRUE.equals(processed);
        } catch (Exception e) {
            log.error("Error processing expired attempt [{}]", attemptId, e);
            return false;
        }
    }

    private boolean processExpiredAttemptInTransaction(Long attemptId, String reason) {
        QuizAttempt currentAttempt = quizAttemptRepository.findById(attemptId)
                .orElse(null);
        if (currentAttempt == null || !AttemptStatusType.IN_PROGRESS.equals(currentAttempt.getStatus())) {
            return false;
        }

        QuizGradingService.GradingResult result =
                quizGradingService.gradeAttempt(currentAttempt, null);

        Instant now = Instant.now();
        currentAttempt.setStatus(AttemptStatusType.EXPIRED);
        currentAttempt.setSubmittedAt(now);
        currentAttempt.setScore(result.totalScore().floatValue());
        currentAttempt.setPassed(result.passed());

        long seconds = java.time.temporal.ChronoUnit.SECONDS
                .between(currentAttempt.getStartedAt(), now);
        currentAttempt.setTimeSpentS((int) seconds);

        quizAttemptRepository.save(currentAttempt);

        log.info("Attempt [{}] (Quiz [{}], User [{}]) marked as EXPIRED. Reason: {}. " +
                        "Time spent: {}s. Score: {}/{}",
                currentAttempt.getAttemptId(),
                currentAttempt.getQuiz().getQuizId(),
                currentAttempt.getUserId(),
                reason,
                currentAttempt.getTimeSpentS(),
                currentAttempt.getScore(),
                currentAttempt.getTotalScore());

        return true;
    }

    private TransactionTemplate newCleanupTransactionTemplate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }
}
