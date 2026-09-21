import React, {useEffect, useRef, useState} from 'react';
import {createPendingQuestionImage, revokePendingQuestionImage,} from '../../../utils/questionImageUpload';
import AssignmentMessageDialog from '../../../../shared/components/AssignmentMessageDialog';
import '../../../styles/teacher/CreateQuiz/stepReview.css';

const TYPE_LABELS = {
    SINGLE: 'Một đáp án',
    MULTIPLE: 'Nhiều đáp án',
    TRUE_FALSE: 'Đúng/Sai',
};

function normalizeAnswersForType(type, answers) {
    const current = answers?.length ? answers : [
        {content: '', correct: true, orderIndex: 0},
        {content: '', correct: false, orderIndex: 1},
    ];

    if (type === 'TRUE_FALSE') {
        const firstCorrect = current.findIndex(answer => answer.correct);
        return [
            {...current[0], content: current[0]?.content || 'Đúng', correct: firstCorrect <= 0, orderIndex: 0},
            {...current[1], content: current[1]?.content || 'Sai', correct: firstCorrect > 0, orderIndex: 1},
        ];
    }

    if (type === 'SINGLE') {
        const correctIndex = Math.max(0, current.findIndex(answer => answer.correct));
        return current.map((answer, index) => ({
            ...answer,
            correct: index === correctIndex,
            orderIndex: index,
        }));
    }

    const correctIndexes = current.reduce((indexes, answer, index) => (
        answer.correct ? [...indexes, index] : indexes
    ), []);
    return current.map((answer, index) => ({
        ...answer,
        correct: correctIndexes.length >= 2 ? answer.correct : index < 2,
        orderIndex: index,
    }));
}

export default function StepReview({
                                       questions = [],
                                       setQuestions,
                                       importResult,
                                       onBack,
                                       onAddQuestion,
                                       onDeleteQuestion,
                                       onReuseQuestions,
                                       onNext,
                                   }) {
    const [activeQuestionId, setActiveQuestionId] = useState(questions[0]?.id ?? questions[0]?.questionId ?? null);
    const [openExtraByQuestion, setOpenExtraByQuestion] = useState({});
    const [messageDialog, setMessageDialog] = useState(null);
    const questionRefs = useRef({});
    const imageInputRefs = useRef({});

    const isManualMode = importResult?.source === 'manual';
    const importedCount = importResult?.validCount ?? importResult?.importedCount ?? questions.length;

    useEffect(() => {
        if (!questions.length) {
            setActiveQuestionId(null);
            return;
        }

        if (!questions.some(question => question.id === activeQuestionId)) {
            setActiveQuestionId(questions[0].id);
        }
    }, [activeQuestionId, questions]);

    const activeExtraFor = (question, key) => (
        openExtraByQuestion[question.id] === key || Boolean(question[key])
    );

    const toggleExtra = (questionId, key) => {
        setOpenExtraByQuestion(prev => ({
            ...prev,
            [questionId]: prev[questionId] === key ? null : key,
        }));
    };

    const updateQuestion = (id, patch) => {
        setQuestions(prev => prev.map((question, index) => (
            question.id === id
                ? {...question, ...patch, index}
                : {...question, index}
        )));
    };

    const updateAnswer = (questionId, answerIndex, patch) => {
        setQuestions(prev => prev.map((question, qIndex) => {
            if (question.id !== questionId) return {...question, index: qIndex};
            const answers = question.answers.map((answer, index) => (
                index === answerIndex ? {...answer, ...patch} : answer
            ));
            return {...question, answers: normalizeAnswersForType(question.type, answers), index: qIndex};
        }));
    };

    const handleImageSelect = (question, file) => {
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
        updateQuestion(question.id, createPendingQuestionImage(file));
        setOpenExtraByQuestion(prev => ({...prev, [question.id]: 'imageUrl'}));
    };

    const handleClearImage = (question) => {
        revokePendingQuestionImage(question);
        updateQuestion(question.id, {
            imageUrl: '',
            pendingImageFile: null,
            pendingImagePreviewUrl: null,
            pendingImagePath: null,
        });
    };

    const handleSelectQuestion = (question) => {
        setActiveQuestionId(question.id);
        questionRefs.current[question.id]?.scrollIntoView({behavior: 'smooth', block: 'start'});
    };

    const handleAddQuestion = () => {
        const newQuestion = onAddQuestion?.();
        if (!newQuestion?.id) return;

        setActiveQuestionId(newQuestion.id);
        window.setTimeout(() => {
            questionRefs.current[newQuestion.id]?.scrollIntoView({behavior: 'smooth', block: 'start'});
        }, 0);
    };

    const handleDeleteQuestion = (question) => {
        const nextActiveQuestionId = onDeleteQuestion?.(question.id);
        setActiveQuestionId(nextActiveQuestionId ?? null);
    };

    const handleCloneForEditing = (question) => {
        updateQuestion(question.id, {
            questionId: null,
            reuseExisting: false,
            bankLocked: false,
            sourceQuizTitle: '',
            answers: question.answers.map(answer => ({
                content: answer.content,
                correct: answer.correct,
                orderIndex: answer.orderIndex,
            })),
        });
    };

    const handleTypeChange = (question, type) => {
        updateQuestion(question.id, {
            type,
            answers: normalizeAnswersForType(type, question.answers),
        });
    };

    const handleCorrectChange = (question, answerIndex, checked) => {
        const answers = question.answers.map((answer, index) => {
            if (question.type === 'MULTIPLE') {
                return index === answerIndex ? {...answer, correct: checked} : answer;
            }
            return {...answer, correct: index === answerIndex};
        });
        updateQuestion(question.id, {answers: normalizeAnswersForType(question.type, answers)});
    };

    const handleAddAnswer = (question) => {
        if (question.type === 'TRUE_FALSE') return;
        const answers = [
            ...question.answers,
            {content: '', correct: false, orderIndex: question.answers.length},
        ];
        updateQuestion(question.id, {answers: normalizeAnswersForType(question.type, answers)});
    };

    const handleRemoveAnswer = (question, answerIndex) => {
        if (question.type === 'TRUE_FALSE' || question.answers.length <= 2) return;
        const answers = question.answers
            .filter((_, index) => index !== answerIndex)
            .map((answer, index) => ({...answer, orderIndex: index}));
        updateQuestion(question.id, {answers: normalizeAnswersForType(question.type, answers)});
    };

    if (!importResult && questions.length === 0) {
        return (
            <div className="step-2-review">
                <div className="review-empty">
                    <span className="material-symbols-outlined">error</span>
                    <p>Không có dữ liệu import. Vui lòng quay lại và tải file lên.</p>
                    <button className="btn-outline" onClick={onBack}>Tải file khác</button>
                </div>
            </div>
        );
    }

    if (questions.length === 0) {
        return (
            <div className="step-2-review">
                <div className="review-empty">
                    <span className="material-symbols-outlined icon-status">quiz</span>
                    <p>Chưa có câu hỏi trong quiz. Thêm câu hỏi thủ công hoặc tái sử dụng từ quiz cũ để tiếp tục.</p>
                    <div className="review-empty-actions">
                        <button className="btn-outline" onClick={handleAddQuestion}>
                            <span className="material-symbols-outlined">add</span>
                            Thêm câu hỏi
                        </button>
                        <button className="btn-outline" onClick={onReuseQuestions}>
                            <span className="material-symbols-outlined">content_copy</span>
                            Tái sử dụng từ quiz cũ
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="step-2-review">
            <div className="import-summary">
                <div className="summary-compact">
          <span className="summary-icon material-symbols-outlined">
            {isManualMode ? 'edit_note' : 'fact_check'}
          </span>
                    <div className="summary-copy">
                        <strong>{isManualMode ? 'Đang nhập thủ công' : `${importedCount} đã import`}</strong>
                        <span>{questions.length} câu hỏi {isManualMode ? 'đang soạn' : 'đã import'}</span>
                    </div>
                </div>
            </div>

            <div className="review-layout">
                <aside className="pane-left">
                    <div className="pane-header">
                        <span className="list-subtitle">DANH SÁCH CÂU HỎI</span>
                    </div>
                    <div className="question-list">
                        {questions.map((question, index) => (
                            <button
                                type="button"
                                key={question.id}
                                className={`q-item valid ${activeQuestionId === question.id ? 'active' : ''}`}
                                onClick={() => handleSelectQuestion(question)}
                            >
                                <span className="material-symbols-outlined icon-status">quiz</span>
                                <span className="q-info">
                  <strong>Câu {index + 1}</strong>
                  <small>{TYPE_LABELS[question.type] || question.type}</small>
                </span>
                                <span className="q-score">{question.score || 10}đ</span>
                            </button>
                        ))}
                    </div>
                    <div className="review-side-actions">
                        {isManualMode ? (
                            <button className="btn-secondary" onClick={handleAddQuestion}>
                                <span className="material-symbols-outlined">add</span>
                                Thêm câu hỏi
                            </button>
                        ) : (
                            <button className="btn-secondary" onClick={onBack}>
                                <span className="material-symbols-outlined">upload_file</span>
                                Tải file khác
                            </button>
                        )}
                        <button className="btn-secondary" onClick={onReuseQuestions}>
                            <span className="material-symbols-outlined">content_copy</span>
                            Nhập từ ngân hàng câu hỏi
                        </button>
                    </div>
                </aside>

                <main className="pane-middle">
                    <div className="pane-header review-editor-header">
            <span className="preview-label">
              <span className="material-symbols-outlined">edit_note</span>
              KIỂM TRA VÀ CHỈNH SỬA
            </span>
                        <span className="review-hint">Các thay đổi sẽ được lưu khi lưu nháp hoặc xuất bản.</span>
                    </div>

                    <div className="preview-content">
                        {questions.map((question, index) => (
                            <section
                                key={question.id}
                                ref={element => {
                                    questionRefs.current[question.id] = element;
                                }}
                                className={`preview-card question-editor-card ${activeQuestionId === question.id ? 'active-card' : ''}`}
                                onFocus={() => setActiveQuestionId(question.id)}
                            >
                                {question.bankLocked && (
                                    <div className="reuse-readonly-banner">
                                        <span className="material-symbols-outlined">link</span>
                                        <span>
                      Câu hỏi này đang liên kết từ quiz cũ{question.sourceQuizTitle ? `: ${question.sourceQuizTitle}` : ''}.
                    </span>
                                        <button type="button" onClick={() => handleCloneForEditing(question)}>
                                            Tạo bản sao để chỉnh sửa
                                        </button>
                                    </div>
                                )}
                                <div className="card-header">
                                    <span className="q-badge">CÂU {index + 1}</span>
                                    <div className="review-card-controls">
                                        <label className="review-inline-field">
                                            Loại
                                            <select
                                                value={question.type}
                                                disabled={question.bankLocked}
                                                onChange={(event) => handleTypeChange(question, event.target.value)}
                                            >
                                                <option value="SINGLE">SINGLE</option>
                                                <option value="MULTIPLE">MULTIPLE</option>
                                                <option value="TRUE_FALSE">TRUE_FALSE</option>
                                            </select>
                                        </label>
                                        <label className="review-inline-field">
                                            Điểm
                                            <input
                                                type="number"
                                                min="1"
                                                max="100"
                                                value={question.score || 10}
                                                onChange={(event) => updateQuestion(question.id, {score: Number(event.target.value) || 1})}
                                            />
                                        </label>
                                        <button
                                            type="button"
                                            className="btn-delete-question"
                                            onClick={() => handleDeleteQuestion(question)}
                                            title="Xóa câu hỏi"
                                        >
                                            <span className="material-symbols-outlined">delete</span>
                                            Xóa
                                        </button>
                                    </div>
                                </div>

                                <label className="review-field">
                                    Nội dung câu hỏi
                                    <textarea
                                        value={question.content}
                                        disabled={question.bankLocked}
                                        onChange={(event) => updateQuestion(question.id, {content: event.target.value})}
                                        rows={3}
                                    />
                                </label>

                                <div className="question-extra-toolbar" aria-label="Thông tin bổ sung">
                                    <button
                                        type="button"
                                        className={activeExtraFor(question, 'topic') ? 'active' : ''}
                                        onClick={() => toggleExtra(question.id, 'topic')}
                                        disabled={question.bankLocked}
                                        title="Thêm nhãn/chủ đề"
                                    >
                                        <span className="material-symbols-outlined">sell</span>
                                        Nhãn
                                    </button>
                                    <button
                                        type="button"
                                        className={activeExtraFor(question, 'imageUrl') ? 'active' : ''}
                                        onClick={() => toggleExtra(question.id, 'imageUrl')}
                                        disabled={question.bankLocked}
                                        title="Thêm ảnh minh họa"
                                    >
                                        <span className="material-symbols-outlined">image</span>
                                        Ảnh
                                    </button>
                                    <button
                                        type="button"
                                        className={activeExtraFor(question, 'explanation') ? 'active' : ''}
                                        onClick={() => toggleExtra(question.id, 'explanation')}
                                        disabled={question.bankLocked}
                                        title="Thêm giải thích"
                                    >
                                        <span className="material-symbols-outlined">psychology_alt</span>
                                        Giải thích
                                    </button>
                                </div>

                                {(activeExtraFor(question, 'topic') || activeExtraFor(question, 'imageUrl') || activeExtraFor(question, 'explanation')) && (
                                    <div className="question-extra-panel">
                                        {activeExtraFor(question, 'topic') && (
                                            <label className="review-field compact">
                                                Nhãn / Topic
                                                <input
                                                    type="text"
                                                    value={question.topic || ''}
                                                    disabled={question.bankLocked}
                                                    onChange={(event) => updateQuestion(question.id, {topic: event.target.value})}
                                                    placeholder="VD: Vòng lặp, OOP, Cơ sở dữ liệu"
                                                />
                                            </label>
                                        )}

                                        {activeExtraFor(question, 'imageUrl') && (
                                            <div className="review-field compact">
                                                <span>Ảnh minh họa</span>
                                                <div className="question-image-row">
                                                    {question.pendingImagePath ? (
                                                        <div className="question-image-local-path"
                                                             title={question.pendingImagePath}>
                                                            {question.pendingImagePath}
                                                        </div>
                                                    ) : !question.imageUrl ? (
                                                        <div className="question-image-local-path muted">
                                                            Chưa chọn ảnh từ máy
                                                        </div>
                                                    ) : null}
                                                    <input
                                                        ref={element => {
                                                            imageInputRefs.current[question.id] = element;
                                                        }}
                                                        type="file"
                                                        accept="image/*"
                                                        hidden
                                                        onChange={(event) => {
                                                            try {
                                                                handleImageSelect(question, event.target.files?.[0]);
                                                            } finally {
                                                                event.target.value = '';
                                                            }
                                                        }}
                                                    />
                                                    <button
                                                        type="button"
                                                        className="btn-question-upload"
                                                        disabled={question.bankLocked}
                                                        onClick={() => imageInputRefs.current[question.id]?.click()}
                                                    >
                                                        <span className="material-symbols-outlined">upload</span>
                                                        Chọn ảnh
                                                    </button>
                                                    {question.imageUrl && (
                                                        <button
                                                            type="button"
                                                            className="btn-question-clear"
                                                            onClick={() => handleClearImage(question)}
                                                            disabled={question.bankLocked}
                                                            title="Gỡ ảnh"
                                                        >
                                                            <span className="material-symbols-outlined">close</span>
                                                        </button>
                                                    )}
                                                </div>
                                                {question.imageUrl && (
                                                    <div className="question-image-preview">
                                                        <img src={question.imageUrl} alt="Ảnh minh họa câu hỏi"/>
                                                    </div>
                                                )}
                                            </div>
                                        )}

                                        {activeExtraFor(question, 'explanation') && (
                                            <label className="review-field compact full">
                                                Giải thích đáp án
                                                <textarea
                                                    value={question.explanation || ''}
                                                    disabled={question.bankLocked}
                                                    onChange={(event) => updateQuestion(question.id, {explanation: event.target.value})}
                                                    rows={3}
                                                    placeholder="Nhập giải thích hiển thị sau khi học viên xem kết quả."
                                                />
                                            </label>
                                        )}
                                    </div>
                                )}

                                <div className="preview-options">
                                    <div className="answer-editor-header">
                                        <span>Đáp án</span>
                                        <span>{question.type === 'MULTIPLE' ? 'Chọn ít nhất 2 đáp án đúng' : 'Chọn 1 đáp án đúng'}</span>
                                    </div>
                                    {question.answers.map((answer, answerIndex) => (
                                        <div key={answer.answerId || answerIndex}
                                             className={`p-option editable${answer.correct ? ' correct' : ''}`}>
                                            <button
                                                type="button"
                                                className="answer-label"
                                                onClick={() => handleCorrectChange(question, answerIndex, !answer.correct)}
                                                disabled={question.bankLocked}
                                                title="Chọn đáp án này"
                                            >
                                                {String.fromCharCode(65 + answerIndex)}
                                            </button>
                                            <input
                                                type={question.type === 'MULTIPLE' ? 'checkbox' : 'radio'}
                                                name={`correct-${question.id}`}
                                                checked={!!answer.correct}
                                                disabled={question.bankLocked}
                                                onChange={(event) => handleCorrectChange(question, answerIndex, event.target.checked)}
                                            />
                                            <input
                                                type="text"
                                                value={answer.content}
                                                disabled={question.bankLocked}
                                                onChange={(event) => updateAnswer(question.id, answerIndex, {content: event.target.value})}
                                                placeholder={`Đáp án ${String.fromCharCode(65 + answerIndex)}`}
                                            />
                                            <button
                                                type="button"
                                                className="btn-remove-answer"
                                                onClick={() => handleRemoveAnswer(question, answerIndex)}
                                                disabled={question.bankLocked || question.type === 'TRUE_FALSE' || question.answers.length <= 2}
                                                title="Xóa đáp án"
                                            >
                                                <span className="material-symbols-outlined">delete</span>
                                            </button>
                                        </div>
                                    ))}
                                    <button
                                        type="button"
                                        className="btn-add-answer"
                                        onClick={() => handleAddAnswer(question)}
                                        disabled={question.bankLocked || question.type === 'TRUE_FALSE'}
                                    >
                                        <span className="material-symbols-outlined">add</span>
                                        Thêm đáp án
                                    </button>
                                </div>
                            </section>
                        ))}
                    </div>
                </main>
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
