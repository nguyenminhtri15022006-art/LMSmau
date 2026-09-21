package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.AutoSaveAttemptRequest;
import com.lms.assignmentservice.dto.request.PracticeAnswerRequest;
import com.lms.assignmentservice.dto.request.SubmitAttemptRequest;
import com.lms.assignmentservice.dto.response.AttemptHistoryResponse;
import com.lms.assignmentservice.dto.response.PracticeAnswerResponse;
import com.lms.assignmentservice.dto.response.QuizAttemptResponse;
import com.lms.assignmentservice.dto.response.QuizResultResponse;
import com.lms.assignmentservice.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho Quiz Attempts — Học viên làm bài.
 *
 * CRITICAL SECURITY NOTES:
 * - X-User-Id header được set bởi API Gateway (AuthFilter)
 * - Tất cả hàm phải lấy userId từ GatewayAuthentication.currentUserId()
 * - API Gateway đã verify JWT trước khi forward request, nên ta chỉ cần tin tưởng header
 *
 * Endpoints:
 * 1. POST   /{quizId}/attempts                      — Bắt đầu làm bài
 * 2. GET    /{quizId}/attempts/{attemptId}          — Lấy bài đang làm (F5 recovery)
 * 3. PUT    /{quizId}/attempts/{attemptId}          — Auto-save câu trả lời
 * 4. POST   /{quizId}/attempts/{attemptId}/submit   — Nộp bài
 * 5. GET    /{quizId}/attempts/{attemptId}/result   — Xem kết quả
 * 6. GET    /{quizId}/attempts                      — Lịch sử làm bài
 */
@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizAttemptController {

    QuizAttemptService quizAttemptService;

    // ==================== 1. START OR RESUME ATTEMPT ====================

    /**
     * Bắt đầu hoặc tiếp tục làm bài (gộp start + resume).
     *
     * Mục đích:
     * - Nếu đã có attempt IN_PROGRESS chưa hết giờ → resume (trả về attempt cũ)
     * - Nếu chưa có attempt → start (tạo attempt mới)
     * - Tránh race condition khi frontend gọi 2 API riêng biệt
     *
     * Response: QuizAttemptResponse (chứa questions + answers nhưng KHÔNG có field `correct`)
     */
    @PostMapping(value = "/{quizId}/attempts/start-or-resume")
    ApiResponse<QuizAttemptResponse> startOrResumeAttempt(@PathVariable Integer quizId) {
        log.info("API: startOrResumeAttempt(quizId={})", quizId);
        return ApiResponse.<QuizAttemptResponse>builder()
                .data(quizAttemptService.startOrResumeAttempt(quizId))
                .message("Bắt đầu làm bài thành công")
                .build();
    }

    // ==================== 2. GET IN-PROGRESS ATTEMPT ====================

    /**
     * Lấy lại bài đang làm (F5 recovery).
     *
     * Mục đích:
     * - Học viên lỡ F5 load lại trang hoặc đóng tab
     * - API này trả về lại câu hỏi + các đáp án họ đã chọn trước đó
     *
     * Kiểm tra bảo mật:
     * - Attempt phải của user hiện tại
     * - Attempt phải IN_PROGRESS (chưa submit)
     * - Chưa hết giờ (expiresAt)
     *
     * Response: QuizAttemptResponse (giống startAttempt)
     */
    @GetMapping(value = "/{quizId}/attempts/{attemptId}")
    ApiResponse<QuizAttemptResponse> getInProgressAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId) {
        log.info("API: getInProgressAttempt(quizId={}, attemptId={})", quizId, attemptId);
        return ApiResponse.<QuizAttemptResponse>builder()
                .data(quizAttemptService.getInProgressAttempt(quizId, attemptId))
                .message("Lấy bài đang làm thành công")
                .build();
    }

    // ==================== 3. AUTO-SAVE ATTEMPT ====================

    /**
     * Lưu đáp án tạm thời (Auto-save).
     *
     * Mục đích:
     * - Client (Frontend) gọi API này ngầm mỗi 30 giây hoặc mỗi khi học viên click chọn đáp án
     * - Tránh rớt mạng phút cuối làm mất toàn bộ bài
     *
     * IDEMPOTENCY (Tính lũy đẳng):
     * - Dù Frontend gọi API 1 lần hay 100 lần với cùng dữ liệu
     * - DB sẽ chỉ cập nhật đúng như vậy, không sinh dữ liệu rác hay lỗi trùng lặp
     * - Cách implement: Xóa records cũ, insert mới (delete-then-insert pattern)
     *
     * Request body:
     * {
     *   "answers": [
     *     { "questionId": 5, "selectedAnswerIds": [12, 13] },
     *     { "questionId": 6, "selectedAnswerIds": [15] }
     *   ]
     * }
     *
     * Response: Chỉ trả về success message (no data)
     */
    @PutMapping(value = "/{quizId}/attempts/{attemptId}")
    ApiResponse<Void> autoSaveAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @Valid @RequestBody AutoSaveAttemptRequest request) {
        log.info("API: autoSaveAttempt(quizId={}, attemptId={}, {} answers)", quizId, attemptId, request.getAnswers().size());
        quizAttemptService.autoSaveAttempt(quizId, attemptId, request);
        return ApiResponse.<Void>builder()
                .message("Lưu đáp án thành công")
                .build();
    }

    @PostMapping(value = "/{quizId}/attempts/{attemptId}/practice-check")
    ApiResponse<PracticeAnswerResponse> checkPracticeAnswer(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @RequestBody PracticeAnswerRequest request) {
        if (request == null) {
            request = new PracticeAnswerRequest();
        }
        log.info("API: checkPracticeAnswer(quizId={}, attemptId={}, questionId={})",
                quizId, attemptId, request.getQuestionId());
        return ApiResponse.<PracticeAnswerResponse>builder()
                .data(quizAttemptService.checkPracticeAnswer(quizId, attemptId, request))
                .message("Kiểm tra đáp án thành công")
                .build();
    }

    // ==================== 4. SUBMIT ATTEMPT ====================

    /**
     * Nộp bài (Submit Quiz).
     *
     * Mục đích:
     * - Chuyển status từ IN_PROGRESS → SUBMITTED
     * - Tự động chấm điểm (score)
     * - Cập nhật vào bảng điểm (grades table)
     *
     * SERVER-SIDE TIMER VALIDATION (CRITICAL):
     * - Backend KHÔNG tin tưởng thời gian nộp từ Frontend
     * - Validate: now() <= startedAt + duration
     * - Nếu hết giờ, tự động chuyển status → EXPIRED
     * - Chỉ chấm những câu đã auto-save
     *
     * Response: QuizResultResponse (kết quả chấm điểm)
     */
    @PostMapping(value = "/{quizId}/attempts/{attemptId}/submit")
    ApiResponse<QuizResultResponse> submitAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        log.info("API: submitAttempt(quizId={}, attemptId={})", quizId, attemptId);
        return ApiResponse.<QuizResultResponse>builder()
                .data(quizAttemptService.submitAttempt(quizId, attemptId, request))
                .message("Nộp bài thành công")
                .build();
    }

    // ==================== 5. GET QUIZ RESULT ====================

    /**
     * Xem kết quả sau khi nộp (Get Quiz Result).
     *
     * Mục đích:
     * - Trả về chi tiết điểm số
     * - So sánh đáp án học viên chọn vs đáp án đúng (correctAnswerIds)
     * - Hiển thị lời giải thích (explanation)
     *
     * SHOW RESULT POLICY:
     * - IMMEDIATELY / AFTER_SUBMIT: luôn cho xem ngay sau khi submit
     * - AFTER_DEADLINE: chỉ khi now() > quiz.endTime, nếu gọi sớm trả về 403 Forbidden
     *
     * Response: QuizResultResponse (có chứa field `correct` của answers)
     */
    @GetMapping(value = "/{quizId}/attempts/{attemptId}/result")
    ApiResponse<QuizResultResponse> getQuizResult(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId) {
        log.info("API: getQuizResult(quizId={}, attemptId={})", quizId, attemptId);
        return ApiResponse.<QuizResultResponse>builder()
                .data(quizAttemptService.getQuizResult(quizId, attemptId))
                .message("Lấy kết quả thành công")
                .build();
    }

    // ==================== 6. GET ATTEMPT HISTORY ====================

    /**
     * Xem lịch sử làm bài (Get Attempt History).
     *
     * Mục đích:
     * - Liệt kê các lần học viên đã thử làm quiz này
     * - Dùng cho quy định max_attempts
     * - Hiển thị: điểm số, thời gian bắt đầu/kết thúc, thời gian làm bài
     *
     * Pagination: Có thể specify page, size, sort
     * Default: page=0, size=20
     *
     * Response: Page<AttemptHistoryResponse>
     */
    @GetMapping(value = "/{quizId}/attempts")
    ApiResponse<Page<AttemptHistoryResponse>> getAttemptHistory(
            @PathVariable Integer quizId,
            @RequestParam(defaultValue = "") String userId,
            @PageableDefault(value = 20, size = 20) Pageable pageable) {
        log.info("API: getAttemptHistory(quizId={}, userId={}, page={})", quizId, userId, pageable.getPageNumber());
        // Nếu userId trống, lấy userId của request user
        if (userId == null || userId.isEmpty()) {
            userId = com.lms.assignmentservice.configuration.GatewayAuthentication.currentUserId();
        }
        return ApiResponse.<Page<AttemptHistoryResponse>>builder()
                .data(quizAttemptService.getAttemptHistory(quizId, userId, pageable))
                .message("Lấy lịch sử làm bài thành công")
                .build();
    }
}

