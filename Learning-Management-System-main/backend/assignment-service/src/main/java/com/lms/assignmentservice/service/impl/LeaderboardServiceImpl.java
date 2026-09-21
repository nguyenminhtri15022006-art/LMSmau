package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.configuration.GatewayAuthentication;
import com.lms.assignmentservice.dto.response.InternalUserResponse;
import com.lms.assignmentservice.dto.response.LeaderboardEntryResponse;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.httpClient.IdentityClient;
import com.lms.assignmentservice.service.LeaderboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    MongoTemplate mongoTemplate;
    GradeRepository gradeRepository;
    QuizAttemptRepository quizAttemptRepository;
    IdentityClient identityClient;

    @Override
    public List<LeaderboardEntryResponse> getCodingLeaderboard(String courseId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("course_id").is(courseId)),
                Aggregation.group("user_id", "problem_id")
                        .max("score").as("maxScore")
                        .max(
                                ConditionalOperators.when(Criteria.where("status").is(SubmissionStatus.AC.name()))
                                        .then(1)
                                        .otherwise(0)
                        ).as("solved"),
                Aggregation.group("_id.user_id")
                        .sum("maxScore").as("totalScore")
                        .sum("solved").as("solvedCount")
                        .count().as("totalCount"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalScore")),
                Aggregation.limit(100)
        );

        List<Document> docs = mongoTemplate.aggregate(aggregation, "submissions", Document.class)
                .getMappedResults();

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (Document doc : docs) {
            String userId = doc.getString("_id");
            if (userId == null) continue;

            int totalScore = toInt(doc.get("totalScore"));
            int solvedCount = toInt(doc.get("solvedCount"));
            int totalCount = toInt(doc.get("totalCount"));

            double accuracy = totalCount > 0
                    ? Math.round(solvedCount * 1000.0 / totalCount) / 10.0
                    : 0.0;

            entries.add(LeaderboardEntryResponse.builder()
                    .userId(userId)
                    .score(totalScore)
                    .solvedCount(solvedCount)
                    .accuracy(accuracy)
                    .build());
        }

        enrichWithUserProfiles(entries);
        markCurrentUser(entries);
        return entries;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    @Override
    public List<LeaderboardEntryResponse> getQuizLeaderboard(String courseId) {
        List<Object[]> gradeRows = gradeRepository.aggregateLeaderboardByCourse(courseId);
        List<Object[]> timeRows = quizAttemptRepository.aggregateAvgTimeByCourse(courseId);

        Map<String, Long> avgTimeMap = new HashMap<>();
        for (Object[] row : timeRows) {
            String userId = (String) row[0];
            Number avgSec = (Number) row[1];
            avgTimeMap.put(userId, avgSec != null ? Math.round(avgSec.doubleValue() / 60.0) : 0L);
        }

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (Object[] row : gradeRows) {
            String userId = (String) row[0];
            Number totalScore = (Number) row[1];
            Number quizCount = (Number) row[2];
            Long avgTimeMin = avgTimeMap.getOrDefault(userId, 0L);

            LeaderboardEntryResponse entry = LeaderboardEntryResponse.builder()
                    .userId(userId)
                    .score(totalScore != null ? totalScore.intValue() : 0)
                    .completedCount(quizCount != null ? quizCount.intValue() : 0)
                    .avgTime(avgTimeMin.intValue())
                    .build();
            entries.add(entry);
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntryResponse::getScore).reversed());

        enrichWithUserProfiles(entries);
        markCurrentUser(entries);
        return entries;
    }

    private void enrichWithUserProfiles(List<LeaderboardEntryResponse> entries) {
        if (entries.isEmpty()) return;

        Map<String, InternalUserResponse> userCache;
        try {
            Set<String> userIds = entries.stream()
                    .map(LeaderboardEntryResponse::getUserId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            userCache = identityClient.getUsersByIds(userIds)
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getUserId() != null)
                    .collect(java.util.stream.Collectors.toMap(InternalUserResponse::getUserId, user -> user));
        } catch (Exception e) {
            log.warn("Failed to fetch leaderboard user profiles", e);
            userCache = Map.of();
        }

        for (LeaderboardEntryResponse entry : entries) {
            InternalUserResponse user = userCache.get(entry.getUserId());
            if (user != null) {
                entry.setName(user.getFullname() != null ? user.getFullname() : entry.getUserId());
                entry.setAvatarUrl(user.getAvatarUrl());
            } else {
                entry.setName(entry.getUserId());
            }
        }
    }

    private void markCurrentUser(List<LeaderboardEntryResponse> entries) {
        String currentUserId;
        try {
            currentUserId = GatewayAuthentication.currentUserId();
        } catch (Exception e) {
            return;
        }
        for (LeaderboardEntryResponse entry : entries) {
            if (currentUserId.equals(entry.getUserId())) {
                entry.setCurrentUser(true);
                break;
            }
        }
    }
}
