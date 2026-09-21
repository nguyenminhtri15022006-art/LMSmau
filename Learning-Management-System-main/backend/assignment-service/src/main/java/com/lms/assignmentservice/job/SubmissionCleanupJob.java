package com.lms.assignmentservice.job;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.enums.SubmissionStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Dọn dẹp submission bị stuck ở PENDING/JUDGING do lỗi Judge0 hoặc async crash.
 * Dùng updateMulti() trên MongoDB để xử lý hàng loạt nhanh chóng.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubmissionCleanupJob {

    MongoTemplate mongoTemplate;

    @Value("${assignment.cleanup.submission-stuck-threshold-minutes:10}")
    @NonFinal
    long stuckThresholdMinutes;

    @Scheduled(cron = "${assignment.cleanup.submission-stuck-cron:0 */5 * * * *}")
    public void cleanupStuckSubmissions() {
        Instant threshold = Instant.now().minus(stuckThresholdMinutes, ChronoUnit.MINUTES);

        Query query = new Query(Criteria.where("status").in("PENDING", "JUDGING")
                .and("submittedAt").lt(threshold));

        Update update = new Update()
                .set("status", SubmissionStatus.IE.name())
                .set("score", 0)
                .set("judgeAt", Instant.now());

        var result = mongoTemplate.updateMulti(query, update, Submission.class);
        long modified = result.getModifiedCount();

        if (modified > 0) {
            log.info("Submission cleanup: {} stuck submissions marked IE (threshold: {} min)",
                    modified, stuckThresholdMinutes);
        }
    }
}
