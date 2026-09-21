package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.entity.Grade;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.entity.Quiz;
import com.lms.assignmentservice.entity.QuizAnswerRecord;
import com.lms.assignmentservice.entity.QuizAttempt;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizAnswerRecordRepository;
import com.lms.assignmentservice.service.QuizGradingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizGradingServiceImpl implements QuizGradingService {

    private static final QuizAnswerRecord.AnswerIdsConverter ANSWER_IDS_CONVERTER =
            new QuizAnswerRecord.AnswerIdsConverter();

    QuestionRepository questionRepository;
    QuizAnswerRecordRepository quizAnswerRecordRepository;
    GradeRepository gradeRepository;

    @Transactional
    @Override
    public GradingResult gradeAttempt(QuizAttempt attempt, Map<Integer, List<Integer>> overrides) {
        Long attemptId = attempt.getAttemptId();

        Map<Integer, QuizAnswerRecord> existingRecordsMap = quizAnswerRecordRepository
                .findByQuizAttempt_AttemptId(attemptId)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getQuestion().getQuestionId(),
                        r -> r,
                        (v1, v2) -> v2
                ));

        Quiz quiz = attempt.getQuiz();
        List<Question> allQuestions = questionRepository.findAllByQuizIdWithAnswers(quiz.getQuizId());
        Map<Integer, BigDecimal> effectiveScoreByQuestionId = quiz.getQuizQuestions().stream()
                .collect(Collectors.toMap(
                        quizQuestion -> quizQuestion.getQuestion().getQuestionId(),
                        quizQuestion -> BigDecimal.valueOf(quizQuestion.getEffectiveScore()),
                        (left, right) -> right
                ));

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxQuestionScore = allQuestions.stream()
                .map(question -> effectiveScoreByQuestionId.getOrDefault(
                        question.getQuestionId(),
                        BigDecimal.valueOf(question.getScore())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal quizTotalScore = attempt.getTotalScore();

        for (Question question : allQuestions) {
            Integer qId = question.getQuestionId();

            List<Integer> selectedIds;
            if (!attempt.isExpired() && overrides != null && overrides.containsKey(qId)) {
                selectedIds = overrides.get(qId);
            } else if (existingRecordsMap.containsKey(qId)) {
                selectedIds = existingRecordsMap.get(qId).getSelectedAnswerIds();
            } else {
                selectedIds = List.of();
            }

            List<Integer> correctAnswerIds = question.getAnswers().stream()
                    .filter(Answer::getCorrect)
                    .map(Answer::getAnswerId)
                    .toList();

            boolean isCorrect = !selectedIds.isEmpty()
                    && selectedIds.size() == correctAnswerIds.size()
                    && new HashSet<>(correctAnswerIds).containsAll(selectedIds);

            BigDecimal rawQuestionScore = effectiveScoreByQuestionId.getOrDefault(
                    question.getQuestionId(),
                    BigDecimal.valueOf(question.getScore()));
            BigDecimal questionScore = calculateNormalizedQuestionScore(rawQuestionScore, maxQuestionScore, quizTotalScore);
            BigDecimal earnedScore = isCorrect ? questionScore : BigDecimal.ZERO;

            totalScore = totalScore.add(earnedScore);

            upsertAnswerRecord(attemptId, qId, selectedIds, earnedScore);
        }

        boolean passed = totalScore.compareTo(quizTotalScore
                .multiply(BigDecimal.valueOf(quiz.getPassScore()))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)) >= 0;

        upsertGrade(attempt, totalScore.floatValue());

        return new GradingResult(totalScore, passed);
    }

    private BigDecimal calculateNormalizedQuestionScore(
            BigDecimal questionScore,
            BigDecimal maxQuestionScore,
            BigDecimal quizTotalScore) {
        if (maxQuestionScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return quizTotalScore
                .multiply(questionScore)
                .divide(maxQuestionScore, 4, RoundingMode.HALF_UP);
    }

    private void upsertAnswerRecord(Long attemptId, Integer questionId, List<Integer> selectedIds, BigDecimal earnedScore) {
        quizAnswerRecordRepository.upsertRecord(
                attemptId,
                questionId,
                ANSWER_IDS_CONVERTER.convertToDatabaseColumn(selectedIds),
                earnedScore);
    }

    private void upsertGrade(QuizAttempt attempt, float score) {
        Quiz quiz = attempt.getQuiz();
        String userId = attempt.getUserId();
        Integer quizId = quiz.getQuizId();

        Optional<Grade> existing = gradeRepository.findByUserIdAndQuiz_QuizId(userId, quizId);

        Grade grade;
        if (existing.isPresent()) {
            grade = existing.get();
            if (score > grade.getBestScore()) {
                grade.setBestScore(score);
            }
            grade.setAttemptsCount(grade.getAttemptsCount() + 1);
        } else {
            grade = Grade.builder()
                    .userId(userId)
                    .courseId(quiz.getCourseId())
                    .quiz(quiz)
                    .bestScore(score)
                    .attemptsCount(1)
                    .build();
        }
        grade.setLastAttemptAt(Instant.now());

        gradeRepository.save(grade);
        log.info("Upsert grade: user={} quiz={} best_score={} attempts={}",
                userId, quizId, grade.getBestScore(), grade.getAttemptsCount());
    }
}
