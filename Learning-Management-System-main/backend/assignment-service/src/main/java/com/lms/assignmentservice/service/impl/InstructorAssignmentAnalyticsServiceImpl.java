package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.response.InstructorActivityPointResponse;
import com.lms.assignmentservice.dto.response.InstructorAssignmentAnalyticsResponse;
import com.lms.assignmentservice.dto.response.InstructorSubmissionSummaryResponse;
import com.lms.assignmentservice.dto.response.InternalUserResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.QuizAttempt;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.repository.httpClient.IdentityClient;
import com.lms.assignmentservice.service.InstructorAssignmentAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InstructorAssignmentAnalyticsServiceImpl implements InstructorAssignmentAnalyticsService {

    private static final String TYPE_QUIZ = "quiz";
    private static final String TYPE_CODING = "coding";
    private static final String FALLBACK_STUDENT_NAME = "Người học";
    private static final ZoneId ZONE_ID = ZoneId.of("UTC");

    QuizRepository quizRepository;
    QuizAttemptRepository quizAttemptRepository;
    ProblemRepository problemRepository;
    CourseClient courseClient;
    IdentityClient identityClient;
    MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public InstructorAssignmentAnalyticsResponse getAnalytics(String courseId, String type) {
        String normalizedType = normalizeType(type);
        long studentCount = Objects.requireNonNullElse(courseClient.countStudents(courseId), 0L);

        if (TYPE_QUIZ.equals(normalizedType)) {
            return getQuizAnalytics(courseId, studentCount);
        }

        return getCodingAnalytics(courseId, studentCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorSubmissionSummaryResponse> getSubmissions(String courseId, String type, Pageable pageable) {
        String normalizedType = normalizeType(type);
        if (TYPE_QUIZ.equals(normalizedType)) {
            Page<QuizAttempt> attempts = quizAttemptRepository
                    .findSubmittedByCourseIdOrderBySubmittedAtDesc(courseId, pageable);
            Map<String, InternalUserResponse> userById = getUsersByIdsSafely(attempts.getContent().stream()
                    .map(QuizAttempt::getUserId)
                    .collect(Collectors.toSet()));
            return attempts.map(attempt -> toQuizSubmissionSummary(attempt, userById.get(attempt.getUserId())));
        }

        return getCodingSubmissions(courseId, pageable);
    }

    private InstructorAssignmentAnalyticsResponse getQuizAnalytics(String courseId, long studentCount) {
        long quizCount = quizRepository.countByCourseIdAndPublishedTrueAndDeletedFalse(courseId);
        long completedPairs = quizAttemptRepository.countUniqueSubmittedStudentQuizPairs(courseId);
        long totalSubmissions = quizAttemptRepository.countSubmittedByCourseId(courseId);
        List<InstructorSubmissionSummaryResponse> recentSubmissions = toQuizSubmissionSummaries(quizAttemptRepository
                .findSubmittedByCourseIdOrderBySubmittedAtDesc(courseId, PageRequest.of(0, 3))
                .getContent());

        return InstructorAssignmentAnalyticsResponse.builder()
                .completionPercent(calculatePercent(completedPairs, studentCount * quizCount))
                .completionTrendPercent(calculateQuizTrend(courseId))
                .activity(getQuizActivity(courseId))
                .recentSubmissions(recentSubmissions)
                .totalSubmissions(totalSubmissions)
                .build();
    }

    private InstructorAssignmentAnalyticsResponse getCodingAnalytics(String courseId, long studentCount) {
        long publicProblemCount = problemRepository.countByCourseIdAndIsPublicTrueAndDeletedFalse(courseId);
        long completedPairs = countAcceptedStudentProblemPairs(courseId);
        long totalSubmissions = countCodingSubmissions(courseId);

        return InstructorAssignmentAnalyticsResponse.builder()
                .completionPercent(calculatePercent(completedPairs, studentCount * publicProblemCount))
                .completionTrendPercent(calculateCodingTrend(courseId))
                .activity(getCodingActivity(courseId))
                .recentSubmissions(getCodingSubmissions(courseId, PageRequest.of(0, 3)).getContent())
                .totalSubmissions(totalSubmissions)
                .build();
    }

    private List<InstructorActivityPointResponse> getQuizActivity(String courseId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate startDate = today.minusDays(6);
        Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant();

        Map<LocalDate, Long> countByDate = quizAttemptRepository
                .countSubmittedActivityByCourse(courseId, start, end)
                .stream()
                .collect(Collectors.toMap(this::toActivityDate, row -> ((Number) row[1]).longValue()));

        return buildSevenDayActivity(today, countByDate);
    }

    private List<InstructorActivityPointResponse> getCodingActivity(String courseId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate startDate = today.minusDays(6);
        Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
        Map<LocalDate, Long> countByDate = new HashMap<>();

        List<Document> pipeline = List.of(
                new Document("$match", new Document("course_id", courseId)
                        .append("submitted_at", new Document("$gte", start).append("$lt", end))),
                new Document("$group", new Document("_id", new Document("$dateToString", new Document("format", "%Y-%m-%d")
                        .append("date", "$submitted_at")
                        .append("timezone", ZONE_ID.getId())))
                        .append("count", new Document("$sum", 1)))
        );

        for (Document row : mongoTemplate.getCollection(mongoTemplate.getCollectionName(Submission.class)).aggregate(pipeline)) {
            countByDate.put(LocalDate.parse(row.getString("_id")), row.get("count", Number.class).longValue());
        }

        return buildSevenDayActivity(today, countByDate);
    }

    private Page<InstructorSubmissionSummaryResponse> getCodingSubmissions(String courseId, Pageable pageable) {
        Query query = Query.query(Criteria.where("course_id").is(courseId))
                .with(Sort.by(Sort.Direction.DESC, "submitted_at"))
                .with(pageable);
        List<Submission> submissions = mongoTemplate.find(query, Submission.class);

        Query countQuery = Query.query(Criteria.where("course_id").is(courseId));
        long total = mongoTemplate.count(countQuery, Submission.class);

        Map<Integer, Problem> problemById = problemRepository.findAllById(
                        submissions.stream()
                                .map(Submission::getProblemId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Problem::getProblemId, Function.identity()));
        Map<String, InternalUserResponse> userById = getUsersByIdsSafely(submissions.stream()
                .map(Submission::getUserId)
                .collect(Collectors.toSet()));

        List<InstructorSubmissionSummaryResponse> content = submissions.stream()
                .map(submission -> toCodingSubmissionSummary(
                        submission,
                        problemById.get(submission.getProblemId()),
                        userById.get(submission.getUserId())))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private List<InstructorSubmissionSummaryResponse> toQuizSubmissionSummaries(List<QuizAttempt> attempts) {
        Map<String, InternalUserResponse> userById = getUsersByIdsSafely(attempts.stream()
                .map(QuizAttempt::getUserId)
                .collect(Collectors.toSet()));
        return attempts.stream()
                .map(attempt -> toQuizSubmissionSummary(attempt, userById.get(attempt.getUserId())))
                .toList();
    }

    private InstructorSubmissionSummaryResponse toQuizSubmissionSummary(QuizAttempt attempt, InternalUserResponse user) {
        int scorePercent = calculateQuizScorePercent(attempt);

        return InstructorSubmissionSummaryResponse.builder()
                .studentName(resolveStudentName(user))
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .exerciseTitle(attempt.getQuiz().getTitle())
                .scorePercent(scorePercent)
                .submittedAt(attempt.getSubmittedAt())
                .status(attempt.getStatus().name())
                .type("quiz")
                .build();
    }

    private InstructorSubmissionSummaryResponse toCodingSubmissionSummary(
            Submission submission,
            Problem problem,
            InternalUserResponse user) {
        String title = problem != null ? problem.getTitle() : "Coding challenge";
        int scorePercent = calculatePercent(submission.getScore() != null ? submission.getScore() : 0, 100);

        return InstructorSubmissionSummaryResponse.builder()
                .studentName(resolveStudentName(user))
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .exerciseTitle(title)
                .scorePercent(scorePercent)
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .type("coding")
                .build();
    }

    private long countAcceptedStudentProblemPairs(String courseId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("course_id").is(courseId)
                        .and("status").is(SubmissionStatus.AC.name())),
                Aggregation.group("user_id", "problem_id"),
                Aggregation.count().as("count")
        );

        CountResult result = mongoTemplate.aggregate(aggregation, "submissions", CountResult.class)
                .getUniqueMappedResult();
        return result != null ? result.count() : 0L;
    }

    private long countCodingSubmissions(String courseId) {
        return mongoTemplate.count(Query.query(Criteria.where("course_id").is(courseId)), Submission.class);
    }

    private int calculateQuizTrend(String courseId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        Instant currentStart = today.minusDays(6).atStartOfDay(ZONE_ID).toInstant();
        Instant currentEnd = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
        Instant previousStart = today.minusDays(13).atStartOfDay(ZONE_ID).toInstant();
        Instant previousEnd = currentStart;

        long current = sumQuizActivity(courseId, currentStart, currentEnd);
        long previous = sumQuizActivity(courseId, previousStart, previousEnd);
        return calculateTrend(current, previous);
    }

    private int calculateCodingTrend(String courseId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        Instant currentStart = today.minusDays(6).atStartOfDay(ZONE_ID).toInstant();
        Instant currentEnd = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
        Instant previousStart = today.minusDays(13).atStartOfDay(ZONE_ID).toInstant();
        Instant previousEnd = currentStart;

        long current = mongoTemplate.count(Query.query(Criteria.where("course_id").is(courseId)
                .and("submitted_at").gte(currentStart).lt(currentEnd)), Submission.class);
        long previous = mongoTemplate.count(Query.query(Criteria.where("course_id").is(courseId)
                .and("submitted_at").gte(previousStart).lt(previousEnd)), Submission.class);
        return calculateTrend(current, previous);
    }

    private long sumQuizActivity(String courseId, Instant start, Instant end) {
        return quizAttemptRepository.countSubmittedActivityByCourse(courseId, start, end)
                .stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
    }

    private List<InstructorActivityPointResponse> buildSevenDayActivity(
            LocalDate today,
            Map<LocalDate, Long> countByDate) {
        LocalDate startDate = today.minusDays(6);
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> {
                    LocalDate date = startDate.plusDays(index);
                    return InstructorActivityPointResponse.builder()
                            .date(date)
                            .label(date.equals(today) ? "Today" : date.getDayOfWeek().name().substring(0, 3))
                            .count(countByDate.getOrDefault(date, 0L))
                            .build();
                })
                .toList();
    }

    private LocalDate toActivityDate(Object[] row) {
        Object value = row[0];
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private int calculateQuizScorePercent(QuizAttempt attempt) {
        BigDecimal totalScore = attempt.getTotalScore();
        if (totalScore == null || totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            Short quizTotal = attempt.getQuiz().getTotalScore();
            return calculatePercent(attempt.getScore() != null ? attempt.getScore() : 0, quizTotal != null ? quizTotal : 100);
        }
        return calculatePercent(attempt.getScore() != null ? attempt.getScore() : 0, totalScore.doubleValue());
    }

    private int calculatePercent(double value, double denominator) {
        if (denominator <= 0) {
            return 0;
        }
        int percent = (int) Math.round((value * 100.0d) / denominator);
        return Math.max(0, Math.min(100, percent));
    }

    private int calculateTrend(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round(((current - previous) * 100.0d) / previous);
    }

    private Map<String, InternalUserResponse> getUsersByIdsSafely(Set<String> userIds) {
        Set<String> normalizedIds = userIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .collect(Collectors.toSet());
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        try {
            return identityClient.getUsersByIds(normalizedIds)
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getUserId() != null)
                    .collect(Collectors.toMap(InternalUserResponse::getUserId, Function.identity()));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String resolveStudentName(InternalUserResponse user) {
        if (user != null && user.getFullname() != null && !user.getFullname().isBlank()) {
            return user.getFullname();
        }
        return FALLBACK_STUDENT_NAME;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? TYPE_QUIZ : type.trim().toLowerCase();
        if (TYPE_QUIZ.equals(normalized) || TYPE_CODING.equals(normalized)) {
            return normalized;
        }
        throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
    }

    private record CountResult(long count) {
    }
}
