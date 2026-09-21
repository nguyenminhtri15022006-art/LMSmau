package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.configuration.SecurityConfig;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.CachedPage;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.dto.response.QuizDetailResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.service.ProblemService;
import com.lms.assignmentservice.service.QuizService;
import com.lms.assignmentservice.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProblemController.class,
        QuizController.class,
        CodeJudgeController.class
})
@Import(SecurityConfig.class)
class AssignmentApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ProblemService problemService;

    @MockBean
    QuizService quizService;

    @MockBean
    SubmissionService submissionService;

    @Test
    void privateEndpointRejectsRequestWithoutGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/problems")
                        .param("courseId", "course-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3001));

        verify(problemService, never()).getProblemList(any(), any(), any(), any());
    }

    @Test
    void studentRoleCannotAccessInstructorQuizQuestions() throws Exception {
        mockMvc.perform(get("/quizzes/10/questions")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "ROLE_STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(3002));

        verify(quizService, never()).getQuizQuestions(any());
    }

    @Test
    void createQuizDelegatesToServiceAndSerializesResponse() throws Exception {
        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(10);
        response.setCourseId("course-1");
        response.setTitle("Java basics quiz");
        response.setDuration(30);
        response.setPublished(false);
        when(quizService.createQuiz(eq("course-1"), any(CreateQuizRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/quizzes/courses/course-1")
                        .header("X-User-Id", "instructor-1")
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java basics quiz",
                                  "duration": 30,
                                  "totalScore": 100,
                                  "passScore": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.quizId").value(10))
                .andExpect(jsonPath("$.data.courseId").value("course-1"))
                .andExpect(jsonPath("$.data.title").value("Java basics quiz"));

        ArgumentCaptor<CreateQuizRequest> requestCaptor = ArgumentCaptor.forClass(CreateQuizRequest.class);
        verify(quizService).createQuiz(eq("course-1"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTitle()).isEqualTo("Java basics quiz");
        assertThat(requestCaptor.getValue().getDuration()).isEqualTo(30);
    }

    @Test
    void listProblemsMapsCachedPageToPageResponse() throws Exception {
        ProblemListResponse problem = ProblemListResponse.builder()
                .problemId(1)
                .courseId("course-1")
                .title("Two Sum")
                .slug("two-sum")
                .difficulty(DifficultyType.EASY)
                .totalSubmit(4)
                .acceptanceRate(75)
                .build();
        when(problemService.getProblemList(eq("course-1"), eq(null), eq(DifficultyType.EASY), any(Pageable.class)))
                .thenReturn(CachedPage.<ProblemListResponse>builder()
                        .content(List.of(problem))
                        .totalElements(1)
                        .build());

        mockMvc.perform(get("/problems")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .param("courseId", "course-1")
                        .param("difficulty", "EASY")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].problemId").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("two-sum"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void invalidRunCodeRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/submissions/run")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemId": 1,
                                  "language": "JAVA",
                                  "sourceCode": "class Main {}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3401))
                .andExpect(jsonPath("$.data.testCases").exists());

        verify(submissionService, never()).runCode(any(RunCodeRequest.class));
    }
}
