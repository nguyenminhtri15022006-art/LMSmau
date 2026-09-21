// =============================================
// Assignment Data Mappers
// Maps backend API response DTOs → frontend component shapes
// =============================================

// ─────────────────────────────────────────────
// Student View Mappers
// ─────────────────────────────────────────────

/**
 * Map QuizStudentDetailResponse (paginated page) → quizExercises shape
 * Frontend uses: { id, title, questionsCount, level, completions, icon, timeLimit }
 */
export function mapQuizToExerciseHubItem(quiz) {
  return {
    id: quiz.quizId,
    title: quiz.title,
    questionsCount: quiz.questionCount ?? 0,
    level: quiz.difficulty ?? 'MEDIUM',
    completions: quiz.attemptCount ?? '0',
    icon: quiz.questionCount > 20 ? 'cloud' : quiz.questionCount > 10 ? 'settings_input_component' : 'database',
    timeLimit: (quiz.duration ?? 120) * 60,
    passScore: quiz.passScore,
    totalScore: quiz.totalScore,
    maxAttempts: quiz.maxAttempts,
    startTime: quiz.startTime,
    endTime: quiz.endTime,
    lessonId: quiz.lessonId,
    completed: !!quiz.completed,
  };
}

/**
 * Map QuizAttemptResponse → quiz-taking page data
 * Backend returns attempt with questions (answers WITHOUT correct field)
 */
export function mapAttemptToQuizSession(attempt) {
  return {
    attemptId: attempt.attemptId,
    quizId: attempt.quizId,
    title: attempt.quizTitle,
    duration: attempt.duration * 60, // minutes → seconds
    totalScore: attempt.totalScore,
    startedAt: attempt.startedAt,
    expiresAt: attempt.expiresAt,
    status: attempt.status,
    questions: (attempt.questions || []).map((q, idx) => ({
      id: q.questionId,
      index: idx,
      question: q.content,
      topic: q.topic || '',
      imageUrl: q.imageUrl || '',
      type: q.type,
      options: (q.answers || []).map((a, ai) => ({
        label: a.content,
        answerId: a.answerId,
      })),
      score: q.score,
    })),
    savedAnswers: {},
  };
}

/**
 * Map QuizResultResponse → quiz result page data
 * Backend returns result with answers INCLUDING correct field
 */
function arraysMatch(a, b) {
  if (!a || !b) return false;
  const sortedA = [...a].sort();
  const sortedB = [...b].sort();
  return sortedA.length === sortedB.length && sortedA.every((v, i) => v === sortedB[i]);
}

export function mapResultToQuizResult(result) {
  return {
    attemptId: result.attemptId,
    quizId: result.quizId,
    title: result.quizTitle,
    attemptNumber: result.attemptNumber,
    score: result.score,
    totalScore: result.totalScore,
    passed: result.passed,
    timeSpentS: result.timeSpentSeconds,
    submittedAt: result.submittedAt,
    showResult: result.showResult,
    resultAvailable: result.resultAvailable !== false,
    resultAvailableAt: result.resultAvailableAt,
    scorePercentage: result.totalScore > 0 ? Math.round((result.score / result.totalScore) * 100) : 0,
    totalQuestions: result.totalQuestions ?? (result.questions || []).length,
    questions: (result.questions || []).map((q, idx) => {
      const correctAnswerIds = (q.answers || [])
        .filter(a => a.correct)
        .map(a => a.answerId);
      const selectedIds = q.selectedAnswerIds || [];
      return {
        id: q.questionId,
        index: idx,
        question: q.content,
        topic: q.topic || '',
        imageUrl: q.imageUrl || '',
        correctAnswerIds,
        selectedAnswerIds: selectedIds,
        isCorrect: arraysMatch(selectedIds, correctAnswerIds),
        questionScore: q.score,
        earnedScore: q.earnedScore,
        explanation: q.explanation || '',
        options: (q.answers || []).map(a => ({
          label: a.content,
          answerId: a.answerId,
          isCorrectOption: a.correct,
        })),
      };
    }),
  };
}

/**
 * Map ProblemListResponse → challenge card shape
 * Frontend uses: { id, title, description, difficulty, timeEstimate, successRate, status }
 */
export function mapProblemToChallenge(problem, completionMap = {}) {
  const isCompleted = problem.completed ?? !!completionMap[problem.problemId];
  const acceptanceRate = problem.acceptanceRate ?? (problem.totalSubmit > 0
    ? Math.round((problem.totalAccepted / problem.totalSubmit) * 100)
    : 0);

  return {
    id: problem.problemId,
    courseId: problem.courseId,
    title: problem.title,
    description: problem.description || '',
    difficulty: problem.difficulty ?? 'MEDIUM',
    timeEstimate: problem.timeLimitMs ? `${Math.round(problem.timeLimitMs / 60000)} min` : '120 mins',
    successRate: `${acceptanceRate}%`,
    status: isCompleted ? 'Completed' : 'New',
    completed: isCompleted,
    slug: problem.slug,
    score: problem.score,
    topicId: problem.topicId,
    lessonId: problem.lessonId,
  };
}

/**
 * Map StudentProblemDetailResponse → full code challenge page data
 * Used for ExerciseCodePage
 */
export function mapProblemToCodeChallenge(problem) {
  return {
    id: problem.problemId,
    courseId: problem.courseId,
    title: problem.title,
    description: problem.description || '',
    difficulty: problem.difficulty ?? 'MEDIUM',
    timeEstimate: problem.timeLimitMs ? `${Math.round(problem.timeLimitMs / 60000)} min` : '120 mins',
    problemStatement: problem.description || '',
    examples: (problem.examples || []).map((example) => ({
      input: example.input,
      output: example.expectedOutput,
    })),
    initialCode: problem.starterCode || null,
    testCases: [],
    allowedLangs: problem.allowedLangs || [],
    memoryLimitMb: problem.memoryLimitMb,
    timeLimitMs: problem.timeLimitMs,
    score: problem.score ?? null,
    slug: problem.slug || null,
  };
}

/**
 * Map SubmissionResponse → code result page data
 */
export function mapSubmissionToResult(submission) {
  return {
    submissionId: submission.submissionId,
    problemId: submission.problemId,
    status: submission.status,
    score: submission.score,
    execTimeMs: submission.execTimeMs,
    memoryUsedKb: submission.memoryUsedKb,
    submittedAt: submission.submittedAt,
    judgeAt: submission.judgedAt,
  };
}

// ─────────────────────────────────────────────
// Instructor View Mappers
// ─────────────────────────────────────────────

/**
 * Map QuizInstructorDetailResponse → teacher exercise table item
 * Instructor table uses: { id, chapter, title, meta, difficulty, status, questions }
 */
export function mapQuizToInstructorExercise(quiz) {
  const difficultyMap = { EASY: 'Beginner', MEDIUM: 'Intermediate', HARD: 'Advanced' };
  const isDeleted = quiz.deleted === true;
  return {
    id: quiz.quizId,
    chapter: quiz.lessonId ? `Lesson ${quiz.lessonId}` : 'Uncategorized',
    title: quiz.title,
    meta: `${quiz.questionCount || 0} Questions • ${quiz.duration || 20} Mins`,
    difficulty: difficultyMap[quiz.difficulty] ?? 'Beginner',
    difficultyTone: (quiz.difficulty || 'EASY').toLowerCase(),
    status: isDeleted ? 'Deleted' : (quiz.published ? 'Published' : 'Draft'),
    doneCount: quiz.attemptCount ?? 0,
    questions: [],
    timeLimit: quiz.duration,
    passingScore: quiz.passScore,
    deleted: isDeleted,
    deletedAt: quiz.deletedAt,
  };
}

/**
 * Map ProblemListResponse → teacher coding challenge table item
 */
export function mapProblemToInstructorChallenge(problem) {
  const difficultyMap = { EASY: 'Beginner', MEDIUM: 'Intermediate', HARD: 'Advanced' };
  const isDeleted = problem.deleted === true;
  return {
    id: problem.problemId,
    chapter: problem.lessonId ? `Lesson ${problem.lessonId}` : 'Uncategorized',
    title: problem.title,
    meta: `Complexity: ${(problem.difficulty || 'EASY').charAt(0) + (problem.difficulty || 'EASY').slice(1).toLowerCase()} • ${problem.allowedLangs?.join(', ') || 'Multi'}`,
    difficulty: difficultyMap[problem.difficulty] ?? 'Intermediate',
    difficultyTone: (problem.difficulty || 'MEDIUM').toLowerCase(),
    status: isDeleted ? 'Deleted' : (problem.isPublic ? 'Published' : 'Draft'),
    doneCount: problem.totalSubmit ?? 0,
    deleted: isDeleted,
    deletedAt: problem.deletedAt,
  };
}

/**
 * Map a course object (from course service or teacher data) → CourseExerciseCard shape
 */
export function mapCourseToExerciseHubCard(course) {
  return {
    id: course.id || course.courseId,
    name: course.title || course.name,
    category: course.category || course.difficulty || 'General',
  };
}

/**
 * Map attempt history + quiz → student results table row
 * Instructor results page uses: { id, student, exercise, type, score, date, status }
 */
export function mapAttemptToResultRow(attempt, quizTitle, studentName) {
  const isPassed = attempt.passed !== undefined ? attempt.passed : attempt.score >= (attempt.totalScore * 0.5);
  return {
    id: attempt.attemptId,
    student: studentName || `User ${attempt.userId}`,
    exercise: quizTitle || `Quiz ${attempt.quizId}`,
    type: 'Quiz',
    score: attempt.totalScore > 0 ? Math.round((attempt.score / attempt.totalScore) * 100) : 0,
    date: attempt.submittedAt || attempt.startedAt,
    status: isPassed ? 'Passed' : 'Failed',
  };
}

/**
 * Map submission + problem → coding result row
 */
export function mapSubmissionToResultRow(submission, problemTitle, studentName) {
  const isAccepted = submission.status === 'AC' || submission.status === 'ACCEPTED';
  return {
    id: submission.submissionId,
    student: studentName || `User ${submission.userId}`,
    exercise: problemTitle || `Problem ${submission.problemId}`,
    type: 'Coding',
    score: submission.score ?? (isAccepted ? 100 : 0),
    date: submission.submittedAt,
    status: isAccepted ? 'Passed' : 'Failed',
  };
}

export default {
  mapQuizToExerciseHubItem,
  mapAttemptToQuizSession,
  mapResultToQuizResult,
  mapProblemToChallenge,
  mapProblemToCodeChallenge,
  mapSubmissionToResult,
  mapQuizToInstructorExercise,
  mapProblemToInstructorChallenge,
  mapCourseToExerciseHubCard,
  mapAttemptToResultRow,
  mapSubmissionToResultRow,
};
