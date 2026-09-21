import React from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import '../../../styles/teacher/InstructorExerciseDetail/createExerciseModal.css';

export default function CreateExerciseModal({ isOpen, onClose, courseId, courseName }) {
  const navigate = useNavigate();

  if (!isOpen) return null;
  if (typeof document === 'undefined') return null;

  return createPortal(
    <div className="exercise-create-modal-overlay" onClick={onClose}>
      <div className="exercise-create-modal" onClick={e => e.stopPropagation()}>
        <h2 className="exercise-create-modal-title">Chọn loại bài tập</h2>
        <p className="exercise-create-modal-subtitle">Chọn loại bài tập bạn muốn thêm vào khóa học.</p>

        <div className="exercise-create-options">
          <div className="exercise-create-type-card">
            <div className="exercise-create-type-icon">
              <span className="material-symbols-outlined">quiz</span>
            </div>
            <div className="exercise-create-type-info">
              <h3>Bài tập trắc nghiệm</h3>
              <p>Câu hỏi trắc nghiệm, đúng/sai, hoặc câu trả lời đơn để kiểm tra kiến thức.</p>
              <button
                className="exercise-create-select-btn"
                onClick={() => navigate(`/instructor/exercises/${courseId}/create-quiz`, { state: { courseTitle: courseName } })}
              >
                Chọn
              </button>
            </div>
          </div>

          <div className="exercise-create-type-card">
            <div className="exercise-create-type-icon">
              <span className="material-symbols-outlined">code</span>
            </div>
            <div className="exercise-create-type-info">
              <h3>Bài tập code</h3>
              <p>Trình soạn thảo code tương tác với kiểm tra tự động cho các bài đánh giá lập trình.</p>
              <button
                className="exercise-create-select-btn"
                onClick={() => navigate(`/instructor/exercises/${courseId}/create-coding`, { state: { courseTitle: courseName } })}
              >
                Chọn
              </button>
            </div>
          </div>
        </div>

        <button className="exercise-create-cancel-btn" onClick={onClose}>Hủy</button>
      </div>
    </div>,
    document.body
  );
}
