package com.lms.chatbotservice.controller;

import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.dto.response.GenerateQuizResponse;
import com.lms.chatbotservice.dto.response.GenerateTestCaseResponse;
import com.lms.chatbotservice.service.GenerateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final GenerateService generateService;

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.<String>builder()
                .code(200)
                .message("OK")
                .data("chatbot-service is running")
                .build();
    }

    @PostMapping("/generate/quiz")
    public ApiResponse<GenerateQuizResponse> generateQuiz(
            @RequestBody @Valid GenerateQuizRequest request) {
        return ApiResponse.<GenerateQuizResponse>builder()
                .data(generateService.generateQuiz(request))
                .build();
    }

    @PostMapping("/generate/testcase")
    public ApiResponse<GenerateTestCaseResponse> generateTestCase(
            @RequestBody @Valid GenerateTestCaseRequest request) {
        return ApiResponse.<GenerateTestCaseResponse>builder()
                .data(generateService.generateTestCase(request))
                .build();
    }
}
