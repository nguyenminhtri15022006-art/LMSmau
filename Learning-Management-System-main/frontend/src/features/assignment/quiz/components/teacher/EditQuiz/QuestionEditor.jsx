import React, {useRef, useState} from 'react';
import {createPendingQuestionImage, revokePendingQuestionImage,} from '../../../utils/questionImageUpload';
import AssignmentMessageDialog from '../../../../shared/components/AssignmentMessageDialog';
import '../../../styles/teacher/ManualQuiz/questionEditor.css';

const TYPE_LABELS = {
    SINGLE: 'Một đáp án',
    MULTIPLE: 'Nhiều đáp án',
    TRUE_FALSE: 'Đúng/Sai',
};

export default function QuestionEditor({
                                           question,
                                           active = false,
                                           onUpdate,
                                           onTypeChange,
                                           onDeleteOption,
                                           onDeleteQuestion,
                                           onAddOption,
                                           onCloneForEditing,
                                       }) {
    const [openExtra, setOpenExtra] = useState(null);
    const [messageDialog, setMessageDialog] = useState(null);
    const imageInputRef = useRef(null);

    if (!question) return null;

    const isMultiple = question.type === 'MULTIPLE';
    const isTrueFalse = question.type === 'TRUE_FALSE';
    const isLocked = !!question.bankLocked;
    const isExtraActive = (key) => openExtra === key || Boolean(question[key]);
    const imageUrl = question.imageUrl || '';

    const handleClearImage = () => {
        revokePendingQuestionImage(question);
        onUpdate({
            ...question,
            imageUrl: '',
            pendingImageFile: null,
            pendingImagePreviewUrl: null,
            pendingImagePath: null,
        });
    };

    const handleImageSelect = (file) => {
        if (!file) return;
        if (!file.type?.startsWith('image/')) {
            setMessageDialog({
                title: 'Tệp không hợp lệ',
                message: 'Vui lòng chọn tệp ảnh.',
                detail: file.name,
                tone: 'error',
            });
            return;
        }

        revokePendingQuestionImage(question);
        onUpdate({...question, ...createPendingQuestionImage(file)});
        setOpenExtra('imageUrl');
    };

    return (
        <div className={`question-editor-card ${active ? 'active' : ''}`}>
            {isLocked && (
                <div className="reuse-readonly-banner">
                    <span className="material-symbols-outlined">link</span>
                    <span>
            Câu hỏi này đang liên kết từ quiz cũ{question.sourceQuizTitle ? `: ${question.sourceQuizTitle}` : ''}.
          </span>
                    <button type="button" onClick={onCloneForEditing}>
                        Tạo bản sao để chỉnh sửa
                    </button>
                </div>
            )}
            <div className="question-header">
                <h2 className="card-title" style={{marginBottom: 0}}>
                    <span className="material-symbols-outlined">quiz</span>
                    Câu hỏi {question.index + 1}
                </h2>
                <div className="question-header-controls">
                    <label className="question-inline-field">
                        Loại
                        <select
                            value={question.type ?? 'SINGLE'}
                            disabled={isLocked}
                            onChange={(event) => onTypeChange(event.target.value)}
                        >
                            <option value="SINGLE">SINGLE</option>
                            <option value="MULTIPLE">MULTIPLE</option>
                            <option value="TRUE_FALSE">TRUE_FALSE</option>
                        </select>
                    </label>
                    <label className="question-inline-field score-field">
                        Điểm
                        <input
                            type="number"
                            min="1"
                            max="100"
                            value={question.score ?? 10}
                            onChange={(event) => onUpdate({...question, score: event.target.value})}
                        />
                    </label>
                    <button
                        type="button"
                        className="btn-delete-question"
                        onClick={onDeleteQuestion}
                        title="Xóa câu hỏi"
                    >
                        <span className="material-symbols-outlined">delete</span>
                        Xóa
                    </button>
                </div>
            </div>

            <div className="form-group" style={{marginTop: '20px'}}>
                <label>Nội dung câu hỏi</label>
                <div className="input-with-icon" style={{alignItems: 'flex-start'}}>
          <textarea
              placeholder="Nhập câu hỏi của bạn..."
              rows="4"
              value={question.text}
              disabled={isLocked}
              onChange={(e) => onUpdate({...question, text: e.target.value})}
              style={{paddingLeft: '16px'}}
          ></textarea>
                </div>
            </div>

            <div className="question-extra-toolbar" aria-label="Thông tin bổ sung">
                <button
                    type="button"
                    className={isExtraActive('topic') ? 'active' : ''}
                    onClick={() => setOpenExtra(openExtra === 'topic' ? null : 'topic')}
                    disabled={isLocked}
                    title="Thêm nhãn/chủ đề"
                >
                    <span className="material-symbols-outlined">sell</span>
                    Nhãn
                </button>
                <button
                    type="button"
                    className={isExtraActive('imageUrl') ? 'active' : ''}
                    onClick={() => setOpenExtra(openExtra === 'imageUrl' ? null : 'imageUrl')}
                    disabled={isLocked}
                    title="Thêm ảnh minh họa"
                >
                    <span className="material-symbols-outlined">image</span>
                    Ảnh
                </button>
                <button
                    type="button"
                    className={isExtraActive('explanation') ? 'active' : ''}
                    onClick={() => setOpenExtra(openExtra === 'explanation' ? null : 'explanation')}
                    disabled={isLocked}
                    title="Thêm giải thích"
                >
                    <span className="material-symbols-outlined">psychology_alt</span>
                    Giải thích
                </button>
            </div>

            {(isExtraActive('topic') || isExtraActive('imageUrl') || isExtraActive('explanation')) && (
                <div className="question-extra-panel">
                    {isExtraActive('topic') && (
                        <label className="question-extra-field">
                            Nhãn / Topic
                            <input
                                type="text"
                                value={question.topic || ''}
                                disabled={isLocked}
                                onChange={(event) => onUpdate({...question, topic: event.target.value})}
                                placeholder="VD: Mảng, OOP, SQL"
                            />
                        </label>
                    )}

                    {isExtraActive('imageUrl') && (
                        <div className="question-extra-field">
                            <span>Ảnh minh họa</span>
                            <div className="question-image-row">
                                {question.pendingImagePath ? (
                                    <div className="question-image-local-path" title={question.pendingImagePath}>
                                        {question.pendingImagePath}
                                    </div>
                                ) : !imageUrl ? (
                                    <div className="question-image-local-path muted">
                                        Chưa chọn ảnh từ máy
                                    </div>
                                ) : null}
                                <input
                                    ref={imageInputRef}
                                    type="file"
                                    accept="image/*"
                                    hidden
                                    onChange={(event) => {
                                        try {
                                            handleImageSelect(event.target.files?.[0]);
                                        } finally {
                                            event.target.value = '';
                                        }
                                    }}
                                />
                                <button
                                    type="button"
                                    className="btn-question-upload"
                                    disabled={isLocked}
                                    onClick={() => imageInputRef.current?.click()}
                                >
                                    <span className="material-symbols-outlined">upload</span>
                                    Chọn ảnh
                                </button>
                                {imageUrl && (
                                    <button
                                        type="button"
                                        className="btn-question-clear"
                                        onClick={handleClearImage}
                                        disabled={isLocked}
                                        title="Gỡ ảnh"
                                    >
                                        <span className="material-symbols-outlined">close</span>
                                    </button>
                                )}
                            </div>
                            {imageUrl && (
                                <div className="question-image-preview">
                                    <img src={imageUrl} alt="Ảnh minh họa câu hỏi"/>
                                </div>
                            )}
                        </div>
                    )}

                    {isExtraActive('explanation') && (
                        <label className="question-extra-field full">
                            Giải thích đáp án
                            <textarea
                                rows="3"
                                value={question.explanation || ''}
                                disabled={isLocked}
                                onChange={(event) => onUpdate({...question, explanation: event.target.value})}
                                placeholder="Nhập giải thích hiển thị sau khi học viên xem kết quả."
                            />
                        </label>
                    )}
                </div>
            )}

            <div className="answer-options-section">
                <div className="section-header">
                    <h3>Đáp án</h3>
                    <span
                        className="helper-text">{isMultiple ? 'Chọn ít nhất 2 đáp án đúng' : 'Chọn 1 đáp án đúng'}</span>
                </div>

                <div className="options-list">
                    {question.options.map((option, idx) => (
                        <div key={idx} className="option-item">
                            <input
                                type={isMultiple ? 'checkbox' : 'radio'}
                                name={`correct-${question.id}`}
                                className="option-radio"
                                checked={option.isCorrect}
                                disabled={isLocked}
                                onChange={(event) => {
                                    const newOptions = question.options.map((o, i) => (
                                        isMultiple
                                            ? {...o, isCorrect: i === idx ? event.target.checked : o.isCorrect}
                                            : {...o, isCorrect: i === idx}
                                    ));
                                    onUpdate({...question, options: newOptions});
                                }}
                            />
                            <input
                                type="text"
                                className="option-input"
                                value={option.text}
                                placeholder={`Đáp án ${String.fromCharCode(65 + idx)}`}
                                disabled={isLocked || isTrueFalse}
                                onChange={(e) => {
                                    const newOptions = [...question.options];
                                    newOptions[idx].text = e.target.value;
                                    onUpdate({...question, options: newOptions});
                                }}
                            />
                            <button
                                className="btn-delete-option"
                                onClick={() => onDeleteOption(idx)}
                                disabled={isLocked || isTrueFalse || question.options.length <= 2}
                                title="Xóa đáp án"
                            >
                                <span className="material-symbols-outlined">delete</span>
                            </button>
                        </div>
                    ))}
                </div>

                <button className="btn-add-option" onClick={onAddOption} disabled={isLocked || isTrueFalse}>
                    <span className="material-symbols-outlined">add</span>
                    Thêm đáp án
                </button>
            </div>
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
