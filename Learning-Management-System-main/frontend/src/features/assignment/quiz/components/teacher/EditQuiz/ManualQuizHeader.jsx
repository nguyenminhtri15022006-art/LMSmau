import React from 'react';
import {Link} from 'react-router-dom';
import '../../../../shared/styles/ExerciseShared.css';

export default function ManualQuizHeader({
                                             courseId,
                                             courseTitle = '',
                                             quizTitle = '',
                                             isEditMode = false,
                                             pageTitle,
                                             onSave,
                                             onPublish,
                                             saving,
                                         }) {
    return (
        <div className="quiz-header-info">
            <div className="header-top-bar">
                <div className="header-left">
                    <nav className="ch-breadcrumb" style={{margin: 0}}>
                        <Link to="/instructor/exercises">Bài tập</Link>
                        <span className="ch-breadcrumb-separator" style={{margin: '0 8px'}}>
              <span className="material-symbols-outlined" style={{fontSize: '16px'}}>chevron_right</span>
            </span>
                        <Link
                            to={`/instructor/exercises/${courseId}`}
                            className="ch-breadcrumb-current"
                        >
                            {courseTitle || 'Khóa học'}
                        </Link>
                        <span className="ch-breadcrumb-separator" style={{margin: '0 8px'}}>
              <span className="material-symbols-outlined" style={{fontSize: '16px'}}>chevron_right</span>
            </span>
                        <span className="ch-breadcrumb-current">
              {isEditMode ? `Chỉnh sửa: ${quizTitle || 'Chỉnh sửa bài kiểm tra'}` : 'Tạo bài tập trắc nghiệm'}
            </span>
                    </nav>
                </div>
                <div className="header-right">
                    <button
                        className="btn-save-changes"
                        onClick={onSave}
                        disabled={saving}
                    >
                        <span className="material-symbols-outlined">save</span>
                        Lưu tất cả thay đổi
                    </button>
                    <button
                        className="btn-publish-header"
                        onClick={onPublish}
                        disabled={saving}
                    >
                        <span className="material-symbols-outlined">check_circle</span>
                        Xuất bản
                    </button>
                </div>
            </div>
            <h1>{pageTitle}</h1>
            <p>Thiết kế một bài kiểm tra mới cho học sinh của bạn. Tất cả các thay đổi đều được lưu tự động.</p>
        </div>
    );
}
