package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.QuizMapper;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizQuestionRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    QuestionRepository questionRepository;

    @Mock
    QuizQuestionRepository quizQuestionRepository;

    @Mock
    QuizMapper quizMapper;

    @Mock
    CourseClient courseClient;

    @Mock
    AssignmentAuthorizationService authorizationService;

    QuestionServiceImpl questionService;
    Question question;

    @BeforeEach
    void setUp() {
        CourseResourceAuthorizationService resourceAuthorizationService =
                new CourseResourceAuthorizationServiceImpl(authorizationService, courseClient);
        questionService = new QuestionServiceImpl(
                questionRepository,
                quizQuestionRepository,
                quizMapper,
                courseClient,
                authorizationService,
                resourceAuthorizationService);

        question = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .content("Old")
                .type(QuestionType.SINGLE)
                .answers(new ArrayList<>(List.of(Answer.builder()
                        .answerId(1)
                        .content("old")
                        .correct(true)
                        .orderIndex((short) 0)
                        .build())))
                .createdBy("creator-1")
                .build();

        lenient().when(authorizationService.isAdmin()).thenReturn(false);
        lenient().when(authorizationService.currentUserId()).thenReturn("course-instructor");
        InternalCourseResponse course = new InternalCourseResponse();
        course.setInstructorId("course-instructor");
        lenient().when(courseClient.getCourse("course-1")).thenReturn(course);
        lenient().when(questionRepository.findById(100)).thenReturn(Optional.of(question));
    }

    @Test
    void updateQuestionBlocksQuestionReferencedByPublishedOrAttemptedQuiz() {
        when(quizQuestionRepository.countLockedQuizReferencesByQuestionId(100)).thenReturn(1L);

        assertThatThrownBy(() -> questionService.updateQuestion(100, new CreateQuestionRequest()))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUESTION_IN_LOCKED_QUIZ_CANNOT_EDIT));

        verify(quizMapper, never()).updateQuestion(any(), any());
    }

    @Test
    void updateQuestionPreservesExistingAnswerByAnswerIdWhenEditable() {
        Answer existingAnswer = question.getAnswers().getFirst();
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setContent("New");
        request.setType(QuestionType.SINGLE);
        request.setScore((byte) 10);

        CreateQuestionRequest.AnswerRequest first = new CreateQuestionRequest.AnswerRequest();
        first.setAnswerId(1);
        first.setContent("A");
        first.setCorrect(true);
        first.setOrderIndex((short) 0);
        CreateQuestionRequest.AnswerRequest second = new CreateQuestionRequest.AnswerRequest();
        second.setContent("B");
        second.setCorrect(false);
        second.setOrderIndex((short) 1);
        request.setAnswers(List.of(first, second));

        QuestionDetailResponse response = QuestionDetailResponse.builder().questionId(100).build();
        when(quizQuestionRepository.countLockedQuizReferencesByQuestionId(100)).thenReturn(0L);
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(question)).thenReturn(question);
        when(quizMapper.toQuestionResponse(question)).thenReturn(response);

        assertThat(questionService.updateQuestion(100, request)).isSameAs(response);
        assertThat(question.getAnswers().getFirst()).isSameAs(existingAnswer);
        assertThat(question.getAnswers())
                .extracting(Answer::getContent)
                .containsExactly("A", "B");
        assertThat(existingAnswer.getCorrect()).isTrue();
        assertThat(existingAnswer.getOrderIndex()).isEqualTo((short) 0);
        assertThat(question.getAnswers())
                .allSatisfy(answer -> assertThat(answer.getQuestion()).isSameAs(question));
    }
}
