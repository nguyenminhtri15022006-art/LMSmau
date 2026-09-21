package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.configuration.SecurityConfig;
import com.lms.assignmentservice.dto.request.CreateSubmissionRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.CachedPage;
import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.dto.response.RunCodeResponse;
import com.lms.assignmentservice.dto.response.SubmissionResponse;
import com.lms.assignmentservice.enums.DifficultyType;
import com.lms.assignmentservice.service.ProblemService;
import com.lms.assignmentservice.service.QuizService;
import com.lms.assignmentservice.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class AssignmentFunctionalTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ProblemService problemService;

    @MockBean
    QuizService quizService;

    @MockBean
    SubmissionService submissionService;

    @Test
    void studentCompletesCodePracticeWorkflow() throws Exception {
        ProblemListResponse problem = ProblemListResponse.builder()
                .problemId(7)
                .courseId("course-1")
                .title("Sum two numbers")
                .slug("sum-two-numbers")
                .difficulty(DifficultyType.EASY)
                .completed(false)
                .build();
        when(problemService.getProblemList(eq("course-1"), eq(null), eq(DifficultyType.EASY), any(Pageable.class)))
                .thenReturn(CachedPage.<ProblemListResponse>builder()
                        .content(List.of(problem))
                        .totalElements(1)
                        .build());

        RunCodeResponse runResponse = new RunCodeResponse();
        runResponse.setStatus("ACCEPTED");
        runResponse.setAllPassed(true);
        runResponse.setScore(100);
        when(submissionService.runCode(any(RunCodeRequest.class))).thenReturn(runResponse);

        SubmissionResponse submissionResponse = new SubmissionResponse();
        submissionResponse.setSubmissionId("submission-1");
        submissionResponse.setProblemId(7);
        submissionResponse.setLanguage("JAVA");
        submissionResponse.setStatus("ACCEPTED");
        submissionResponse.setScore(100);
        submissionResponse.setSubmittedAt(Instant.parse("2026-06-02T03:00:00Z"));
        when(submissionService.submit(any(CreateSubmissionRequest.class))).thenReturn(submissionResponse);
        when(submissionService.getLatestAccepted(7)).thenReturn(submissionResponse);

        mockMvc.perform(get("/problems")
                        .headers(studentHeaders())
                        .param("courseId", "course-1")
                        .param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].problemId").value(7))
                .andExpect(jsonPath("$.data.content[0].completed").value(false));

        mockMvc.perform(post("/submissions/run")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemId": 7,
                                  "language": "JAVA",
                                  "sourceCode": "class Main { public static void main(String[] args) {} }",
                                  "testCases": [
                                    { "input": "1 2", "expectedOutput": "3" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.allPassed").value(true));

        mockMvc.perform(post("/submissions")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemId": 7,
                                  "language": "JAVA",
                                  "sourceCode": "class Main { public static void main(String[] args) {} }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").value("submission-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.score").value(100));

        mockMvc.perform(get("/submissions/latest-accepted")
                        .headers(studentHeaders())
                        .param("problemId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").value("submission-1"))
                .andExpect(jsonPath("$.data.problemId").value(7));

        verify(problemService).getProblemList(eq("course-1"), eq(null), eq(DifficultyType.EASY), any(Pageable.class));
        verify(submissionService).runCode(any(RunCodeRequest.class));
        verify(submissionService).submit(any(CreateSubmissionRequest.class));
        verify(submissionService).getLatestAccepted(7);
    }

    private org.springframework.http.HttpHeaders studentHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", "student-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "student@example.com");
        return headers;
    }
}
