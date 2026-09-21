import React from 'react';
import '../../../styles/teacher/ManualQuiz/sidebarQuestionList.css';

const TYPE_SHORT = {
  SINGLE: 'S',
  MULTIPLE: 'M',
  TRUE_FALSE: 'T/F',
};

export default function SidebarQuestionList({
  questions,
  activeQuestionId,
  onSelectQuestion,
  onAddQuestion,
  onReuseQuestions,
}) {
  return (
    <aside className="quiz-sidebar">
      <button className="btn-add-question" onClick={onAddQuestion}>
        <span className="material-symbols-outlined">add</span>
        Thêm câu hỏi
      </button>
      <button className="btn-reuse-question" onClick={onReuseQuestions}>
        <span className="material-symbols-outlined">content_copy</span>
        Tái sử dụng
      </button>

      <div className="question-list-section">
        <div className="question-list-heading">
          <h4 className="question-list-title">Danh sách câu hỏi</h4>
          <span className="question-count">{questions.length}</span>
        </div>
        <div className="question-nav-list" aria-label="Danh sách câu hỏi">
          {questions.map((q, index) => (
            <button
              type="button"
              key={q.id}
              className={`question-nav-item ${activeQuestionId === q.id ? 'active' : ''}`}
              onClick={() => onSelectQuestion(q.id)}
              title={`${q.text || `Câu hỏi ${index + 1}`} - ${q.score || 10} điểm`}
            >
              <span className="q-number">{index + 1}</span>
              <span className="q-meta">
                <span>{TYPE_SHORT[q.type] || 'S'}</span>
                <span>{q.score || 10}đ</span>
              </span>
            </button>
          ))}
        </div>
      </div>
    </aside>
  );
}
