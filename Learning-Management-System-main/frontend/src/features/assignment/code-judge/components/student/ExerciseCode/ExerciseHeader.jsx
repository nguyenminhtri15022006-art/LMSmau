import React from 'react';

const DIFFICULTY_CONFIG = {
  EASY: { label: 'Dễ', color: '#22c55e', bg: 'rgba(34, 197, 94, 0.1)' },
  MEDIUM: { label: 'Trung bình', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.1)' },
  HARD: { label: 'Khó', color: '#ef4444', bg: 'rgba(239, 68, 68, 0.1)' },
};

export default function ExerciseHeader({ challenge }) {
  const diff = DIFFICULTY_CONFIG[challenge.difficulty] || DIFFICULTY_CONFIG.MEDIUM;

  return (
    <header className="exercise-detail-header">
      <div className="exercise-detail-title-group">
        <h2>{challenge.title}</h2>
      </div>

      <div className="exercise-detail-meta">
        <span
          className={`meta-badge difficulty-${(challenge.difficulty || 'MEDIUM').toLowerCase()}`}
          style={{
            backgroundColor: diff.bg,
            color: diff.color,
            border: `1px solid ${diff.color}30`,
          }}
        >
          {diff.label}
        </span>
        {challenge.score != null && (
          <span className="meta-score">
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>stars</span>
            {challenge.score} điểm
          </span>
        )}
      </div>
    </header>
  );
}
