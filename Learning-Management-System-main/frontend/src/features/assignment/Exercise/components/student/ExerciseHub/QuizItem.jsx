import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../../../styles/student/ExerciseHub/QuizItem.css';

const QuizItem = ({ quiz, courseTitle, courseSlug }) => {
  const navigate = useNavigate();

  return (
    <div
      className={`quiz-item ${quiz.completed ? 'is-completed' : ''}`}
      onClick={() => navigate(`/exercises/quiz/${quiz.id}/overview`, {
        state: { courseTitle, courseSlug }
      })}
      style={{ cursor: 'pointer' }}
    >
      <div className="quiz-info-group">
        <div className="quiz-icon-container">
          <span className="material-symbols-outlined">{quiz.icon}</span>
        </div>
        <div className="quiz-name">
          <h4>{quiz.title}</h4>
          <p className="quiz-meta">
            {quiz.questionsCount} câu hỏi • {quiz.level === 'EASY' ? 'Dễ' : quiz.level === 'MEDIUM' ? 'Trung bình' : 'Khó'}
          </p>
        </div>
      </div>
      <span className="material-symbols-outlined quiz-arrow">chevron_right</span>
      {quiz.completed && (
        <span className="completion-check-icon material-symbols-outlined" title="Đã hoàn thành" aria-label="Đã hoàn thành">
          check_circle
        </span>
      )}
    </div>
  );
};

export default QuizItem;
