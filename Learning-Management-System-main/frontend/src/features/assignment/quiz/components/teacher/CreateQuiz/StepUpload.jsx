import React, { useState, useRef } from 'react';
import { assignmentApi } from '../../../../../../services/assignment.api';
import AssignmentMessageDialog from '../../../../shared/components/AssignmentMessageDialog';
import '../../../styles/teacher/CreateQuiz/stepUpload.css';

export default function StepUpload({ courseId, onImportSuccess, onManualStart }) {
  const [aiInputMode, setAiInputMode] = useState('upload');
  const [aiContent, setAiContent] = useState('');
  const [aiFile, setAiFile] = useState(null);
  const [aiQuestionCount, setAiQuestionCount] = useState(10);
  const [aiDifficulty, setAiDifficulty] = useState('MEDIUM');
  const [aiQuestionType, setAiQuestionType] = useState('SINGLE');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [aiError, setAiError] = useState(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importError, setImportError] = useState(null);
  const [templateFormat, setTemplateFormat] = useState('docx');
  const [isDownloading, setIsDownloading] = useState(false);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isAiDragActive, setIsAiDragActive] = useState(false);
  const [importDialog, setImportDialog] = useState(null);
  const [messageDialog, setMessageDialog] = useState(null);
  const fileInputRef = useRef(null);
  const aiFileInputRef = useRef(null);

  const showImportFailureDialog = (resultOrError) => {
    const data = resultOrError?.response?.data?.data ?? resultOrError?.data ?? resultOrError;
    const errors = data?.errors ?? [];
    const failedCount = data?.failedCount ?? errors.length;
    const message = resultOrError?.response?.data?.message
      || data?.message
      || resultOrError?.message
      || 'Không thể nhập file.';

    setImportDialog({
      title: 'Import thất bại',
      message: failedCount > 0
        ? `File có ${failedCount} lỗi. Vui lòng chỉnh sửa file rồi import lại.`
        : `${message} Vui lòng chỉnh sửa file rồi import lại.`,
      errors: errors.slice(0, 3),
    });
  };

  const importFile = async (file) => {
    if (!file) return;

    setIsImporting(true);
    setImportError(null);
    setImportDialog(null);
    try {
      const result = await assignmentApi.previewQuestionFile(courseId, file);
      if ((result?.failedCount ?? 0) > 0) {
        showImportFailureDialog(result);
        return;
      }
      onImportSuccess(result);
    } catch (err) {
      showImportFailureDialog(err);
      const message = err?.response?.data?.message || err?.message || 'Không thể nhập file. Vui lòng thử lại.';
      setImportError(message);
    } finally {
      setIsImporting(false);
    }
  };

  const handleFormattedUpload = async (e) => {
    const file = e.target.files[0];
    try {
      await importFile(file);
    } finally {
      e.target.value = '';
    }
  };

  const handleDropZoneClick = () => {
    if (!isImporting) {
      fileInputRef.current.click();
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isImporting) {
      setIsDragActive(true);
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
  };

  const handleDrop = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);

    if (isImporting) return;

    const file = e.dataTransfer.files?.[0];
    await importFile(file);
  };

  const handleDownloadTemplate = async () => {
    setIsDownloading(true);
    try {
      await assignmentApi.downloadQuizImportTemplate(templateFormat);
    } catch (err) {
      setMessageDialog({
        title: 'Không thể tải file mẫu',
        message: 'Vui lòng thử lại sau.',
        detail: err?.response?.data?.message || err?.message,
        tone: 'error',
      });
    } finally {
      setIsDownloading(false);
    }
  };

  const selectAiFile = (file) => {
    setAiFile(file ?? null);
    setAiError(null);
  };

  const handleAiFileChange = (e) => {
    selectAiFile(e.target.files?.[0]);
    e.target.value = '';
  };

  const handleAiDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isGeneratingAi) {
      setIsAiDragActive(true);
    }
  };

  const handleAiDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsAiDragActive(false);
  };

  const handleAiDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsAiDragActive(false);

    if (isGeneratingAi) return;

    selectAiFile(e.dataTransfer.files?.[0]);
  };

  const canGenerateAi = aiInputMode === 'upload'
    ? Boolean(aiFile)
    : Boolean(aiContent.trim());

  const handleAIGeneration = async () => {
    if (!canGenerateAi || isGeneratingAi) return;

    setIsGeneratingAi(true);
    setAiError(null);
    try {
      const result = await assignmentApi.previewAiGeneratedQuestions(courseId, {
        file: aiInputMode === 'upload' ? aiFile : null,
        content: aiInputMode === 'text' ? aiContent.trim() : '',
        questionCount: aiQuestionCount,
        difficulty: aiDifficulty,
        questionType: aiQuestionType,
        topic: 'AI Generated',
      });
      if ((result?.failedCount ?? 0) > 0) {
        showImportFailureDialog(result);
        return;
      }
      onImportSuccess(result);
    } catch (err) {
      const message = err?.response?.data?.message || err?.message || 'Không thể tạo câu hỏi bằng AI. Vui lòng thử lại.';
      setAiError(message);
    } finally {
      setIsGeneratingAi(false);
    }
  };

  return (
    <div className="step-1-upload">
      <div className="upload-options-grid">
        {/* Manual Entry Card */}
        <button
          type="button"
          className="upload-card manual-entry-card"
          onClick={onManualStart}
        >
          <div className="card-icon green">
            <span className="material-symbols-outlined">edit_note</span>
          </div>
          <h3>Nhập thủ công</h3>
          <p>Tạo câu hỏi trực tiếp trong màn kiểm tra. Phù hợp khi bạn muốn tự soạn từng câu, thêm đáp án, ảnh minh họa và giải thích mà không cần tải file.</p>
          <div className="manual-entry-preview" aria-hidden="true">
            <div className="manual-preview-line strong"></div>
            <div className="manual-preview-options">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          <ul className="manual-entry-benefits">
            <li>
              <span className="material-symbols-outlined">playlist_add</span>
              Thêm từng câu hỏi khi cần
            </li>
            <li>
              <span className="material-symbols-outlined">rule</span>
              Chọn một đáp án, nhiều đáp án hoặc đúng/sai
            </li>
            <li>
              <span className="material-symbols-outlined">image</span>
              Gắn ảnh, nhãn và giải thích đáp án
            </li>
          </ul>
          <span className="manual-entry-action">
            Bắt đầu soạn
            <span className="material-symbols-outlined">arrow_forward</span>
          </span>
        </button>

        {/* Upload Formatted Document Card */}
        <div className="upload-card">
          <div className="card-icon blue">
            <span className="material-symbols-outlined">upload_file</span>
          </div>
          <h3>Tải lên</h3>
          <p>Tải lên tệp tài liệu Word (.docx), văn bản (.txt) hoặc Excel (.csv) được định dạng với câu hỏi và câu trả lời (sử dụng * cho câu trả lời đúng trong file .txt/.docx).</p>

          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFormattedUpload}
            style={{ display: 'none' }}
            accept=".docx,.txt,.csv"
          />

          <div
            className={`drop-zone ${isDragActive ? 'drag-active' : ''}`}
            onClick={handleDropZoneClick}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <span className="material-symbols-outlined">cloud_upload</span>
            <span>Kéo và thả tệp vào đây hoặc</span>
            <button type="button" className="btn-outline" disabled={isImporting}>Duyệt tệp</button>
          </div>

          {importError && (
            <div className="import-error">{importError}</div>
          )}

          <div className="format-hint">
            <span className="material-symbols-outlined">info</span>
            <label className="format-label">Định dạng:</label>
            <select
              className="format-select"
              value={templateFormat}
              onChange={(e) => setTemplateFormat(e.target.value)}
            >
              <option value="docx">.docx (Word)</option>
              <option value="txt">.txt (Văn bản)</option>
              <option value="csv">.csv (Excel)</option>
            </select>
            <button
              className="btn-download-template"
              onClick={handleDownloadTemplate}
              disabled={isDownloading}
            >
              {isDownloading ? (
                <>
                  <span className="material-symbols-outlined rotating">autorenew</span>
                  Đang tải...
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">download</span>
                  Tải mẫu
                </>
              )}
            </button>
          </div>
        </div>

        {/* AI Generation Card */}
        <div className="upload-card ai-card">
          <div className="card-icon purple">
            <span className="material-symbols-outlined">auto_awesome</span>
          </div>
          <h3>Tạo với AI</h3>
          <p>Để AI của chúng tôi tạo câu hỏi trắc nghiệm dựa trên tài liệu khóa học hoặc giáo trình của bạn.</p>

          <div className="ai-tabs">
            <button
              className={`ai-tab ${aiInputMode === 'upload' ? 'active' : ''}`}
              onClick={() => setAiInputMode('upload')}
            >
              Tải lên tài liệu
            </button>
            <button
              className={`ai-tab ${aiInputMode === 'text' ? 'active' : ''}`}
              onClick={() => setAiInputMode('text')}
            >
              Dán nội dung
            </button>
          </div>

          <div className="ai-input-area">
            {aiInputMode === 'upload' ? (
              <>
                <input
                  type="file"
                  ref={aiFileInputRef}
                  onChange={handleAiFileChange}
                  style={{ display: 'none' }}
                  accept=".txt,.docx,.csv"
                />
                <div
                  className={`drop-zone sm ${isAiDragActive ? 'drag-active' : ''}`}
                  onClick={() => {
                    if (!isGeneratingAi) {
                      aiFileInputRef.current.click();
                    }
                  }}
                  onDragOver={handleAiDragOver}
                  onDragLeave={handleAiDragLeave}
                  onDrop={handleAiDrop}
                >
                  <span className="material-symbols-outlined">document_scanner</span>
                  <span>{aiFile ? aiFile.name : 'Tải lên TXT, Word hoặc CSV'}</span>
                  <button type="button" className="btn-outline sm-btn" disabled={isGeneratingAi}>Duyệt</button>
                </div>
              </>
            ) : (
              <textarea
                value={aiContent}
                onChange={(event) => {
                  setAiContent(event.target.value);
                  setAiError(null);
                }}
                maxLength={10000}
                placeholder="Dán nội dung khóa học, giáo trình hoặc tài liệu đọc của bạn vào đây..."
              ></textarea>
            )}

            <div className="ai-settings">
              <select
                className="form-input"
                value={aiQuestionCount}
                onChange={(event) => setAiQuestionCount(Number(event.target.value))}
              >
                <option value={10}>10 câu hỏi</option>
                <option value={20}>20 câu hỏi</option>
                <option value={30}>30 câu hỏi</option>
              </select>
              <select
                className="form-input"
                value={aiDifficulty}
                onChange={(event) => setAiDifficulty(event.target.value)}
              >
                <option value="MEDIUM">Trung bình</option>
                <option value="EASY">Dễ</option>
                <option value="HARD">Khó</option>
              </select>
              <select
                className="form-input ai-setting-wide"
                value={aiQuestionType}
                onChange={(event) => setAiQuestionType(event.target.value)}
              >
                <option value="SINGLE">Một đáp án</option>
                <option value="MULTIPLE">Nhiều đáp án</option>
                <option value="TRUE_FALSE">Đúng / Sai</option>
              </select>
            </div>

            {aiError && (
              <div className="import-error">{aiError}</div>
            )}

            <button
              className="btn-ai-generate"
              onClick={handleAIGeneration}
              disabled={!canGenerateAi || isGeneratingAi}
            >
              <span className={`material-symbols-outlined ${isGeneratingAi ? 'rotating' : ''}`}>
                {isGeneratingAi ? 'autorenew' : 'magic_button'}
              </span>
              {isGeneratingAi ? 'Đang tạo...' : 'Tạo câu hỏi'}
            </button>
          </div>
        </div>
      </div>

      {importDialog && (
        <div className="import-dialog-backdrop" role="dialog" aria-modal="true">
          <div className="import-dialog">
            <div className="import-dialog__icon">
              <span className="material-symbols-outlined">error</span>
            </div>
            <h4>{importDialog.title}</h4>
            <p>{importDialog.message}</p>
            {importDialog.errors.length > 0 && (
              <ul>
                {importDialog.errors.map((error, index) => (
                  <li key={`${error.rowNumber || index}-${index}`}>
                    Dòng {error.rowNumber || '?'}: {error.message}
                  </li>
                ))}
              </ul>
            )}
            <button type="button" className="btn-primary" onClick={() => setImportDialog(null)}>
              Đã hiểu
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isImporting && (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>Đang kiểm tra câu hỏi...</p>
        </div>
      )}

      {isGeneratingAi && (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>AI đang tạo câu hỏi...</p>
        </div>
      )}

      <AssignmentMessageDialog
        open={!!messageDialog}
        title={messageDialog?.title}
        message={messageDialog?.message}
        detail={messageDialog?.detail}
        tone={messageDialog?.tone}
        onClose={() => setMessageDialog(null)}
      />
    </div>
  );
}
