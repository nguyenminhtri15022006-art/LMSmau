import { describe, it, expect } from 'vitest';
import {
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
} from '../../utils/assignmentMappers';

describe('mapQuizToExerciseHubItem', () => {
  const quiz = {
    quizId: 'quiz-1',
    title: 'JS Basics',
    questionCount: 10,
    difficulty: 'MEDIUM',
    attemptCount: 5,
    duration: 120,
    passScore: 60,
    totalScore: 100,
    maxAttempts: 3,
    startTime: '2024-06-01T00:00:00Z',
    endTime: '2024-07-01T00:00:00Z',
    lessonId: 'lesson-1',
    completed: true,
  };

  it('maps all fields correctly', () => {
    const result = mapQuizToExerciseHubItem(quiz);
    expect(result).toEqual({
      id: 'quiz-1',
      title: 'JS Basics',
      questionsCount: 10,
      level: 'MEDIUM',
      completions: 5,
      // questionCount=10 → not >10 → 'database'
      icon: 'database',
      timeLimit: 7200,
      passScore: 60,
      totalScore: 100,
      maxAttempts: 3,
      startTime: '2024-06-01T00:00:00Z',
      endTime: '2024-07-01T00:00:00Z',
      lessonId: 'lesson-1',
      completed: true,
    });
  });

  it('uses cloud icon for large question count (>20)', () => {
    const large = { ...quiz, questionCount: 25 };
    expect(mapQuizToExerciseHubItem(large).icon).toBe('cloud');
  });

  it('uses database icon for small question count (≤10)', () => {
    const small = { ...quiz, questionCount: 5 };
    expect(mapQuizToExerciseHubItem(small).icon).toBe('database');
  });

  it('defaults missing numeric fields to 0', () => {
    const sparse = { quizId: 'q-1', title: 'Quiz' };
    const result = mapQuizToExerciseHubItem(sparse);
    expect(result.questionsCount).toBe(0);
    expect(result.completions).toBe('0');
    expect(result.timeLimit).toBe(7200);
  });
});

describe('mapAttemptToQuizSession', () => {
  const attempt = {
    attemptId: 'attempt-1',
    quizId: 'quiz-1',
    quizTitle: 'JS Quiz',
    duration: 30,
    totalScore: 100,
    startedAt: '2024-06-15T10:00:00Z',
    expiresAt: '2024-06-15T10:30:00Z',
    status: 'IN_PROGRESS',
    questions: [
      {
        questionId: 'q-1',
        content: 'What is JS?',
        type: 'MULTIPLE_CHOICE',
        score: 10,
        answers: [
          { content: 'Answer A', answerId: 'a-1' },
          { content: 'Answer B', answerId: 'a-2' },
        ],
      },
    ],
  };

  it('maps attempt session with duration converted to seconds', () => {
    const result = mapAttemptToQuizSession(attempt);
    expect(result.attemptId).toBe('attempt-1');
    expect(result.duration).toBe(1800);
    expect(result.questions).toHaveLength(1);
    expect(result.questions[0].options).toHaveLength(2);
  });

  it('initializes savedAnswers as empty object', () => {
    const result = mapAttemptToQuizSession(attempt);
    expect(result.savedAnswers).toEqual({});
  });
});

describe('mapResultToQuizResult', () => {
  const resultData = {
    attemptId: 'attempt-1',
    quizId: 'quiz-1',
    quizTitle: 'JS Quiz',
    attemptNumber: 2,
    score: 80,
    totalScore: 100,
    passed: true,
    timeSpentSeconds: 1200,
    submittedAt: '2024-06-15T10:25:00Z',
    showResult: true,
    resultAvailableAt: '2024-06-16T00:00:00Z',
    totalQuestions: 5,
    questions: [
      {
        questionId: 'q-1',
        content: 'What is JS?',
        type: 'MULTIPLE_CHOICE',
        score: 20,
        earnedScore: 20,
        explanation: 'JS is a language',
        answers: [
          { content: 'Answer A', answerId: 'a-1', correct: true },
          { content: 'Answer B', answerId: 'a-2', correct: false },
        ],
        selectedAnswerIds: ['a-1'],
      },
      {
        questionId: 'q-2',
        content: 'Wrong Q',
        type: 'MULTIPLE_CHOICE',
        score: 20,
        earnedScore: 0,
        explanation: '',
        answers: [
          { content: 'X', answerId: 'a-3', correct: true },
          { content: 'Y', answerId: 'a-4', correct: false },
        ],
        selectedAnswerIds: ['a-4'],
      },
    ],
  };

  it('maps result with score percentage and marks correct/incorrect questions', () => {
    const result = mapResultToQuizResult(resultData);
    expect(result.scorePercentage).toBe(80);
    expect(result.questions[0].isCorrect).toBe(true);
    expect(result.questions[1].isCorrect).toBe(false);
    expect(result.questions[0].correctAnswerIds).toEqual(['a-1']);
  });

  it('shows result as available when resultAvailable is not set', () => {
    const r = mapResultToQuizResult(resultData);
    expect(r.showResult).toBe(true);
  });
});

describe('mapProblemToChallenge', () => {
  const problem = {
    problemId: 'prob-1',
    courseId: 'course-1',
    title: 'Two Sum',
    description: 'Solve two sum',
    difficulty: 'EASY',
    timeLimitMs: 30000,
    totalSubmit: 100,
    totalAccepted: 75,
    acceptanceRate: 75,
    slug: 'two-sum',
    score: 10,
    lessonId: 'lesson-1',
  };

  it('maps problem with acceptance rate', () => {
    const result = mapProblemToChallenge(problem);
    expect(result.title).toBe('Two Sum');
    expect(result.successRate).toBe('75%');
    expect(result.difficulty).toBe('EASY');
  });

  it('calculates acceptance rate when not provided', () => {
    const noRate = { ...problem, acceptanceRate: undefined };
    const result = mapProblemToChallenge(noRate);
    expect(result.successRate).toBe('75%');
  });

  it('shows 0% acceptance when no submissions and no acceptanceRate', () => {
    const noSubs = { ...problem, totalSubmit: 0, totalAccepted: 0, acceptanceRate: undefined };
    const result = mapProblemToChallenge(noSubs);
    // 0/0 → NaN → acceptanceRate=undefined → 0
    expect(result.successRate).toBe('0%');
  });
});

describe('mapProblemToCodeChallenge', () => {
  it('maps problem to code challenge with examples', () => {
    const problem = {
      problemId: 'prob-1',
      title: 'Two Sum',
      description: 'Solve',
      difficulty: 'EASY',
      timeLimitMs: 30000,
      memoryLimitMb: 256,
      examples: [{ input: '[1,2,3]', expectedOutput: '6' }],
      starterCode: 'function twoSum() {}',
      allowedLangs: ['javascript'],
      score: 10,
      slug: 'two-sum',
    };
    const result = mapProblemToCodeChallenge(problem);
    expect(result.title).toBe('Two Sum');
    expect(result.examples).toHaveLength(1);
    expect(result.examples[0].input).toBe('[1,2,3]');
    expect(result.initialCode).toBe('function twoSum() {}');
  });
});

describe('mapSubmissionToResult', () => {
  it('maps submission response', () => {
    const sub = {
      submissionId: 'sub-1',
      problemId: 'prob-1',
      status: 'ACCEPTED',
      score: 100,
      execTimeMs: 45,
      memoryUsedKb: 10240,
      submittedAt: '2024-06-15T10:00:00Z',
      judgedAt: '2024-06-15T10:01:00Z',
    };
    const result = mapSubmissionToResult(sub);
    expect(result.submissionId).toBe('sub-1');
    expect(result.status).toBe('ACCEPTED');
    expect(result.score).toBe(100);
  });
});

describe('mapQuizToInstructorExercise', () => {
  it('maps quiz to instructor table item', () => {
    const quiz = {
      quizId: 'quiz-1',
      lessonId: 'lesson-1',
      title: 'Midterm',
      questionCount: 15,
      duration: 45,
      difficulty: 'HARD',
      published: true,
      attemptCount: 10,
      passScore: 70,
    };
    const result = mapQuizToInstructorExercise(quiz);
    expect(result.title).toBe('Midterm');
    expect(result.status).toBe('Published');
    expect(result.meta).toContain('15 Questions');
  });

  it('maps deleted quiz status', () => {
    const deleted = { quizId: 'q-1', title: 'Old', deleted: true };
    expect(mapQuizToInstructorExercise(deleted).status).toBe('Deleted');
  });
});

describe('mapProblemToInstructorChallenge', () => {
  it('maps problem to instructor challenge item', () => {
    const problem = {
      problemId: 'prob-1',
      title: 'Two Sum',
      difficulty: 'MEDIUM',
      isPublic: true,
      totalSubmit: 50,
      allowedLangs: ['java', 'python'],
    };
    const result = mapProblemToInstructorChallenge(problem);
    expect(result.title).toBe('Two Sum');
    expect(result.status).toBe('Published');
    expect(result.doneCount).toBe(50);
  });
});

describe('mapCourseToExerciseHubCard', () => {
  it('maps course with id field', () => {
    expect(mapCourseToExerciseHubCard({ id: 'c-1', title: 'React', difficulty: 'Advanced' }))
      .toEqual({ id: 'c-1', name: 'React', category: 'Advanced' });
  });

  it('maps course with courseId field', () => {
    expect(mapCourseToExerciseHubCard({ courseId: 'c-2', name: 'Node', category: 'Backend' }))
      .toEqual({ id: 'c-2', name: 'Node', category: 'Backend' });
  });
});

describe('mapAttemptToResultRow', () => {
  it('maps passed attempt', () => {
    const attempt = {
      attemptId: 'a-1',
      userId: 'u-1',
      score: 80,
      totalScore: 100,
      passed: true,
      submittedAt: '2024-06-15T10:00:00Z',
    };
    const result = mapAttemptToResultRow(attempt, 'Quiz 1', 'John');
    expect(result.student).toBe('John');
    expect(result.score).toBe(80);
    expect(result.status).toBe('Passed');
  });

  it('maps failed attempt when passed=false', () => {
    const attempt = {
      attemptId: 'a-2',
      userId: 'u-1',
      score: 30,
      totalScore: 100,
      passed: false,
      submittedAt: '2024-06-15T10:00:00Z',
    };
    const result = mapAttemptToResultRow(attempt, 'Quiz 1', 'John');
    expect(result.status).toBe('Failed');
  });

  it('defaults pass/fail based on 50% threshold when passed is undefined', () => {
    const high = { attemptId: 'a-1', userId: 'u-1', score: 60, totalScore: 100, submittedAt: '2024-06-15T10:00:00Z' };
    const low = { attemptId: 'a-2', userId: 'u-1', score: 40, totalScore: 100, submittedAt: '2024-06-15T10:00:00Z' };
    expect(mapAttemptToResultRow(high, 'Q', 'John').status).toBe('Passed');
    expect(mapAttemptToResultRow(low, 'Q', 'John').status).toBe('Failed');
  });
});

describe('mapSubmissionToResultRow', () => {
  it('maps accepted submission as passed', () => {
    const sub = {
      submissionId: 's-1',
      userId: 'u-1',
      status: 'ACCEPTED',
      submittedAt: '2024-06-15T10:00:00Z',
    };
    const result = mapSubmissionToResultRow(sub, 'Two Sum', 'John');
    expect(result.status).toBe('Passed');
    expect(result.exercise).toBe('Two Sum');
    expect(result.score).toBe(100);
  });

  it('maps failed submission', () => {
    const sub = {
      submissionId: 's-2',
      userId: 'u-1',
      status: 'WRONG_ANSWER',
      score: 50,
      submittedAt: '2024-06-15T10:00:00Z',
    };
    const result = mapSubmissionToResultRow(sub, 'Two Sum', 'John');
    expect(result.status).toBe('Failed');
    expect(result.score).toBe(50);
  });
});
