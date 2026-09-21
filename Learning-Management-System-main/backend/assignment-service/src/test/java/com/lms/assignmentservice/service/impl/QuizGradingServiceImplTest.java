package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Grade;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.entity.Quiz;
import com.lms.assignmentservice.entity.QuizAttempt;
import com.lms.assignmentservice.entity.QuizQuestion;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizAnswerRecordRepository;
import com.lms.assignmentservice.service.QuizGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizGradingServiceImplTest {

    @Mock
    QuestionRepository questionRepository;

    @Mock
    QuizAnswerRecordRepository quizAnswerRecordRepository;

    @Mock
    GradeRepository gradeRepository;

    QuizGradingServiceImpl gradingService;

    @BeforeEach
    void setUp() {
        gradingService = new QuizGradingServiceImpl(
                questionRepository,
                quizAnswerRecordRepository,
                gradeRepository);
    }

    @Test
    void gradeAttemptNormalizesQuestionScoresToAttemptTotalScoreSnapshot() {
        Quiz quiz = Quiz.builder()
                .quizId(10)
                .courseId("course-1")
                .title("Quiz")
                .createdBy("instructor-1")
                .totalScore((short) 120)
                .passScore((byte) 50)
                .quizQuestions(new ArrayList<>())
                .build();
        List<Question> questions = new ArrayList<>();
        Map<Integer, List<Integer>> answers = new HashMap<>();

        for (int index = 1; index <= 20; index++) {
            Question question = question(index);
            questions.add(question);
            quiz.getQuizQuestions().add(QuizQuestion.builder()
                    .quiz(quiz)
                    .question(question)
                    .overrideScore((short) 10)
                    .orderIndex((short) index)
                    .build());

            if (index <= 15) {
                answers.put(question.getQuestionId(), List.of(question.getQuestionId() * 10));
            }
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .attemptId(1L)
                .quiz(quiz)
                .userId("student-1")
                .totalScore(BigDecimal.valueOf(100))
                .build();

        when(quizAnswerRecordRepository.findByQuizAttempt_AttemptId(1L)).thenReturn(List.of());
        when(questionRepository.findAllByQuizIdWithAnswers(10)).thenReturn(questions);
        when(gradeRepository.findByUserIdAndQuiz_QuizId("student-1", 10)).thenReturn(Optional.empty());

        QuizGradingService.GradingResult result = gradingService.gradeAttempt(attempt, answers);

        assertThat(result.totalScore()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(result.passed()).isTrue();
        verify(gradeRepository).save(any(Grade.class));
    }

    private Question question(int index) {
        Answer correctAnswer = Answer.builder()
                .answerId(index * 10)
                .content("Correct")
                .correct(true)
                .orderIndex((short) 0)
                .build();
        Answer wrongAnswer = Answer.builder()
                .answerId(index * 10 + 1)
                .content("Wrong")
                .correct(false)
                .orderIndex((short) 1)
                .build();

        return Question.builder()
                .questionId(index)
                .courseId("course-1")
                .content("Question " + index)
                .type(QuestionType.SINGLE)
                .score((byte) 10)
                .answers(List.of(correctAnswer, wrongAnswer))
                .build();
    }
}
