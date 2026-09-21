package com.lms.assignmentservice.validator;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Kiểm tra số lượng đáp án đúng (isCorrect=true) theo QuestionType:
 * <p>
 * SINGLE     → đúng 1 đáp án
 * TRUE_FALSE → đúng 1 đáp án, tổng đáp án phải đúng 2
 * MULTIPLE   → ít nhất 2 đáp án đúng
 * <p>
 * Throw AssignmentException với ErrorCode tương ứng thay vì return false
 */
public class AnswerCountValidator
        implements ConstraintValidator<ValidAnswerCount, CreateQuestionRequest> {

    @Override
    public boolean isValid(CreateQuestionRequest req, ConstraintValidatorContext ctx) {
        if (req.getType() == null || req.getAnswers() == null) return true;

        long correctCount = req.getAnswers().stream()
                .filter(CreateQuestionRequest.AnswerRequest::isCorrect)
                .count();

        switch (req.getType()) {
            case SINGLE -> {
                if (correctCount != 1) {
                    throw new AssignmentException(ErrorCode.QUESTION_SINGLE_ANSWER_COUNT);
                }
            }
            case TRUE_FALSE -> {
                boolean twoAnswers = req.getAnswers().size() == 2;
                boolean oneCorrect = correctCount == 1;
                if (!twoAnswers || !oneCorrect) {
                    throw new AssignmentException(ErrorCode.QUESTION_TRUE_FALSE_ANSWER_COUNT);
                }
            }
            case MULTIPLE -> {
                if (correctCount < 2) {
                    throw new AssignmentException(ErrorCode.QUESTION_MULTIPLE_ANSWER_COUNT);
                }
            }
        }

        return true;
    }
}