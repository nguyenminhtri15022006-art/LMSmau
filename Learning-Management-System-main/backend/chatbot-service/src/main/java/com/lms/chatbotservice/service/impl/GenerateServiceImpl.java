package com.lms.chatbotservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.dto.feign.ProblemDTO;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.dto.response.*;
import com.lms.chatbotservice.enums.QuestionType;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.feign.AssignmentClient;
import com.lms.chatbotservice.service.GeminiClientService;
import com.lms.chatbotservice.service.GenerateService;
import com.lms.chatbotservice.service.PromptEngineService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateServiceImpl implements GenerateService {

    private final PromptEngineService promptEngineService;
    private final GeminiClientService geminiClientService;
    private final AssignmentClient assignmentClient;  // thêm mới
    private final ObjectMapper objectMapper;

    @Value("${chatbot.generate.json-parse-max-retry:2}")
    int jsonParseMaxRetry;

    // ── UC4a — Generate Quiz ──────────────────────────────

    @Override
    public GenerateQuizResponse generateQuiz(GenerateQuizRequest request) {

        // Bước 1: Build prompt cho quiz
        List<GeminiMessage> messages = promptEngineService.buildGenerateQuizPrompt(
                request.getContent(),
                request.getQuestionCount(),
                request.getDifficulty(),
                request.getQuestionType());

        // Bước 2: Gọi Gemini lấy JSON, retry nếu parse thất bại
        String jsonResponse = callWithRetry(messages, ErrorCode.GENERATE_QUIZ_FAILED);

        // Bước 3: Parse JSON thành response
        return parseQuizResponse(jsonResponse, request.getQuestionCount());
    }

    // ── UC4b — Generate Test Case ─────────────────────────

    @Override
    public GenerateTestCaseResponse generateTestCase(GenerateTestCaseRequest request) {

        // Resolve thông tin đề bài từ 2 hướng
        TestCaseProblemInfo info = resolveProblemInfo(request);

        List<GeminiMessage> messages = promptEngineService.buildGenerateTestCasePrompt(
                info.title(),
                info.description(),
                info.constraints(),
                request.getCount(),
                request.getAllowedLangs(),
                request.getExistingTestCases());

        String jsonResponse = callWithRetry(
                messages, ErrorCode.GENERATE_TESTCASE_FAILED);
        return parseTestCaseResponse(jsonResponse, request.getCount());
    }

    // ── Resolve problem info ──────────────────────────────

    private TestCaseProblemInfo resolveProblemInfo(GenerateTestCaseRequest request) {

        // Hướng 2: nhập thủ công — không có problemId
        if (request.getProblemId() == null) {
            validateManualInput(request);
            return new TestCaseProblemInfo(
                    request.getTitle(),
                    request.getDescription(),
                    request.getConstraints());
        }

        // Hướng 1: có problemId → fetch từ Assignment Service
        log.debug("Fetching problem {} from Assignment Service",
                request.getProblemId());

        ProblemDTO problem;
        try {
            ApiResponse<ProblemDTO> response = assignmentClient.getProblem(request.getProblemId());
            if (response == null || response.getData() == null) {
                throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
            }
            problem = response.getData();
        } catch (FeignException.NotFound e) {
            throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
        } catch (FeignException e) {
            log.error("Assignment service error fetching problem {}: {}",
                    request.getProblemId(), e.getMessage());
            throw new ChatbotException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        // Nếu giảng viên có nhập thêm thủ công thì ưu tiên thông tin thủ công
        // Hữu ích khi muốn override hoặc bổ sung thêm ràng buộc
        String title = isBlank(request.getTitle())
                ? problem.getTitle()
                : request.getTitle();

        String description = isBlank(request.getDescription())
                ? problem.getDescription()
                : request.getDescription();

        String constraints = isBlank(request.getConstraints())
                ? buildConstraintsFromProblem(problem)
                : request.getConstraints();

        log.debug("Resolved problem info: title={}", title);

        return new TestCaseProblemInfo(title, description, constraints);
    }

    // Build constraints từ các field có sẵn trong ProblemDTO
    private String buildConstraintsFromProblem(ProblemDTO problem) {
        StringBuilder sb = new StringBuilder();

        if (problem.getTimeLimitMs() != null) {
            sb.append("Giới hạn thời gian: ")
                    .append(problem.getTimeLimitMs())
                    .append("ms\n");
        }

        if (problem.getMemoryLimitMb() != null) {
            sb.append("Giới hạn bộ nhớ: ")
                    .append(problem.getMemoryLimitMb())
                    .append("MB\n");
        }

        if (problem.getAllowedLangs() != null && !problem.getAllowedLangs().isEmpty()) {
            sb.append("Ngôn ngữ cho phép: ")
                    .append(String.join(", ", problem.getAllowedLangs()))
                    .append("\n");
        }

        if (problem.getDifficulty() != null) {
            sb.append("Độ khó: ")
                    .append(problem.getDifficulty())
                    .append("\n");
        }

        return sb.toString().trim();
    }

    // Validate khi nhập thủ công — title và description bắt buộc
    private void validateManualInput(GenerateTestCaseRequest request) {
        if (isBlank(request.getTitle())) {
            throw new ChatbotException(ErrorCode.TESTCASE_TITLE_BLANK);
        }
        if (isBlank(request.getDescription())) {
            throw new ChatbotException(ErrorCode.TESTCASE_DESCRIPTION_BLANK);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Record để truyền nội dung đề bài giữa các method
    private record TestCaseProblemInfo(
            String title,
            String description,
            String constraints) {
    }

    // ── Retry logic ───────────────────────────────────────

    // Gọi Gemini và retry nếu JSON không hợp lệ
    // Tối đa MAX_RETRY lần — mỗi lần gọi lại prompt engine với cùng messages
    private String callWithRetry(List<GeminiMessage> messages, ErrorCode failureCode) {
        int attempt = 0;

        int maxAttempts = jsonParseMaxRetry + 1;

        while (attempt <= jsonParseMaxRetry) {
            try {
                String json = geminiClientService.generateJson(messages);

                // Validate JSON hợp lệ trước khi trả về
                objectMapper.readTree(json);
                return json;

            } catch (Exception e) {
                attempt++;
                log.warn("Gemini JSON invalid on attempt {}/{}: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt > jsonParseMaxRetry) {
                    log.error("All {} attempts failed", maxAttempts);
                    throw new ChatbotException(failureCode);
                }
            }
        }

        throw new ChatbotException(failureCode);
    }

    // ── Quiz parser ───────────────────────────────────────

    private GenerateQuizResponse parseQuizResponse(String json, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.path("questions");

            if (questionsNode.isMissingNode() || !questionsNode.isArray()) {
                log.error("Invalid quiz JSON structure: missing 'questions' array");
                throw new ChatbotException(ErrorCode.GENERATE_QUIZ_FAILED);
            }

            List<QuizQuestionResponse> questions = new ArrayList<>();

            for (JsonNode questionNode : questionsNode) {
                QuizQuestionResponse question = parseQuizQuestion(questionNode);
                questions.add(question);
            }

            log.info("Generated {} quiz questions (expected {})",
                    questions.size(), expectedCount);

            return GenerateQuizResponse.builder()
                    .totalQuestions(questions.size())
                    .questions(questions)
                    .build();

        } catch (ChatbotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse quiz JSON: {}", e.getMessage());
            throw new ChatbotException(ErrorCode.GENERATE_QUIZ_FAILED);
        }
    }

    private QuizQuestionResponse parseQuizQuestion(JsonNode node) {
        String question = node.path("question").asText("");
        String explanation = node.path("explanation").asText("");

        // Parse String từ JSON → QuestionType enum
        // Gemini trả về "SINGLE", "MULTIPLE", "TRUE_FALSE" — khớp với enum name
        QuestionType questionType;
        try {
            questionType = QuestionType.valueOf(
                    node.path("questionType").asText("SINGLE").toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown questionType from Gemini, defaulting to SINGLE");
            questionType = QuestionType.SINGLE;
        }

        JsonNode answersNode = node.path("answers");
        List<AnswerResponse> answers = new ArrayList<>();

        for (JsonNode answerNode : answersNode) {
            answers.add(AnswerResponse.builder()
                    .content(answerNode.path("content").asText(""))
                    .correct(answerNode.path("correct").asBoolean(false))
                    .build());
        }

        return QuizQuestionResponse.builder()
                .question(question)
                .questionType(questionType)
                .answers(answers)
                .explanation(explanation)
                .build();
    }

    // ── Test case parser ──────────────────────────────────

    private GenerateTestCaseResponse parseTestCaseResponse(
            String json, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode testCasesNode = root.path("testCases");

            if (testCasesNode.isMissingNode() || !testCasesNode.isArray()) {
                log.error("Invalid test case JSON structure: missing 'testCases' array");
                throw new ChatbotException(ErrorCode.GENERATE_TESTCASE_FAILED);
            }

            List<TestCaseResponse> testCases = new ArrayList<>();

            int index = 0;
            for (JsonNode testCaseNode : testCasesNode) {
                testCases.add(parseTestCase(testCaseNode, index));
                index++;
            }

            log.info("Generated {} test cases (expected {})",
                    testCases.size(), expectedCount);

            return GenerateTestCaseResponse.builder()
                    .totalTestCases(testCases.size())
                    .testCases(testCases)
                    .build();

        } catch (ChatbotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse test case JSON: {}", e.getMessage());
            throw new ChatbotException(ErrorCode.GENERATE_TESTCASE_FAILED);
        }
    }

    private TestCaseResponse parseTestCase(JsonNode node, int index) {
        return TestCaseResponse.builder()
                .input(node.path("input").asText(""))
                .expectedOutput(node.path("expectedOutput").asText(""))
                .isHidden(readBoolean(node, "isHidden", "hidden", true))
                .scoreWeight((float) node.path("scoreWeight").asDouble(1.0))
                .orderIndex((short) node.path("orderIndex").asInt(index))
                .description(node.path("description").asText(""))
                .build();
    }

    private boolean readBoolean(JsonNode node, String primaryField, String fallbackField, boolean defaultValue) {
        if (node.has(primaryField)) {
            return node.path(primaryField).asBoolean(defaultValue);
        }
        return node.path(fallbackField).asBoolean(defaultValue);
    }
}
