import React, {useEffect, useRef, useState} from 'react';
import blogApi from '../../../../services/blog.api';
import {mapBlogTagToCategory, unwrapApiData} from '../../../../utils/blogMappers';
import {
    BLOG_THUMBNAIL_MAX_BYTES,
    IMAGE_UPLOAD_ACCEPT,
    validateImageUpload,
} from '../../../../utils/imageUploadValidation';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import AnimatedPage from '../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../components/ui/LockedFeature';

import '../../styles/student/BlogCommon.css';
import '../../styles/student/BlogEditor.css';

export default function CreateBlogPostPage() {
    const [title, setTitle] = useState('');
    const [summary, setSummary] = useState('');
    const [content, setContent] = useState('');
    const [categories, setCategories] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState('');
    const [previewImage, setPreviewImage] = useState(null);
    const [selectedFile, setSelectedFile] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [imageError, setImageError] = useState('');
    const editorRef = useRef(null);
    const fileInputRef = useRef(null);

    const location = useLocation();
    const navigate = useNavigate();
    const basePath = location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';

    useEffect(() => {
        let ignore = false;

        const fetchCategories = async () => {
            try {
                const response = await blogApi.getTags();
                const tagCategories = (unwrapApiData(response) || []).map(mapBlogTagToCategory);
                if (!ignore) {
                    setCategories(tagCategories);
                }
            } catch (_err) {
                if (!ignore) {
                    setCategories([]);
                }
            }
        };

        fetchCategories();

        return () => {
            ignore = true;
        };
    }, []);

    const handleFormat = (command, value = null) => {
        document.execCommand(command, false, value);
        editorRef.current.focus();
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            const validationError = validateImageUpload(file, {
                maxBytes: BLOG_THUMBNAIL_MAX_BYTES,
                label: 'Ảnh đại diện bài viết',
            });

            if (validationError) {
                setImageError(validationError);
                if (fileInputRef.current) fileInputRef.current.value = '';
                return;
            }

            setImageError('');
            setSelectedFile({
                file,
                name: file.name,
                type: file.type,
                size: (file.size / 1024 / 1024).toFixed(2),
            });

            const reader = new FileReader();
            reader.onloadend = () => {
                setPreviewImage(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const triggerUpload = () => {
        fileInputRef.current?.click();
    };

    const clearSelectedFile = () => {
        setSelectedFile(null);
        setPreviewImage(null);
        setImageError('');
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const submitPost = async (status) => {
        setError('');

        if (!title.trim() || !content.trim()) {
            setError('Vui lòng nhập tiêu đề và nội dung bài viết.');
            return;
        }

        const formData = new FormData();
        formData.append('title', title.trim());
        formData.append('summary', summary.trim());
        formData.append('content', content);
        formData.append('status', status);

        if (selectedCategoryId) {
            formData.append('tagIds', selectedCategoryId);
        }

        if (selectedFile?.file) {
            formData.append('thumbnail', selectedFile.file);
        }

        try {
            setSubmitting(true);
            const response = await blogApi.createPost(formData);
            const savedPost = unwrapApiData(response);
            const detailParam = status === 'PUBLISHED'
                ? savedPost?.slug || savedPost?.id
                : savedPost?.id;
            navigate(detailParam ? `${basePath}/${detailParam}` : basePath);
        } catch (_err) {
            setError('Không thể lưu bài viết. Vui lòng kiểm tra dữ liệu và thử lại.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <AnimatedPage>
            <LockedFeature featureName="Blog Management">
                <div className="blog-page">
                    {/* Breadcrumb */}
                                                        <nav className="ch-breadcrumb">
                                                                <Link to={basePath}>Bài viết</Link>
                                                                <span className="ch-breadcrumb-separator">
                                            <span className="material-symbols-outlined">chevron_right</span>
                                        </span>
                                                                <span className="breadcrumb-title">Tạo bài viết mới</span>
                                                        </nav>

                    <div className="editor-container">
                        <header className="editor-header">
                            <h1>Tạo bài viết mới</h1>
                            <p>Chia sẻ kiến thức của bạn với cộng đồng EduLearn.</p>
                        </header>

                        <form className="blog-editor-form" onSubmit={(e) => e.preventDefault()}>
                            {error && <div className="blog-empty-state">{error}</div>}

                            <div className="form-group">
                                <label className="form-label">Tiêu đề bài viết</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder="Nhập tiêu đề bài viết"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Tóm tắt</label>
                                <textarea
                                    className="form-input"
                                    placeholder="Nhập tóm tắt ngắn cho bài viết"
                                    value={summary}
                                    onChange={(e) => setSummary(e.target.value)}
                                    rows={3}
                                />
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label className="form-label">Danh mục</label>
                                    <select
                                        className="form-input"
                                        value={selectedCategoryId}
                                        onChange={(e) => setSelectedCategoryId(e.target.value)}
                                    >
                                        <option value="">Chọn danh mục</option>
                                        {categories.map(category => (
                                            <option key={category.id} value={category.id}>{category.name}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-group">
                                <label className="form-label">Trạng thái</label>
                                <div className="input-with-icon">
                                        <input type="text" className="form-input"
                                               value="Lưu nháp hoặc xuất bản bằng nút bên dưới" readOnly/>
                                        <span className="material-symbols-outlined input-icon">sell</span>
                                    </div>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Ảnh đại diện bài viết</label>
                                <input
                                    type="file"
                                    ref={fileInputRef}
                                    className="hidden-input"
                                    accept={IMAGE_UPLOAD_ACCEPT}
                                    onChange={handleFileChange}
                                />
                                <div className={`upload-area${imageError ? ' upload-area--error' : ''}`} onClick={triggerUpload}>
                                    {(selectedFile || previewImage) ? (
                                        <div className="upload-preview">
                                            {previewImage ? (
                                                <img
                                                    src={previewImage}
                                                    alt="Preview"
                                                    className="preview-img"
                                                />
                                            ) : (
                                                <div className="preview-placeholder">
                                                    <span className="material-symbols-outlined preview-icon">article</span>
                                                    <h4 className="preview-title">{selectedFile.name}</h4>
                                                    <p className="preview-meta">{selectedFile.size} MB</p>
                                                </div>
                                            )}
                                            <button
                                                type="button"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    clearSelectedFile();
                                                }}
                                                className="close-btn"
                                            >
                                                <span className="material-symbols-outlined icon-sm">close</span>
                                            </button>
                                            <p className="upload-hint mt12">Nhấp để thay đổi ảnh</p>
                                        </div>
                                    ) : (
                                        <>
                                            <span className="material-symbols-outlined upload-icon">upload_file</span>
                                            <p className="upload-text"><span>Tải lên ảnh đại diện</span></p>
                                            <p className="upload-hint">JPG, PNG, GIF, WebP, BMP, SVG, tối đa 5MB</p>
                                        </>
                                    )}
                                </div>
                                {imageError && (
                                    <p className="upload-error-message" role="alert">
                                        {imageError}
                                    </p>
                                )}
                            </div>

                            <div className="form-group">
                                <label className="form-label">Nội dung bài viết</label>
                                <div className="rich-editor">
                                    <div className="editor-toolbar">
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('bold')}><span className="material-symbols-outlined toolbar-icon">format_bold</span></button>
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('italic')}><span className="material-symbols-outlined toolbar-icon">format_italic</span></button>
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('underline')}><span className="material-symbols-outlined toolbar-icon">format_underlined</span></button>
                                        <span className="toolbar-divider"></span>
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('insertUnorderedList')}><span className="material-symbols-outlined toolbar-icon">format_list_bulleted</span></button>
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('insertOrderedList')}><span className="material-symbols-outlined toolbar-icon">format_list_numbered</span></button>
                                        <span className="toolbar-divider"></span>
                                        <button type="button" className="toolbar-btn" onClick={() => { const url = prompt('Nhập URL liên kết:'); if (url) handleFormat('createLink', url); }}><span className="material-symbols-outlined toolbar-icon">link</span></button>
                                        <button type="button" className="toolbar-btn" onClick={() => handleFormat('formatBlock', 'pre')}><span className="material-symbols-outlined toolbar-icon">code</span></button>
                                    </div>
                                    <div
                                        ref={editorRef}
                                        className="editor-content-area"
                                        contentEditable="true"
                                        onInput={(e) => setContent(e.currentTarget.innerHTML)}
                                        suppressContentEditableWarning={true}
                                        data-placeholder="Bắt đầu viết nội dung bài viết..."
                                    ></div>
                                </div>
                            </div>

                            <div className="editor-actions">
                                <button type="button" className="btn-secondary" disabled={submitting}
                                        onClick={() => submitPost('DRAFT')}>
                                    Lưu nháp
                                </button>
                                <button type="button" className="btn-primary" disabled={submitting}
                                        onClick={() => submitPost('PUBLISHED')}>
                                    {submitting ? 'Đang lưu...' : 'Xuất bản'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </LockedFeature>
        </AnimatedPage>
    );
}
