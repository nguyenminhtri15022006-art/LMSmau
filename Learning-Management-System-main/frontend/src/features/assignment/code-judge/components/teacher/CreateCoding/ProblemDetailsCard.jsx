import React from 'react';
import '../../../styles/teacher/CreateCoding/problemDetailsCard.css';

export default function ProblemDetailsCard({ problemInfo, onUpdate }) {
  const updateField = (field, value) => {
    onUpdate(prev => ({ ...prev, [field]: value }));
  };

  return (
    <div className="editor-card problem-details-card">
      <div className="create-card-heading">
        <span className="material-symbols-outlined">article</span>
        <div>
          <h2>Thông tin đề bài</h2>
          <p>Tên, yêu cầu và bối cảnh của challenge.</p>
        </div>
      </div>

      <div className="form-group">
        <label>Tên bài tập</label>
        <input
          type="text"
          className="option-input"
          placeholder="VD: Viết hàm tìm kiếm nhị phân"
          value={problemInfo.title}
          onChange={(event) => updateField('title', event.target.value)}
        />
      </div>

      <div className="form-group problem-description-group">
        <label>Mô tả bài tập</label>
        <div className="markdown-support-note">
          <span className="material-symbols-outlined">markdown</span>
          <span>Hỗ trợ định dạng Markdown: tiêu đề, danh sách, bảng, inline code và code block.</span>
        </div>
        <textarea
          className="option-input"
          rows="6"
          placeholder="Mô tả bài toán, input/output, ràng buộc và yêu cầu chấm..."
          value={problemInfo.description}
          onChange={(event) => updateField('description', event.target.value)}
        />
      </div>
    </div>
  );
}
