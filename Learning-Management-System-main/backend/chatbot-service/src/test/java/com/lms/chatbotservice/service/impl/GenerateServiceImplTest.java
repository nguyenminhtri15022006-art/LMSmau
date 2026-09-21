package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.dto.feign.ProblemDTO;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.enums.DifficultyType;
import com.lms.chatbotservice.enums.QuestionType;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.feign.AssignmentClient;
import com.lms.chatbotservice.service.GeminiClientService;
import com.lms.chatbotservice.service.PromptEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateServiceImplTest {

    @Mock
    PromptEngineService promptEngineService;
    @Mock
    GeminiClientService geminiClientService;
    @Mock
    AssignmentClient assignmentClient;

    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    GenerateServiceImpl generateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generateService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(generateService, "jsonParseMaxRetry", 1);
    }

    @Test
    void generateQuizBuildsPromptParsesJsonAndDefaultsUnknownQuestionType() {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .content("Java basics")
                .questionCount(1)
                .difficulty(DifficultyType.EASY)
                .questionType(QuestionType.SINGLE)
                .build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());
        when(promptEngineService.buildGenerateQuizPrompt("Java basics", 1, DifficultyType.EASY, QuestionType.SINGLE))
                .thenReturn(prompt);
        when(geminiClientService.generateJson(prompt)).thenReturn("""
                {
                  "questions": [
                    {
                      "question": "Java la gi?",
                      "questionType": "unknown",
                      "answers": [
                        { "content": "Ngon ngu lap trinh", "correct": true },
                        { "content": "CSDL", "correct": false }
                      ],
                      "explanation": "Java la ngon ngu lap trinh."
                    }
                  ]
                }
                """);

        var response = generateService.generateQuiz(request);

        assertThat(response.getTotalQuestions()).isEqualTo(1);
        assertThat(response.getQuestions()).singleElement().satisfies(question -> {
            assertThat(question.getQuestion()).isEqualTo("Java la gi?");
            assertThat(question.getQuestionType()).isEqualTo(QuestionType.SINGLE);
            assertThat(question.getAnswers()).hasSize(2);
        });
    }

    @Test
    void generateQuizRetriesInvalidJsonBeforeParsingSuccessfulResponse() {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .content("Java basics")
                .questionCount(1)
                .difficulty(DifficultyType.EASY)
                .questionType(QuestionType.SINGLE)
                .build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());
        when(promptEngineService.buildGenerateQuizPrompt(any(), any(Integer.class), any(), any()))
                .thenReturn(prompt);
        when(geminiClientService.generateJson(prompt))
                .thenReturn("not json")
                .thenReturn("""
                        {"questions":[{"question":"Q","questionType":"SINGLE","answers":[],"explanation":"E"}]}
                        """);

        var response = generateService.generateQuiz(request);

        assertThat(response.getTotalQuestions()).isEqualTo(1);
        verify(geminiClientService, times(2)).generateJson(prompt);
    }

    @Test
    void generateQuizThrowsAfterJsonRetryExhausted() {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .content("Java basics")
                .questionCount(1)
                .difficulty(DifficultyType.EASY)
                .questionType(QuestionType.SINGLE)
                .build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());
        when(promptEngineService.buildGenerateQuizPrompt(any(), any(Integer.class), any(), any()))
                .thenReturn(prompt);
        when(geminiClientService.generateJson(prompt)).thenReturn("not json");

        assertThatThrownBy(() -> generateService.generateQuiz(request))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GENERATE_QUIZ_FAILED);

        verify(geminiClientService, times(2)).generateJson(prompt);
    }

    @Test
    void generateTestCaseUsesManualProblemInfoAndParsesHiddenFallbackField() {
        GenerateTestCaseRequest request = GenerateTestCaseRequest.builder()
                .title("Two Sum")
                .description("Find indices")
                .constraints("n <= 1000")
                .count(1)
                .allowedLangs(List.of("java"))
                .build();
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());
        when(promptEngineService.buildGenerateTestCasePrompt(
                "Two Sum", "Find indices", "n <= 1000", 1, List.of("java"), null))
                .thenReturn(prompt);
        when(geminiClientService.generateJson(prompt)).thenReturn("""
                {
                  "testCases": [
                    {
                      "input": "2 7 11 15\\n9",
                      "expectedOutput": "0 1",
                      "hidden": false,
                      "scoreWeight": 2.5,
                      "orderIndex": 3,
                      "description": "happy path"
                    }
                  ]
                }
                """);

        var response = generateService.generateTestCase(request);

        assertThat(response.getTotalTestCases()).isEqualTo(1);
        assertThat(response.getTestCases()).singleElement().satisfies(testCase -> {
            assertThat(testCase.getInput()).contains("9");
            assertThat(testCase.isHidden()).isFalse();
            assertThat(testCase.getScoreWeight()).isEqualTo(2.5f);
            assertThat(testCase.getOrderIndex()).isEqualTo((short) 3);
        });
    }

    @Test
    void generateTestCaseFetchesProblemAndAllowsManualOverride() {
        ProblemDTO problem = new ProblemDTO();
        problem.setProblemId(42);
        problem.setTitle("Original");
        problem.setDescription("Original description");
        problem.setTimeLimitMs(1000);
        problem.setMemoryLimitMb(256);
        problem.setAllowedLangs(List.of("java", "cpp"));
        problem.setDifficulty("EASY");
        when(assignmentClient.getProblem(42)).thenReturn(ApiResponse.of(problem));
        List<GeminiMessage> prompt = List.of(GeminiMessage.builder().role("user").build());
        when(promptEngineService.buildGenerateTestCasePrompt(
                org.mockito.ArgumentMatchers.eq("Override"),
                org.mockito.ArgumentMatchers.eq("Original description"),
                org.mockito.ArgumentMatchers.contains("1000ms"),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(null),
                org.mockito.ArgumentMatchers.eq(null)))
                .thenReturn(prompt);
        when(geminiClientService.generateJson(prompt))
                .thenReturn("{\"testCases\":[{\"input\":\"1\",\"expectedOutput\":\"1\"}]}");

        var response = generateService.generateTestCase(GenerateTestCaseRequest.builder()
                .problemId(42)
                .title("Override")
                .count(1)
                .build());

        assertThat(response.getTotalTestCases()).isEqualTo(1);
    }

    @Test
    void generateTestCaseRejectsManualInputWithoutTitle() {
        GenerateTestCaseRequest request = GenerateTestCaseRequest.builder()
                .description("Find sum")
                .count(1)
                .build();

        assertThatThrownBy(() -> generateService.generateTestCase(request))
                .isInstanceOf(ChatbotException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TESTCASE_TITLE_BLANK);
    }
}
