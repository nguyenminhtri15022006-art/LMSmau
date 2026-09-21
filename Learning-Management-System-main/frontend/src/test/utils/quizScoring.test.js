import { describe, expect, it } from 'vitest';
import { distributeQuizQuestionScores } from '../../features/assignment/quiz/utils/quizScoring';

describe('distributeQuizQuestionScores', () => {
  it('returns the same array when there are no questions', () => {
    const questions = [];
    expect(distributeQuizQuestionScores(questions, 100)).toBe(questions);
  });

  it('distributes scores evenly and gives remainder to earlier questions', () => {
    const result = distributeQuizQuestionScores([{ id: 1 }, { id: 2 }, { id: 3 }], 10);
    expect(result.map((question) => question.score)).toEqual([4, 3, 3]);
  });

  it('normalizes invalid totals and keeps each score at least one', () => {
    expect(distributeQuizQuestionScores([{ id: 1 }, { id: 2 }], 'bad').map((q) => q.score)).toEqual([50, 50]);
    expect(distributeQuizQuestionScores([{ id: 1 }, { id: 2 }], 1).map((q) => q.score)).toEqual([1, 1]);
  });
});
