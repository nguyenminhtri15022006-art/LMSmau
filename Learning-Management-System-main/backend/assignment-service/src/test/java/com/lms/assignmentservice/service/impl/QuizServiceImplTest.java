package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.AddBulkQuestionQuizRequest;
import com.lms.assignmentservice.dto.request.CreateImportedQuizRequest;
import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.request.SaveQuizDraftRequest;
import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.dto.response.QuestionDetailResponse;
import com.lms.assignmentservice.dto.response.QuizDetailResponse;
import com.lms.assignmentservice.dto.response.QuizInstructorDetailResponse;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.entity.Quiz;
import com.lms.assignmentservice.entity.QuizQuestion;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.QuizMapper;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
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
class QuizServiceImplTest {

    @Mock
    QuizRepository quizRepository;

    @Mock
    QuestionRepository questionRepository;

    @Mock
    QuizAttemptRepository quizAttemptRepository;

    @Mock
    QuizMapper quizMapper;

    @Mock
    AssignmentAuthorizationService authorizationService;

    @Mock
    CourseClient courseClient;

    QuizServiceImpl quizService;
    Quiz quiz;

    @BeforeEach
    void setUp() {
        CourseResourceAuthorizationService resourceAuthorizationService =
                new CourseResourceAuthorizationServiceImpl(authorizationService, courseClient);
        quizService = new QuizServiceImpl(
                quizRepository,
                questionRepository,
                quizAttemptRepository,
                quizMapper,
                authorizationService,
                resourceAuthorizationService);

        quiz = Quiz.builder()
                .quizId(10)
                .courseId("course-1")
                .createdBy("creator-1")
                .published(false)
                .build();

        lenient().when(authorizationService.isAdmin()).thenReturn(false);
        lenient().when(authorizationService.currentUserId()).thenReturn("course-instructor");

        InternalCourseResponse course = new InternalCourseResponse();
        course.setInstructorId("course-instructor");
        lenient().when(courseClient.getCourse("course-1")).thenReturn(course);
        lenient().when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
    }

    @Test
    void internalCourseResponseAcceptsUuidStringId() throws Exception {
        InternalCourseResponse course = new ObjectMapper().readValue(
                "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"instructorId\":\"course-instructor\"}",
                InternalCourseResponse.class);

        assertThat(course.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void instructorQuizListPopulatesAttemptCountInBulk() {
        var pageable = PageRequest.of(0, 20);
        Quiz secondQuiz = Quiz.builder()
                .quizId(11)
                .courseId("course-1")
                .createdBy("creator-1")
                .published(false)
                .build();
        QuizInstructorDetailResponse firstResponse = new QuizInstructorDetailResponse();
        firstResponse.setQuizId(10);
        QuizInstructorDetailResponse secondResponse = new QuizInstructorDetailResponse();
        secondResponse.setQuizId(11);

        when(quizRepository.findByCourseId("course-1", pageable))
                .thenReturn(new PageImpl<>(List.of(quiz, secondQuiz), pageable, 2));
        when(quizMapper.toInstructorResponse(quiz)).thenReturn(firstResponse);
        when(quizMapper.toInstructorResponse(secondQuiz)).thenReturn(secondResponse);
        when(quizAttemptRepository.countGroupByQuizIds(List.of(10, 11)))
                .thenReturn(Collections.singletonList(new Object[]{10, 3L}));

        var result = quizService.getQuizzesForInstructor("course-1", null, pageable);

        assertThat(result.getContent())
                .extracting(QuizInstructorDetailResponse::getAttemptCount)
                .containsExactly(3L, 0L);
        verify(quizAttemptRepository).countGroupByQuizIds(List.of(10, 11));
    }

    @Test
    void courseInstructorCanLoadQuizQuestionsCreatedByAnotherUser() {
        Question question = Question.builder().questionId(100).build();
        quiz.setQuizQuestions(List.of(QuizQuestion.builder()
                .quiz(quiz)
                .question(question)
                .orderIndex((short) 0)
                .build()));

        QuestionDetailResponse response = QuestionDetailResponse.builder()
                .questionId(100)
                .build();
        when(questionRepository.findAllByQuizIdWithAnswers(10)).thenReturn(List.of(question));
        when(quizMapper.toQuestionResponse(question)).thenReturn(response);

        List<QuestionDetailResponse> result = quizService.getQuizQuestions(10);

        assertThat(result).containsExactly(response);
    }

    @Test
    void courseInstructorCanUpdateQuizCreatedByAnotherUser() {
        CreateQuizRequest request = new CreateQuizRequest();
        QuizDetailResponse response = new QuizDetailResponse();

        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toQuizResponse(quiz)).thenReturn(response);

        QuizDetailResponse result = quizService.updateQuiz(10, request);

        assertThat(result).isSameAs(response);
        verify(quizMapper).updateQuiz(request, quiz);
    }

    @Test
    void updateQuizBlocksPublishedQuizEvenWithoutAttempts() {
        quiz.setPublished(true);
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(false);

        assertThatThrownBy(() -> quizService.updateQuiz(10, new CreateQuizRequest()))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUIZ_PUBLISHED_CANNOT_EDIT));

        verify(quizMapper, never()).updateQuiz(any(), any());
    }

    @Test
    void updateQuizBlocksQuizWithAttempts() {
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(true);

        assertThatThrownBy(() -> quizService.updateQuiz(10, new CreateQuizRequest()))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUIZ_HAS_ATTEMPTS_CANNOT_EDIT));

        verify(quizMapper, never()).updateQuiz(any(), any());
    }

    @Test
    void createQuizFromImportedQuestionsCreatesQuizQuestionsAndPublishesInOneServiceCall() {
        CreateImportedQuizRequest request = importedQuizRequest(true, validQuestion("Imported question", true));
        Quiz importedQuiz = Quiz.builder()
                .quizId(20)
                .courseId("course-1")
                .createdBy("course-instructor")
                .published(false)
                .quizQuestions(new ArrayList<>())
                .build();
        Question mappedQuestion = Question.builder()
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        Question savedQuestion = Question.builder()
                .questionId(100)
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(20);

        when(quizMapper.toQuiz(request.getQuiz())).thenReturn(importedQuiz);
        when(quizRepository.save(importedQuiz)).thenReturn(importedQuiz);
        when(quizMapper.toQuestion(any(CreateQuestionRequest.class))).thenReturn(mappedQuestion);
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(mappedQuestion)).thenReturn(savedQuestion);
        when(quizMapper.toQuizResponse(importedQuiz)).thenReturn(response);

        QuizDetailResponse result = quizService.createQuizFromImportedQuestions("course-1", request);

        assertThat(result).isSameAs(response);
        assertThat(importedQuiz.getPublished()).isTrue();
        assertThat(importedQuiz.getQuizQuestions()).hasSize(1);
        QuizQuestion link = importedQuiz.getQuizQuestions().getFirst();
        assertThat(link.getQuestion()).isSameAs(savedQuestion);
        assertThat(link.getOrderIndex()).isEqualTo((short) 0);
        assertThat(link.getOverrideScore()).isEqualTo((short) 10);
        verify(questionRepository).save(mappedQuestion);
        verify(quizRepository, org.mockito.Mockito.times(2)).save(importedQuiz);
    }

    @Test
    void createQuizFromImportedQuestionsRejectsInvalidQuestionBeforeSaving() {
        CreateImportedQuizRequest request = importedQuizRequest(false, validQuestion("Invalid question", false));

        assertThatThrownBy(() -> quizService.createQuizFromImportedQuestions("course-1", request))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUESTION_SINGLE_ANSWER_COUNT));

        verify(quizRepository, never()).save(any());
        verify(questionRepository, never()).save(any());
    }

    @Test
    void createQuizFromImportedQuestionsLinksReusableQuestionWithoutCreatingCopy() {
        SaveQuizDraftRequest.QuestionDraftRequest reusableRequest = new SaveQuizDraftRequest.QuestionDraftRequest();
        reusableRequest.setQuestionId(100);
        reusableRequest.setReuseExisting(true);
        CreateImportedQuizRequest request = importedQuizRequest(false, reusableRequest);
        Quiz importedQuiz = Quiz.builder()
                .quizId(20)
                .courseId("course-1")
                .createdBy("course-instructor")
                .published(false)
                .quizQuestions(new ArrayList<>())
                .build();
        Question existingQuestion = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(20);

        when(quizMapper.toQuiz(request.getQuiz())).thenReturn(importedQuiz);
        when(quizRepository.save(importedQuiz)).thenReturn(importedQuiz);
        when(questionRepository.findById(100)).thenReturn(Optional.of(existingQuestion));
        when(quizMapper.toQuizResponse(importedQuiz)).thenReturn(response);

        QuizDetailResponse result = quizService.createQuizFromImportedQuestions("course-1", request);

        assertThat(result).isSameAs(response);
        assertThat(importedQuiz.getQuizQuestions()).hasSize(1);
        assertThat(importedQuiz.getQuizQuestions().getFirst().getQuestion()).isSameAs(existingQuestion);
        verify(quizMapper, never()).toQuestion(any(CreateQuestionRequest.class));
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void syncQuizUpdatesDraftInPlaceAndReplacesQuestionLinks() {
        SaveQuizDraftRequest request = saveDraftRequest(false, questionDraft(100, "Updated question"));
        Question existingQuestion = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .createdBy("creator-1")
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        QuizQuestion existingLink = QuizQuestion.builder()
                .quiz(quiz)
                .question(existingQuestion)
                .overrideScore((short) 5)
                .orderIndex((short) 2)
                .build();
        Question omittedQuestion = Question.builder()
                .questionId(101)
                .courseId("course-1")
                .build();
        QuizQuestion omittedLink = QuizQuestion.builder()
                .quiz(quiz)
                .question(omittedQuestion)
                .orderIndex((short) 1)
                .build();
        quiz.setQuizQuestions(new ArrayList<>(List.of(existingLink, omittedLink)));
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(10);

        when(questionRepository.findById(100)).thenReturn(Optional.of(existingQuestion));
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(existingQuestion)).thenReturn(existingQuestion);
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toQuizResponse(quiz)).thenReturn(response);

        QuizDetailResponse result = quizService.syncQuiz(10, request);

        assertThat(result.getQuizId()).isEqualTo(10);
        assertThat(quiz.getPublished()).isFalse();
        assertThat(quiz.getQuizQuestions()).hasSize(1);
        assertThat(quiz.getQuizQuestions().getFirst()).isSameAs(existingLink);
        assertThat(existingLink.getQuestion()).isSameAs(existingQuestion);
        assertThat(existingLink.getOrderIndex()).isEqualTo((short) 0);
        assertThat(existingLink.getOverrideScore()).isEqualTo((short) 10);
        verify(quizMapper).updateQuiz(request.getQuiz(), quiz);
        verify(quizMapper).updateQuestion(request.getQuestions().getFirst(), existingQuestion);
    }

    @Test
    void syncQuizReconcilesAnswersByAnswerIdInDraft() {
        Answer retainedAnswer = Answer.builder()
                .answerId(200)
                .content("Old A")
                .correct(false)
                .orderIndex((short) 1)
                .build();
        Answer omittedAnswer = Answer.builder()
                .answerId(201)
                .content("Old B")
                .correct(true)
                .orderIndex((short) 0)
                .build();
        Question existingQuestion = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .createdBy("creator-1")
                .score((byte) 10)
                .answers(new ArrayList<>(List.of(retainedAnswer, omittedAnswer)))
                .build();
        retainedAnswer.setQuestion(existingQuestion);
        omittedAnswer.setQuestion(existingQuestion);
        QuizQuestion existingLink = QuizQuestion.builder()
                .quiz(quiz)
                .question(existingQuestion)
                .orderIndex((short) 0)
                .build();
        quiz.setQuizQuestions(new ArrayList<>(List.of(existingLink)));

        SaveQuizDraftRequest.QuestionDraftRequest questionRequest = questionDraft(100, "Updated question");
        questionRequest.setAnswers(List.of(
                answer(200, "Updated A", true, (short) 0),
                answer("New B", false, (short) 1)));
        SaveQuizDraftRequest request = saveDraftRequest(false, questionRequest);
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(10);

        when(questionRepository.findById(100)).thenReturn(Optional.of(existingQuestion));
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(existingQuestion)).thenReturn(existingQuestion);
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toQuizResponse(quiz)).thenReturn(response);

        assertThat(quizService.syncQuiz(10, request)).isSameAs(response);
        assertThat(existingQuestion.getAnswers()).hasSize(2);
        assertThat(existingQuestion.getAnswers().getFirst()).isSameAs(retainedAnswer);
        assertThat(retainedAnswer.getContent()).isEqualTo("Updated A");
        assertThat(retainedAnswer.getCorrect()).isTrue();
        assertThat(retainedAnswer.getOrderIndex()).isEqualTo((short) 0);
        assertThat(existingQuestion.getAnswers()).noneMatch(answer -> Integer.valueOf(201).equals(answer.getAnswerId()));
        assertThat(existingQuestion.getAnswers().get(1).getAnswerId()).isNull();
        assertThat(existingQuestion.getAnswers().get(1).getContent()).isEqualTo("New B");
        assertThat(existingQuestion.getAnswers().get(1).getQuestion()).isSameAs(existingQuestion);
    }

    @Test
    void syncQuizClonesPublishedQuizEvenWithoutAttemptsAsUnpublishedDraft() {
        quiz.setPublished(true);
        quiz.setTitle("Original");
        quiz.setQuizQuestions(new ArrayList<>());
        SaveQuizDraftRequest request = saveDraftRequest(true, questionDraft(100, "Updated"));
        Question mappedQuestion = Question.builder()
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        Question savedQuestion = Question.builder()
                .questionId(101)
                .courseId("course-1")
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(30);
        response.setPublished(false);

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz saved = invocation.getArgument(0);
            if (saved.getQuizId() == null) {
                saved.setQuizId(30);
            }
            return saved;
        });
        when(quizMapper.toQuestion(any(CreateQuestionRequest.class))).thenReturn(mappedQuestion);
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(mappedQuestion)).thenReturn(savedQuestion);
        when(quizMapper.toQuizResponse(any(Quiz.class))).thenReturn(response);

        QuizDetailResponse result = quizService.syncQuiz(10, request);

        assertThat(result.getQuizId()).isEqualTo(30);
        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository, org.mockito.Mockito.times(2)).save(quizCaptor.capture());
        Quiz clonedQuiz = quizCaptor.getAllValues().getLast();
        assertThat(clonedQuiz.getPublished()).isFalse();
        assertThat(quiz.getQuizQuestions()).isEmpty();
        assertThat(clonedQuiz.getQuizQuestions()).hasSize(1);
        assertThat(clonedQuiz.getQuizQuestions().getFirst().getQuestion()).isSameAs(savedQuestion);
        verify(questionRepository, never()).findById(100);
    }

    @Test
    void syncQuizClonesQuizWithAttemptsAndAppliesSubmittedQuestions() {
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(true);
        quiz.setTitle("Original");
        quiz.setQuizQuestions(new ArrayList<>());
        SaveQuizDraftRequest request = saveDraftRequest(true, questionDraft(100, "Updated question"));
        Question mappedQuestion = Question.builder()
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        Question savedQuestion = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .score((byte) 10)
                .answers(new ArrayList<>())
                .build();
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(30);
        response.setPublished(false);

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz saved = invocation.getArgument(0);
            if (saved.getQuizId() == null) {
                saved.setQuizId(30);
            }
            return saved;
        });
        when(quizMapper.toQuestion(any(CreateQuestionRequest.class))).thenReturn(mappedQuestion);
        when(quizMapper.toAnswer(any(CreateQuestionRequest.AnswerRequest.class))).thenAnswer(invocation -> {
            CreateQuestionRequest.AnswerRequest source = invocation.getArgument(0);
            return Answer.builder()
                    .content(source.getContent())
                    .correct(source.isCorrect())
                    .orderIndex(source.getOrderIndex())
                    .build();
        });
        when(questionRepository.save(mappedQuestion)).thenReturn(savedQuestion);
        when(quizMapper.toQuizResponse(any(Quiz.class))).thenReturn(response);

        QuizDetailResponse result = quizService.syncQuiz(10, request);

        assertThat(result.getQuizId()).isEqualTo(30);
        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository, org.mockito.Mockito.times(2)).save(quizCaptor.capture());
        Quiz clonedQuiz = quizCaptor.getAllValues().getLast();
        assertThat(clonedQuiz.getPublished()).isFalse();
        assertThat(clonedQuiz.getQuizId()).isEqualTo(30);
        assertThat(clonedQuiz.getQuizQuestions()).hasSize(1);
        assertThat(clonedQuiz.getQuizQuestions().getFirst().getQuestion()).isSameAs(savedQuestion);
        assertThat(clonedQuiz.getQuizQuestions().getFirst().getOrderIndex()).isEqualTo((short) 0);
        verify(quizMapper).updateQuiz(request.getQuiz(), clonedQuiz);
        verify(questionRepository, never()).findById(100);
        verify(quizMapper, never()).updateQuestion(any(), any());
    }

    @Test
    void addQuestionBlocksPublishedQuizEvenWithoutAttempts() {
        quiz.setPublished(true);
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(false);

        assertThatThrownBy(() -> quizService.bulkAddQuestionToQuiz(10, new AddBulkQuestionQuizRequest()))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUIZ_PUBLISHED_CANNOT_EDIT));

        verify(questionRepository, never()).findAllById(any());
    }

    @Test
    void removeQuestionBlocksQuizWithAttempts() {
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(true);

        assertThatThrownBy(() -> quizService.removeQuestionInQuiz(10, 100))
                .isInstanceOfSatisfying(AssignmentException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUIZ_HAS_ATTEMPTS_CANNOT_EDIT));
    }

    @Test
    void cloneQuizDeepCopiesQuestionsAndAnswersAsDraft() {
        Answer sourceAnswer = Answer.builder()
                .answerId(200)
                .content("A")
                .correct(true)
                .orderIndex((short) 0)
                .build();
        Question sourceQuestion = Question.builder()
                .questionId(100)
                .courseId("course-1")
                .content("Question")
                .type(QuestionType.SINGLE)
                .topic("Topic")
                .explanation("Explain")
                .score((byte) 10)
                .answers(new ArrayList<>(List.of(sourceAnswer)))
                .createdBy("creator-1")
                .build();
        sourceAnswer.setQuestion(sourceQuestion);
        quiz.setTitle("Original");
        quiz.setQuizQuestions(new ArrayList<>(List.of(QuizQuestion.builder()
                .quiz(quiz)
                .question(sourceQuestion)
                .overrideScore((short) 8)
                .orderIndex((short) 2)
                .build())));

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz saved = invocation.getArgument(0);
            if (saved.getQuizId() == null) {
                saved.setQuizId(20);
            }
            return saved;
        });
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            saved.setQuestionId(101);
            saved.getAnswers().getFirst().setAnswerId(201);
            return saved;
        });

        Integer clonedQuizId = quizService.cloneQuiz(10);

        assertThat(clonedQuizId).isEqualTo(20);

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository, org.mockito.Mockito.times(2)).save(quizCaptor.capture());
        Quiz clonedQuiz = quizCaptor.getAllValues().getLast();

        assertThat(clonedQuiz.getPublished()).isFalse();
        assertThat(clonedQuiz.getTitle()).isEqualTo("Original (Copy)");
        assertThat(clonedQuiz.getQuizQuestions()).hasSize(1);
        QuizQuestion clonedQuizQuestion = clonedQuiz.getQuizQuestions().getFirst();
        assertThat(clonedQuizQuestion.getOverrideScore()).isEqualTo((short) 8);
        assertThat(clonedQuizQuestion.getOrderIndex()).isEqualTo((short) 2);
        assertThat(clonedQuizQuestion.getQuestion()).isNotSameAs(sourceQuestion);
        assertThat(clonedQuizQuestion.getQuestion().getQuestionId()).isEqualTo(101);
        assertThat(clonedQuizQuestion.getQuestion().getAnswers().getFirst()).isNotSameAs(sourceAnswer);
        assertThat(clonedQuizQuestion.getQuestion().getAnswers().getFirst().getAnswerId()).isEqualTo(201);
    }

    @Test
    void courseInstructorCanDeleteQuizCreatedByAnotherUserWhenNoAttemptsExist() {
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(false);

        quizService.deleteQuiz(10);

        verify(quizRepository).delete(quiz);
    }

    @Test
    void deleteQuizSoftDeletesWhenAttemptsExist() {
        when(quizAttemptRepository.existsByQuiz_QuizId(10)).thenReturn(true);

        quizService.deleteQuiz(10);

        verify(quizRepository, never()).delete(any());
        verify(quizRepository).save(quiz);
        assertThat(quiz.getDeleted()).isTrue();
        assertThat(quiz.getDeletedAt()).isNotNull();
        assertThat(quiz.getDeletedBy()).isEqualTo("course-instructor");
    }

    private CreateImportedQuizRequest importedQuizRequest(boolean publish, SaveQuizDraftRequest.QuestionDraftRequest question) {
        CreateQuizRequest quizRequest = new CreateQuizRequest();
        quizRequest.setTitle("Imported quiz");
        quizRequest.setTotalScore((short) 100);
        quizRequest.setPassScore((byte) 50);
        quizRequest.setMaxAttempts((byte) 0);

        CreateImportedQuizRequest request = new CreateImportedQuizRequest();
        request.setQuiz(quizRequest);
        request.setQuestions(List.of(question));
        request.setPublish(publish);
        return request;
    }

    private SaveQuizDraftRequest saveDraftRequest(
            boolean publish,
            SaveQuizDraftRequest.QuestionDraftRequest question) {
        CreateQuizRequest quizRequest = new CreateQuizRequest();
        quizRequest.setTitle("Draft quiz");
        quizRequest.setTotalScore((short) 100);
        quizRequest.setPassScore((byte) 50);
        quizRequest.setMaxAttempts((byte) 0);

        SaveQuizDraftRequest request = new SaveQuizDraftRequest();
        request.setQuiz(quizRequest);
        request.setQuestions(List.of(question));
        request.setPublish(publish);
        return request;
    }

    private SaveQuizDraftRequest.QuestionDraftRequest questionDraft(Integer questionId, String content) {
        SaveQuizDraftRequest.QuestionDraftRequest request = new SaveQuizDraftRequest.QuestionDraftRequest();
        request.setQuestionId(questionId);
        request.setContent(content);
        request.setType(QuestionType.SINGLE);
        request.setScore((byte) 10);
        request.setAnswers(List.of(
                answer("A", true, (short) 0),
                answer("B", false, (short) 1)));
        return request;
    }

    private SaveQuizDraftRequest.QuestionDraftRequest validQuestion(String content, boolean hasCorrectAnswer) {
        SaveQuizDraftRequest.QuestionDraftRequest request = new SaveQuizDraftRequest.QuestionDraftRequest();
        request.setContent(content);
        request.setType(QuestionType.SINGLE);
        request.setScore((byte) 10);
        request.setAnswers(List.of(
                answer("A", hasCorrectAnswer, (short) 0),
                answer("B", false, (short) 1)));
        return request;
    }

    private CreateQuestionRequest.AnswerRequest answer(String content, boolean correct, short orderIndex) {
        return answer(null, content, correct, orderIndex);
    }

    private CreateQuestionRequest.AnswerRequest answer(
            Integer answerId,
            String content,
            boolean correct,
            short orderIndex) {
        CreateQuestionRequest.AnswerRequest answer = new CreateQuestionRequest.AnswerRequest();
        answer.setAnswerId(answerId);
        answer.setContent(content);
        answer.setCorrect(correct);
        answer.setOrderIndex(orderIndex);
        return answer;
    }
}
