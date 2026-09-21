package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.dto.feign.CourseDTO;
import com.lms.chatbotservice.dto.feign.ProblemDTO;
import com.lms.chatbotservice.dto.feign.SubmissionDTO;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.feign.AssignmentClient;
import com.lms.chatbotservice.feign.CourseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextBuilderServiceImplTest {

    @Mock
    AssignmentClient assignmentClient;
    @Mock
    CourseClient courseClient;

    @InjectMocks
    ContextBuilderServiceImpl contextBuilderService;

    @Test
    void buildGeneralContextOnlyIncludesUserId() {
        var context = contextBuilderService.build(null, ContextType.GENERAL, "student-1");

        assertThat(context.getUserId()).isEqualTo("student-1");
        assertThat(context.getProblem()).isNull();
        assertThat(context.getCourses()).isNull();
    }

    @Test
    void buildProblemContextIncludesProblemAndLatestSubmissionWhenAvailable() {
        ProblemDTO problem = new ProblemDTO();
        problem.setProblemId(42);
        problem.setTitle("Two Sum");
        problem.setDescription("Find pair");
        problem.setDifficulty("EASY");
        problem.setTimeLimitMs(1000);
        problem.setMemoryLimitMb(256);
        problem.setAllowedLangs(List.of("java"));
        SubmissionDTO submission = new SubmissionDTO();
        submission.setSourceCode("class Main {}");
        submission.setLanguage("java");
        submission.setStatus("WRONG_ANSWER");
        submission.setCompileError("missing semicolon");

        when(assignmentClient.getProblem(42)).thenReturn(ApiResponse.of(problem));
        when(assignmentClient.getLatestSubmission(42, "student-1")).thenReturn(ApiResponse.of(submission));

        var context = contextBuilderService.build("42", ContextType.PROBLEM, "student-1");

        assertThat(context.getProblem().getProblemId()).isEqualTo(42);
        assertThat(context.getProblem().getLatestSourceCode()).isEqualTo("class Main {}");
        assertThat(context.getProblem().getCompileError()).isEqualTo("missing semicolon");
    }

    @Test
    void buildProblemContextRejectsInvalidProblemId() {
        assertThatThrownBy(() -> contextBuilderService.build("abc", ContextType.PROBLEM, "student-1"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROBLEM_NOT_FOUND);
    }

    @Test
    void buildProblemContextRejectsMissingProblemResponse() {
        when(assignmentClient.getProblem(42)).thenReturn(ApiResponse.of(null));

        assertThatThrownBy(() -> contextBuilderService.build("42", ContextType.PROBLEM, "student-1"))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROBLEM_NOT_FOUND);
    }

    @Test
    void buildCourseContextMergesAllCoursesWithEnrolledFlag() {
        CourseDTO enrolled = course("course-1", "Java");
        CourseDTO other = course("course-2", "Spring");
        when(courseClient.getEnrolledCourses("student-1")).thenReturn(ApiResponse.of(List.of(enrolled)));
        when(courseClient.getAllCourses()).thenReturn(ApiResponse.of(List.of(enrolled, other)));

        var context = contextBuilderService.build(null, ContextType.COURSE, "student-1");

        assertThat(context.getCourses()).hasSize(2);
        assertThat(context.getCourses())
                .filteredOn(course -> course.getCourseId().equals("course-1"))
                .singleElement()
                .extracting("enrolled")
                .isEqualTo(true);
        assertThat(context.getCourses())
                .filteredOn(course -> course.getCourseId().equals("course-2"))
                .singleElement()
                .extracting("enrolled")
                .isEqualTo(false);
    }

    private CourseDTO course(String id, String title) {
        CourseDTO course = new CourseDTO();
        course.setId(id);
        course.setTitle(title);
        course.setDescription(title + " description");
        course.setStatus("PUBLIC");
        course.setLevel("BEGINNER");
        course.setDuration(10);
        course.setCategoryNames(List.of("Programming"));
        return course;
    }
}
