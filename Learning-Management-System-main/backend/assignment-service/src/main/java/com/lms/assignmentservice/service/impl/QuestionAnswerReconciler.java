package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.QuizMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class QuestionAnswerReconciler {

    private QuestionAnswerReconciler() {
    }

    static void reconcile(
            Question question,
            List<CreateQuestionRequest.AnswerRequest> answerRequests,
            QuizMapper quizMapper) {

        if (question.getAnswers() == null) {
            question.setAnswers(new ArrayList<>());
        }

        Map<Integer, Answer> existingAnswersById = question.getAnswers().stream()
                .filter(answer -> answer.getAnswerId() != null)
                .collect(Collectors.toMap(
                        Answer::getAnswerId,
                        answer -> answer,
                        (first, second) -> first
                ));
        Set<Answer> retainedAnswers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        for (CreateQuestionRequest.AnswerRequest answerRequest : answerRequests) {
            Answer answer;
            if (answerRequest.getAnswerId() != null) {
                answer = existingAnswersById.get(answerRequest.getAnswerId());
                if (answer == null) {
                    throw new AssignmentException(ErrorCode.ANSWER_NOT_IN_QUESTION);
                }
                applyAnswerRequest(answer, answerRequest);
            } else {
                answer = quizMapper.toAnswer(answerRequest);
                question.getAnswers().add(answer);
            }
            answer.setQuestion(question);
            retainedAnswers.add(answer);
        }

        question.getAnswers().removeIf(answer -> !retainedAnswers.contains(answer));
        question.getAnswers().sort(Comparator.comparing(answer -> answer.getOrderIndex() == null
                ? Short.MAX_VALUE
                : answer.getOrderIndex()));
    }

    private static void applyAnswerRequest(Answer answer, CreateQuestionRequest.AnswerRequest answerRequest) {
        answer.setContent(answerRequest.getContent());
        answer.setCorrect(answerRequest.isCorrect());
        answer.setOrderIndex(answerRequest.getOrderIndex());
    }
}
