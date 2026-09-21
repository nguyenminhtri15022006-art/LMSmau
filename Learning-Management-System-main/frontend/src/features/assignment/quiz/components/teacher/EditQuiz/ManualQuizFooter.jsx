import React from 'react';

export default function ManualQuizFooter({ onAddQuestion, onReuseQuestions, onPublish, saving = false }) {
  return (
    <div className="quiz-footer-actions">
      <button className="btn-add-another" onClick={onAddQuestion} disabled={saving}>
        <span className="material-symbols-outlined">library_add</span>
        Thêm câu hỏi
      </button>
      <button className="btn-add-another" onClick={onReuseQuestions} disabled={saving}>
        <span className="material-symbols-outlined">content_copy</span>
        Tái sử dụng
      </button>
      <button className="btn-publish-quiz" onClick={onPublish} disabled={saving}>
        <span className="material-symbols-outlined">check_circle</span>
        {saving ? 'Đang lưu...' : 'Hoàn tất & Xuất bản'}
      </button>
    </div>
  );
}
