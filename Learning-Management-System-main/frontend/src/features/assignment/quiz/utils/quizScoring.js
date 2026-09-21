export function distributeQuizQuestionScores(questions = [], totalScore = 100) {
  const count = questions.length;
  const normalizedTotal = Math.max(1, Math.round(Number(totalScore) || 100));

  if (count === 0) return questions;

  const baseScore = Math.max(1, Math.floor(normalizedTotal / count));
  let remaining = Math.max(0, normalizedTotal - baseScore * count);

  return questions.map((question) => {
    const extra = remaining > 0 ? 1 : 0;
    if (remaining > 0) remaining -= 1;
    return {
      ...question,
      score: baseScore + extra,
    };
  });
}
