package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GatewayAuthentication;
import com.lms.chatbotservice.dto.context.CourseContext;
import com.lms.chatbotservice.dto.context.DomainContext;
import com.lms.chatbotservice.dto.context.ProblemContext;
import com.lms.chatbotservice.dto.feign.CourseDTO;
import com.lms.chatbotservice.dto.feign.ProblemDTO;
import com.lms.chatbotservice.dto.feign.SubmissionDTO;
import com.lms.chatbotservice.dto.ApiResponse;
import com.lms.chatbotservice.enums.ContextType;
import com.lms.chatbotservice.exception.ChatbotException;
import com.lms.chatbotservice.exception.ErrorCode;
import com.lms.chatbotservice.feign.AssignmentClient;
import com.lms.chatbotservice.feign.CourseClient;
import com.lms.chatbotservice.service.ContextBuilderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderServiceImpl implements ContextBuilderService {

    private final AssignmentClient assignmentClient;
    private final CourseClient courseClient;

    @Override
    public DomainContext build(String contextRefId, ContextType contextType) {
        return build(contextRefId, contextType, GatewayAuthentication.currentUserId());
    }

    @Override
    public DomainContext build(String contextRefId, ContextType contextType, String userId) {
        return switch (contextType) {
            case GENERAL, GENERATE -> buildGeneralContext(userId);
            case PROBLEM -> buildProblemContext(userId, contextRefId);
            case COURSE -> buildCourseContext(userId);
        };
    }

    // ── UC1, UC4 — không cần gọi thêm service ────────────

    private DomainContext buildGeneralContext(String userId) {
        return DomainContext.builder()
                .userId(userId)
                .build();
    }

    // ── UC2 — lấy đề bài + lần nộp gần nhất ─────────────

    private DomainContext buildProblemContext(String userId, String contextRefId) {
        if (contextRefId == null || contextRefId.isBlank()) {
            throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        Integer problemId = parseProblemId(contextRefId);

        // Gọi Assignment Service lấy đề bài
        ProblemDTO problem;
        try {
            ApiResponse<ProblemDTO> problemResponse = assignmentClient.getProblem(problemId);
            if (problemResponse == null || problemResponse.getData() == null) {
                throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
            }
            problem = problemResponse.getData();
        } catch (FeignException.NotFound e) {
            throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
        } catch (FeignException e) {
            log.error("Assignment service error fetching problem {}: {}", problemId, e.getMessage());
            throw new ChatbotException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        // Gọi Assignment Service lấy lần nộp gần nhất
        // Không throw nếu null — sinh viên có thể chưa nộp lần nào
        SubmissionDTO submission = null;
        try {
            ApiResponse<SubmissionDTO> submissionResponse =
                    assignmentClient.getLatestSubmission(problemId, userId);
            if (submissionResponse != null) {
                submission = submissionResponse.getData();
            }
        } catch (FeignException e) {
            log.warn("Could not fetch latest submission for problem {} user {}: {}",
                    problemId, userId, e.getMessage());
        }

        return DomainContext.builder()
                .userId(userId)
                .problem(toProblemContext(problem, submission))
                .build();
    }

    // ── UC3 — lấy khóa học đã đăng ký + tất cả khóa học ─

    private DomainContext buildCourseContext(String userId) {

        // Gọi song song 2 API để giảm latency
        CompletableFuture<List<CourseDTO>> enrolledFuture = CompletableFuture
                .supplyAsync(() -> {
                    ApiResponse<List<CourseDTO>> res =
                            courseClient.getEnrolledCourses(userId);
                    return res != null && res.getData() != null
                            ? res.getData()
                            : Collections.emptyList();
                });

        CompletableFuture<List<CourseDTO>> allFuture = CompletableFuture
                .supplyAsync(() -> {
                    ApiResponse<List<CourseDTO>> res = courseClient.getAllCourses();
                    return res != null && res.getData() != null
                            ? res.getData()
                            : Collections.emptyList();
                });

        // Chờ cả 2 hoàn thành
        CompletableFuture.allOf(enrolledFuture, allFuture).join();

        List<CourseDTO> enrolled = enrolledFuture.join();
        List<CourseDTO> all = allFuture.join();

        // Merge: đánh dấu enrolled = true cho các khóa đã đăng ký
        Set<String> allIds = all.stream()
                .map(CourseDTO::getId)
                .collect(Collectors.toSet());

        List<String> enrolledIds = enrolled.stream()
                .map(CourseDTO::getId)
                .toList();

        // PUBLIC courses (từ all), đánh dấu enrolled nếu có trong danh sách đã đăng ký
        List<CourseContext> courses = new ArrayList<>(all.stream()
                .map(course -> toCourseContext(course, enrolledIds.contains(course.getId())))
                .toList());

        // Bổ sung PRIVATE courses đã đăng ký (không có trong danh sách all vì chỉ PUBLIC)
        enrolled.stream()
                .filter(e -> !allIds.contains(e.getId()))
                .map(e -> toCourseContext(e, true))
                .forEach(courses::add);

        return DomainContext.builder()
                .userId(userId)
                .courses(courses)
                .build();
    }

    // ── Mappers ───────────────────────────────────────────

    private ProblemContext toProblemContext(ProblemDTO problem,
                                            SubmissionDTO submission) {

        ProblemContext.ProblemContextBuilder builder = ProblemContext.builder()
                .problemId(problem.getProblemId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .allowedLangs(problem.getAllowedLangs());

        // Thêm thông tin submission nếu có
        if (submission != null) {
            builder.latestSourceCode(submission.getSourceCode())
                    .latestStatus(submission.getStatus())
                    .latestLanguage(submission.getLanguage())
                    .compileError(submission.getCompileError());
        }

        return builder.build();
    }

    private CourseContext toCourseContext(CourseDTO course, boolean enrolled) {
        return CourseContext.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorId(course.getInstructorId())
                .status(course.getStatus())
                .duration(course.getDuration())
                .level(course.getLevel())
                .categoryNames(course.getCategoryNames())
                .learningPoints(course.getLearningPoints())
                .requirements(course.getRequirements())
                .enrolled(enrolled)
                .build();
    }

    private Integer parseProblemId(String contextRefId) {
        try {
            return Integer.parseInt(contextRefId);
        } catch (NumberFormatException e) {
            throw new ChatbotException(ErrorCode.PROBLEM_NOT_FOUND);
        }
    }
}