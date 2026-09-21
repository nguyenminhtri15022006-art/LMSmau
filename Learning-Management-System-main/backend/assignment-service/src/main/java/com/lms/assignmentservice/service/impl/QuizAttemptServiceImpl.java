package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.AutoSaveAttemptRequest;
import com.lms.assignmentservice.dto.request.PracticeAnswerRequest;
import com.lms.assignmentservice.dto.request.SubmitAttemptRequest;
import com.lms.assignmentservice.dto.response.AttemptHistoryResponse;
import com.lms.assignmentservice.dto.response.PracticeAnswerResponse;
import com.lms.assignmentservice.dto.response.QuizAttemptResponse;
import com.lms.assignmentservice.dto.response.QuizResultResponse;
import com.lms.assignmentservice.entity.*;
import com.lms.assignmentservice.enums.AttemptStatusType;
import com.lms.assignmentservice.enums.ShowResultType;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizAnswerRecordRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.QuizAttemptService;
import com.lms.assignmentservice.service.QuizGradingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý logic học viên làm bài (Quiz Attempts).
 * <p>
 * Các hàm chính:
 * - startAttempt(): Bắt đầu làm bài
 * - getInProgressAttempt(): Lấy bài đang làm (F5 recovery)
 * - autoSaveAttempt(): Auto-save câu trả lời (Idempotent bulk upsert)
 * - submitAttempt(): Nộp bài + tính điểm
 * - getQuizResult(): Xem kết quả (tuân theo show_result policy)
 * - getAttemptHistory(): Xem lịch sử làm bài
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private static final QuizAnswerRecord.AnswerIdsConverter ANSWER_IDS_CONVERTER =
            new QuizAnswerRecord.AnswerIdsConverter();

    QuizRepository quizRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuestionRepository questionRepository;
    QuizAnswerRecordRepository quizAnswerRecordRepository;
    AssignmentAuthorizationService authorizationService;
    QuizGradingService quizGradingService;
    PlatformTransactionManager transactionManager;

    // ==================== 1. START OR RESUME ATTEMPT ====================

    @Override
    public QuizAttemptResponse startOrResumeAttempt(Integer quizId) {
        String userId = authorizationService.currentUserId();
        TransactionTemplate transactionTemplate = newStartAttemptTransactionTemplate();

        try {
            return transactionTemplate.execute(status -> startOrResumeAttemptInTransaction(quizId, userId));
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate IN_PROGRESS blocked for user [{}] quiz [{}]", userId, quizId);
            return transactionTemplate.execute(status -> resumeAttemptAfterDuplicate(quizId, userId));
        }
    }

    private QuizAttemptResponse startOrResumeAttemptInTransaction(Integer quizId, String userId) {

        // Load Quiz (không cần lock — Unique Index là chốt chặn cứng dưới DB)
        Quiz quiz = findQuizById(quizId);

        // Chặn quiz đã bị xóa mềm
        if (quiz.getDeleted()) {
            throw new AssignmentException(ErrorCode.QUIZ_IS_DELETED);
        }

        // Kiểm tra Quiz đã publish
        if (!quiz.getPublished()) {
            throw new AssignmentException(ErrorCode.QUIZ_NOT_PUBLISHED);
        }

        // Kiểm tra thời gian mở/đóng
        if (!quiz.isTimeWindowOpen()) {
            throw new AssignmentException(ErrorCode.QUIZ_NOT_AVAILABLE);
        }

        // Kiểm tra IN_PROGRESS hiện có (resume flow)
        var existing = quizAttemptRepository.findByQuiz_QuizIdAndUserIdAndStatus(
                quizId, userId, AttemptStatusType.IN_PROGRESS);

        if (existing.isPresent()) {
            QuizAttempt attempt = existing.get();
            return resumeAttempt(quizId, userId, attempt);
        }

        // Kiểm tra max_attempts chỉ khi cần tạo attempt mới
        long attemptCount = quizAttemptRepository.countByUserIdAndQuiz_QuizId(userId, quizId);
        if (quiz.getMaxAttempts() > 0 && attemptCount >= quiz.getMaxAttempts()) {
            throw new AssignmentException(ErrorCode.MAX_ATTEMPTS_REACHED);
        }

        // Tạo attempt mới
        byte attemptNumber = (byte) (attemptCount + 1);
        Instant now = Instant.now();
        Instant expiresAt = null;
        if (quiz.getDuration() != null && quiz.getDuration() > 0) {
            expiresAt = now.plus(quiz.getDuration(), ChronoUnit.MINUTES);
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .userId(userId)
                .attemptNumber(attemptNumber)
                .status(AttemptStatusType.IN_PROGRESS)
                .expiresAt(expiresAt)
                .totalScore(new BigDecimal(quiz.getTotalScore()))
                .build();

        attempt = quizAttemptRepository.save(attempt);
        log.info("User [{}] start Quiz [{}], attempt #{}", userId, quizId, attemptNumber);
        return buildQuizAttemptResponse(attempt, true);
    }

    private QuizAttemptResponse resumeAttemptAfterDuplicate(Integer quizId, String userId) {
        QuizAttempt existingAttempt = quizAttemptRepository
                .findByQuiz_QuizIdAndUserIdAndStatus(quizId, userId, AttemptStatusType.IN_PROGRESS)
                .orElseThrow(() -> new AssignmentException(ErrorCode.ATTEMPT_PROCESSING));

        return resumeAttempt(quizId, userId, existingAttempt);
    }

    private QuizAttemptResponse resumeAttempt(Integer quizId, String userId, QuizAttempt attempt) {
        if (attempt.isExpired()) {
            throw new AssignmentException(ErrorCode.ATTEMPT_EXPIRED);
        }

        log.info("User [{}] resume Quiz [{}], attempt #{}", userId, quizId, attempt.getAttemptNumber());
        return buildQuizAttemptResponse(attempt, true);
    }

    private TransactionTemplate newStartAttemptTransactionTemplate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }

    // ==================== 2. GET IN-PROGRESS ATTEMPT ====================

    @Override
    public QuizAttemptResponse getInProgressAttempt(Integer quizId, Long attemptId) {
        String userId = authorizationService.currentUserId();
        QuizAttempt attempt = findAttemptById(attemptId);

        // Kiểm tra ownership
        if (!attempt.getUserId().equals(userId)) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        if (!attempt.getQuiz().getQuizId().equals(quizId)) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // Chỉ cho phép xem nếu attempt đang IN_PROGRESS
        if (!AttemptStatusType.IN_PROGRESS.equals(attempt.getStatus())) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // Kiểm tra hết giờ chưa
        if (attempt.isExpired()) {
            attempt.setStatus(AttemptStatusType.EXPIRED);
            quizAttemptRepository.save(attempt);
            throw new AssignmentException(ErrorCode.ATTEMPT_EXPIRED);
        }

        return buildQuizAttemptResponse(attempt, true);
    }

    // ==================== 3. AUTO-SAVE ATTEMPT (BULK UPSERT) ====================

    @Transactional
    @Override
    public void autoSaveAttempt(Integer quizId, Long attemptId, AutoSaveAttemptRequest request) {
        String userId = authorizationService.currentUserId();
        QuizAttempt attempt = findAttemptById(attemptId);

        // 1. Kiểm tra ownership + status
        validateAttemptOwnerAndQuiz(attempt, userId, quizId);

        if (AttemptStatusType.SUBMITTED.equals(attempt.getStatus())
                || AttemptStatusType.EXPIRED.equals(attempt.getStatus())) {
            return;
        }

        if (!AttemptStatusType.IN_PROGRESS.equals(attempt.getStatus())) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // 2. Kiểm tra hết giờ chưa
        if (attempt.isExpired()) {
            throw new AssignmentException(ErrorCode.ATTEMPT_EXPIRED);
        }

        // === TỐI ƯU IDEMPOTENCY: THE UPSERT PATTERN ===

        // Bước A: Gom nhóm Request gửi lên để chặn lỗi gửi trùng ID trong cùng 1 cục JSON
        Map<Integer, List<Integer>> payloadMap = request.getAnswers().stream()
                .collect(Collectors.toMap(
                        AutoSaveAttemptRequest.AnswerItem::getQuestionId,
                        item -> item.getSelectedAnswerIds() != null ? item.getSelectedAnswerIds() : List.of(),
                        (oldVal, newVal) -> newVal // Nếu request trùng questionId, lấy cái sau cùng
                ));

        // Native upsert để auto-save/submit/cleanup chạy song song không vấp uq_record.
        int upsertedRecords = 0;

        for (Map.Entry<Integer, List<Integer>> entry : payloadMap.entrySet()) {
            upsertedRecords += quizAnswerRecordRepository.upsertRecord(
                    attemptId,
                    entry.getKey(),
                    ANSWER_IDS_CONVERTER.convertToDatabaseColumn(entry.getValue()),
                    BigDecimal.ZERO);
        }

        if (upsertedRecords > 0) {
            log.info("Auto-save thành công {} câu trả lời cho attempt [{}]", payloadMap.size(), attemptId);
        }
    }

    // ==================== 4. PRACTICE CHECK ====================

    @Transactional
    @Override
    public PracticeAnswerResponse checkPracticeAnswer(Integer quizId, Long attemptId, PracticeAnswerRequest request) {
        String userId = authorizationService.currentUserId();
        QuizAttempt attempt = findAttemptById(attemptId);
        validateAttemptAccess(attempt, userId, quizId);

        Quiz quiz = attempt.getQuiz();
        if (!ShowResultType.IMMEDIATELY.equals(quiz.getShowResult())) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        if (attempt.isExpired()) {
            throw new AssignmentException(ErrorCode.ATTEMPT_EXPIRED);
        }

        List<Integer> selectedIds = request.getSelectedAnswerIds() != null
                ? request.getSelectedAnswerIds()
                : List.of();

        Question question = questionRepository
                .findByQuizIdAndQuestionIdWithAnswers(quizId, request.getQuestionId())
                .orElseThrow(() -> {
                    log.warn("FRAUD DETECTED: Question [{}] not in Quiz [{}] from user [{}]",
                            request.getQuestionId(), quizId, userId);
                    return new AssignmentException(ErrorCode.QUESTION_NOT_IN_ATTEMPT_QUIZ);
                });

        List<Integer> validAnswerIds = question.getAnswers().stream()
                .map(Answer::getAnswerId)
                .toList();

        for (Integer selectedAnswerId : selectedIds) {
            if (!validAnswerIds.contains(selectedAnswerId)) {
                log.warn("FRAUD DETECTED: AnswerId [{}] not in Question [{}] from user [{}]",
                        selectedAnswerId, request.getQuestionId(), userId);
                throw new AssignmentException(ErrorCode.ANSWER_NOT_IN_QUESTION);
            }
        }

        List<Integer> correctAnswerIds = question.getAnswers().stream()
                .filter(Answer::getCorrect)
                .map(Answer::getAnswerId)
                .toList();

        Set<Integer> selectedAnswerIdSet = new HashSet<>(selectedIds);
        boolean isCorrect = !selectedAnswerIdSet.isEmpty()
                && selectedAnswerIdSet.size() == selectedIds.size()
                && selectedAnswerIdSet.equals(new HashSet<>(correctAnswerIds));

        Optional<QuizAnswerRecord> existingRecord =
                quizAnswerRecordRepository.findByAttemptAndQuestion(attemptId, request.getQuestionId());
        boolean firstAnswerRecorded = existingRecord.isEmpty();
        if (firstAnswerRecorded) {
            quizAnswerRecordRepository.upsertRecord(
                    attemptId,
                    request.getQuestionId(),
                    ANSWER_IDS_CONVERTER.convertToDatabaseColumn(selectedIds),
                    BigDecimal.ZERO);
        }

        return PracticeAnswerResponse.builder()
                .questionId(request.getQuestionId())
                .correct(isCorrect)
                .firstAnswerRecorded(firstAnswerRecorded)
                .explanation(isCorrect ? question.getExplanation() : null)
                .build();
    }

    // ==================== 5. SUBMIT ATTEMPT ====================

    @Transactional
    @Override
    public QuizResultResponse submitAttempt(Integer quizId, Long attemptId, SubmitAttemptRequest request) {
        String userId = authorizationService.currentUserId();
        QuizAttempt attempt = findAttemptById(attemptId);

        // Kiểm tra ownership + status
        validateAttemptOwnerAndQuiz(attempt, userId, quizId);

        if (AttemptStatusType.SUBMITTED.equals(attempt.getStatus())
                || AttemptStatusType.EXPIRED.equals(attempt.getStatus())) {
            return buildQuizResultResponseForPolicy(attempt);
        }

        if (!AttemptStatusType.IN_PROGRESS.equals(attempt.getStatus())) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // === SERVER-SIDE TIMER VALIDATION ===
        // Không ném Exception khi quá hạn. Nếu nộp muộn (> grace period), vẫn chấm điểm
        // nhưng đánh dấu EXPIRED thay vì SUBMITTED.
        boolean isLate = attempt.isExpired();

        Quiz quiz = attempt.getQuiz();

        // JOIN FETCH: Lấy Question + Answers để validate (QuizGradingService tự load lại khi chấm)
        List<Question> allQuestions = questionRepository.findAllByQuizIdWithAnswers(quizId);

        // Validate và convert Request của học viên thành Map để dễ tra cứu (O(1) lookup)
        //  Truyền allQuestions vào để tránh N+1 query trong validation
        validateAndSanitizeAnswers(quiz, allQuestions, request); // Validate client không gian lận
        Map<Integer, List<Integer>> payloadMap = ShowResultType.IMMEDIATELY.equals(quiz.getShowResult())
                ? null
                : buildPayloadMap(request);

        // === GỌI QUIZ GRADING SERVICE (DRY - dùng chung logic chấm điểm) ===
        QuizGradingService.GradingResult result = quizGradingService.gradeAttempt(attempt, payloadMap);

        // === CẬP NHẬT ATTEMPT & LƯU ===
        if (isLate) {
            attempt.setStatus(AttemptStatusType.EXPIRED);
            log.info("User [{}] nộp Quiz [{}] muộn (grace period exceeded), đánh dấu EXPIRED", userId, quizId);
        } else {
            attempt.setStatus(AttemptStatusType.SUBMITTED);
        }
        attempt.setSubmittedAt(Instant.now());
        attempt.setScore(result.totalScore().floatValue());
        attempt.setPassed(result.passed());

        // Tính thời gian làm bài
        long seconds = ChronoUnit.SECONDS.between(attempt.getStartedAt(), attempt.getSubmittedAt());
        attempt.setTimeSpentS((int) seconds);

        attempt = quizAttemptRepository.save(attempt);
        log.info("User [{}] nộp Quiz [{}], điểm: {}/{}, passed: {}, timeSpent: {}s, status: {}",
                userId, quizId, result.totalScore(), attempt.getTotalScore(), attempt.getPassed(),
                attempt.getTimeSpentS(), attempt.getStatus());

        return buildQuizResultResponseForPolicy(attempt);
    }

    /**
     * Validate answers từ client request.
     * <p>
     * KHÔNG TIN CLIENT
     * <p>
     * Phòng hờ:
     * 1. Client tiêm câu hỏi từ đề khác vào
     * 2. Client gửi bừa ID đáp án không liên quan
     * 3. Client thay đổi request sau khi FE validate
     * <p>
     * OPT: Nhận allQuestions để tra cứu trên RAM thay vì query DB (tránh N+1)
     *
     * @return Danh sách questionId đã validate
     */
    private List<Integer> validateAndSanitizeAnswers(Quiz quiz, List<Question> allQuestions, SubmitAttemptRequest request) {
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            // Học viên nộp bài trắng - hợp lệ
            return new ArrayList<>();
        }

        //  Ép list allQuestions thành Map để tra cứu O(1)
        Map<Integer, Question> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        List<Integer> quizQuestionIds = quiz.getQuizQuestions().stream()
                .map(qq -> qq.getQuestion().getQuestionId())
                .toList();

        List<Integer> validatedQuestionIds = new ArrayList<>();

        for (SubmitAttemptRequest.AnswerItem item : request.getAnswers()) {
            Integer questionId = item.getQuestionId();

            // Kiểm tra 1: Câu hỏi này có nằm trong bài thi không?
            if (!quizQuestionIds.contains(questionId)) {
                log.warn("FRAUD DETECTED: Question [{}] not in Quiz [{}] from user [{}]",
                        questionId, quiz.getQuizId(), authorizationService.currentUserId());
                throw new AssignmentException(ErrorCode.QUESTION_NOT_IN_ATTEMPT_QUIZ);
            }

            //  TỐI ƯU: Không query DB, lấy từ Map trên RAM
            Question question = questionMap.get(questionId);
            if (question == null) {
                log.warn("Question [{}] không tồn tại trong quiz", questionId);
                throw new AssignmentException(ErrorCode.QUESTION_NOT_FOUND);
            }

            // Kiểm tra 2: Các answerId có thực sự thuộc về question này không?
            List<Integer> validAnswerIds = question.getAnswers().stream()
                    .map(Answer::getAnswerId)
                    .toList();

            if (item.getSelectedAnswerIds() != null) {
                for (Integer selectedAnswerId : item.getSelectedAnswerIds()) {
                    if (!validAnswerIds.contains(selectedAnswerId)) {
                        log.warn("FRAUD DETECTED: AnswerId [{}] not in Question [{}] from user [{}]",
                                selectedAnswerId, questionId, authorizationService.currentUserId());
                        throw new AssignmentException(ErrorCode.ANSWER_NOT_IN_QUESTION);
                    }
                }
            }

            validatedQuestionIds.add(questionId);
        }

        return validatedQuestionIds;
    }

    /**
     * Helper: Convert SubmitAttemptRequest thành Map<QuestionId, SelectedAnswerIds>.
     * Dùng để O(1) lookup trong vòng lặp chấm điểm.
     */
    private Map<Integer, List<Integer>> buildPayloadMap(SubmitAttemptRequest request) {
        if (request == null || request.getAnswers() == null) {
            return new HashMap<>();
        }

        return request.getAnswers().stream()
                .collect(Collectors.toMap(
                        SubmitAttemptRequest.AnswerItem::getQuestionId,
                        item -> item.getSelectedAnswerIds() != null
                                ? item.getSelectedAnswerIds()
                                : new ArrayList<>(),
                        (oldVal, newVal) -> newVal  // Nếu request trùng questionId, lấy cái sau cùng
                ));
    }

    // ==================== 5. GET QUIZ RESULT ====================

    @Override
    public QuizResultResponse getQuizResult(Integer quizId, Long attemptId) {
        String userId = authorizationService.currentUserId();
        QuizAttempt attempt = findAttemptById(attemptId);

        // Kiểm tra ownership
        if (!attempt.getUserId().equals(userId)) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        if (!attempt.getQuiz().getQuizId().equals(quizId)) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // Cho xem kết quả nếu đã SUBMITTED hoặc EXPIRED (nộp muộn)
        if (!AttemptStatusType.SUBMITTED.equals(attempt.getStatus())
                && !AttemptStatusType.EXPIRED.equals(attempt.getStatus())) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }

        // === CHECK SHOW RESULT POLICY ===
        Quiz quiz = attempt.getQuiz();
        if (ShowResultType.AFTER_DEADLINE.equals(quiz.getShowResult())) {
            if (quiz.getEndTime() != null && Instant.now().isBefore(quiz.getEndTime())) {
                // Chưa đến deadline, không cho xem
                throw new AssignmentException(ErrorCode.SHOW_RESULT_NOT_ALLOWED);
            }
        }
        // IMMEDIATELY, AFTER_SUBMIT: luôn cho xem

        return buildQuizResultResponse(attempt);
    }

    // ==================== 6. GET ATTEMPT HISTORY ====================

    @Override
    public Page<AttemptHistoryResponse> getAttemptHistory(Integer quizId, String userId, Pageable pageable) {
        authorizationService.checkCurrentUserOrAdminOrInstructor(userId);

        // Lấy page attempts
        Page<QuizAttempt> attemptPage = quizAttemptRepository
                .findByUserIdAndQuiz_QuizIdOrderByStartedAtDesc(userId, quizId, pageable);

        // Convert to response
        List<AttemptHistoryResponse> responses = attemptPage.getContent().stream()
                .map(this::buildAttemptHistoryResponse)
                .toList();

        return new PageImpl<>(responses, pageable, attemptPage.getTotalElements());
    }

    // ==================== HELPER METHODS ====================

    private Quiz findQuizById(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private QuizAttempt findAttemptById(Long attemptId) {
        return quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND));
    }

    private void validateAttemptOwnerAndQuiz(QuizAttempt attempt, String userId, Integer quizId) {
        if (!attempt.getUserId().equals(userId)) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }
        if (!attempt.getQuiz().getQuizId().equals(quizId)) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
    }

    private void validateAttemptAccess(QuizAttempt attempt, String userId, Integer quizId) {
        validateAttemptOwnerAndQuiz(attempt, userId, quizId);
        if (!AttemptStatusType.IN_PROGRESS.equals(attempt.getStatus())) {
            throw new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
    }

    private List<QuizQuestion> orderedQuizQuestionsForAttempt(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        List<QuizQuestion> quizQuestions = quiz.getQuizQuestions().stream()
                .sorted((a, b) -> Short.compare(a.getOrderIndex(), b.getOrderIndex()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (Boolean.TRUE.equals(quiz.getShuffleQuestions())) {
            Collections.shuffle(
                    quizQuestions,
                    new Random(shuffleSeed(attempt.getAttemptId(), quiz.getQuizId(), 17)));
        }

        return quizQuestions;
    }

    private List<Answer> orderedAnswersForAttempt(QuizAttempt attempt, Question question) {
        List<Answer> answers = question.getAnswers().stream()
                .sorted(Comparator.comparingInt(Answer::getOrderIndex))
                .collect(Collectors.toCollection(ArrayList::new));

        if (Boolean.TRUE.equals(attempt.getQuiz().getShuffleAnswers())) {
            Collections.shuffle(
                    answers,
                    new Random(shuffleSeed(attempt.getAttemptId(), question.getQuestionId(), 31)));
        }

        return answers;
    }

    private long shuffleSeed(Long attemptId, Integer scopeId, int salt) {
        long seed = attemptId != null ? attemptId : 0L;
        seed = seed * 31 + (scopeId != null ? scopeId : 0);
        seed = seed * 31 + salt;
        return seed;
    }

    // ==================== DTO BUILDERS ====================

    /**
     * Build QuizAttemptResponse cho việc bắt đầu hoặc lấy lại in-progress attempt.
     * IMPORTANT: Không include trường `correct` của answers để tránh gian lận.
     */
    private QuizAttemptResponse buildQuizAttemptResponse(QuizAttempt attempt, boolean includeAnswerRecords) {
        Quiz quiz = attempt.getQuiz();

        var quizQuestions = orderedQuizQuestionsForAttempt(attempt);

        // Lấy các record đã save nếu cần
        final Map<Integer, List<Integer>> savedAnswers;
        if (includeAnswerRecords) {
            List<QuizAnswerRecord> records = quizAnswerRecordRepository.findByQuizAttempt_AttemptId(attempt.getAttemptId());
            savedAnswers = records.stream()
                    .collect(Collectors.toMap(
                            r -> r.getQuestion().getQuestionId(),
                            QuizAnswerRecord::getSelectedAnswerIds
                    ));
        } else {
            savedAnswers = new java.util.HashMap<>();
        }

        // Build questions list
        List<QuizAttemptResponse.AttemptQuestionItem> questionItems = quizQuestions.stream()
                .map(qq -> {
                    Question question = qq.getQuestion();
                    final Integer questionId = question.getQuestionId();
                    return QuizAttemptResponse.AttemptQuestionItem.builder()
                            .questionId(questionId)
                            .content(question.getContent())
                            .explanation(question.getExplanation())
                            .topic(question.getTopic())
                            .imageUrl(question.getImageUrl())
                            .type(question.getType().toString())
                            .score(qq.getEffectiveScore())
                            // Build answers WITHOUT `correct` field
                            .answers(orderedAnswersForAttempt(attempt, question).stream()
                                    .map(answer -> QuizAttemptResponse.AttemptAnswerItem.builder()
                                            .answerId(answer.getAnswerId())
                                            .content(answer.getContent())
                                            .orderIndex(answer.getOrderIndex())
                                            .build()
                                    )
                                    .toList()
                            )
                            .selectedAnswerIds(savedAnswers.getOrDefault(questionId, List.of()))
                            .build();
                })
                .toList();

        return QuizAttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .duration(quiz.getDuration())
                .totalScore(attempt.getTotalScore().shortValue())
                .startedAt(attempt.getStartedAt())
                .expiresAt(attempt.getExpiresAt())
                .serverTime(Instant.now())
                .status(attempt.getStatus().toString())
                .showResult(quiz.getShowResult().name())
                .questions(questionItems)
                .build();
    }

    private QuizResultResponse buildQuizResultResponseForPolicy(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        if (ShowResultType.AFTER_DEADLINE.equals(quiz.getShowResult()) && !isResultAvailable(quiz)) {
            return buildQuizResultSummary(attempt, false);
        }
        return buildQuizResultResponse(attempt);
    }

    private boolean isResultAvailable(Quiz quiz) {
        return quiz.getEndTime() == null || !Instant.now().isBefore(quiz.getEndTime());
    }

    private QuizResultResponse buildQuizResultSummary(QuizAttempt attempt, boolean resultAvailable) {
        Quiz quiz = attempt.getQuiz();
        return QuizResultResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .score(new BigDecimal(attempt.getScore()))
                .totalScore(attempt.getTotalScore().shortValue())
                .passed(attempt.getPassed())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeSpentSeconds(attempt.getTimeSpentS())
                .showResult(quiz.getShowResult().name())
                .resultAvailable(resultAvailable)
                .resultAvailableAt(quiz.getEndTime())
                .totalQuestions(quiz.getQuizQuestions().size())
                .build();
    }

    /**
     * Build QuizResultResponse sau khi submit.
     * Lần này CÓ include trường `correct` của answers.
     * <p>
     * IMPORTANT: Lặp qua quiz.getQuizQuestions() (SOURCE OF TRUTH) chứ KHÔNG lặp qua records!
     * Nếu học viên bỏ câu nào trắng, nó vẫn phải hiển thị với score=0
     */
    private QuizResultResponse buildQuizResultResponse(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        List<QuizAnswerRecord> records = quizAnswerRecordRepository.findByQuizAttempt_AttemptId(attempt.getAttemptId());

        // 1. Convert records thành Map để tra cứu cực nhanh
        Map<Integer, QuizAnswerRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        r -> r.getQuestion().getQuestionId(),
                        r -> r
                ));

        List<QuizResultResponse.ResultQuestionItem> resultItems = new ArrayList<>();

        var quizQuestions = orderedQuizQuestionsForAttempt(attempt);
        BigDecimal quizTotalScore = attempt.getTotalScore();
        BigDecimal rawTotalScore = quizQuestions.stream()
                .map(qq -> BigDecimal.valueOf(qq.getEffectiveScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (QuizQuestion qq : quizQuestions) {
            Question question = qq.getQuestion();
            QuizAnswerRecord matchedRecord = recordMap.get(question.getQuestionId());

            // Lấy đáp án đã nộp (nếu không có thì trả mảng rỗng)
            List<Integer> selectedIds = (matchedRecord != null && matchedRecord.getSelectedAnswerIds() != null)
                    ? matchedRecord.getSelectedAnswerIds() : new ArrayList<>();

            BigDecimal earnedScore = matchedRecord != null ? matchedRecord.getEarnedScore() : BigDecimal.ZERO;
            BigDecimal normalizedQuestionScore = normalizeQuestionScore(
                    BigDecimal.valueOf(qq.getEffectiveScore()),
                    rawTotalScore,
                    quizTotalScore);

            resultItems.add(QuizResultResponse.ResultQuestionItem.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .explanation(question.getExplanation())
                    .type(question.getType().toString())
                    .score(normalizedQuestionScore.setScale(0, RoundingMode.HALF_UP).shortValue())
                    .earnedScore(earnedScore)
                    .selectedAnswerIds(selectedIds)
                    // Build answers WITH `correct` field
                    .answers(orderedAnswersForAttempt(attempt, question).stream()
                            .map(answer -> QuizResultResponse.ResultAnswerItem.builder()
                                    .answerId(answer.getAnswerId())
                                    .content(answer.getContent())
                                    .correct(answer.getCorrect())
                                    .orderIndex(answer.getOrderIndex())
                                    .build()
                            )
                            .toList()
                    )
                    .build());
        }

        return QuizResultResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .score(new BigDecimal(attempt.getScore()))
                .totalScore(attempt.getTotalScore().shortValue())
                .passed(attempt.getPassed())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeSpentSeconds(attempt.getTimeSpentS())
                .showResult(quiz.getShowResult().name())
                .resultAvailable(true)
                .resultAvailableAt(quiz.getEndTime())
                .totalQuestions(resultItems.size())
                .questions(resultItems)
                .build();
    }

    private BigDecimal normalizeQuestionScore(
            BigDecimal questionScore,
            BigDecimal rawTotalScore,
            BigDecimal quizTotalScore) {
        if (rawTotalScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return quizTotalScore
                .multiply(questionScore)
                .divide(rawTotalScore, 4, RoundingMode.HALF_UP);
    }

    /**
     * Build AttemptHistoryResponse từ QuizAttempt entity.
     */
    private AttemptHistoryResponse buildAttemptHistoryResponse(QuizAttempt attempt) {
        return AttemptHistoryResponse.builder()
                .attemptId(attempt.getAttemptId())
                .attemptNumber(attempt.getAttemptNumber())
                .score(new BigDecimal(attempt.getScore()))
                .totalScore(attempt.getTotalScore().shortValue())
                .passed(attempt.getPassed())
                .status(attempt.getStatus().toString())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeSpentSeconds(attempt.getTimeSpentS())
                .build();
    }
}
