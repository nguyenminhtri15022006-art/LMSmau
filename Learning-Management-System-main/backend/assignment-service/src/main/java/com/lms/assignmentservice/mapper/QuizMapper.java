package com.lms.assignmentservice.mapper;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.response.*;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.entity.Quiz;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface QuizMapper {

    // ===== Quiz =====

    @Mapping(target = "quizId", ignore = true)
    @Mapping(target = "published", constant = "false")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "quizQuestions", ignore = true)
    Quiz toQuiz(CreateQuizRequest request);

    @Mapping(target = "showResult", expression = "java(quiz.getShowResult().name())")
    @Mapping(target = "questionCount", expression = "java(quiz.getQuizQuestions().size())")
    QuizDetailResponse toQuizResponse(Quiz quiz);

    /**
     * Map Quiz entity to QuizInstructorDetailResponse (full details for instructors).
     */
    @Mapping(target = "showResult", expression = "java(quiz.getShowResult().name())")
    @Mapping(target = "questionCount", expression = "java(quiz.getQuizQuestions().size())")
    @Mapping(target = "attemptCount", ignore = true)
    QuizInstructorDetailResponse toInstructorResponse(Quiz quiz);

    /**
     * Map Quiz entity to QuizStudentDetailResponse (student-visible fields only).
     */
    @Mapping(target = "questionCount", expression = "java(quiz.getQuizQuestions().size())")
    @Mapping(target = "completed", ignore = true)
    QuizStudentDetailResponse toStudentResponse(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "quizId", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "quizQuestions", ignore = true)
    void updateQuiz(CreateQuizRequest request, @MappingTarget Quiz quiz);

    // ===== Question =====

    @Mapping(target = "questionId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "answers", ignore = true)
    Question toQuestion(CreateQuestionRequest request);

    QuestionDetailResponse toQuestionResponse(Question question);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "questionId", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "answers", ignore = true)
    void updateQuestion(CreateQuestionRequest request, @MappingTarget Question question);

    @Mapping(target = "usedInQuizCount", source = "usedCount")
    QuestionDetailResponse toQuestionResponse(Question question, long usedCount);

    @Mapping(target = "question", ignore = true)
    @Mapping(target = "answerId", ignore = true)
    Answer toAnswer(CreateQuestionRequest.AnswerRequest request);


}
