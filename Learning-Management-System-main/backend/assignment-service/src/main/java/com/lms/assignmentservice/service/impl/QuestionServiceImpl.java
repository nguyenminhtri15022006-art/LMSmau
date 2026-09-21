package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.service.QuestionService;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.ImportBulkQuestionRequest;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    QuestionRepository questionRepository;
    QuizQuestionRepository quizQuestionRepository;
    QuizMapper quizMapper;
    CourseClient courseClient;
    AssignmentAuthorizationService authorizationService;
    CourseResourceAuthorizationService courseResourceAuthorizationService;

    @Override
    @Transactional
    public QuestionDetailResponse createQuestion(String courseId, CreateQuestionRequest request) {
        String userId = authorizationService.currentUserId();
        Question question = buildQuestionEntity(courseId, userId, request, request.getTopic());
        Question saved = questionRepository.save(question);
        log.info("Tạo câu hỏi [{}] type={} bởi user [{}]",
                saved.getQuestionId(), saved.getType(), userId);
        return quizMapper.toQuestionResponse(saved);
    }

    @Override
    @Transactional
    public Page<QuestionDetailResponse> importBatchQuestion(
            String courseId, ImportBulkQuestionRequest request, Pageable pageable) {

        String userId = authorizationService.currentUserId();

        List<Question> questionList = new ArrayList<>();
        for (CreateQuestionRequest questionDto : request.getQuestions()) {
            questionList.add(buildQuestionEntity(courseId, userId, questionDto, request.getTopic()));
        }

        List<Question> savedQuestions = questionRepository.saveAll(questionList);
        log.info("Import thành công {} câu hỏi vào course [{}] bởi user [{}]",
                savedQuestions.size(), courseId, userId);
        List<QuestionDetailResponse> responseList = savedQuestions.stream()
                .map(question -> quizMapper.toQuestionResponse(question, 0L))
                .toList();
        return new PageImpl<>(responseList, pageable, responseList.size());
    }

    @Override
    public Page<QuestionDetailResponse> getQuestionsByCourse(
            String courseId, String topic, QuestionType type, Pageable pageable) {

        Page<Question> page;
        if (topic != null && type != null) {
            page = questionRepository.findByCourseIdAndTopicAndType(courseId, topic, type, pageable);
        } else if (topic != null) {
            page = questionRepository.findByCourseIdAndTopic(courseId, topic, pageable);
        } else if (type != null) {
            page = questionRepository.findByCourseIdAndType(courseId, type, pageable);
        } else {
            page = questionRepository.findByCourseId(courseId, pageable);
        }

        return page.map(q -> {
            long usedCount = quizQuestionRepository.countByQuestionQuestionId(q.getQuestionId());
            return quizMapper.toQuestionResponse(q, usedCount);
        });
    }

    @Override
    public List<String> getTopics(String courseId) {
        return questionRepository.findDistinctTopicsByCourseId(courseId);
    }

    @Override
    public QuestionDetailResponse getQuestion(Integer questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUESTION_NOT_FOUND));

        return quizMapper.toQuestionResponse(q);
    }


    @Override
    @Transactional
    public QuestionDetailResponse updateQuestion(Integer questionId, CreateQuestionRequest request) {
        //validate course

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUESTION_NOT_FOUND));
        checkOwner(question);
        validateQuestionEditable(questionId);
        quizMapper.updateQuestion(request, question);
        QuestionAnswerReconciler.reconcile(question, request.getAnswers(), quizMapper);

        return quizMapper.toQuestionResponse(questionRepository.save(question));
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUESTION_NOT_FOUND));

        checkOwner(question);

        long usedCount = quizQuestionRepository.countByQuestionQuestionId(questionId);
        if (usedCount > 0) {
            throw new AssignmentException(ErrorCode.QUESTION_IN_USE);
        }

        questionRepository.delete(question);
        log.info("Xóa câu hỏi [{}]", questionId);
    }

    private Question buildQuestionEntity(String courseId, String userId, CreateQuestionRequest request, String topic) {
        Question question = quizMapper.toQuestion(request);
        question.setCreatedBy(userId);
        question.setCourseId(courseId);
        if (topic != null && !topic.isBlank()) {
            question.setTopic(topic);
        }
        for (CreateQuestionRequest.AnswerRequest ar : request.getAnswers()) {
            Answer answer = quizMapper.toAnswer(ar);
            answer.setQuestion(question);
            question.getAnswers().add(answer);
        }
        return question;
    }

    private void checkOwner(Question question) {
        courseResourceAuthorizationService.checkManager(question.getCourseId(), question.getCreatedBy());
    }

    private void validateQuestionEditable(Integer questionId) {
        if (quizQuestionRepository.countLockedQuizReferencesByQuestionId(questionId) > 0) {
            throw new AssignmentException(ErrorCode.QUESTION_IN_LOCKED_QUIZ_CANNOT_EDIT);
        }
    }
}
