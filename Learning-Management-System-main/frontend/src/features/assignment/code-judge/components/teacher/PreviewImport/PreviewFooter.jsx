import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../../../styles/teacher/PreviewImport/previewFooter.css';

export default function PreviewFooter({ courseId }) {
  const navigate = useNavigate();

  return (
    <div className="preview-footer-actions">
      <button className="btn-preview-data" onClick={() => navigate(-1)}>Trở về</button>
      <div className="preview-footer-right">
        <button className="btn-preview-data preview-save-button">Lưu thay đổi</button>
        <button className="btn-import-now" onClick={() => navigate(`/instructor/exercises/${courseId}`)}>Tạo bài tập</button>
      </div>
    </div>
  );
}
