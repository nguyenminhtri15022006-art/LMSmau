import React from 'react';
import '../../../styles/teacher/PreviewImport/previewToolbar.css';

export default function PreviewToolbar() {
  return (
    <div className="preview-toolbar">
      <span className="status-badge valid">
        <span className="material-symbols-outlined">check_circle</span>
        12 Hợp lệ
      </span>
      <span className="status-badge error">
        <span className="material-symbols-outlined">error</span>
        3 Lỗi
      </span>
      <span className="preview-total">Tổng số test case: 15</span>
    </div>
  );
}
