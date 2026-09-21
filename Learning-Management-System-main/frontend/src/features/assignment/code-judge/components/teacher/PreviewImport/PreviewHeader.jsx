import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../../../styles/teacher/PreviewImport/previewHeader.css';

export default function PreviewHeader() {
  const navigate = useNavigate();

  return (
    <>
      <button className="code-judge-nav-button" onClick={() => navigate(-1)}>
        <span className="material-symbols-outlined">chevron_left</span>
        Trở về nhập
      </button>

      <div className="code-judge-page-header">
        <h1>Xem trước và chỉnh sửa dữ liệu</h1>
        <p>Xem trước và chỉnh sửa bộ test cases trước khi tạo bài tập</p>
      </div>
    </>
  );
}
