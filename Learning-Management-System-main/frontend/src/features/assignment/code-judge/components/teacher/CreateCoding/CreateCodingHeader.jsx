import React from 'react';
import { Link } from 'react-router-dom';
import '../../../styles/teacher/CreateCoding/createCodingHeader.css';
import '../../../../shared/styles/ExerciseShared.css';

export default function CreateCodingHeader({
  courseId,
  courseTitle = '',
  isEditMode = false,
  problemTitle = '',
  onSaveDraft,
  onPublish,
  saving = false,
}) {
  const exerciseListState = courseTitle ? { courseTitle } : undefined;

  return (
    <header className="create-coding-header">
      <div className="create-coding-header-top">
        <nav className="ch-breadcrumb" style={{ margin: 0 }}>
          <Link to="/instructor/exercises">Bài tập</Link>
          <span className="ch-breadcrumb-separator" style={{ margin: '0 8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
          </span>
          <Link
            to={`/instructor/exercises/${courseId}?tab=coding&page=1`}
            state={exerciseListState}
            className="ch-breadcrumb-current"
          >
            {courseTitle || 'Khóa học'}
          </Link>
          <span className="ch-breadcrumb-separator" style={{ margin: '0 8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
          </span>
          <span className="ch-breadcrumb-current">
            {isEditMode ? `Chỉnh sửa: ${problemTitle || 'Bài tập lập trình'}` : 'Tạo bài tập lập trình'}
          </span>
        </nav>
        <div className="create-coding-autosave-indicator">
          <span className="material-symbols-outlined">cloud_done</span>
          {isEditMode ? 'Dữ liệu từ hệ thống' : 'Bản nháp cục bộ'}
        </div>
      </div>

      <div className="create-coding-hero">
        <div>
          <span className="create-coding-kicker">Code judge challenge</span>
          <h1 className="create-coding-title">
            {isEditMode ? 'Chỉnh sửa bài tập lập trình' : 'Tạo bài tập lập trình'}
          </h1>
          <p className="create-coding-subtitle">Soạn đề, thiết lập môi trường chấm và chuẩn bị bộ test cho học viên.</p>
        </div>
        <div className="create-coding-hero-actions">
          <button
            type="button"
            className="create-coding-top-action create-coding-top-action--draft"
            onClick={onSaveDraft}
            disabled={saving}
          >
            <span className="material-symbols-outlined">draft</span>
            Lưu nháp
          </button>
          <button
            type="button"
            className="create-coding-top-action create-coding-top-action--publish"
            onClick={onPublish}
            disabled={saving}
          >
            <span className="material-symbols-outlined">{saving ? 'sync' : 'publish'}</span>
            {saving ? 'Đang lưu...' : 'Xuất bản'}
          </button>
        </div>
      </div>
    </header>
  );
}
