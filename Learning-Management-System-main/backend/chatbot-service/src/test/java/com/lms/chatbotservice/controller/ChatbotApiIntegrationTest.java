package com.lms.chatbotservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.configuration.SecurityConfig;
import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.response.AnswerResponse;
import com.lms.chatbotservice.dto.response.GenerateQuizResponse;
import com.lms.chatbotservice.dto.response.QuizQuestionResponse;
import com.lms.chatbotservice.enums.DifficultyType;
import com.lms.chatbotservice.enums.QuestionType;
import com.lms.chatbotservice.exception.GlobalExceptionHandler;
import com.lms.chatbotservice.service.ChatMessageService;
import com.lms.chatbotservice.service.ChatService;
import com.lms.chatbotservice.service.ChatSessionService;
import com.lms.chatbotservice.service.GenerateService;
import com.lms.chatbotservice.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ChatController.class, InternalController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChatbotApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ChatService chatService;
    @MockBean
    GenerateService generateService;
    @MockBean
    ChatSessionService chatSessionService;
    @MockBean
    ChatMessageService chatMessageService;
    @MockBean
    RateLimiterService rateLimiterService;

    @Test
    void privateEndpointWithoutGatewayHeaderReturnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(6001));
    }

    @Test
    void wrongRoleOnInstructorEndpointReturnsAccessDenied() throws Exception {
        GenerateQuizRequest request = quizRequest();

        mockMvc.perform(post("/generate/quiz")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(6002));

        verify(rateLimiterService, never()).checkAndIncrement(any(), any());
        verify(generateService, never()).generateQuiz(any());
    }

    @Test
    void instructorGenerateQuizCallsRateLimiterAndSerializesResponse() throws Exception {
        GenerateQuizRequest request = quizRequest();
        when(generateService.generateQuiz(any())).thenReturn(quizResponse());

        mockMvc.perform(post("/generate/quiz")
                        .headers(gatewayHeaders("instructor-1", "ROLE_INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalQuestions").value(1))
                .andExpect(jsonPath("$.data.questions[0].question").value("What is JVM?"));

        verify(rateLimiterService).checkAndIncrement("instructor-1", "ROLE_INSTRUCTOR");
        verify(generateService).generateQuiz(any(GenerateQuizRequest.class));
    }

    @Test
    void validationErrorReturnsMappedErrorCode() throws Exception {
        mockMvc.perform(post("/chat")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "GENERAL",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6602));

        verify(rateLimiterService, never()).checkAndIncrement(any(), any());
        verify(chatService, never()).chat(any());
    }

    @Test
    void chatEndpointStreamsServiceEventsForAuthenticatedStudent() throws Exception {
        when(chatService.chat(any())).thenReturn(Flux.just("[SESSION:session-1]", "hello"));

        MvcResult result = mockMvc.perform(post("/chat")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "GENERAL",
                                  "message": "Explain Java"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("[SESSION:session-1]")))
                .andExpect(content().string(containsString("hello")));

        verify(rateLimiterService).checkAndIncrement("student-1", "ROLE_STUDENT");
        verify(chatService).chat(any());
    }

    @Test
    void internalHealthDoesNotRequireGatewayHeader() throws Exception {
        mockMvc.perform(get("/internal/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("chatbot-service is running"));
    }

    @Test
    void internalGenerateQuizDoesNotRequireGatewayHeaderOrRateLimit() throws Exception {
        when(generateService.generateQuiz(any())).thenReturn(quizResponse());

        mockMvc.perform(post("/internal/generate/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuestions").value(1));

        verify(rateLimiterService, never()).checkAndIncrement(any(), any());
        verify(generateService).generateQuiz(any(GenerateQuizRequest.class));
    }

    private org.springframework.http.HttpHeaders gatewayHeaders(String userId, String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", userId);
        headers.add("X-User-Role", role);
        headers.add("X-User-Email", userId + "@example.test");
        return headers;
    }

    private GenerateQuizRequest quizRequest() {
        return GenerateQuizRequest.builder()
                .content("JVM basics")
                .questionCount(1)
                .difficulty(DifficultyType.EASY)
                .questionType(QuestionType.SINGLE)
                .build();
    }

    private GenerateQuizResponse quizResponse() {
        return GenerateQuizResponse.builder()
                .totalQuestions(1)
                .questions(List.of(QuizQuestionResponse.builder()
                        .question("What is JVM?")
                        .questionType(QuestionType.SINGLE)
                        .answers(List.of(AnswerResponse.builder()
                                .content("Java Virtual Machine")
                                .correct(true)
                                .build()))
                        .explanation("JVM runs Java bytecode.")
                        .build()))
                .build();
    }
}
