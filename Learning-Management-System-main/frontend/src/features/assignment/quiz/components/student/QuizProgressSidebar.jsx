import React from 'react';
import '../../styles/student/ExerciseQuiz/QuizProgressSidebar.css';

const QuizProgressSidebar = ({
  questions,
  currentIdx,
  answers,
  markedForReview,
  onQuestionSelect,
  onToggleReview,
  isPracticeMode = false,
}) => {
  return (
    <aside className="quiz-sidebar">
      <div className={`progress-card ${isPracticeMode ? 'progress-card--locked' : ''}`}>
        <h3 className="progress-header">Tiến độ bài kiểm tra</h3>
        <div className="progress-grid">
          {questions.map((_, i) => {
            let statusClass = '';
            if (i === currentIdx) statusClass = 'active';
            else if (markedForReview.has(i)) statusClass = 'review';
            else if (answers[i] !== undefined) statusClass = 'answered';

            return (
              <div
                key={i}
                className={`progress-num ${statusClass}`}
                role="button"
                aria-disabled={isPracticeMode}
                onClick={() => {
                  if (!isPracticeMode) onQuestionSelect(i);
                }}
              >
                {i + 1}
              </div>
            );
          })}
        </div>
      </div>

      <div className="tip-card">
        <div className="tip-header">
          <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
            tips_and_updates
          </span>
          PRO TIP
        </div>

        {isPracticeMode ? (
          <div className="tip-shortcuts">
            <p className="tip-content">Dùng phím tắt để luyện nhanh hơn.</p>
            <div className="shortcut-row">
              <kbd>1</kbd><kbd>2</kbd><kbd>3</kbd><kbd>4</kbd>
              <span>Chọn A-D</span>
            </div>
            <div className="shortcut-row">
              <kbd className="shortcut-wide">Enter</kbd>
              <span>Kiểm tra / câu tiếp theo</span>
            </div>
          </div>
        ) : (
          <p className="tip-content">
            Sử dụng lưới tiến độ để di chuyển nhanh giữa các câu hỏi. Đánh dấu câu hỏi bạn muốn xem lại sau.
          </p>
        )}
      </div>
    </aside>
  );
};

export default QuizProgressSidebar;
