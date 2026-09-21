import React from 'react';
import '../../styles/student/ExerciseQuiz/QuizTimer.css';

const QuizTimer = ({ timeLeft }) => {
  const formatTime = (seconds) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec < 10 ? '0' : ''}${sec}`;
  };

  let timeClass = '';
  if (timeLeft <= 15) {
    timeClass = 'danger-time';
  } else if (timeLeft <= 60) {
    timeClass = 'warning-time';
  }

  return (
    <div className={`timer-badge ${timeClass}`}>
      <span className="material-symbols-outlined">schedule</span>
      {formatTime(timeLeft)}
    </div>
  );
};

export default QuizTimer;
