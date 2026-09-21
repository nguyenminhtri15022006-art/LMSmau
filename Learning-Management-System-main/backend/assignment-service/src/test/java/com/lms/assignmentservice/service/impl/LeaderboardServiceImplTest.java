package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.LeaderboardEntryResponse;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.httpClient.IdentityClient;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    GradeRepository gradeRepository;

    @Mock
    QuizAttemptRepository quizAttemptRepository;

    @Mock
    IdentityClient identityClient;

    @InjectMocks
    LeaderboardServiceImpl leaderboardService;

    @Test
    void codingLeaderboardUsesMongoFieldNamesAndMapsResults() {
        Document row = new Document("_id", "user-1")
                .append("totalScore", 180)
                .append("solvedCount", 2)
                .append("totalCount", 3);

        when(mongoTemplate.aggregate(
                org.mockito.ArgumentMatchers.any(Aggregation.class),
                eq("submissions"),
                eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(row), new Document()));

        List<LeaderboardEntryResponse> result = leaderboardService.getCodingLeaderboard("course-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo("user-1");
        assertThat(result.getFirst().getScore()).isEqualTo(180);
        assertThat(result.getFirst().getSolvedCount()).isEqualTo(2);
        assertThat(result.getFirst().getAccuracy()).isEqualTo(66.7);

        ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("submissions"), eq(Document.class));
        String pipeline = aggregationCaptor.getValue().toString();
        assertThat(pipeline)
                .contains("course_id")
                .contains("user_id")
                .contains("problem_id");
    }
}
