import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';

const unwrap = (res) => res?.data?.data ?? res?.data;

// =============================================
// Assignment Service API
// =============================================

export const assignmentApi = {
  // ─────────────────────────────────────────────
  // Quiz
  // ─────────────────────────────────────────────

  /** Create quiz in a course */
  createQuiz: (courseId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUIZZES_BY_COURSE(courseId), data).then(unwrap),

  /** Create quiz and imported questions in a single transaction */
  createQuizFromImportedQuestions: (courseId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUIZZES_IMPORTED_BY_COURSE(courseId), data).then(unwrap),

  /** Get quizzes for student (published only) */
  getQuizzesForStudent: (courseId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_STUDENT(courseId), { params }).then(unwrap),

  /** Get quizzes for instructor/admin (all, including unpublished) */
  getQuizzesForInstructor: (courseId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_INSTRUCTOR(courseId), { params }).then(unwrap),

  /** Get single quiz detail */
  getQuizDetail: (quizId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_DETAIL(quizId)).then(unwrap),

  /** Get questions assigned to a quiz */
  getQuizQuestions: (quizId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_QUESTIONS(quizId)).then(unwrap),

  /** Update quiz */
  updateQuiz: (quizId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.QUIZ_DETAIL(quizId), data).then(unwrap),

  /** Upsert quiz settings and questions in a single transaction */
  syncQuiz: (quizId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.QUIZ_SYNC(quizId), data).then(unwrap),

  /** Delete quiz */
  deleteQuiz: (quizId) =>
    apiClient.delete(ENDPOINTS.ASSIGNMENT.QUIZ_DETAIL(quizId)).then(unwrap),

  /** Publish quiz */
  publishQuiz: (quizId) =>
    apiClient.patch(ENDPOINTS.ASSIGNMENT.QUIZ_PUBLISH(quizId)).then(unwrap),

  /** Unpublish quiz */
  unpublishQuiz: (quizId) =>
    apiClient.patch(ENDPOINTS.ASSIGNMENT.QUIZ_UNPUBLISH(quizId)).then(unwrap),

  /** Clone quiz (deep copy, returns new quiz) */
  cloneQuiz: (quizId) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUIZ_CLONE(quizId)).then(unwrap),

  /** Restore soft-deleted quiz */
  restoreQuiz: (quizId) =>
    apiClient.patch(ENDPOINTS.ASSIGNMENT.QUIZ_RESTORE(quizId)).then(unwrap),

  /** Add questions to quiz (bulk) */
  addQuestionsToQuiz: (quizId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUIZ_QUESTIONS(quizId), data).then(unwrap),

  /** Remove question from quiz */
  removeQuestionFromQuiz: (quizId, questionId) =>
    apiClient.delete(ENDPOINTS.ASSIGNMENT.QUIZ_REMOVE_QUESTION(quizId, questionId)).then(unwrap),

  // ─────────────────────────────────────────────
  // Quiz Attempts
  // ─────────────────────────────────────────────

  /** Start or resume quiz attempt (single call, handles both new and existing attempts) */
  startOrResumeAttempt: (quizId) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.ATTEMPT_START_OR_RESUME(quizId)).then(unwrap),

  /** Get in-progress attempt (for recovery after F5 with known attemptId) */
  getInProgressAttempt: (quizId, attemptId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.ATTEMPT_DETAIL(quizId, attemptId)).then(unwrap),

  /** Auto-save answers (idempotent) */
  autoSaveAttempt: (quizId, attemptId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.ATTEMPT_DETAIL(quizId, attemptId), data).then(unwrap),

  /** Submit attempt + grade */
  submitAttempt: (quizId, attemptId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.ATTEMPT_SUBMIT(quizId, attemptId), data).then(unwrap),

  /** Check one answer in practice mode */
  checkPracticeAnswer: (quizId, attemptId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.ATTEMPT_PRACTICE_CHECK(quizId, attemptId), data).then(unwrap),

  /** Get quiz result after submission */
  getQuizResult: (quizId, attemptId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.ATTEMPT_RESULT(quizId, attemptId)).then(unwrap),

  /** Get attempt history for a quiz */
  getAttemptHistory: (quizId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.ATTEMPT_HISTORY(quizId), { params }).then(unwrap),

  /** Download quiz import template file */
  downloadQuizImportTemplate: async (format = 'docx') => {
    const response = await apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_IMPORT_TEMPLATE(format), {
      responseType: 'blob',
    });
    const contentDisposition = response.headers['content-disposition'];
    let filename = `quiz-template.${format}`;
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?(.+?)"?$/);
      if (match) filename = match[1];
    }
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  },

  // ─────────────────────────────────────────────
  // Questions (Question Bank)
  // ─────────────────────────────────────────────

  /** Create a question */
  createQuestion: (courseId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUESTIONS_BY_COURSE(courseId), data).then(unwrap),

  /** Batch import questions */
  importBatchQuestions: (courseId, data, params) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.QUESTIONS_BATCH(courseId), data, { params }).then(unwrap),

  /** Import questions from file (multipart) */
  importQuestionFile: (courseId, file, { topic } = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (topic) formData.append('topic', topic);
    return apiClient.post(ENDPOINTS.ASSIGNMENT.QUESTION_IMPORT_FILE(courseId), formData, {
      transformRequest: [(data, headers) => {
        if (headers?.delete) {
          headers.delete('Content-Type');
        } else if (headers) {
          delete headers['Content-Type'];
          delete headers['content-type'];
        }
        return data;
      }],
    })
      .then(unwrap);
  },

  /** Preview questions from file without saving them */
  previewQuestionFile: (courseId, file, { topic } = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (topic) formData.append('topic', topic);
    return apiClient.post(ENDPOINTS.ASSIGNMENT.QUESTION_IMPORT_FILE_PREVIEW(courseId), formData, {
      transformRequest: [(data, headers) => {
        if (headers?.delete) {
          headers.delete('Content-Type');
        } else if (headers) {
          delete headers['Content-Type'];
          delete headers['content-type'];
        }
        return data;
      }],
    })
      .then(unwrap);
  },

  /** Generate quiz questions with AI from text or a source file, without saving them */
  previewAiGeneratedQuestions: (courseId, { file, content, questionCount, difficulty, questionType, topic }) => {
    const formData = new FormData();
    if (file) formData.append('file', file);
    if (content) formData.append('content', content);
    if (topic) formData.append('topic', topic);
    formData.append('questionCount', questionCount);
    formData.append('difficulty', difficulty);
    formData.append('questionType', questionType);
    return apiClient.post(ENDPOINTS.ASSIGNMENT.QUESTION_AI_GENERATE_PREVIEW(courseId), formData, {
      timeout: 120000,
      transformRequest: [(data, headers) => {
        if (headers?.delete) {
          headers.delete('Content-Type');
        } else if (headers) {
          delete headers['Content-Type'];
          delete headers['content-type'];
        }
        return data;
      }],
    })
      .then(unwrap);
  },

  /** Get questions by course (with optional topic/type filters) */
  getQuestionsByCourse: (courseId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUESTIONS_BY_COURSE(courseId), { params }).then(unwrap),

  /** Get distinct topics for course */
  getQuestionTopics: (courseId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUESTION_TOPICS(courseId)).then(unwrap),

  /** Get single question detail */
  getQuestionDetail: (questionId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUESTION_DETAIL(questionId)).then(unwrap),

  /** Update question */
  updateQuestion: (questionId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.QUESTION_DETAIL(questionId), data).then(unwrap),

  /** Delete question (blocked if used in any quiz) */
  deleteQuestion: (questionId) =>
    apiClient.delete(ENDPOINTS.ASSIGNMENT.QUESTION_DETAIL(questionId)).then(unwrap),

  /** Delete an uploaded assignment file from storage */
  deleteUploadedFile: (fileUrl) =>
    apiClient.delete(ENDPOINTS.UPLOAD.ASSIGNMENT_FILE, { data: { fileUrl } }).then(unwrap),

  // ─────────────────────────────────────────────
  // Problems (Coding Challenges)
  // ─────────────────────────────────────────────

  /** Create a problem */
  createProblem: (data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.PROBLEMS, data).then(unwrap),

  /** Get problems list (optional courseId + difficulty filter) */
  getProblems: (params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.PROBLEMS, { params }).then(unwrap),

  /** Get coding/quiz counts grouped by lesson for a course */
  getLessonCounts: (courseId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.ASSIGNMENT_LESSON_COUNTS(courseId)).then(unwrap),

  /** Get single problem detail */
  getProblemDetail: (problemId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.PROBLEM_DETAIL(problemId)).then(unwrap),

  /** Get single problem detail by slug */
  getProblemDetailBySlug: (slug) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.PROBLEM_DETAIL_BY_SLUG(slug)).then(unwrap),

  /** Update problem */
  updateProblem: (problemId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.PROBLEM_DETAIL(problemId), data).then(unwrap),

  /** Delete problem */
  deleteProblem: (problemId) =>
    apiClient.delete(ENDPOINTS.ASSIGNMENT.PROBLEM_DETAIL(problemId)).then(unwrap),

  /** Unpublish problem */
  unpublishProblem: (problemId) =>
    apiClient.patch(ENDPOINTS.ASSIGNMENT.PROBLEM_UNPUBLISH(problemId)).then(unwrap),

  /** Clone problem (deep copy, returns new problem) */
  cloneProblem: (problemId) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.PROBLEM_CLONE(problemId)).then(unwrap),

  /** Restore soft-deleted problem */
  restoreProblem: (problemId) =>
    apiClient.patch(ENDPOINTS.ASSIGNMENT.PROBLEM_RESTORE(problemId)).then(unwrap),

  // ─────────────────────────────────────────────
  // Code Submissions
  // ─────────────────────────────────────────────

  /** Submit code for judging */
  submitCode: (data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.SUBMISSIONS, data).then(unwrap),

  /** Run code against public example test cases without creating a submission */
  runCode: (data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.RUN_CODE, data, { timeout: 70000 }).then(unwrap),

  /** Get submission detail */
  getSubmission: (submissionId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.SUBMISSION_DETAIL(submissionId)).then(unwrap),

  /** Get latest accepted submission for current user and problem */
  getLatestAcceptedSubmission: (problemId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.LATEST_ACCEPTED_SUBMISSION, { params: { problemId } }).then(unwrap),

  /** Get submission history (filter by problemId, targetUserId) */
  getSubmissionHistory: (params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.SUBMISSIONS, { params }).then(unwrap),

  /** Get instructor analytics for quiz/coding activity in a course */
  getInstructorAnalytics: (courseId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.INSTRUCTOR_ANALYTICS(courseId), { params }).then(unwrap),

  /** Get instructor submission summaries for quiz/coding activity in a course */
  getInstructorSubmissions: (courseId, params) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.INSTRUCTOR_SUBMISSIONS(courseId), { params }).then(unwrap),

  // ─────────────────────────────────────────────
  // Leaderboard
  // ─────────────────────────────────────────────

  /** Get coding leaderboard for a course */
  getCodingLeaderboard: (courseId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.CODING_LEADERBOARD(courseId)).then(unwrap),

  /** Get quiz leaderboard for a course */
  getQuizLeaderboard: (courseId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.QUIZ_LEADERBOARD(courseId)).then(unwrap),

  // ─────────────────────────────────────────────
  // Test Cases
  // ─────────────────────────────────────────────

  /** Create a single test case */
  createTestCase: (problemId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.TESTCASES(problemId), data).then(unwrap),

  /** Bulk create test cases */
  createBulkTestCases: (problemId, data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.TESTCASES_BULK(problemId), data).then(unwrap),

  /** Import test cases from CSV/XLSX file */
  importTestCases: (problemId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(ENDPOINTS.ASSIGNMENT.TESTCASES_IMPORT(problemId), formData, {
      transformRequest: [(data, headers) => {
        if (headers?.delete) {
          headers.delete('Content-Type');
        } else if (headers) {
          delete headers['Content-Type'];
          delete headers['content-type'];
        }
        return data;
      }],
    }).then(unwrap);
  },

  /** Generate test case suggestions with AI without saving them */
  previewAiGeneratedTestCases: (data) =>
    apiClient.post(ENDPOINTS.ASSIGNMENT.TESTCASES_AI_GENERATE_PREVIEW, data, { timeout: 120000 }).then(unwrap),

  /** Download test case import template file */
  downloadTestCaseTemplate: async (format = 'xlsx') => {
    const response = await apiClient.get(ENDPOINTS.ASSIGNMENT.TESTCASE_IMPORT_TEMPLATE(format), {
      responseType: 'blob',
    });
    const contentDisposition = response.headers['content-disposition'];
    let filename = `testcases-template.${format}`;
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?(.+?)"?$/);
      if (match) filename = match[1];
    }
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  },

  /** Reconcile test cases for a problem in one transaction */
  syncTestCases: (problemId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.TESTCASES_SYNC(problemId), data).then(unwrap),

  /** Get test cases for a problem (instructor view) */
  getTestCases: (problemId) =>
    apiClient.get(ENDPOINTS.ASSIGNMENT.TESTCASES(problemId)).then(unwrap),

  /** Update a test case */
  updateTestCase: (problemId, testId, data) =>
    apiClient.put(ENDPOINTS.ASSIGNMENT.TESTCASE_DETAIL(problemId, testId), data).then(unwrap),

  /** Delete test cases by IDs */
  deleteTestCases: (problemId, testIds) => {
    const params = new URLSearchParams();
    testIds.forEach((testId) => params.append('ids', testId));
    return apiClient.delete(`${ENDPOINTS.ASSIGNMENT.TESTCASES(problemId)}?${params.toString()}`).then(unwrap);
  },
};

export default assignmentApi;
