// =============================================
// API Gateway & Microservice Endpoints
// =============================================

import { API_BASE_URL } from '../configurations/env';

const BASE_URL = API_BASE_URL;

export const ENDPOINTS = {
    // Identity Service
    AUTH: {
        LOGIN: `${BASE_URL}/api/identity/auth/login`,
        REGISTER: `${BASE_URL}/api/identity/auth/register`,
        REFRESH: `${BASE_URL}/api/identity/auth/refresh`,
        LOGOUT: `${BASE_URL}/api/identity/auth/logout`,
        PROFILE: `${BASE_URL}/api/identity/users/myinfo`,
        PUBLIC_PROFILE: (userId) => `${BASE_URL}/api/identity/users/${userId}/profile`,
        FORGOT_PASSWORD: `${BASE_URL}/api/identity/auth/forgot-password`,
        RESET_PASSWORD: `${BASE_URL}/api/identity/auth/reset-password`,
        VERIFY_EMAIL_SEND: `${BASE_URL}/api/identity/auth/verify-email/send`,
        VERIFY_EMAIL: `${BASE_URL}/api/identity/auth/verify-email`,
        UPDATE_PASSWORD: `${BASE_URL}/api/identity/users/myinfo/password`,
        SOCIAL_LOGIN: (provider) => `${BASE_URL}/api/identity/auth/social-login/${provider}`,
    },

    // Course Service
    COURSES: {
        BASE: `${BASE_URL}/api/course/courses`,
        MY_COURSES: `${BASE_URL}/api/course/courses/my`,
        DETAIL: (id) => `${BASE_URL}/api/course/courses/${id}`,
        LESSONS: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/lessons`,
        ENROLL: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/enroll`,
        ENROLLED: `${BASE_URL}/api/course/courses/enrolled`,
        PROGRESS: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/progress`,
        AVERAGE_PROGRESS: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/progress/average`,
        SCHEDULES: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/schedules`,
        CATEGORIES: `${BASE_URL}/api/course/categories`,
        STUDENTS_COUNT: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/students/count`,
        STUDENTS: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/students`,
        REMOVE_STUDENT: (courseId, userId) => `${BASE_URL}/api/course/courses/${courseId}/students/${userId}`,
        SEARCH_USER: `${BASE_URL}/api/course/courses/users/search`,
        LESSONS_COUNT: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/lessons/count`,
        STRUCTURE: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/structure`,
        LESSON_NODES: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/structure/lessons`,
        LESSON_DETAIL: (courseId, lessonId) => `${BASE_URL}/api/course/courses/${courseId}/lessons/${lessonId}`,
        LESSON_COMPLETE: (courseId, lessonId) => `${BASE_URL}/api/course/courses/${courseId}/lessons/${lessonId}/complete`,
        ANNOUNCEMENTS: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/announcements`,
        LOCK: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/lock`,
        UNLOCK: (courseId) => `${BASE_URL}/api/course/courses/${courseId}/unlock`,
    },

    // Instructor Dashboard
    INSTRUCTOR: {
        MY_STATS: `${BASE_URL}/api/course/courses/my/stats`,
        RECENT_ENROLLMENTS: `${BASE_URL}/api/course/courses/my/recent-enrollments`,
    },

    // Assignment Service (gateway uses singular path: /api/assignment)
    ASSIGNMENT: {
        // ── Quizzes ──
        QUIZZES_BY_COURSE: (courseId) => `${BASE_URL}/api/assignment/quizzes/courses/${courseId}`,
        QUIZZES_IMPORTED_BY_COURSE: (courseId) => `${BASE_URL}/api/assignment/quizzes/courses/${courseId}/imported`,
        QUIZ_INSTRUCTOR: (courseId) => `${BASE_URL}/api/assignment/quizzes/instructor/courses/${courseId}`,
        QUIZ_STUDENT: (courseId) => `${BASE_URL}/api/assignment/quizzes/student/courses/${courseId}`,
        QUIZ_DETAIL: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}`,
        QUIZ_SYNC: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/sync`,
        QUIZ_PUBLISH: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/publish`,
        QUIZ_UNPUBLISH: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/unpublish`,
        QUIZ_CLONE: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/clone`,
        QUIZ_RESTORE: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/restore`,
        QUIZ_QUESTIONS: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/questions`,
        QUIZ_REMOVE_QUESTION: (quizId, questionId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/questions/${questionId}`,
        QUIZ_IMPORT_TEMPLATE: (format = 'docx') => `${BASE_URL}/api/assignment/templates/quiz-import?format=${format}`,

        // ── Quiz Attempts ──
        ATTEMPT_START_OR_RESUME: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts/start-or-resume`,
        ATTEMPT_DETAIL: (quizId, attemptId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts/${attemptId}`,
        ATTEMPT_SUBMIT: (quizId, attemptId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts/${attemptId}/submit`,
        ATTEMPT_PRACTICE_CHECK: (quizId, attemptId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts/${attemptId}/practice-check`,
        ATTEMPT_RESULT: (quizId, attemptId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts/${attemptId}/result`,
        ATTEMPT_HISTORY: (quizId) => `${BASE_URL}/api/assignment/quizzes/${quizId}/attempts`,

        // ── Questions (Question Bank) ──
        QUESTIONS_BY_COURSE: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}`,
        QUESTIONS_BATCH: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}/batch`,
        QUESTION_TOPICS: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}/topics`,
        QUESTION_DETAIL: (questionId) => `${BASE_URL}/api/assignment/questions/${questionId}`,
        QUESTION_IMPORT_FILE: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}/import-file`,
        QUESTION_IMPORT_FILE_PREVIEW: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}/import-file/preview`,
        QUESTION_AI_GENERATE_PREVIEW: (courseId) => `${BASE_URL}/api/assignment/questions/courses/${courseId}/ai-generate/preview`,

        // ── Problems (Coding Challenges) ──
        PROBLEMS: `${BASE_URL}/api/assignment/problems`,
        ASSIGNMENT_LESSON_COUNTS: (courseId) => `${BASE_URL}/api/assignment/courses/${courseId}/lesson-counts`,
        PROBLEM_DETAIL: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}`,
        PROBLEM_UNPUBLISH: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/unpublish`,
        PROBLEM_CLONE: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/clone`,
        PROBLEM_RESTORE: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/restore`,
        PROBLEM_DETAIL_BY_SLUG: (slug) => `${BASE_URL}/api/assignment/problems/slug/${slug}`,

        // ── Code Submissions ──
        SUBMISSIONS: `${BASE_URL}/api/assignment/submissions`,
        INSTRUCTOR_ANALYTICS: (courseId) => `${BASE_URL}/api/assignment/instructor/courses/${courseId}/analytics`,
        INSTRUCTOR_SUBMISSIONS: (courseId) => `${BASE_URL}/api/assignment/instructor/courses/${courseId}/submissions`,
        RUN_CODE: `${BASE_URL}/api/assignment/submissions/run`,
        SUBMISSION_DETAIL: (submissionId) => `${BASE_URL}/api/assignment/submissions/${submissionId}`,
        LATEST_ACCEPTED_SUBMISSION: `${BASE_URL}/api/assignment/submissions/latest-accepted`,

        // ── Leaderboard ──
        CODING_LEADERBOARD: (courseId) => `${BASE_URL}/api/assignment/courses/${courseId}/leaderboard/coding`,
        QUIZ_LEADERBOARD: (courseId) => `${BASE_URL}/api/assignment/courses/${courseId}/leaderboard/quiz`,

        // ── Test Cases ──
        TESTCASES: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/testcases`,
        TESTCASES_BULK: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/testcases/bulk`,
        TESTCASES_SYNC: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/testcases/sync`,
        TESTCASES_IMPORT: (problemId) => `${BASE_URL}/api/assignment/problems/${problemId}/testcases/import`,
        TESTCASES_AI_GENERATE_PREVIEW: `${BASE_URL}/api/assignment/problems/testcases/ai-generate/preview`,
        TESTCASE_IMPORT_TEMPLATE: (format = 'xlsx') => `${BASE_URL}/api/assignment/templates/testcases?format=${format}`,
        TESTCASE_DETAIL: (problemId, testId) => `${BASE_URL}/api/assignment/problems/${problemId}/testcases/${testId}`,
    },

    // Blog Service
    BLOG: {
        POSTS: `${BASE_URL}/api/blog/posts`,
        ADMIN_POSTS: `${BASE_URL}/api/blog/posts/admin`,
        PUBLIC_POSTS: `${BASE_URL}/api/blog/posts/public`,
        PUBLIC_DETAIL: (slug) => `${BASE_URL}/api/blog/posts/public/${encodeURIComponent(slug)}`,
        MY_POSTS: `${BASE_URL}/api/blog/posts/me`,
        DETAIL: (id) => `${BASE_URL}/api/blog/posts/${id}`,
        TAGS: `${BASE_URL}/api/blog/tags`,
        COMMENTS: (postId) => `${BASE_URL}/api/blog/posts/${postId}/comments`,
        VOTE: (postId) => `${BASE_URL}/api/blog/posts/${postId}/vote`,
        COMMENT_VOTE: (postId, commentId) => `${BASE_URL}/api/blog/posts/${postId}/comments/${commentId}/vote`,
    },

    // MinIO Upload
    UPLOAD: {
        PRESIGNED: `${BASE_URL}/api/course/storage/presigned-url`,
        MULTIPART_INITIATE: `${BASE_URL}/api/course/storage/multipart/initiate`,
        MULTIPART_COMPLETE: `${BASE_URL}/api/course/storage/multipart/complete`,
        ASSIGNMENT_PRESIGNED: `${BASE_URL}/api/assignment/storage/presigned-url`,
        ASSIGNMENT_FILE: `${BASE_URL}/api/assignment/storage/file`,
        IDENTITY_PRESIGNED: `${BASE_URL}/api/identity/storage/presigned-url`,
    },

    // Legacy Judge0 hook endpoints. The current backend proxies judging through assignment submissions.
    JUDGE0: {
        SUBMIT: `${BASE_URL}/api/assignment/submissions`,
        RESULT: (token) => `${BASE_URL}/api/assignment/submissions/${token}`,
    },

    // Chatbot Service
    CHATBOT: {
        CHAT: `${BASE_URL}/api/chatbot/chat`,
        SESSIONS: `${BASE_URL}/api/chatbot/sessions`,
        SESSION_MESSAGES: (sessionId) => `${BASE_URL}/api/chatbot/sessions/${sessionId}/messages`,
        ARCHIVE_SESSION: (sessionId) => `${BASE_URL}/api/chatbot/sessions/${sessionId}/archive`,
    },

    // Admin Dashboard
    ADMIN: {
        USERS: `${BASE_URL}/api/identity/admin/users`,
        USER_STATS: `${BASE_URL}/api/identity/admin/users/stats`,
        COURSES: `${BASE_URL}/api/course/courses`,
        SUBMISSIONS: `${BASE_URL}/api/assignment/submissions`,
        CODE_JUDGE_ACTIVITY: `${BASE_URL}/api/assignment/admin/code-judge/activity`,
    },
};

export default ENDPOINTS;
