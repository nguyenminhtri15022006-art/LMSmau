package com.lms.chatbotservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.configuration.SecurityConfig;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.response.AnswerResponse;
import com.lms.chatbotservice.dto.response.ChatMessageResponse;
import com.lms.chatbotservice.dto.response.ChatSessionResponse;
import com.lms.chatbotservice.dto.response.GenerateQuizResponse;
import com.lms.chatbotservice.dto.response.QuizQuestionResponse;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.enums.QuestionType;
import com.lms.chatbotservice.enums.SessionStatus;
import com.lms.chatbotservice.exception.GlobalExceptionHandler;
import com.lms.chatbotservice.service.ChatMessageService;
import com.lms.chatbotservice.service.ChatService;
import com.lms.chatbotservice.service.ChatSessionService;
import com.lms.chatbotservice.service.GenerateService;
import com.lms.chatbotservice.service.RateLimiterService;
import com.lms.chatbotservice.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ChatController.class, InternalController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChatbotFunctionalTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ChatController chatController;

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
    void instructorGeneratesQuizStudentChatsReviewsHistoryAndArchivesSession() throws Exception {
        when(generateService.generateQuiz(any())).thenReturn(quizResponse());
        when(chatService.chat(any())).thenReturn(Flux.just("[SESSION:session-1]", "Study polymorphism with examples."));
        when(chatSessionService.getSessionsByUser(any(), any()))
                .thenReturn(new PageImpl<>(List.of(ChatSessionResponse.builder()
                        .id("session-1")
                        .title("Explain polymorphism")
                        .contextType(ContextType.GENERAL)
                        .status(SessionStatus.ACTIVE)
                        .totalMessages(2)
                        .build())));
        when(chatMessageService.getMessageResponses("session-1"))
                .thenReturn(List.of(
                        ChatMessageResponse.builder()
                                .id("message-1")
                                .role(MessageRole.USER)
                                .content("Explain polymorphism")
                                .tokenCount(5)
                                .build(),
                        ChatMessageResponse.builder()
                                .id("message-2")
                                .role(MessageRole.MODEL)
                                .content("Study polymorphism with examples.")
                                .tokenCount(8)
                                .build()));

        mockMvc.perform(post("/generate/quiz")
                        .headers(gatewayHeaders("instructor-1", "ROLE_INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Object oriented programming",
                                  "questionCount": 1,
                                  "difficulty": "EASY",
                                  "questionType": "SINGLE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].question").value("What is polymorphism?"));

        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        List<String> chatChunks = chatController.chat(ChatRequest.builder()
                        .contextType(ContextType.GENERAL)
                        .message("Explain polymorphism")
                        .build())
                .collectList()
                .block();
        TestSecurity.clear();

        assertThat(chatChunks)
                .contains("[SESSION:session-1]", "Study polymorphism with examples.");

        mockMvc.perform(get("/sessions")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("session-1"))
                .andExpect(jsonPath("$.data.content[0].totalMessages").value(2));

        mockMvc.perform(get("/sessions/session-1/messages")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("MODEL"));

        mockMvc.perform(post("/sessions/session-1/archive")
                        .headers(gatewayHeaders("student-1", "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(rateLimiterService).checkAndIncrement("instructor-1", "ROLE_INSTRUCTOR");
        verify(rateLimiterService).checkAndIncrement("student-1", "ROLE_STUDENT");
        verify(chatService).chat(any());
        verify(chatSessionService).archiveSessionByUser("session-1", "student-1");
    }

    private org.springframework.http.HttpHeaders gatewayHeaders(String userId, String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", userId);
        headers.add("X-User-Role", role);
        headers.add("X-User-Email", userId + "@example.test");
        return headers;
    }

    private GenerateQuizResponse quizResponse() {
        return GenerateQuizResponse.builder()
                .totalQuestions(1)
                .questions(List.of(QuizQuestionResponse.builder()
                        .question("What is polymorphism?")
                        .questionType(QuestionType.SINGLE)
                        .answers(List.of(AnswerResponse.builder()
                                .content("One interface, many implementations")
                                .correct(true)
                                .build()))
                        .explanation("Polymorphism lets code work with many concrete types.")
                        .build()))
                .build();
    }
}
