import React, { useRef, useState } from 'react';
import '../../../styles/teacher/ImportTestCases/uploadCard.css';

export default function UploadCard({
  onFileChange,
  templateFormat,
  onTemplateFormatChange,
  onDownloadTemplate,
  isDownloadingTemplate = false,
  hasFile = false,
}) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);

  const handleDrop = (event) => {
    event.preventDefault();
    setDragging(false);
    const droppedFile = event.dataTransfer.files?.[0];
    if (droppedFile) {
      onFileChange(droppedFile);
    }
  };

  return (
    <div className={`import-card ${hasFile ? 'import-card--compact' : ''}`}>
      <div
        className={`import-drop-zone ${dragging ? 'import-drop-zone-active' : ''}`}
        onClick={() => inputRef.current?.click()}
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
      >
        <div className="upload-icon-circle">
          <span className="material-symbols-outlined">cloud_upload</span>
        </div>
        <div className="import-drop-copy">
          <h3>Thả file vào đây</h3>
          <p>Hỗ trợ .xlsx, .csv, tối đa 10MB</p>
        </div>
        <button className="btn-browse" type="button">
          Chọn file
        </button>
        <input
          ref={inputRef}
          type="file"
          hidden
          accept=".xlsx,.csv"
          onChange={onFileChange}
        />
      </div>

      <div className="template-download-row">
        <div className="template-format-field">
          <span>File mẫu</span>
          <select
            className="template-format-select"
            value={templateFormat}
            onChange={(event) => onTemplateFormatChange(event.target.value)}
          >
            <option value="xlsx">Excel (.xlsx)</option>
            <option value="csv">CSV (.csv)</option>
          </select>
        </div>
        <button
          type="button"
          className="template-link"
          onClick={onDownloadTemplate}
          disabled={isDownloadingTemplate}
        >
          <span className="material-symbols-outlined">download</span>
          {isDownloadingTemplate ? 'Đang tải...' : 'Tải file mẫu'}
        </button>
      </div>
    </div>
  );
}
