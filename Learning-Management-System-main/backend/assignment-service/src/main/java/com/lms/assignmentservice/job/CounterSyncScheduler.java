package com.lms.assignmentservice.job;

import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.repository.ProblemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Đồng bộ counter cache (total_submit, total_accepted) từ MongoDB → MySQL.
 * Eventual Consistency: chạy mỗi ngày 1 lần, overwrite con số thực tế từ MongoDB.
 * Idempotent: chạy 1 lần hay 100 lần kết quả vẫn đúng.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CounterSyncScheduler {

    ProblemRepository problemRepository;
    MongoTemplate mongoTemplate;

    private static final String SUBMISSIONS_COLLECTION = "submissions";

    @Scheduled(cron = "${assignment.cleanup.counter-sync-cron:0 0 3 * * *}")
    @Transactional
    public void syncCounters() {
        List<Problem> problems = problemRepository.findAll();

        int updated = 0;
        for (Problem problem : problems) {
            Integer problemId = problem.getProblemId();

            long actualTotalSubmit = mongoTemplate.count(
                    Query.query(Criteria.where("problem_id").is(problemId)),
                    SUBMISSIONS_COLLECTION);

            long actualTotalAccepted = mongoTemplate.count(
                    Query.query(Criteria.where("problem_id").is(problemId)
                            .and("status").is(SubmissionStatus.AC.name())),
                    SUBMISSIONS_COLLECTION);

            boolean needsUpdate = false;

            if (problem.getTotalSubmit() != actualTotalSubmit) {
                problemRepository.setTotalSubmit(problemId, (int) actualTotalSubmit);
                needsUpdate = true;
            }
            if (problem.getTotalAccepted() != actualTotalAccepted) {
                problemRepository.setTotalAccepted(problemId, (int) actualTotalAccepted);
                needsUpdate = true;
            }

            if (needsUpdate) {
                updated++;
                log.debug("Synced problem [{}]: totalSubmit {}→{}, totalAccepted {}→{}",
                        problemId,
                        problem.getTotalSubmit(), actualTotalSubmit,
                        problem.getTotalAccepted(), actualTotalAccepted);
            }
        }

        if (updated > 0) {
            log.info("CounterSync: updated {} problems (total: {})", updated, problems.size());
        }
    }
}
