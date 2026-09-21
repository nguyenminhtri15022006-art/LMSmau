package com.lms.assignmentservice.repository.httpClient;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.GenerateQuizRequest;
import com.lms.assignmentservice.dto.request.GenerateTestCaseRequest;
import com.lms.assignmentservice.dto.response.GenerateQuizResponse;
import com.lms.assignmentservice.dto.response.GenerateTestCaseResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "chatbot-service",
        path = "/chatbot",
        fallbackFactory = ChatbotClient.FallbackFactoryImpl.class
)
public interface ChatbotClient {

    @PostMapping("/internal/generate/quiz")
    ApiResponse<GenerateQuizResponse> generateQuiz(@RequestBody GenerateQuizRequest request);

    @PostMapping("/internal/generate/testcase")
    ApiResponse<GenerateTestCaseResponse> generateTestCases(@RequestBody GenerateTestCaseRequest request);

    @Component
    class FallbackFactoryImpl implements FallbackFactory<ChatbotClient> {
        @Override
        public ChatbotClient create(Throwable cause) {
            return new ChatbotClient() {
                @Override
                public ApiResponse<GenerateQuizResponse> generateQuiz(GenerateQuizRequest request) {
                    throw resolveFailure(ErrorCode.AI_GENERATE_QUIZ_FAILED);
                }

                @Override
                public ApiResponse<GenerateTestCaseResponse> generateTestCases(GenerateTestCaseRequest request) {
                    throw resolveFailure(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
                }

                private AssignmentException resolveFailure(ErrorCode clientErrorCode) {
                    if (cause instanceof FeignException feignException && feignException.status() >= 400 && feignException.status() < 500) {
                        return new AssignmentException(clientErrorCode);
                    }
                    return new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }
            };
        }
    }
}
