import React from 'react';
import '../../../styles/teacher/ImportTestCases/fileStatusCard.css';

function formatFileSize(bytes = 0) {
    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

export default function FileStatusCard({
                                           file,
                                           onRemoveFile,
                                           importResult,
                                           onImport,
                                           importing = false,
                                           canImport = true,
                                       }) {
    if (!file) return null;

    const errors = importResult?.errors || [];

    return (
        <div className="file-status-card">
            <div className="file-info-header">
                <span className="material-symbols-outlined file-icon">description</span>
                <div className="file-details">
                    <h4>{file.name}</h4>
                    <span>{formatFileSize(file.size)}</span>
                </div>
                <button className="btn-delete-option file-delete-button" onClick={onRemoveFile} type="button">
                    <span className="material-symbols-outlined">delete</span>
                </button>
            </div>

            <div className="validation-results">
                {errors.length > 0 ? (
                    <div className="validation-alert validation-alert-error">
                        <div className="alert-content">
                            <span className="material-symbols-outlined alert-icon">error</span>
                            <div className="validation-message">
                                <strong>File chưa hợp lệ</strong>
                                <span>
                  {errors.length} lỗi cần sửa. Chưa có test case nào được lưu.
                </span>
                            </div>
                        </div>
                    </div>
                ) : (
                    <div className="validation-alert validation-alert-ready">
                        <div className="alert-content">
                            <span className="material-symbols-outlined alert-icon">check_circle</span>
                            <div className="validation-message">
                                <strong>File đã sẵn sàng</strong>
                                <span>Hệ thống sẽ kiểm tra toàn bộ testcase trước khi lưu.</span>
                            </div>
                        </div>
                    </div>
                )}

                {errors.length > 0 && (
                    <div className="import-error-list">
                        {errors.map((error, index) => (
                            <div className="import-error-item"
                                 key={`${error.rowNumber || index}-${error.field || index}`}>
                                <span>Dòng {error.rowNumber || '-'}</span>
                                <strong>{error.field || 'file'}</strong>
                                <p>{error.reason || 'Dữ liệu không hợp lệ'}</p>
                            </div>
                        ))}
                    </div>
                )}

                <div className="action-buttons">
                    <button
                        className="btn-import-now"
                        onClick={onImport}
                        disabled={importing || !canImport}
                        type="button"
                    >
            <span className={`material-symbols-outlined ${importing ? 'spin-anim' : ''}`}>
              {importing ? 'sync' : 'publish'}
            </span>
                        {importing ? 'Đang nhập...' : 'Nhập test cases'}
                    </button>
                    <button className="btn-preview-data" onClick={onRemoveFile} disabled={importing} type="button">
                        <span className="material-symbols-outlined">close</span>
                        Chọn file khác
                    </button>
                </div>
            </div>
        </div>
    );
}
