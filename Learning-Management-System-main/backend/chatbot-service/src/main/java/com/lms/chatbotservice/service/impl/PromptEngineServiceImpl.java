package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.dto.context.CourseContext;
import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.dto.context.ProblemContext;
import com.lms.chatbotservice.dto.gemini.GeminiMessage;
import com.lms.chatbotservice.dto.gemini.GeminiPart;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.entity.ChatMessage;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.enums.DifficultyType;
import com.lms.chatbotservice.enums.MessageRole;
import com.lms.chatbotservice.enums.QuestionType;
import com.lms.chatbotservice.service.PromptEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PromptEngineServiceImpl implements PromptEngineService {

    // Token budget: tổng token của history không vượt ngưỡng này
    @Value("${chatbot.prompt.max-history-tokens:3000}")
    int maxHistoryTokens;

    @Value("${chatbot.prompt.existing-test-cases-limit:20}")
    int existingTestCasesLimit;

    // ── Public methods ────────────────────────────────────

    @Override
    public List<GeminiMessage> buildChatPrompt(
            String userQuery,
            DomainContext context,
            List<ChatMessage> history,
            ContextType contextType) {

        List<GeminiMessage> messages = new ArrayList<>();

        // Layer 1: System Instruction
        String systemInstruction = buildSystemInstruction(contextType);

        // Layer 2: Domain Context
        String domainContext = buildDomainContext(context, contextType);

        // Ghép Layer 1 + Layer 2 thành tin nhắn đầu tiên role="user"
        // Gemini không có system role riêng — phải ghép vào user message đầu tiên
        String firstUserContent = systemInstruction + "\n\n" + domainContext;

        messages.add(buildUserMessage(firstUserContent));

        // Gemini yêu cầu messages phải xen kẽ user/model
        // Thêm model acknowledgement sau system+context
        messages.add(buildModelMessage(
                "Tôi đã hiểu vai trò và ngữ cảnh. Tôi sẵn sàng hỗ trợ bạn."));

        // Layer 3: Conversation History
        // Trim history theo token budget trước khi thêm vào
        List<ChatMessage> trimmedHistory = trimByTokenBudget(history);
        for (ChatMessage msg : trimmedHistory) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(buildUserMessage(msg.getContent()));
            } else {
                messages.add(buildModelMessage(msg.getContent()));
            }
        }

        // Layer 4: User Query
        messages.add(buildUserMessage(userQuery));

        log.debug("Built chat prompt: {} messages, contextType={}",
                messages.size(), contextType);

        return messages;
    }

    @Override
    public List<GeminiMessage> buildGenerateQuizPrompt(
            String content,
            int questionCount,
            DifficultyType difficulty,
            QuestionType questionType) {

        List<GeminiMessage> messages = new ArrayList<>();

        String prompt = buildQuizSystemInstruction(
                content, questionCount, difficulty, questionType);

        messages.add(buildUserMessage(prompt));

        log.debug("Built generate quiz prompt: count={}, difficulty={}, type={}",
                questionCount, difficulty, questionType);

        return messages;
    }

    @Override
    public List<GeminiMessage> buildGenerateTestCasePrompt(
            String title,
            String description,
            String constraints,
            int count,
            List<String> allowedLangs,
            List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases) {

        List<GeminiMessage> messages = new ArrayList<>();

        String prompt = buildTestCaseSystemInstruction(
                title, description, constraints, count, allowedLangs, existingTestCases);

        messages.add(buildUserMessage(prompt));

        log.debug("Built generate test case prompt: title={}, count={}", title, count);

        return messages;
    }

    // ── Layer 1: System Instructions ─────────────────────

    private String buildSystemInstruction(ContextType contextType) {
        return switch (contextType) {
            case GENERAL -> buildGeneralSystemInstruction();
            case PROBLEM -> buildSocraticSystemInstruction();
            case COURSE -> buildCourseAdvisorSystemInstruction();
            case GENERATE -> "";    // UC4 không dùng method này
        };
    }

    private String buildGeneralSystemInstruction() {
        return """
                [VAI TRÒ]
                Bạn là trợ lý học tập thông minh của hệ thống LMS. Nhiệm vụ của bạn là \
                hỗ trợ sinh viên giải đáp thắc mắc về lập trình, khoa học máy tính và \
                các chủ đề học thuật liên quan.
                
                [NGUYÊN TẮC]
                - Trả lời chính xác, rõ ràng và dễ hiểu
                - Sử dụng ví dụ minh họa khi cần thiết
                - Nếu câu hỏi không liên quan đến học tập, từ chối lịch sự
                - Trả lời bằng ngôn ngữ mà người dùng đang dùng
                """;
    }

    private String buildSocraticSystemInstruction() {
        return """
                [VAI TRÒ]
                Bạn là gia sư lập trình theo phương pháp Socratic. Nhiệm vụ của bạn là \
                hướng dẫn sinh viên tự tìm ra lỗi và giải pháp thông qua câu hỏi gợi mở.
                
                [NGUYÊN TẮC BẮT BUỘC]
                - TUYỆT ĐỐI KHÔNG đưa ra code giải pháp hoàn chỉnh
                - TUYỆT ĐỐI KHÔNG đưa ra đáp án trực tiếp
                - Hãy đặt câu hỏi để sinh viên tự suy nghĩ
                - Gợi ý hướng tiếp cận, không giải thay
                - Khen ngợi khi sinh viên tiến bộ
                - Nếu sinh viên hỏi "cho tôi code đi", hãy từ chối và tiếp tục gợi mở
                """;
    }

    private String buildCourseAdvisorSystemInstruction() {
        return """
                [VAI TRÒ]
                Bạn là cố vấn học tập của hệ thống LMS. Nhiệm vụ của bạn là phân tích \
                lịch sử học tập của sinh viên và đề xuất khóa học phù hợp nhất.
                
                [NGUYÊN TẮC]
                - Gợi ý dựa trên những gì sinh viên đã học và chưa học
                - Giải thích rõ lý do tại sao khóa học đó phù hợp
                - Ưu tiên khóa học chưa đăng ký và phù hợp với trình độ hiện tại
                - Không gợi ý quá 5 khóa học trong một lần
                """;
    }

    // ── Layer 2: Domain Context ───────────────────────────

    private String buildDomainContext(DomainContext context, ContextType contextType) {
        return switch (contextType) {
            case GENERAL -> buildGeneralDomainContext(context);
            case PROBLEM -> buildProblemDomainContext(context);
            case COURSE -> buildCourseDomainContext(context);
            case GENERATE -> "";
        };
    }

    private String buildGeneralDomainContext(DomainContext context) {
        return "[THÔNG TIN NGƯỜI DÙNG]\nUser ID: " + context.getUserId();
    }

    private String buildProblemDomainContext(DomainContext context) {
        ProblemContext problem = context.getProblem();
        if (problem == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("[THÔNG TIN BÀI TẬP]\n");
        sb.append("Tiêu đề: ").append(problem.getTitle()).append("\n");
        sb.append("Độ khó: ").append(problem.getDifficulty()).append("\n");
        sb.append("Giới hạn thời gian: ").append(problem.getTimeLimitMs()).append("ms\n");
        sb.append("Giới hạn bộ nhớ: ").append(problem.getMemoryLimitMb()).append("MB\n");
        sb.append("Ngôn ngữ cho phép: ")
                .append(String.join(", ", problem.getAllowedLangs())).append("\n");
        sb.append("\n[ĐỀ BÀI]\n").append(problem.getDescription()).append("\n");

        // Thêm thông tin submission nếu có
        if (problem.getLatestSourceCode() != null) {
            sb.append("\n[CODE HIỆN TẠI CỦA SINH VIÊN]\n");
            sb.append("Ngôn ngữ: ").append(problem.getLatestLanguage()).append("\n");
            sb.append("Trạng thái: ").append(problem.getLatestStatus()).append("\n");

            if (problem.getCompileError() != null) {
                sb.append("Lỗi compile: ").append(problem.getCompileError()).append("\n");
            }

            sb.append("```").append(problem.getLatestLanguage().toLowerCase()).append("\n");
            sb.append(problem.getLatestSourceCode()).append("\n```\n");
        } else {
            sb.append("\n[LƯU Ý] Sinh viên chưa nộp bài lần nào.\n");
        }

        return sb.toString();
    }

    private String buildCourseDomainContext(DomainContext context) {
        if (context.getCourses() == null || context.getCourses().isEmpty()) {
            return "[THÔNG TIN KHÓA HỌC]\nHiện chưa có khóa học nào trong hệ thống.";
        }

        // Tách thành 2 nhóm
        List<CourseContext> enrolled = context.getCourses().stream()
                .filter(CourseContext::isEnrolled)
                .collect(Collectors.toList());

        List<CourseContext> notEnrolled = context.getCourses().stream()
                .filter(c -> !c.isEnrolled())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();

        sb.append("[KHÓA HỌC ĐÃ ĐĂNG KÝ]\n");
        if (enrolled.isEmpty()) {
            sb.append("Sinh viên chưa đăng ký khóa học nào.\n");
        } else {
            enrolled.forEach(c -> appendCourseBlock(sb, c, false));
        }

        sb.append("\n[KHÓA HỌC CHƯA ĐĂNG KÝ]\n");
        if (notEnrolled.isEmpty()) {
            sb.append("Sinh viên đã đăng ký tất cả khóa học.\n");
        } else {
            notEnrolled.forEach(c -> appendCourseBlock(sb, c, true));
        }

        return sb.toString();
    }

    // ── Layer 1 + Full prompt cho UC4 ────────────────────

    private String buildQuizSystemInstruction(
            String content,
            int questionCount,
            DifficultyType difficulty,
            QuestionType questionType) {

        String typeInstruction = switch (questionType) {
            case SINGLE -> """
                    - Mỗi câu có đúng 4 lựa chọn (A, B, C, D)
                    - Chỉ có duy nhất 1 đáp án đúng
                    - Đánh dấu correct: true cho đáp án đúng, false cho các đáp án còn lại
                    """;
            case MULTIPLE -> """
                    - Mỗi câu có đúng 4 lựa chọn (A, B, C, D)
                    - Có từ 2 đến 3 đáp án đúng
                    - Đánh dấu correct: true cho các đáp án đúng, false cho các đáp án sai
                    """;
            case TRUE_FALSE -> """
                    - Mỗi câu chỉ có 2 lựa chọn: "Đúng" và "Sai"
                    - Chỉ có 1 đáp án đúng
                    - Đánh dấu correct: true cho đáp án đúng, false cho đáp án sai
                    """;
        };

        String difficultyInstruction = switch (difficulty) {
            case EASY -> "Câu hỏi ở mức cơ bản, kiểm tra kiến thức nhớ và hiểu.";
            case MEDIUM -> "Câu hỏi ở mức trung bình, yêu cầu hiểu và áp dụng.";
            case HARD -> "Câu hỏi ở mức nâng cao, yêu cầu phân tích và tổng hợp.";
        };

        return """
                Bạn là chuyên gia thiết kế câu hỏi kiểm tra. Hãy tạo %d câu hỏi trắc nghiệm \
                từ nội dung dưới đây.
                
                [YÊU CẦU ĐỘ KHÓ]
                %s
                
                [YÊU CẦU LOẠI CÂU HỎI]
                %s
                
                [YÊU CẦU FORMAT — BẮT BUỘC TUÂN THEO]
                Trả về JSON hợp lệ theo schema sau, KHÔNG có text thừa bên ngoài JSON:
                Mỗi explanation chỉ viết 1 câu ngắn.
                {
                  "questions": [
                    {
                      "question": "Nội dung câu hỏi",
                      "questionType": "%s",
                      "answers": [
                        { "content": "Nội dung đáp án", "correct": true/false }
                      ],
                      "explanation": "Giải thích tại sao đáp án đúng"
                    }
                  ]
                }
                
                [NỘI DUNG]
                %s
                """.formatted(
                questionCount,
                difficultyInstruction,
                typeInstruction,
                questionType.name(),
                content);
    }

    private String buildTestCaseSystemInstruction(
            String title,
            String description,
            String constraints,
            int count,
            List<String> allowedLangs,
            List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases) {

        String constraintsPart = constraints != null && !constraints.isBlank()
                ? "[RÀNG BUỘC]\n" + constraints
                : "";
        String allowedLangsPart = allowedLangs != null && !allowedLangs.isEmpty()
                ? "[NGON NGU CHO PHEP]\n" + String.join(", ", allowedLangs)
                : "";
        String existingPart = buildExistingTestCasesContext(existingTestCases);

        return """
                Bạn là kỹ sư kiểm thử phần mềm. Hãy sinh %d test case đa dạng cho bài toán sau.
                
                [YÊU CẦU TEST CASE]
                - Happy path cases: input hợp lệ thông thường
                - Edge cases: giá trị biên (min, max, rỗng, 0, âm...)
                - Corner cases: trường hợp đặc biệt (mảng rỗng, null, trùng lặp...)
                - Tránh lặp lại các test case đã có trong mục [TEST CASE HIEN CO]
                - input phải có mặt trong JSON; nếu bài không cần input thì để chuỗi rỗng ""
                - expectedOutput bắt buộc, không được rỗng
                - scoreWeight phải > 0
                - orderIndex phải là số nguyên không trùng nhau, bắt đầu từ 0 nếu không có yêu cầu khác
                
                [YÊU CẦU FORMAT — BẮT BUỘC TUÂN THEO]
                Trả về JSON hợp lệ theo schema sau, KHÔNG có text thừa bên ngoài JSON:
                {
                  "testCases": [
                    {
                      "input": "Dữ liệu đầu vào",
                      "expectedOutput": "Kết quả mong đợi",
                      "isHidden": true/false,
                      "scoreWeight": 1.0,
                      "orderIndex": 0,
                      "description": "Mô tả loại test case này"
                    }
                  ]
                }
                
                [BÀI TOÁN]
                Tiêu đề: %s
                
                [MÔ TẢ]
                %s
                
                %s
                
                %s
                
                %s
                """.formatted(count, title, description, constraintsPart, allowedLangsPart, existingPart);
    }

    private String buildExistingTestCasesContext(List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases) {
        if (existingTestCases == null || existingTestCases.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[TEST CASE HIEN CO]\n");
        int limit = Math.min(existingTestCases.size(), existingTestCasesLimit);
        for (int i = 0; i < limit; i++) {
            GenerateTestCaseRequest.ExistingTestCase testCase = existingTestCases.get(i);
            sb.append(i + 1)
                    .append(". input=")
                    .append(escapeOneLine(testCase.getInput()))
                    .append("; expectedOutput=")
                    .append(escapeOneLine(testCase.getExpectedOutput()))
                    .append("; hidden=")
                    .append(testCase.getHidden())
                    .append("; scoreWeight=")
                    .append(testCase.getScoreWeight())
                    .append("\n");
        }
        return sb.toString();
    }

    private String escapeOneLine(String value) {
        return value == null ? "" : value.replace("\n", "\\n");
    }

    // ── Token budget management ───────────────────────────

    private List<ChatMessage> trimByTokenBudget(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        // history là ASC (cũ → mới). Iterate ngược từ mới nhất để giữ
        // context gần nhất khi bị cắt bởi token budget.
        List<ChatMessage> trimmed = new ArrayList<>();
        int totalTokens = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            totalTokens += message.getTokenCount();
            if (totalTokens > maxHistoryTokens) {
                log.debug("Token budget exceeded at {} tokens, trimmed history to {} messages",
                        totalTokens, trimmed.size());
                break;
            }
            trimmed.add(message);
        }

        // Reverse lại để đúng thứ tự cũ → mới khi thêm vào prompt
        Collections.reverse(trimmed);
        return trimmed;
    }

    // ── Gemini message builders ───────────────────────────

    private void appendCourseBlock(StringBuilder sb, CourseContext c, boolean withDescription) {
        sb.append("• Tên: ").append(c.getTitle()).append("\n");

        if (withDescription && c.getDescription() != null && !c.getDescription().isBlank()) {
            sb.append("  Mô tả: ").append(c.getDescription()).append("\n");
        }

        if (c.getLevel() != null) {
            String levelLabel = switch (c.getLevel()) {
                case "BEGINNER" -> "Người mới bắt đầu";
                case "INTERMEDIATE" -> "Trung cấp";
                case "ADVANCED" -> "Nâng cao";
                default -> c.getLevel();
            };
            sb.append("  Trình độ: ").append(levelLabel).append("\n");
        }

        if (c.getDuration() != null) {
            sb.append("  Thời lượng: ").append(c.getDuration()).append(" tháng\n");
        }

        if (c.getCategoryNames() != null && !c.getCategoryNames().isEmpty()) {
            sb.append("  Danh mục: ").append(String.join(", ", c.getCategoryNames())).append("\n");
        }

        if (c.getLearningPoints() != null && !c.getLearningPoints().isEmpty()) {
            sb.append("  Bạn sẽ học được:\n");
            c.getLearningPoints().forEach(p -> sb.append("    - ").append(p).append("\n"));
        }

        if (c.getRequirements() != null && !c.getRequirements().isEmpty()) {
            sb.append("  Điều kiện tiên quyết:\n");
            c.getRequirements().forEach(r -> sb.append("    - ").append(r).append("\n"));
        }

        sb.append("\n");
    }

    private GeminiMessage buildUserMessage(String text) {
        return GeminiMessage.builder()
                .role("user")
                .parts(List.of(GeminiPart.builder().text(text).build()))
                .build();
    }

    private GeminiMessage buildModelMessage(String text) {
        return GeminiMessage.builder()
                .role("model")
                .parts(List.of(GeminiPart.builder().text(text).build()))
                .build();
    }
}
