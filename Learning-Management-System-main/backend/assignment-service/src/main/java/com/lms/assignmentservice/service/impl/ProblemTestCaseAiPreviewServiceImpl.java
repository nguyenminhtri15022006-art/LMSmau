package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.AiTestCasePreviewRequest;
import com.lms.assignmentservice.dto.request.GenerateTestCaseRequest;
import com.lms.assignmentservice.dto.response.AiTestCasePreviewResponse;
import com.lms.assignmentservice.dto.response.GenerateTestCaseResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.repository.httpClient.ChatbotClient;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import com.lms.assignmentservice.service.ProblemTestCaseAiPreviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProblemTestCaseAiPreviewServiceImpl implements ProblemTestCaseAiPreviewService {

    private static final String REVIEW_WARNING = "Test cases do AI tạo cần được kiểm tra lại trước khi lưu.";

    ProblemRepository problemRepository;
    TestCaseRepository testCaseRepository;
    SubmissionRepository submissionRepository;
    CourseResourceAuthorizationService authorizationService;
    ChatbotClient chatbotClient;

    @Value("${quiz.ai.test-case-max-count:50}")
    @NonFinal
    int maxTestCases;

    @Override
    public AiTestCasePreviewResponse preview(AiTestCasePreviewRequest request) {
        validateCount(request.getCount());

        ProblemContext context = resolveProblemContext(request);
        GenerateTestCaseRequest chatbotRequest = new GenerateTestCaseRequest();
        chatbotRequest.setProblemId(context.problemId());
        chatbotRequest.setTitle(context.title());
        chatbotRequest.setDescription(context.description());
        chatbotRequest.setConstraints(context.constraints());
        chatbotRequest.setCount(request.getCount());
        chatbotRequest.setAllowedLangs(context.allowedLangs());
        chatbotRequest.setExistingTestCases(context.existingTestCases());

        ApiResponse<GenerateTestCaseResponse> response = chatbotClient.generateTestCases(chatbotRequest);
        GenerateTestCaseResponse generated = response == null ? null : response.getData();
        List<GenerateTestCaseResponse.TestCaseItem> normalized = normalizeGeneratedTestCases(generated, request.getCount());

        if (context.problemId() != null && submissionRepository.existsByProblemId(context.problemId())) {
            log.info("AI test case preview generated for locked problem {}. Preview only, no DB mutation.", context.problemId());
        }

        return AiTestCasePreviewResponse.builder()
                .totalTestCases(normalized.size())
                .testCases(normalized)
                .warning(REVIEW_WARNING)
                .build();
    }

    private ProblemContext resolveProblemContext(AiTestCasePreviewRequest request) {
        if (request.getProblemId() != null) {
            Problem problem = problemRepository.findById(request.getProblemId())
                    .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));
            authorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

            List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases = testCaseRepository
                    .findByProblem_ProblemIdOrderByOrderIndexAsc(problem.getProblemId())
                    .stream()
                    .map(this::mapExistingTestCase)
                    .toList();

            return new ProblemContext(
                    problem.getProblemId(),
                    firstNonBlank(request.getTitle(), problem.getTitle()),
                    firstNonBlank(request.getDescription(), problem.getDescription()),
                    buildConstraints(request, problem),
                    request.getAllowedLangs() == null || request.getAllowedLangs().isEmpty()
                            ? problem.getAllowedLangs()
                            : request.getAllowedLangs(),
                    existingTestCases);
        }

        if (isBlank(request.getCourseId())) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }
        authorizationService.checkManager(request.getCourseId(), null);

        if (isBlank(request.getTitle()) || isBlank(request.getDescription())) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }

        return new ProblemContext(
                null,
                request.getTitle().trim(),
                request.getDescription().trim(),
                buildConstraints(request, null),
                request.getAllowedLangs() == null ? List.of() : request.getAllowedLangs(),
                request.getExistingTestCases() == null ? List.of() : request.getExistingTestCases());
    }

    private List<GenerateTestCaseResponse.TestCaseItem> normalizeGeneratedTestCases(
            GenerateTestCaseResponse generated,
            int requestedCount) {
        if (generated == null || generated.getTestCases() == null || generated.getTestCases().isEmpty()) {
            throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
        }

        if (generated.getTestCases().size() > maxTestCases || generated.getTestCases().size() > requestedCount) {
            throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
        }

        Set<Short> orderIndexes = new HashSet<>();
        List<GenerateTestCaseResponse.TestCaseItem> normalized = new ArrayList<>();
        for (int i = 0; i < generated.getTestCases().size(); i++) {
            GenerateTestCaseResponse.TestCaseItem item = generated.getTestCases().get(i);
            if (item == null || item.getInput() == null || isBlank(item.getExpectedOutput())) {
                throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
            }

            Float scoreWeight = item.getScoreWeight() == null ? 1.0f : item.getScoreWeight();
            if (!Float.isFinite(scoreWeight) || scoreWeight <= 0) {
                throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
            }

            Short orderIndex = item.getOrderIndex() == null ? (short) i : item.getOrderIndex();
            if (orderIndex < 0) {
                throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
            }
            if (!orderIndexes.add(orderIndex)) {
                throw new AssignmentException(ErrorCode.AI_GENERATE_TEST_CASES_FAILED);
            }

            GenerateTestCaseResponse.TestCaseItem normalizedItem = new GenerateTestCaseResponse.TestCaseItem();
            normalizedItem.setInput(item.getInput());
            normalizedItem.setExpectedOutput(item.getExpectedOutput());
            normalizedItem.setHidden(item.getHidden() == null ? Boolean.TRUE : item.getHidden());
            normalizedItem.setScoreWeight(scoreWeight);
            normalizedItem.setOrderIndex(orderIndex);
            normalizedItem.setDescription(item.getDescription());
            normalized.add(normalizedItem);
        }
        return normalized;
    }

    private GenerateTestCaseRequest.ExistingTestCase mapExistingTestCase(TestCase testCase) {
        GenerateTestCaseRequest.ExistingTestCase item = new GenerateTestCaseRequest.ExistingTestCase();
        item.setInput(testCase.getInput());
        item.setExpectedOutput(testCase.getExpectedOutput());
        item.setHidden(testCase.getHidden());
        item.setOrderIndex(testCase.getOrderIndex());
        item.setScoreWeight(testCase.getScoreWeight());
        return item;
    }

    private String buildConstraints(AiTestCasePreviewRequest request, Problem problem) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(request.getConstraints())) {
            appendLine(sb, "Rang buoc bai toan", request.getConstraints());
            return sb.toString().trim();
        }
        appendLine(sb, "Ràng buộc bài toán", request.getConstraints());
        appendLine(sb, "Độ khó", valueOrDefault(request.getDifficulty(), problem == null ? null : problem.getDifficulty()));
        appendLine(sb, "Giới hạn thời gian", valueOrDefault(request.getTimeLimitMs(), problem == null ? null : problem.getTimeLimitMs()), "ms");
        appendLine(sb, "Giới hạn bộ nhớ", valueOrDefault(request.getMemoryLimitMb(), problem == null ? null : problem.getMemoryLimitMb()), "MB");
        appendLine(sb, "Điểm", valueOrDefault(request.getScore(), problem == null ? null : problem.getScore()));

        List<String> allowedLangs = request.getAllowedLangs() == null || request.getAllowedLangs().isEmpty()
                ? problem == null ? List.of() : problem.getAllowedLangs()
                : request.getAllowedLangs();
        if (allowedLangs != null && !allowedLangs.isEmpty()) {
            appendLine(sb, "Ngôn ngữ cho phép", String.join(", ", allowedLangs));
        }
        return sb.toString().trim();
    }

    private void validateCount(Integer count) {
        if (count == null || count < 1 || count > maxTestCases) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Object valueOrDefault(Object requestValue, Object fallbackValue) {
        return requestValue == null ? fallbackValue : requestValue;
    }

    private void appendLine(StringBuilder sb, String label, Object value) {
        appendLine(sb, label, value, "");
    }

    private void appendLine(StringBuilder sb, String label, Object value, String suffix) {
        if (value == null) return;
        String text = String.valueOf(value);
        if (text.isBlank()) return;
        sb.append(label).append(": ").append(text).append(suffix).append('\n');
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProblemContext(
            Integer problemId,
            String title,
            String description,
            String constraints,
            List<String> allowedLangs,
            List<GenerateTestCaseRequest.ExistingTestCase> existingTestCases) {
    }
}
