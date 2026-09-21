import React, {useEffect, useMemo, useState} from 'react';
import {assignmentApi} from '../../../../../services/assignment.api';
import '../../styles/teacher/reuseQuizQuestionModal.css';

const TYPE_LABELS = {
    SINGLE: 'Một đáp án',
    MULTIPLE: 'Nhiều đáp án',
    TRUE_FALSE: 'Đúng/Sai',
};

function pageContent(page) {
    if (Array.isArray(page)) return page;
    return page?.content ?? [];
}

export default function ReuseQuizQuestionModal({
                                                   open,
                                                   courseId,
                                                   excludeQuizId,
                                                   existingQuestionIds = [],
                                                   onClose,
                                                   onConfirm,
                                               }) {
    const [quizzes, setQuizzes] = useState([]);
    const [selectedQuizId, setSelectedQuizId] = useState(null);
    const [questionsByQuizId, setQuestionsByQuizId] = useState({});
    const [selectedIds, setSelectedIds] = useState(() => new Set());
    const [loadingQuizzes, setLoadingQuizzes] = useState(false);
    const [loadingQuestions, setLoadingQuestions] = useState(false);
    const [error, setError] = useState('');

    const existingIds = useMemo(
        () => new Set(existingQuestionIds.filter(Boolean).map(Number)),
        [existingQuestionIds],
    );

    useEffect(() => {
        if (!open || !courseId) return;

        let cancelled = false;
        setLoadingQuizzes(true);
        setError('');
        setSelectedQuizId(null);
        setQuestionsByQuizId({});
        setSelectedIds(new Set());

        assignmentApi.getQuizzesForInstructor(courseId, {page: 0, size: 100})
            .then(result => {
                if (cancelled) return;
                const items = pageContent(result)
                    .filter(quiz => !quiz.deleted && String(quiz.quizId) !== String(excludeQuizId ?? ''));
                setQuizzes(items);
                setSelectedQuizId(items[0]?.quizId ?? null);
            })
            .catch(err => {
                if (!cancelled) setError(err.response?.data?.message || err.message || 'Không thể tải danh sách quiz.');
            })
            .finally(() => {
                if (!cancelled) setLoadingQuizzes(false);
            });

        return () => {
            cancelled = true;
        };
    }, [courseId, excludeQuizId, open]);

    useEffect(() => {
        if (!open || !selectedQuizId) return;

        const quizKey = String(selectedQuizId);
        if (questionsByQuizId[quizKey]) {
            setLoadingQuestions(false);
            return;
        }

        let cancelled = false;
        setLoadingQuestions(true);
        setError('');

        assignmentApi.getQuizQuestions(selectedQuizId)
            .then(result => {
                if (!cancelled) {
                    setQuestionsByQuizId(prev => ({
                        ...prev,
                        [quizKey]: result ?? [],
                    }));
                }
            })
            .catch(err => {
                if (!cancelled) setError(err.response?.data?.message || err.message || 'Không thể tải câu hỏi của quiz.');
            })
            .finally(() => {
                if (!cancelled) setLoadingQuestions(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, questionsByQuizId, selectedQuizId]);

    if (!open) return null;

    const selectedQuiz = quizzes.find(quiz => quiz.quizId === selectedQuizId);
    const activeQuizKey = selectedQuizId ? String(selectedQuizId) : '';
    const questions = questionsByQuizId[activeQuizKey] ?? [];
    const selectableQuestions = questions.filter(question => !existingIds.has(Number(question.questionId)));
    const allSelectableIds = selectableQuestions.map(question => question.questionId);
    const selectedQuestions = quizzes.flatMap(quiz => (
        questionsByQuizId[String(quiz.quizId)] ?? []
    ).filter(question => selectedIds.has(question.questionId))
        .map(question => ({...question, sourceQuiz: quiz})));
    const allSelected = allSelectableIds.length > 0 && allSelectableIds.every(id => selectedIds.has(id));

    const toggleQuestion = (questionId) => {
        if (existingIds.has(Number(questionId))) return;
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(questionId)) {
                next.delete(questionId);
            } else {
                next.add(questionId);
            }
            return next;
        });
    };

    const toggleAll = () => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (allSelected) {
                allSelectableIds.forEach(id => next.delete(id));
            } else {
                allSelectableIds.forEach(id => next.add(id));
            }
            return next;
        });
    };

    const handleConfirm = () => {
        onConfirm?.(selectedQuestions, selectedQuiz);
    };

    return (
        <div className="reuse-modal-backdrop" role="dialog" aria-modal="true">
            <div className="reuse-modal">
                <header className="reuse-modal__header">
                    <div>
                        <span className="reuse-modal__eyebrow">Ngân hàng câu hỏi</span>
                        <h2>Chọn câu hỏi đã có trong khóa học</h2>
                    </div>
                    <button type="button" className="reuse-modal__close" onClick={onClose} title="Đóng">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </header>

                {error && (
                    <div className="reuse-modal__error">
                        <span className="material-symbols-outlined">error</span>
                        {error}
                    </div>
                )}

                <div className="reuse-modal__body">
                    <aside className="reuse-modal__quiz-list">
                        <div className="reuse-modal__pane-title">Quiz trong khóa</div>
                        {loadingQuizzes ? (
                            <div className="reuse-modal__empty">Đang tải quiz...</div>
                        ) : quizzes.length === 0 ? (
                            <div className="reuse-modal__empty">Chưa có quiz khác để tái sử dụng.</div>
                        ) : quizzes.map(quiz => (
                            <button
                                type="button"
                                key={quiz.quizId}
                                className={`reuse-quiz-item ${quiz.quizId === selectedQuizId ? 'active' : ''}`}
                                onClick={() => setSelectedQuizId(quiz.quizId)}
                            >
                                <span className="material-symbols-outlined">assignment</span>
                                <span>
                  <strong>{quiz.title || `Quiz #${quiz.quizId}`}</strong>
                  <small>{quiz.questionCount ?? 0} câu hỏi</small>
                </span>
                            </button>
                        ))}
                    </aside>

                    <section className="reuse-modal__questions">
                        <div className="reuse-modal__questions-head">
                            <div>
                                <div className="reuse-modal__pane-title">Câu hỏi</div>
                                <p>{selectedQuiz?.title || 'Chọn một quiz để xem câu hỏi'}</p>
                            </div>
                            <button
                                type="button"
                                className="reuse-select-all"
                                onClick={toggleAll}
                                disabled={allSelectableIds.length === 0}
                            >
                                <span
                                    className="material-symbols-outlined">{allSelected ? 'remove_done' : 'done_all'}</span>
                                {allSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả'}
                            </button>
                        </div>

                        <div className="reuse-question-list">
                            {loadingQuestions ? (
                                <div className="reuse-modal__empty">Đang tải câu hỏi...</div>
                            ) : questions.length === 0 ? (
                                <div className="reuse-modal__empty">Quiz này chưa có câu hỏi.</div>
                            ) : questions.map((question, index) => {
                                const duplicate = existingIds.has(Number(question.questionId));
                                const checked = selectedIds.has(question.questionId);
                                return (
                                    <button
                                        type="button"
                                        key={question.questionId}
                                        className={`reuse-question-item ${checked ? 'selected' : ''} ${duplicate ? 'disabled' : ''}`}
                                        onClick={() => toggleQuestion(question.questionId)}
                                        disabled={duplicate}
                                    >
                    <span className="reuse-question-check material-symbols-outlined">
                      {duplicate ? 'block' : checked ? 'check_box' : 'check_box_outline_blank'}
                    </span>
                                        <span className="reuse-question-copy">
                      <strong>Câu {index + 1}: {question.content}</strong>
                      <small>
                        {TYPE_LABELS[question.type] || question.type} · {question.score ?? 10}đ
                          {question.topic ? ` · ${question.topic}` : ''}
                          {duplicate ? ' · đã có trong quiz hiện tại' : ''}
                      </small>
                    </span>
                                    </button>
                                );
                            })}
                        </div>
                    </section>
                </div>

                <footer className="reuse-modal__footer">
                    <span>{selectedQuestions.length} câu hỏi được chọn</span>
                    <div>
                        <button type="button" className="reuse-modal__secondary" onClick={onClose}>Huỷ</button>
                        <button
                            type="button"
                            className="reuse-modal__primary"
                            onClick={handleConfirm}
                            disabled={selectedQuestions.length === 0}
                        >
                            Thêm vào quiz
                        </button>
                    </div>
                </footer>
            </div>
        </div>
    );
}
