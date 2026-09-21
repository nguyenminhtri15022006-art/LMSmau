import React, { useRef, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faImage, faLightbulb, faPlus, faTrash, faSearch, faTimes, faEye, faEyeSlash } from '@fortawesome/free-solid-svg-icons';
import {
    COURSE_THUMBNAIL_MAX_BYTES,
    IMAGE_UPLOAD_ACCEPT,
    validateImageUpload,
} from '../../../../../utils/imageUploadValidation';
import '../../../styles/teacher/EditCourse/editCourseInfo.css';

const LEVEL_CONFIG = {
    BEGINNER:     { label: 'Cơ bản',    desc: 'Phù hợp cho người mới bắt đầu',   bars: 1, color: '#22c55e', bg: '#f0fdf4', border: '#bbf7d0' },
    INTERMEDIATE: { label: 'Trung cấp', desc: 'Yêu cầu kiến thức nền tảng',      bars: 2, color: '#3b82f6', bg: '#eff6ff', border: '#bfdbfe' },
    ADVANCED:     { label: 'Nâng cao',  desc: 'Dành cho người có kinh nghiệm',   bars: 3, color: '#ef4444', bg: '#fff5f5', border: '#fecaca' },
};

const LevelSelect = ({ value, onChange }) => (
    <div className="level-select-group">
        {Object.entries(LEVEL_CONFIG).map(([key, cfg]) => {
            const selected = value === key;
            return (
                <button
                    key={key}
                    type="button"
                    className={`level-card${selected ? ' selected' : ''}`}
                    style={{ '--lc': cfg.color, '--lb': cfg.bg, '--lbd': cfg.border }}
                    onClick={() => onChange(selected ? '' : key)}
                >
                    <div className="level-bars">
                        {[1, 2, 3].map(i => (
                            <span key={i} className={`level-bar${i <= cfg.bars ? ' lit' : ''}`} />
                        ))}
                    </div>
                    <div className="level-card-body">
                        <span className="level-card-name">{cfg.label}</span>
                        <span className="level-card-desc">{cfg.desc}</span>
                    </div>
                    {selected && <span className="level-check">✓</span>}
                </button>
            );
        })}
    </div>
);

/* ── Category search-select ── */
const CategorySelect = ({ categories, selectedIds, onChange }) => {
    const [query, setQuery] = useState('');
    const [open, setOpen] = useState(false);
    const wrapperRef = useRef(null);

    const selectedCategories = (categories || []).filter(c => selectedIds.includes(c.id));
    const filtered = (categories || []).filter(
        c => !selectedIds.includes(c.id) && c.name.toLowerCase().includes(query.toLowerCase())
    );

    // Close only when focus leaves the entire wrapper (input + dropdown)
    const handleBlur = (e) => {
        if (wrapperRef.current?.contains(e.relatedTarget)) return;
        setOpen(false);
    };

    const select = (id) => {
        onChange([...selectedIds, id]);
        setQuery('');
    };

    const remove = (id) => {
        onChange(selectedIds.filter(x => x !== id));
    };

    return (
        <div className="category-select" ref={wrapperRef} onBlur={handleBlur}>
            {selectedCategories.length > 0 && (
                <div className="category-selected-tags">
                    {selectedCategories.map(cat => (
                        <span key={cat.id} className="category-tag">
                            {cat.name}
                            <button type="button" onClick={() => remove(cat.id)} className="category-tag-remove">
                                <FontAwesomeIcon icon={faTimes} />
                            </button>
                        </span>
                    ))}
                </div>
            )}

            <div className="category-search-input-wrapper">
                <FontAwesomeIcon icon={faSearch} className="category-search-icon" />
                <input
                    type="text"
                    className="category-search-input"
                    placeholder="Tìm kiếm danh mục..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={() => setOpen(true)}
                />
            </div>

            {open && (
                <div className="category-dropdown">
                    {filtered.length > 0 ? (
                        filtered.map(cat => (
                            <button
                                key={cat.id}
                                type="button"
                                tabIndex={0}
                                className="category-dropdown-item"
                                onClick={() => select(cat.id)}
                            >
                                {cat.name}
                            </button>
                        ))
                    ) : (
                        <div className="category-dropdown-empty">
                            {query ? 'Không tìm thấy danh mục phù hợp' : 'Tất cả danh mục đã được chọn'}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

/* ── Main component ── */
const EditCourseInfo = ({ data, updateData, categories, errors = {}, requireLearningPoints = false }) => {
    const fileInputRef = useRef(null);
    const [thumbnailError, setThumbnailError] = useState('');

    const thumbnailPreview = data.thumbnailFile
        ? URL.createObjectURL(data.thumbnailFile)
        : data.thumbnailUrl || null;

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const validationError = validateImageUpload(file, {
            maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
            label: 'Ảnh đại diện khóa học',
        });

        if (validationError) {
            setThumbnailError(validationError);
            if (fileInputRef.current) fileInputRef.current.value = '';
            return;
        }

        setThumbnailError('');
        updateData({ thumbnailFile: file });
    };

    const handleDrop = (e) => {
        e.preventDefault();
        const file = e.dataTransfer.files[0];
        if (!file) return;

        const validationError = validateImageUpload(file, {
            maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
            label: 'Ảnh đại diện khóa học',
        });

        if (validationError) {
            setThumbnailError(validationError);
            return;
        }

        setThumbnailError('');
        updateData({ thumbnailFile: file });
    };

    const updateListItem = (field, index, value) => {
        const list = [...(data[field] || [])];
        list[index] = value;
        updateData({ [field]: list });
    };

    const addListItem = (field) => {
        updateData({ [field]: [...(data[field] || []), ''] });
    };

    const removeListItem = (field, index) => {
        const list = [...(data[field] || [])];
        list.splice(index, 1);
        updateData({ [field]: list });
    };

    return (
        <div className="edit-course-info">

            {/* Title */}
            <div className="form-field">
                <label>Tiêu đề khóa học <span className="field-required">*</span></label>
                <input
                    type="text"
                    className={errors.title ? 'input-error' : ''}
                    value={data.title || ''}
                    onChange={(e) => updateData({ title: e.target.value })}
                    placeholder="Ví dụ: Lập trình Python từ cơ bản đến nâng cao"
                />
                {errors.title && <span className="field-error-msg">{errors.title}</span>}
            </div>

            {/* Level */}
            <div className="form-field">
                <label>Cấp độ khóa học</label>
                <LevelSelect value={data.level || ''} onChange={(val) => updateData({ level: val })} />
            </div>

            {/* Duration */}
            <div className="form-field" style={{ maxWidth: '320px' }}>
                <label>Thời hạn khóa học (tháng) <span className="field-required">*</span></label>
                <input
                    type="number"
                    min="1"
                    className={errors.duration ? 'input-error' : ''}
                    value={data.duration || ''}
                    onChange={(e) => updateData({ duration: parseInt(e.target.value) || '' })}
                    placeholder="Ví dụ: 3"
                />
                {errors.duration && <span className="field-error-msg">{errors.duration}</span>}
            </div>

            {/* Category search-select */}
            <div className="form-field">
                <label>Danh mục</label>
                <CategorySelect
                    categories={categories}
                    selectedIds={data.categoryIds || []}
                    onChange={(ids) => updateData({ categoryIds: ids })}
                />
            </div>

            {/* Thumbnail */}
            <div className="form-field">
                <label>Ảnh đại diện khóa học</label>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept={IMAGE_UPLOAD_ACCEPT}
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                />
                {thumbnailPreview ? (
                    <div className="thumbnail-preview-wrapper">
                        <img src={thumbnailPreview} alt="Thumbnail khóa học" className="thumbnail-preview" />
                        <button
                            type="button"
                            className="btn-change-thumbnail"
                            onClick={() => fileInputRef.current?.click()}
                        >
                            Đổi ảnh
                        </button>
                    </div>
                ) : (
                    <div
                        className={`upload-thumbnail-area${thumbnailError ? ' upload-thumbnail-area--error' : ''}`}
                        onClick={() => fileInputRef.current?.click()}
                        onDrop={handleDrop}
                        onDragOver={(e) => e.preventDefault()}
                    >
                        <FontAwesomeIcon icon={faImage} className="upload-icon" />
                        <p><span className="upload-link">Chọn ảnh từ máy</span> hoặc kéo và thả</p>
                        <p className="upload-hint">JPG, PNG, GIF, WebP, BMP, SVG, tối đa 10MB</p>
                    </div>
                )}
                {thumbnailError && (
                    <span className="field-error-msg thumbnail-upload-error" role="alert">
                        {thumbnailError}
                    </span>
                )}
            </div>

            {/* Description */}
            <div className="form-field">
                <label>Mô tả chi tiết</label>
                <textarea
                    rows="6"
                    value={data.description || ''}
                    onChange={(e) => updateData({ description: e.target.value })}
                />
            </div>

            {/* Learning Points */}
            <div className="form-field">
                <label>
                    Mục tiêu học viên đạt được
                    {requireLearningPoints && <span className="field-required">*</span>}
                </label>
                <div className="list-editor">
                    {(data.learningPoints || []).map((point, idx) => (
                        <div key={idx} className="list-editor-item">
                            <input
                                type="text"
                                value={point}
                                placeholder={`Mục tiêu ${idx + 1}`}
                                onChange={(e) => updateListItem('learningPoints', idx, e.target.value)}
                            />
                            <button
                                type="button"
                                className="btn-list-remove"
                                onClick={() => removeListItem('learningPoints', idx)}
                            >
                                <FontAwesomeIcon icon={faTrash} />
                            </button>
                        </div>
                    ))}
                    <button type="button" className="btn-list-add" onClick={() => addListItem('learningPoints')}>
                        <FontAwesomeIcon icon={faPlus} /> Thêm mục tiêu
                    </button>
                </div>
                {errors.learningPoints && <span className="field-error-msg">{errors.learningPoints}</span>}
            </div>

            {/* Requirements */}
            <div className="form-field">
                <label>Điều kiện tiên quyết</label>
                <div className="list-editor">
                    {(data.requirements || []).map((req, idx) => (
                        <div key={idx} className="list-editor-item">
                            <input
                                type="text"
                                value={req}
                                placeholder={`Điều kiện ${idx + 1}`}
                                onChange={(e) => updateListItem('requirements', idx, e.target.value)}
                            />
                            <button
                                type="button"
                                className="btn-list-remove"
                                onClick={() => removeListItem('requirements', idx)}
                            >
                                <FontAwesomeIcon icon={faTrash} />
                            </button>
                        </div>
                    ))}
                    <button type="button" className="btn-list-add" onClick={() => addListItem('requirements')}>
                        <FontAwesomeIcon icon={faPlus} /> Thêm điều kiện
                    </button>
                </div>
            </div>

            {/* Status */}
            <div className="form-field">
                <label>Trạng thái hiển thị</label>
                <div className="status-radio-group">
                    <label className={`status-radio-option ${data.status === 'PUBLIC' ? 'selected' : ''}`}>
                        <input
                            type="radio"
                            name="courseStatus"
                            value="PUBLIC"
                            checked={data.status === 'PUBLIC'}
                            onChange={() => updateData({ status: 'PUBLIC' })}
                        />
                        <div className="status-radio-icon public">
                            <FontAwesomeIcon icon={faEye} />
                        </div>
                        <div className="status-radio-text">
                            <h4>Công khai</h4>
                            <p>Bất kỳ ai cũng có thể tìm thấy và đăng ký khóa học này.</p>
                        </div>
                    </label>

                    <label className={`status-radio-option ${data.status === 'PRIVATE' ? 'selected' : ''}`}>
                        <input
                            type="radio"
                            name="courseStatus"
                            value="PRIVATE"
                            checked={data.status === 'PRIVATE'}
                            onChange={() => updateData({ status: 'PRIVATE' })}
                        />
                        <div className="status-radio-icon private">
                            <FontAwesomeIcon icon={faEyeSlash} />
                        </div>
                        <div className="status-radio-text">
                            <h4>Riêng tư</h4>
                            <p>Chỉ hiển thị với bạn và những người được ủy quyền.</p>
                        </div>
                    </label>
                </div>
            </div>

            {/* Pro Tip */}
            <div className="pro-tip-box">
                <FontAwesomeIcon icon={faLightbulb} className="tip-icon" />
                <div className="tip-content">
                    <h4>Mẹo</h4>
                    <p>
                        Tiêu đề khóa học dài từ 40-60 ký tự thường hoạt động tốt hơn 20% trong kết quả tìm kiếm.
                        Tiêu đề hiện tại của bạn dài {data.title?.length || 0} ký tự.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default EditCourseInfo;
