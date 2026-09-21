package com.lms.chatbotservice.controller;

import com.lms.chatbotservice.configuration.GatewayAuthentication;
import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.dto.response.*;
import com.lms.chatbotservice.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final GenerateService generateService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final RateLimiterService rateLimiterService;

    // ── UC1, UC2, UC3 — Streaming chat ───────────────────

    @PostMapping(value = "/chat",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<String> chat(@RequestBody @Valid ChatRequest request) {
        String userId = GatewayAuthentication.currentUserId();
        String role   = GatewayAuthentication.currentRole();

        rateLimiterService.checkAndIncrement(userId, role);

        return chatService.chat(request);
    }

    // ── UC4a — Generate Quiz ──────────────────────────────

    @PostMapping("/generate/quiz")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<GenerateQuizResponse> generateQuiz(
            @RequestBody @Valid GenerateQuizRequest request) {
        String userId = GatewayAuthentication.currentUserId();
        String role   = GatewayAuthentication.currentRole();

        rateLimiterService.checkAndIncrement(userId, role);

        return ApiResponse.<GenerateQuizResponse>builder()
                .data(generateService.generateQuiz(request))
                .build();
    }

    // ── UC4b — Generate Test Case ─────────────────────────

    @PostMapping("/generate/testcase")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<GenerateTestCaseResponse> generateTestCase(
            @RequestBody @Valid GenerateTestCaseRequest request) {
        String userId = GatewayAuthentication.currentUserId();
        String role   = GatewayAuthentication.currentRole();

        rateLimiterService.checkAndIncrement(userId, role);

        return ApiResponse.<GenerateTestCaseResponse>builder()
                .data(generateService.generateTestCase(request))
                .build();
    }

    // ── Session management ────────────────────────────────

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ChatSessionResponse>> getSessions(
            @PageableDefault(size = 20) Pageable pageable) {
        String userId = GatewayAuthentication.currentUserId();

        return ApiResponse.<Page<ChatSessionResponse>>builder()
                .data(chatSessionService.getSessionsByUser(userId, pageable))
                .build();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable String sessionId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .data(chatMessageService.getMessageResponses(sessionId))
                .build();
    }

    // Sinh viên bấm "New Chat" → archive session hiện tại
    // Lần chat tiếp theo sẽ tạo session mới
    @PostMapping("/sessions/{sessionId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> archiveSession(@PathVariable String sessionId) {
        String userId = GatewayAuthentication.currentUserId();
        chatSessionService.archiveSessionByUser(sessionId, userId);
        return ApiResponse.<Void>builder().build();
    }
}