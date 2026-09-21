package com.lms.assignmentservice.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AnswerCountValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAnswerCount {
    String message() default "Số lượng đáp án đúng không hợp lệ cho loại câu hỏi này";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}