package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.response.AdminCodeJudgeActivityResponse;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.service.AdminCodeJudgeActivityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCodeJudgeActivityServiceImpl implements AdminCodeJudgeActivityService {

    static final int DEFAULT_HOURS = 24;

    MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<AdminCodeJudgeActivityResponse> getActivity(int hours) {
        int effectiveHours = hours > 0 ? hours : DEFAULT_HOURS;
        Instant start = Instant.now().minus(Duration.ofHours(effectiveHours));

        List<Document> pipeline = List.of(
                new Document("$match", new Document("submitted_at", new Document("$gte", start))),
                new Document("$group", new Document("_id", "$language")
                        .append("total", new Document("$sum", 1))
                        .append("accepted", countStatus(SubmissionStatus.AC))
                        .append("wrongAnswer", countStatus(SubmissionStatus.WA))
                        .append("runtimeError", countStatus(SubmissionStatus.RE))
                        .append("avgTimeMs", new Document("$avg", "$exec_time_ms"))),
                new Document("$sort", new Document("total", -1))
        );

        return mongoTemplate
                .getCollection(mongoTemplate.getCollectionName(Submission.class))
                .aggregate(pipeline)
                .map(this::toResponse)
                .into(new java.util.ArrayList<>());
    }

    private Document countStatus(SubmissionStatus status) {
        return new Document("$sum", new Document("$cond", List.of(
                new Document("$eq", List.of("$status", status.name())),
                1,
                0
        )));
    }

    private AdminCodeJudgeActivityResponse toResponse(Document row) {
        long total = numberValue(row, "total");
        long accepted = numberValue(row, "accepted");
        long wrongAnswer = numberValue(row, "wrongAnswer");
        long runtimeError = numberValue(row, "runtimeError");
        Double avgTimeMs = row.get("avgTimeMs", Number.class) != null ? row.get("avgTimeMs", Number.class).doubleValue() : 0.0;

        return AdminCodeJudgeActivityResponse.builder()
                .language(row.getString("_id"))
                .total(total)
                .accepted(accepted)
                .wrongAnswer(wrongAnswer)
                .runtimeError(runtimeError)
                .acceptanceRate(total > 0 ? (int) Math.round((accepted * 100.0d) / total) : 0)
                .avgTimeMs(avgTimeMs)
                .build();
    }

    private long numberValue(Document row, String key) {
        Number value = row.get(key, Number.class);
        return value != null ? value.longValue() : 0L;
    }
}
