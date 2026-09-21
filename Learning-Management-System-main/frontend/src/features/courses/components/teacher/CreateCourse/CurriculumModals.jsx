import React, { useState, useEffect, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faTimes, faLayerGroup, faFolderOpen,
    faVideo, faBook,
    faFileAlt, faCheckCircle, faTimesCircle,
    faSpinner,
} from '@fortawesome/free-solid-svg-icons';
import { courseApi } from '../../../../../services/course.api';
import apiClient from '../../../../../services/api.client';
import { ENDPOINTS } from '../../../../../constants/endpoints';
import '../../../styles/teacher/CreateCourse/curriculumModals.css';

const MULTIPART_THRESHOLD = 20 * 1024 * 1024;  // 20 MB
const CHUNK_SIZE          = 10 * 1024 * 1024;  // 10 MB per part

/* ── Shared modal shell ── */
const Modal = ({ isOpen, onClose, title, icon, children, onSubmit, submitLabel, submitDisabled }) => {
    if (!isOpen) return null;

    const handleKeyDown = (e) => {
        if (e.key === 'Escape') onClose();
    };

    return (
        <div className="modal-overlay" onKeyDown={handleKeyDown}>
            <div className="modal-content">
                <div className="modal-header">
                    <div className="modal-title-row">
                        {icon && <FontAwesomeIcon icon={icon} className="modal-title-icon" />}
                        <h2>{title}</h2>
                    </div>
                    <button className="close-btn" onClick={onClose}>
                        <FontAwesomeIcon icon={faTimes} />
                    </button>
                </div>
                <div className="modal-body">{children}</div>
                <div className="modal-footer">
                    <button className="btn-modal-cancel" onClick={onClose}>Hủy</button>
                    <button className="btn-modal-submit" onClick={onSubmit} disabled={submitDisabled}>
                        {submitLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};

/* ── Chapter modal (add / edit) ── */
export const ChapterSettingsModal = ({ isOpen, onClose, chapterData, onSave }) => {
    const [title, setTitle] = useState('');

    useEffect(() => {
        if (isOpen) setTitle(chapterData?.title || '');
    }, [isOpen, chapterData]);

    const handleSubmit = () => {
        if (!title.trim()) return;
        onSave(title.trim());
        onClose();
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={chapterData ? 'Chỉnh sửa chương' : 'Thêm chương mới'}
            icon={faLayerGroup}
            onSubmit={handleSubmit}
            submitLabel={chapterData ? 'Lưu thay đổi' : 'Thêm chương'}
        >
            <div className="form-field">
                <label>Tên chương</label>
                <input
                    autoFocus
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                    placeholder="VD: Giới thiệu về thiết kế UI/UX"
                />
            </div>
        </Modal>
    );
};

/* ── Section modal (add / edit) ── */
export const AddSectionModal = ({ isOpen, onClose, sectionData, onSave }) => {
    const [title, setTitle] = useState('');

    useEffect(() => {
        if (isOpen) setTitle(sectionData?.title || '');
    }, [isOpen, sectionData]);

    const handleSubmit = () => {
        if (!title.trim()) return;
        onSave(title.trim());
        onClose();
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={sectionData ? 'Chỉnh sửa mục' : 'Thêm mục mới'}
            icon={faFolderOpen}
            onSubmit={handleSubmit}
            submitLabel={sectionData ? 'Lưu thay đổi' : 'Thêm mục'}
        >
            <div className="form-field">
                <label>Tên mục</label>
                <input
                    autoFocus
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                    placeholder="VD: Các nguyên tắc cốt lõi"
                />
            </div>
        </Modal>
    );
};

/* ── Content type config ── */
const CONTENT_TYPES = [
    {
        key: 'video',
        label: 'Video',
        icon: faVideo,
        accept: '.mp4,.mov,.webm',
        folder: 'lessons/videos',
        allowedExtensions: ['mp4', 'mov', 'webm'],
        hint: 'MP4, MOV, WebM',
        validationLabel: 'File video',
    },
    {
        key: 'document',
        label: 'Tài liệu',
        icon: faBook,
        accept: '.pdf,.doc,.docx,.ppt,.pptx,.xlsx',
        folder: 'lessons/documents',
        allowedExtensions: ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xlsx'],
        hint: 'PDF, DOC, DOCX, PPT, PPTX, XLSX',
        validationLabel: 'Tài liệu',
    },
];

const getFileExtension = (fileName = '') => {
    const lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex <= 0 || lastDotIndex === fileName.length - 1) return '';
    return fileName.slice(lastDotIndex + 1).toLowerCase();
};

const formatExtensions = (extensions = []) => extensions.map((ext) => ext.toUpperCase()).join(', ');

export const validateLessonFileExtension = (file, contentType) => {
    const typeConfig = CONTENT_TYPES.find(t => t.key === contentType);
    if (!typeConfig || !file) return null;

    const extension = getFileExtension(file.name);
    if (typeConfig.allowedExtensions.includes(extension)) return null;

    return `${typeConfig.validationLabel} chỉ hỗ trợ ${formatExtensions(typeConfig.allowedExtensions)}.`;
};

const EMPTY_FILES_BY_TYPE = {
    video: { fileUrl: null, fileType: null },
    document: { fileUrl: null, fileType: null },
};

const createFilesByType = (partData) => {
    const filesByType = {
        video: { ...EMPTY_FILES_BY_TYPE.video },
        document: { ...EMPTY_FILES_BY_TYPE.document },
    };

    const initialType = partData?.contentType || 'video';
    if (partData?.fileUrl && filesByType[initialType]) {
        filesByType[initialType] = {
            fileUrl: partData.fileUrl,
            fileType: partData.fileType || null,
        };
    }

    return filesByType;
};

/* ── File upload area ── */
const FileUpload = ({ contentType, existingUrl, onUploaded, onRemoved, onUploadingChange }) => {
    const [uploadState, setUploadState] = useState('idle'); // idle | uploading | done | error
    const [progress, setProgress] = useState(0);
    const [fileName, setFileName] = useState('');
    const [fileUrl, setFileUrl] = useState(existingUrl || null);
    const [uploadError, setUploadError] = useState('');
    const fileInputRef = useRef(null);
    const xhrRef = useRef(null);
    const cancelledRef = useRef(false); // true khi user chủ động hủy hoặc component unmount

    const typeConfig = CONTENT_TYPES.find(t => t.key === contentType);

    // Abort XHR khi component unmount (modal đóng trong lúc đang upload)
    useEffect(() => {
        return () => {
            if (xhrRef.current) {
                cancelledRef.current = true;
                xhrRef.current.abort();
            }
        };
    }, []);

    useEffect(() => {
        // Reset when content type changes
        setUploadState(existingUrl ? 'done' : 'idle');
        setFileUrl(existingUrl || null);
        setFileName(existingUrl ? existingUrl.split('/').pop() : '');
        setProgress(0);
        setUploadError('');
    }, [contentType, existingUrl]);

    // Upload một chunk, không cần capture ETag
    const uploadChunk = (presignedUrl, chunk, fileType) =>
        new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhrRef.current = xhr;
            xhr.timeout = 60000; // 60s per 10MB chunk là quá đủ
            xhr.onload    = () => xhr.status < 300 ? resolve() : reject(new Error(`Chunk upload failed: ${xhr.status}`));
            xhr.onerror   = () => reject(new Error('Network error'));
            xhr.onabort   = () => reject(new Error('aborted'));
            xhr.ontimeout = () => reject(new Error('Chunk upload timed out'));
            xhr.open('PUT', presignedUrl);
            xhr.setRequestHeader('Content-Type', fileType || 'application/octet-stream');
            xhr.send(chunk);
        });

    const handleFileChange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const validationError = validateLessonFileExtension(file, contentType);
        if (validationError) {
            setFileName(file.name);
            setUploadError(validationError);
            setUploadState('error');
            setProgress(0);
            onUploadingChange?.(false);
            if (fileInputRef.current) fileInputRef.current.value = '';
            return;
        }

        cancelledRef.current = false;
        setFileName(file.name);
        setUploadError('');
        setUploadState('uploading');
        setProgress(0);
        onUploadingChange?.(true);

        try {
            let publicUrl;

            if (file.size > MULTIPART_THRESHOLD) {
                // ── Multipart upload cho file lớn ──
                const partCount = Math.ceil(file.size / CHUNK_SIZE);

                const { data: { data: initData } } = await apiClient.post(ENDPOINTS.UPLOAD.MULTIPART_INITIATE, {
                    fileName: file.name,
                    fileType: file.type,
                    folder: typeConfig.folder,
                    partCount,
                });
                const { uploadId, objectName, presignedUrls } = initData;

                if (cancelledRef.current) return;

                for (let i = 0; i < partCount; i++) {
                    if (cancelledRef.current) return;

                    const start = i * CHUNK_SIZE;
                    const chunk = file.slice(start, start + CHUNK_SIZE);
                    await uploadChunk(presignedUrls[i], chunk, file.type);
                    setProgress(Math.round(((i + 1) / partCount) * 100));
                }

                if (cancelledRef.current) return;

                const { data: { data: completeData } } = await apiClient.post(ENDPOINTS.UPLOAD.MULTIPART_COMPLETE, {
                    uploadId,
                    objectName,
                    chunkCount: partCount,
                });
                publicUrl = completeData.publicUrl;
            } else {
                // ── Single PUT cho file nhỏ (≤ 20 MB) ──
                const { presignedUrl, publicUrl: singlePublicUrl } = await courseApi.getPresignedUrl(
                    file.name, file.type, typeConfig.folder
                );
                publicUrl = singlePublicUrl;

                await new Promise((resolve, reject) => {
                    const xhr = new XMLHttpRequest();
                    xhrRef.current = xhr;
                    xhr.timeout = 120000; // 2 phút là quá đủ cho ≤ 20 MB
                    xhr.upload.onprogress = (ev) => {
                        if (ev.lengthComputable) setProgress(Math.round((ev.loaded / ev.total) * 100));
                    };
                    xhr.onload    = () => xhr.status < 300 ? resolve() : reject(new Error(`Upload failed: ${xhr.status}`));
                    xhr.onerror   = () => reject(new Error('Network error'));
                    xhr.onabort   = () => reject(new Error('aborted'));
                    xhr.ontimeout = () => reject(new Error('Upload timed out'));
                    xhr.open('PUT', presignedUrl);
                    xhr.setRequestHeader('Content-Type', file.type);
                    xhr.send(file);
                });
            }

            if (cancelledRef.current) return;

            setFileUrl(publicUrl);
            setUploadState('done');
            onUploadingChange?.(false);
            onUploaded?.(publicUrl, file.type);
        } catch (err) {
            if (cancelledRef.current) return;
            console.error('Upload error:', err);
            setUploadError('Tải lên thất bại. Vui lòng thử lại.');
            setUploadState('error');
            onUploadingChange?.(false);
        }
    };

    // Hủy upload đang chạy (nhấn "Hủy" trên thanh tiến trình)
    const handleRemove = () => {
        cancelledRef.current = true;
        if (xhrRef.current) xhrRef.current.abort();
        onUploadingChange?.(false);
        onRemoved?.(fileUrl);
        setFileUrl(null);
        setFileName('');
        setUploadError('');
        setUploadState('idle');
        setProgress(0);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    if (uploadState === 'done' && fileUrl) {
        return (
            <div className="upload-done-row">
                <FontAwesomeIcon icon={faCheckCircle} className="upload-done-icon" />
                <span className="upload-done-name" title={fileName}>{fileName || fileUrl.split('/').pop()}</span>
                <button type="button" className="upload-remove-btn" onClick={handleRemove} title="Xóa file">
                    <FontAwesomeIcon icon={faTimesCircle} />
                </button>
            </div>
        );
    }

    if (uploadState === 'uploading') {
        return (
            <div className="upload-progress-wrap">
                <div className="upload-progress-name">
                    <FontAwesomeIcon icon={faSpinner} spin />
                    <span>{fileName}</span>
                </div>
                <div className="upload-progress-bar-track">
                    <div className="upload-progress-bar-fill" style={{ width: `${progress}%` }} />
                </div>
                <span className="upload-progress-pct">{progress}%</span>
                <button type="button" className="upload-remove-btn" onClick={handleRemove}>Hủy</button>
            </div>
        );
    }

    if (uploadState === 'error') {
        return (
            <div className="upload-error-row">
                <FontAwesomeIcon icon={faTimesCircle} className="upload-error-icon" />
                <span className="upload-error-text">{uploadError || 'Tải lên thất bại'}</span>
                <button
                    type="button"
                    className="upload-retry-btn"
                    onClick={() => {
                        setUploadState('idle');
                        setUploadError('');
                        setFileName('');
                    }}
                >
                    Chọn lại
                </button>
            </div>
        );
    }

    return (
        <div className="upload-dropzone" onClick={() => fileInputRef.current?.click()}>
            <FontAwesomeIcon icon={contentType === 'video' ? faVideo : faFileAlt} className="upload-dropzone-icon" />
            <span className="upload-dropzone-label">
                {contentType === 'video' ? 'Chọn file video' : 'Chọn tài liệu'}
            </span>
            <span className="upload-dropzone-hint">
                {typeConfig?.hint}
            </span>
            <input
                ref={fileInputRef}
                type="file"
                accept={typeConfig?.accept}
                className="upload-file-input"
                onChange={handleFileChange}
            />
        </div>
    );
};

/* ── Content part modal (add / edit lesson) ── */
export const AddContentPartModal = ({ isOpen, onClose, partData, onSave, onFileUploaded, onFileRemoved }) => {
    const [title, setTitle]             = useState('');
    const [contentType, setContentType] = useState('video');
    const [filesByType, setFilesByType] = useState(EMPTY_FILES_BY_TYPE);
    const [isUploading, setIsUploading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setTitle(partData?.title || '');
            setContentType(partData?.contentType || 'video');
            setFilesByType(createFilesByType(partData));
            setIsUploading(false);
        }
    }, [isOpen, partData]);

    const handleContentTypeChange = (key) => {
        setContentType(key);
    };

    const handleFileUploaded = (url, mimeType) => {
        setFilesByType((prev) => ({
            ...prev,
            [contentType]: {
                fileUrl: url,
                fileType: mimeType,
            },
        }));
        onFileUploaded?.(url);
    };

    const handleFileRemoved = (url) => {
        setFilesByType((prev) => ({
            ...prev,
            [contentType]: { ...EMPTY_FILES_BY_TYPE[contentType] },
        }));
        if (url && url !== partData?.fileUrl) {
            onFileRemoved?.(url);
        }
    };

    const cleanupUnsavedUploads = (savedType = null) => {
        Object.entries(filesByType).forEach(([type, fileData]) => {
            if (type === savedType) return;
            if (fileData.fileUrl && fileData.fileUrl !== partData?.fileUrl) {
                onFileRemoved?.(fileData.fileUrl);
            }
        });
    };

    const getReplacedUrl = () => {
        if (!partData?.fileUrl) return null;
        if (contentType !== partData.contentType) return partData.fileUrl;
        const activeFileUrl = filesByType[contentType]?.fileUrl;
        if (!activeFileUrl || activeFileUrl !== partData.fileUrl) return partData.fileUrl;
        return null;
    };

    const activeFile = filesByType[contentType] || EMPTY_FILES_BY_TYPE[contentType];

    // Khi hủy: xóa file đã upload trong session này (chưa được lưu vào lesson)
    const handleCancel = () => {
        cleanupUnsavedUploads();
        onClose();
    };

    const handleSubmit = () => {
        if (!title.trim()) return;

        const replacedUrl = getReplacedUrl();
        const payload = {
            title: title.trim(),
            contentType,
            fileUrl: activeFile?.fileUrl || null,
            fileType: activeFile?.fileType || null,
            replacedUrl,
        };

        if (partData) {
            cleanupUnsavedUploads(contentType);
        } else {
            Object.entries(filesByType).forEach(([type, fileData]) => {
                if (type === contentType) return;
                if (fileData.fileUrl) onFileRemoved?.(fileData.fileUrl);
            });
        }

        onSave(payload);
        onClose();
    };

    const needsFile = true; // video và document đều cần file

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleCancel}
            title={partData ? 'Chỉnh sửa bài học' : 'Thêm bài học mới'}
            onSubmit={handleSubmit}
            submitLabel={partData ? 'Lưu thay đổi' : 'Thêm bài học'}
            submitDisabled={!title.trim() || isUploading}
        >
            {/* Content type tabs */}
            <div className="modal-type-tabs">
                {CONTENT_TYPES.map(({ key, label, icon }) => (
                    <button
                        key={key}
                        type="button"
                        className={`modal-type-tab ${contentType === key ? 'active' : ''} ${isUploading ? 'disabled' : ''}`}
                        onClick={() => !isUploading && handleContentTypeChange(key)}
                        disabled={isUploading}
                        title={isUploading ? 'Đang tải lên, vui lòng chờ...' : ''}
                    >
                        <FontAwesomeIcon icon={icon} />
                        {label}
                    </button>
                ))}
            </div>

            <div className="form-field">
                <label>Tên bài học</label>
                <input
                    autoFocus
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                    placeholder="VD: Nguyên tắc thiết kế nâng cao"
                />
            </div>

            {needsFile && (
                <div className="form-field">
                    <label>{contentType === 'video' ? 'File video' : 'File tài liệu'}</label>
                    <FileUpload
                        contentType={contentType}
                        existingUrl={activeFile?.fileUrl}
                        onUploaded={handleFileUploaded}
                        onRemoved={handleFileRemoved}
                        onUploadingChange={setIsUploading}
                    />
                </div>
            )}
        </Modal>
    );
};
