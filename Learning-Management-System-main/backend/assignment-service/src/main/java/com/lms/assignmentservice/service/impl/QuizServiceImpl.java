package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.AddBulkQuestionQuizRequest;
import com.lms.assignmentservice.dto.request.CreateImportedQuizRequest;
import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.dto.request.CreateQuizRequest;
import com.lms.assignmentservice.dto.request.SaveQuizDraftRequest;
import com.lms.assignmentservice.dto.response.*;
import com.lms.assignmentservice.entity.Answer;
import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.entity.Quiz;
import com.lms.assignmentservice.entity.QuizQuestion;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.QuizMapper;
import com.lms.assignmentservice.repository.QuestionRepository;
import com.lms.assignmentservice.repository.QuizAttemptRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import com.lms.assignmentservice.service.QuizService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizServiceImpl implements QuizService {

    static final Validator REQUEST_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    QuizRepository quizRepository;
    QuestionRepository questionRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizMapper quizMapper;
    AssignmentAuthorizationService authorizationService;
    CourseResourceAuthorizationService courseResourceAuthorizationService;

    // ==================== CRUD ====================

    @Transactional
    @Override
    public QuizDetailResponse createQuiz(String courseId, CreateQuizRequest request) {
        courseResourceAuthorizationService.checkCourseNotLocked(courseId);
        String userId = authorizationService.currentUserId();

        Quiz quiz = quizMapper.toQuiz(request);
        quiz.setCreatedBy(userId);
        quiz.setCourseId(courseId);

        quiz = quizRepository.save(quiz);

        log.info("Tạo quiz [{}] course=[{}] bởi user [{}]", quiz.getQuizId(), quiz.getCourseId(), userId);
        return quizMapper.toQuizResponse(quiz);
    }

    @Transactional
    @Override
    public QuizDetailResponse createQuizFromImportedQuestions(String courseId, CreateImportedQuizRequest request) {
        courseResourceAuthorizationService.checkCourseNotLocked(courseId);
        validateImportedQuizRequest(request);
        request.getQuestions().stream()
                .filter(questionRequest -> !isReuseOnlyRequest(questionRequest))
                .forEach(this::validateQuestionRequest);

        String userId = authorizationService.currentUserId();
        Quiz quiz = quizMapper.toQuiz(request.getQuiz());
        quiz.setCreatedBy(userId);
        quiz.setCourseId(courseId);
        quiz = quizRepository.save(quiz);

        List<QuizQuestion> quizQuestions = new ArrayList<>();
        for (int index = 0; index < request.getQuestions().size(); index++) {
            SaveQuizDraftRequest.QuestionDraftRequest questionRequest = request.getQuestions().get(index);
            Question question = upsertQuestionForQuiz(courseId, questionRequest, false);
            quizQuestions.add(QuizQuestion.builder()
                    .quiz(quiz)
                    .question(question)
                    .overrideScore(question.getScore() != null ? question.getScore().shortValue() : null)
                    .orderIndex((short) index)
                    .build());
        }

        if (quiz.getQuizQuestions() == null) {
            quiz.setQuizQuestions(new ArrayList<>());
        }
        quiz.getQuizQuestions().addAll(quizQuestions);
        if (request.isPublish()) {
            quiz.setPublished(true);
        }

        quiz = quizRepository.save(quiz);
        log.info("Tao quiz [{}] tu import: {} questions, publish={} course=[{}] user=[{}]",
                quiz.getQuizId(), request.getQuestions().size(), request.isPublish(), courseId, userId);
        return quizMapper.toQuizResponse(quiz);
    }

    @Override
    public CachedPage<QuizDetailResponse> getQuizzesByCourse(String courseId, String lessonId, Pageable pageable) {
        boolean isPrivileged = authorizationService.isAdminOrInstructor();

        var page = isPrivileged
                ? (hasText(lessonId)
                    ? quizRepository.findByCourseIdAndLessonId(courseId, lessonId, pageable)
                    : quizRepository.findByCourseId(courseId, pageable)).map(quizMapper::toQuizResponse)
                : (hasText(lessonId)
                    ? quizRepository.findByCourseIdAndLessonIdAndPublishedTrueAndDeletedFalse(courseId, lessonId, pageable)
                    : quizRepository.findByCourseIdAndPublishedTrueAndDeletedFalse(courseId, pageable)).map(quizMapper::toQuizResponse);

        return CachedPage.<QuizDetailResponse>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .build();
    }

    @Override
    public CachedPage<QuizInstructorDetailResponse> getQuizzesForInstructor(String courseId, String lessonId, Pageable pageable) {
        var page = (hasText(lessonId)
                ? quizRepository.findByCourseIdAndLessonId(courseId, lessonId, pageable)
                : quizRepository.findByCourseId(courseId, pageable))
                .map(quizMapper::toInstructorResponse);
        List<QuizInstructorDetailResponse> content = page.getContent();
        populateAttemptCounts(content);

        return CachedPage.<QuizInstructorDetailResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .build();
    }

    @Override
    public CachedPage<QuizStudentDetailResponse> getQuizzesForStudent(String courseId, String lessonId, Pageable pageable) {
        var page = (hasText(lessonId)
                ? quizRepository.findByCourseIdAndLessonIdAndPublishedTrueAndDeletedFalse(courseId, lessonId, pageable)
                : quizRepository.findByCourseIdAndPublishedTrueAndDeletedFalse(courseId, pageable))
                .map(quizMapper::toStudentResponse);
        List<QuizStudentDetailResponse> content = page.getContent();
        markCompletedQuizzes(courseId, content);

        return CachedPage.<QuizStudentDetailResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .build();
    }

    @Override
    public QuizDetailResponse getQuiz(Integer quizId) {
        return quizMapper.toQuizResponse(findQuizById(quizId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<QuestionDetailResponse> getQuizQuestions(Integer quizId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        Map<Integer, Short> orderByQuestionId = quiz.getQuizQuestions().stream()
                .collect(Collectors.toMap(
                        quizQuestion -> quizQuestion.getQuestion().getQuestionId(),
                        QuizQuestion::getOrderIndex,
                        (first, second) -> first
                ));

        return questionRepository.findAllByQuizIdWithAnswers(quizId).stream()
                .sorted(Comparator.comparingInt(left -> orderByQuestionId.getOrDefault(left.getQuestionId(), Short.MAX_VALUE)))
                .map(quizMapper::toQuestionResponse)
                .toList();
    }

    @Transactional
    @Override
    public QuizDetailResponse updateQuiz(Integer quizId, CreateQuizRequest request) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        // Validation: Check nếu quiz published và có attempt thì không cho sửa
        validateQuizEditable(quiz);

        quizMapper.updateQuiz(request, quiz);

        return quizMapper.toQuizResponse(quizRepository.save(quiz));
    }

    @Transactional
    @Override
    public QuizDetailResponse syncQuiz(Integer quizId, SaveQuizDraftRequest request) {
        validateSaveQuizDraftRequest(request);

        Quiz sourceQuiz = findQuizById(quizId);
        checkQuizManager(sourceQuiz);
        checkQuizNotDeleted(sourceQuiz);

        boolean locked = Boolean.TRUE.equals(sourceQuiz.getPublished())
                || quizAttemptRepository.existsByQuiz_QuizId(quizId);
        Quiz targetQuiz = locked ? createEditableClone(sourceQuiz) : sourceQuiz;
        quizMapper.updateQuiz(request.getQuiz(), targetQuiz);
        replaceQuizQuestions(targetQuiz, request.getQuestions(), locked);
        targetQuiz.setPublished(locked ? false : request.isPublish());

        Quiz savedQuiz = quizRepository.save(targetQuiz);
        log.info("Synced quiz source=[{}] target=[{}] questions={} cloned={} publish={}",
                quizId, savedQuiz.getQuizId(), request.getQuestions().size(), locked, savedQuiz.getPublished());
        return quizMapper.toQuizResponse(savedQuiz);
    }

    @Transactional
    @Override
    public void deleteQuiz(Integer quizId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);

        if (quizAttemptRepository.existsByQuiz_QuizId(quizId)) {
            // Soft-delete: quiz đã có lượt làm, giữ nguyên dữ liệu
            quiz.setDeleted(true);
            quiz.setDeletedAt(java.time.Instant.now());
            quiz.setDeletedBy(authorizationService.currentUserId());
            quizRepository.save(quiz);
            log.info("Soft-delete quiz [{}] bởi user [{}]", quizId, authorizationService.currentUserId());
            return;
        }

        // Hard-delete: chưa có lượt làm
        quizRepository.delete(quiz);
        log.info("Hard-delete quiz [{}]", quizId);
    }

    @Transactional
    @Override
    public void restoreQuiz(Integer quizId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);

        if (!quiz.getDeleted()) {
            throw new AssignmentException(ErrorCode.QUIZ_NOT_FOUND);
        }

        quiz.setDeleted(false);
        quiz.setDeletedAt(null);
        quiz.setDeletedBy(null);
        quizRepository.save(quiz);
        log.info("Restore quiz [{}] bởi user [{}]", quizId, authorizationService.currentUserId());
    }

    @Transactional
    @Override
    public void publishQuiz(Integer quizId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        if (quiz.getPublished()) {
            throw new AssignmentException(ErrorCode.QUIZ_ALREADY_PUBLISHED);
        }

        if (quiz.getQuizQuestions() == null || quiz.getQuizQuestions().isEmpty()) {
            log.warn("Cố gắng publish quiz rỗng: quizId={}", quizId);
            throw new AssignmentException(ErrorCode.QUIZ_EMPTY_CANNOT_PUBLISH);
        }

        quiz.setPublished(true);

        quizRepository.save(quiz);
        log.info("Quiz [{}] đã được xuất bản thành công", quizId);
    }

    @Transactional
    @Override
    public void unpublishQuiz(Integer quizId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        if (!quiz.getPublished()) return;

        boolean hasAttempts = quizAttemptRepository.existsByQuiz_QuizId(quizId);
        if (hasAttempts) {
            log.warn("Lỗi: Cố gắng Unpublish quiz đã có người thi (quizId={})", quizId);
            throw new AssignmentException(ErrorCode.QUIZ_HAS_ATTEMPTS_CANNOT_UNPUBLISH);
        }

        quiz.setPublished(false);
        quizRepository.save(quiz);
        log.info("Quiz [{}] đã được ngưng xuất bản (Unpublished)", quizId);
    }

    // ==================== Quản lý câu hỏi trong quiz ====================

    @Transactional
    @Override
    public void bulkAddQuestionToQuiz(Integer quizId, AddBulkQuestionQuizRequest request) {
        // validate quiz
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        // Validation: Check nếu quiz published và có attempt thì không cho thêm câu
        validateQuizEditable(quiz);

        List<AddBulkQuestionQuizRequest.AddQuestionToQuizRequest> items = request.getQuestionItems();

        // lay danh sach questionId tu request
        List<Integer> requestQuestionIds = items.stream()
                .map(AddBulkQuestionQuizRequest.AddQuestionToQuizRequest::getQuestionId)
                .toList();

        // lay danh sach Question tu danh sach id
        List<Question> questionList = questionRepository.findAllById(requestQuestionIds);

        if (questionList.size() != requestQuestionIds.size()) {
            throw new AssignmentException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // Chuyển List thành Map<questionId, Question> để truy xuất nhanh O(1) trong vòng lặp
        Map<Integer, Question> questionMap = questionList.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Lấy danh sách ID các câu hỏi ĐÃ CÓ TRONG QUIZ để tránh add trùng
        Set<Integer> existingQuestionIds = quiz.getQuizQuestions().stream()
                .map(qq -> qq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        // khởi tạo danh sách mới, tính orderindex mới
        int currentSize = quiz.getQuizQuestions().size();
        List<QuizQuestion> newQuizQuestions = new ArrayList<>();

        for (AddBulkQuestionQuizRequest.AddQuestionToQuizRequest item : items) {
            Integer questionId = item.getQuestionId();

            // Nếu câu hỏi đã có trong quiz rồi thì bỏ qua (Skip), không báo lỗi để tránh làm hỏng cả mảng
            if (existingQuestionIds.contains(questionId)) {
                log.warn("Câu hỏi [{}] đã tồn tại trong quiz [{}], bỏ qua.", questionId, quizId);
                continue;
            }

            Question question = questionMap.get(questionId);

            // Xử lý orderIndex: Nếu FE không truyền thì tự động tăng dần vào cuối
            short orderIndex = item.getOrderIndex() != null
                    ? item.getOrderIndex()
                    : (short) currentSize;

            newQuizQuestions.add(QuizQuestion.builder()
                    .quiz(quiz)
                    .question(question)
                    .overrideScore(item.getOverrideScore())
                    .orderIndex(orderIndex)
                    .build());

            currentSize++;
        }
        //  Thêm tất cả vào Quiz và Save
        if (!newQuizQuestions.isEmpty()) {
            quiz.getQuizQuestions().addAll(newQuizQuestions);
            quizRepository.save(quiz); // Cascade type ALL sẽ tự động insert QuizQuestion xuống DB
            log.info("Đã thêm thành công [{}] câu hỏi vào quiz [{}]", newQuizQuestions.size(), quizId);
        } else {
            log.info("Không có câu hỏi mới nào được thêm vào quiz [{}]", quizId);
        }
    }

    @Transactional
    @Override
    public Integer cloneQuiz(Integer quizId) {
        Quiz oldQuiz = findQuizById(quizId);
        checkQuizManager(oldQuiz);
        checkQuizNotDeleted(oldQuiz);

        Quiz cloneQuiz = Quiz.builder()
                .courseId(oldQuiz.getCourseId())
                .lessonId(oldQuiz.getLessonId())
                .title(oldQuiz.getTitle() + " (Copy)")
                .description(oldQuiz.getDescription())
                .duration(oldQuiz.getDuration())
                .totalScore(oldQuiz.getTotalScore())
                .passScore(oldQuiz.getPassScore())
                .maxAttempts(oldQuiz.getMaxAttempts())
                .shuffleQuestions(oldQuiz.getShuffleQuestions())
                .shuffleAnswers(oldQuiz.getShuffleAnswers())
                .showResult(oldQuiz.getShowResult())
                .published(false)
                .createdBy(authorizationService.currentUserId())
                .build();

        cloneQuiz = quizRepository.save(cloneQuiz);

        Quiz finalCloneQuiz = cloneQuiz;

        List<QuizQuestion> quizQuestionList = oldQuiz.getQuizQuestions().stream()
                .map(quizQuestion -> cloneQuizQuestion(quizQuestion, finalCloneQuiz))
                .collect(Collectors.toList());

        cloneQuiz.setQuizQuestions(quizQuestionList);
        quizRepository.save(cloneQuiz);

        log.info("Đã nhân bản Quiz [{}] thành Quiz mới [{}]", quizId, cloneQuiz.getQuizId());

        return cloneQuiz.getQuizId();
    }

    @Transactional
    @Override
    public void removeQuestionInQuiz(Integer quizId, Integer questionId) {
        Quiz quiz = findQuizById(quizId);
        checkQuizManager(quiz);
        checkQuizNotDeleted(quiz);

        //  Validation: Check nếu quiz published và có attempt thì không cho xóa câu
        validateQuizEditable(quiz);

        boolean removed = quiz.getQuizQuestions().removeIf(
                quizQuestion -> quizQuestion.getQuestion().getQuestionId().equals(questionId)
        );

        if (!removed) {
            throw new AssignmentException(ErrorCode.QUESTION_NOT_IN_QUIZ);
        }

        quizRepository.save(quiz);
        log.info("Đã gỡ câu hỏi [{}] khỏi quiz [{}]", questionId, quizId);
    }


    // =========================== HELPER =================================

    @Override
    public Quiz findQuizById(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUIZ_NOT_FOUND));
    }

//    public QuizAttempt findAttemptById(Long attemptId) {
//        return attemptRepository.findById(attemptId)
//                .orElseThrow(() -> new AssignmentException(ErrorCode.ATTEMPT_NOT_FOUND));
//    }

    private void checkQuizManager(Quiz quiz) {
        courseResourceAuthorizationService.checkManager(quiz.getCourseId(), quiz.getCreatedBy());
    }

    private void checkQuizNotDeleted(Quiz quiz) {
        if (quiz.getDeleted()) {
            throw new AssignmentException(ErrorCode.QUIZ_IS_DELETED);
        }
    }

    private void validateImportedQuizRequest(CreateImportedQuizRequest request) {
        if (request == null) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }

        Set<ConstraintViolation<CreateImportedQuizRequest>> violations;
        try {
            violations = REQUEST_VALIDATOR.validate(request);
        } catch (ValidationException e) {
            if (e.getCause() instanceof AssignmentException assignmentException) {
                throw assignmentException;
            }
            throw e;
        }

        if (!violations.isEmpty()) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateSaveQuizDraftRequest(SaveQuizDraftRequest request) {
        if (request == null) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }

        Set<ConstraintViolation<SaveQuizDraftRequest>> violations;
        try {
            violations = REQUEST_VALIDATOR.validate(request);
        } catch (ValidationException e) {
            if (e.getCause() instanceof AssignmentException assignmentException) {
                throw assignmentException;
            }
            throw e;
        }

        if (!violations.isEmpty()) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Quiz createEditableClone(Quiz sourceQuiz) {
        Quiz cloneQuiz = Quiz.builder()
                .courseId(sourceQuiz.getCourseId())
                .lessonId(sourceQuiz.getLessonId())
                .title(sourceQuiz.getTitle() + " (Copy)")
                .description(sourceQuiz.getDescription())
                .duration(sourceQuiz.getDuration())
                .totalScore(sourceQuiz.getTotalScore())
                .passScore(sourceQuiz.getPassScore())
                .maxAttempts(sourceQuiz.getMaxAttempts())
                .shuffleQuestions(sourceQuiz.getShuffleQuestions())
                .shuffleAnswers(sourceQuiz.getShuffleAnswers())
                .showResult(sourceQuiz.getShowResult())
                .published(false)
                .createdBy(authorizationService.currentUserId())
                .quizQuestions(new ArrayList<>())
                .build();

        return quizRepository.save(cloneQuiz);
    }

    private void replaceQuizQuestions(
            Quiz quiz,
            List<SaveQuizDraftRequest.QuestionDraftRequest> questionRequests,
            boolean forceCreateQuestions) {

        if (quiz.getQuizQuestions() == null) {
            quiz.setQuizQuestions(new ArrayList<>());
        }

        Map<Integer, QuizQuestion> existingLinksByQuestionId = quiz.getQuizQuestions().stream()
                .filter(quizQuestion -> quizQuestion.getQuestion() != null
                        && quizQuestion.getQuestion().getQuestionId() != null)
                .collect(Collectors.toMap(
                        quizQuestion -> quizQuestion.getQuestion().getQuestionId(),
                        quizQuestion -> quizQuestion,
                        (first, second) -> first
                ));
        Set<QuizQuestion> retainedLinks = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int index = 0; index < questionRequests.size(); index++) {
            SaveQuizDraftRequest.QuestionDraftRequest questionRequest = questionRequests.get(index);
            Question question = upsertQuestionForQuiz(quiz.getCourseId(), questionRequest, forceCreateQuestions);
            QuizQuestion quizQuestion = null;

            if (!forceCreateQuestions && questionRequest.getQuestionId() != null) {
                quizQuestion = existingLinksByQuestionId.get(question.getQuestionId());
            }

            if (quizQuestion == null) {
                quizQuestion = QuizQuestion.builder()
                        .quiz(quiz)
                        .question(question)
                        .build();
                quiz.getQuizQuestions().add(quizQuestion);
            }

            quizQuestion.setQuiz(quiz);
            quizQuestion.setQuestion(question);
            quizQuestion.setOverrideScore(question.getScore() != null ? question.getScore().shortValue() : null);
            quizQuestion.setOrderIndex((short) index);
            retainedLinks.add(quizQuestion);
        }

        quiz.getQuizQuestions().removeIf(quizQuestion -> !retainedLinks.contains(quizQuestion));
    }

    private Question upsertQuestionForQuiz(
            String courseId,
            SaveQuizDraftRequest.QuestionDraftRequest request,
            boolean forceCreateQuestion) {

        if (isReuseOnlyRequest(request)) {
            Question question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new AssignmentException(ErrorCode.QUESTION_NOT_FOUND));
            if (!courseId.equals(question.getCourseId())) {
                throw new AssignmentException(ErrorCode.QUESTION_NOT_FOUND);
            }
            return question;
        }

        if (request.getQuestionId() == null || forceCreateQuestion) {
            validateQuestionRequest(request);
            return questionRepository.save(buildQuestion(courseId, authorizationService.currentUserId(), request));
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new AssignmentException(ErrorCode.QUESTION_NOT_FOUND));
        if (!courseId.equals(question.getCourseId())) {
            throw new AssignmentException(ErrorCode.QUESTION_NOT_FOUND);
        }

        validateQuestionRequest(request);
        quizMapper.updateQuestion(request, question);
        QuestionAnswerReconciler.reconcile(question, request.getAnswers(), quizMapper);

        return questionRepository.save(question);
    }

    private boolean isReuseOnlyRequest(SaveQuizDraftRequest.QuestionDraftRequest request) {
        return request.getQuestionId() != null
                && (request.isReuseExisting()
                || request.getContent() == null
                || request.getAnswers() == null);
    }

    private void validateQuestionRequest(CreateQuestionRequest request) {
        Set<ConstraintViolation<CreateQuestionRequest>> violations;
        try {
            violations = REQUEST_VALIDATOR.validate(request);
        } catch (ValidationException e) {
            if (e.getCause() instanceof AssignmentException assignmentException) {
                throw assignmentException;
            }
            throw e;
        } catch (AssignmentException e) {
            throw e;
        }

        if (!violations.isEmpty()) {
            throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private QuizQuestion cloneQuizQuestion(QuizQuestion sourceQuizQuestion, Quiz targetQuiz) {
        Question clonedQuestion = cloneQuestion(sourceQuizQuestion.getQuestion());
        return QuizQuestion.builder()
                .quiz(targetQuiz)
                .question(clonedQuestion)
                .overrideScore(sourceQuizQuestion.getOverrideScore())
                .orderIndex(sourceQuizQuestion.getOrderIndex())
                .build();
    }

    private Question buildQuestion(String courseId, String userId, CreateQuestionRequest request) {
        Question question = quizMapper.toQuestion(request);
        question.setCourseId(courseId);
        question.setCreatedBy(userId);
        if (question.getAnswers() == null) {
            question.setAnswers(new ArrayList<>());
        }

        request.getAnswers().forEach(answerRequest -> {
            Answer answer = quizMapper.toAnswer(answerRequest);
            answer.setQuestion(question);
            question.getAnswers().add(answer);
        });
        return question;
    }

    private Question cloneQuestion(Question source) {
        Question clonedQuestion = Question.builder()
                .courseId(source.getCourseId())
                .content(source.getContent())
                .type(source.getType())
                .topic(source.getTopic())
                .explanation(source.getExplanation())
                .score(source.getScore())
                .imageUrl(source.getImageUrl())
                .createdBy(authorizationService.currentUserId())
                .build();

        List<Answer> clonedAnswers = source.getAnswers().stream()
                .map(sourceAnswer -> Answer.builder()
                        .question(clonedQuestion)
                        .content(sourceAnswer.getContent())
                        .correct(sourceAnswer.getCorrect())
                        .orderIndex(sourceAnswer.getOrderIndex())
                        .build())
                .toList();
        clonedQuestion.getAnswers().addAll(clonedAnswers);
        return questionRepository.save(clonedQuestion);
    }

    private void markCompletedQuizzes(String courseId, List<QuizStudentDetailResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }

        Set<Integer> completedQuizIds = Set.copyOf(
                quizAttemptRepository.findPassedQuizIdsByUserIdAndCourseId(
                        authorizationService.currentUserId(),
                        courseId));

        responses.forEach(response ->
                response.setCompleted(completedQuizIds.contains(response.getQuizId())));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void populateAttemptCounts(List<QuizInstructorDetailResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }

        List<Integer> quizIds = responses.stream()
                .map(QuizInstructorDetailResponse::getQuizId)
                .toList();

        Map<Integer, Long> attemptCounts = quizAttemptRepository.countGroupByQuizIds(quizIds).stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]
                ));

        responses.forEach(response ->
                response.setAttemptCount(attemptCounts.getOrDefault(response.getQuizId(), 0L)));
    }

    /**
     * Kiểm tra xem quiz có thể sửa được không
     * - Nếu chưa publish → OK (có thể sửa)
     * - Nếu published nhưng chưa có attempt → OK (có thể sửa)
     * - Nếu published và có attempt → NOT OK (không được sửa)
     */
    private void validateQuizEditable(Quiz quiz) {
        if (quizAttemptRepository.existsByQuiz_QuizId(quiz.getQuizId())) {
            log.warn("Cố gắng sửa quiz published có attempt: quizId={}, title={}", quiz.getQuizId(), quiz.getTitle());
            throw new AssignmentException(ErrorCode.QUIZ_HAS_ATTEMPTS_CANNOT_EDIT);
        }

        if (quiz.getPublished()) {
            throw new AssignmentException(ErrorCode.QUIZ_PUBLISHED_CANNOT_EDIT);
        }
    }
}
