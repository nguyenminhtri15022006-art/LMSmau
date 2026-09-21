package com.lms.assignmentservice.judge.impl;

import com.lms.assignmentservice.judge.JudgeEngine;
import com.lms.assignmentservice.judge.JudgeEngineException;
import com.lms.assignmentservice.judge.JudgeRequest;
import com.lms.assignmentservice.judge.JudgeResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Engine tự build — implement interface này khi có máy chấm riêng.
 * <p>
 * Chỉ cần:
 * 1. Implement 4 method của JudgeEngine
 * 2. Đổi config: judge.engine=custom
 * 3. Không sửa bất kỳ code nào khác (SubmissionService, Controller, ...)
 * <p>
 * Đây là sức mạnh của Open/Closed Principle.
 */
@Component
@ConditionalOnProperty(name = "judge.engine", havingValue = "custom")
@Slf4j
public class CustomEngine implements JudgeEngine {

    @Value("${judge.custom.base-url}")
    private String baseUrl;

    @Value("${judge.custom.api-key:}")
    private String apiKey;

    @Value("${judge.custom.max-poll-attempts:10}")
    private int maxPollAttempts;

    @Value("${judge.custom.poll-interval-ms:2000}")
    private long pollIntervalMs;

    private final RestTemplate restTemplate = new RestTemplate();

    // Ngôn ngữ engine tự build hỗ trợ — có thể nhiều hơn Judge0
    private static final List<String> SUPPORTED = List.of(
            "JAVA", "PYTHON", "CPP", "JAVASCRIPT", "GOLANG", "RUST"
    );

    @PostConstruct
    void failFastUntilImplemented() {
        throw new IllegalStateException("judge.engine=custom is not implemented; use judge.engine=judge0");
    }

    @Override
    public String engineName() {
        return "custom";
    }

    @Override
    public List<String> supportedLanguages() {
        return SUPPORTED;
    }

    @Override
    public JudgeResult judge(JudgeRequest request) throws JudgeEngineException {
        log.info("Custom engine: chấm {} test cases, lang={}",
                request.getTestCases().size(), request.getLanguage());

        try {
            /*
             * TODO: Implement theo API của engine tự build.
             *
             * Gợi ý flow cho engine tự build:
             * 1. POST /judge/submit → { jobId }
             * 2. GET  /judge/result/{jobId} → poll đến khi done
             * 3. Map kết quả về JudgeResult
             *
             * Dù API của engine tự build khác hoàn toàn Judge0,
             * output cuối cùng vẫn phải là JudgeResult chuẩn.
             */

            // Ví dụ call engine tự build:
            var submitResponse = restTemplate.postForObject(
                    baseUrl + "/judge/submit",
                    buildRequest(request),
                    Map.class
            );

            if (submitResponse == null) {
                throw new JudgeEngineException(
                        JudgeEngineException.ErrorType.INVALID_RESPONSE,
                        "Custom engine trả về null");
            }

            String jobId = (String) submitResponse.get("jobId");
            return pollAndMap(jobId, request);

        } catch (JudgeEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new JudgeEngineException(
                    JudgeEngineException.ErrorType.CONNECTION_FAILED,
                    "Custom engine lỗi: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            restTemplate.getForEntity(baseUrl + "/health", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Private ====================

    private Map<String, Object> buildRequest(JudgeRequest request) {
        // Convert JudgeRequest → format của engine tự build
        return Map.of(
                "language", request.getLanguage().toLowerCase(),
                "code", request.getSourceCode(),
                "timeLimit", request.getTimeLimitMs(),
                "memLimit", request.getMemoryLimitMb(),
                "testCases", request.getTestCases().stream()
                        .map(tc -> Map.of(
                                "id", tc.getTestCaseId(),
                                "input", tc.getInput(),
                                "output", tc.getExpectedOutput()
                        )).toList()
        );
    }

    @SuppressWarnings("unchecked")
    private JudgeResult pollAndMap(String jobId, JudgeRequest request)
            throws JudgeEngineException, InterruptedException {

        for (int i = 0; i < maxPollAttempts; i++) {
            Thread.sleep(pollIntervalMs);

            Map<String, Object> result = restTemplate.getForObject(
                    baseUrl + "/judge/result/" + jobId, Map.class);

            if (result == null) continue;

            String status = (String) result.get("status");
            if (!"DONE".equals(status) && !"FAILED".equals(status)) continue;

            // Map kết quả custom engine → JudgeResult chuẩn
            return mapCustomResult(result, request);
        }

        throw new JudgeEngineException(JudgeEngineException.ErrorType.TIMEOUT,
                "Custom engine timeout");
    }

    private JudgeResult mapCustomResult(Map<String, Object> raw, JudgeRequest request) {
        // TODO: implement theo response format của engine tự build
        // Đây là nơi duy nhất biết format của custom engine
        throw new JudgeEngineException(
                JudgeEngineException.ErrorType.INVALID_RESPONSE,
                "Custom engine result mapping is not implemented");
    }
}
