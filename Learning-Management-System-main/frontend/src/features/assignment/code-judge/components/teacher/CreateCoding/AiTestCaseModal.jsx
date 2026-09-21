import React, {useEffect, useMemo, useState} from 'react';
import {assignmentApi} from '../../../../../../services/assignment.api';
import '../../../styles/teacher/CreateCoding/aiTestCaseModal.css';

const MAX_COUNT = 50;
const REVIEW_WARNING = 'Test cases do AI tạo cần được kiểm tra lại trước khi lưu.';
const DIFFICULTY_LABELS = {
    EASY: 'Dễ',
    MEDIUM: 'Trung bình',
    HARD: 'Khó',
};

function normalizeGeneratedCase(item, index) {
    return {
        id: `ai-${Date.now()}-${index}`,
        input: item.input ?? '',
        output: item.expectedOutput ?? item.output ?? '',
        hidden: true,
        orderIndex: item.orderIndex ?? index,
        scoreWeight: item.scoreWeight ?? 1,
        description: item.description ?? '',
    };
}

export default function AiTestCaseModal({
                                            open,
                                            problemContext,
                                            onClose,
                                            onApply,
                                            onGeneratingChange,
                                        }) {
    const [count, setCount] = useState(5);
    const [constraints, setConstraints] = useState('');
    const [previewCases, setPreviewCases] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const title = problemContext?.title ?? '';
    const description = problemContext?.description ?? '';
    const hasPreview = previewCases.length > 0;
    const canClose = !loading && !hasPreview;
    const summaryItems = useMemo(() => ([
        {icon: 'speed', label: 'Độ khó', value: DIFFICULTY_LABELS[problemContext?.difficulty] ?? problemContext?.difficulty ?? '-'},
        {icon: 'timer', label: 'Thời gian', value: `${problemContext?.timeLimitMs ?? '-'} ms`},
        {icon: 'memory', label: 'Bộ nhớ', value: `${problemContext?.memoryLimitMb ?? '-'} MB`},
        {icon: 'grade', label: 'Điểm', value: problemContext?.score ?? '-'},
    ]), [problemContext]);

    useEffect(() => {
        if (!open) return;
        setCount(5);
        setConstraints(problemContext?.constraints ?? '');
        setPreviewCases([]);
        setError('');
    }, [open, problemContext]);

    useEffect(() => {
        onGeneratingChange?.(loading);
    }, [loading, onGeneratingChange]);

    const canGenerate = useMemo(() => {
        const numericCount = Number(count);
        return title.trim() && description.trim() && Number.isInteger(numericCount) && numericCount >= 1 && numericCount <= MAX_COUNT;
    }, [count, description, title]);

    if (!open) return null;

    const handleGenerate = async () => {
        if (!canGenerate) {
            setError(`Vui lòng nhập tiêu đề, mô tả và số lượng test case từ 1 đến ${MAX_COUNT}.`);
            return;
        }

        setLoading(true);
        setError('');
        try {
            const response = await assignmentApi.previewAiGeneratedTestCases({
                ...problemContext,
                constraints,
                count: Number(count),
            });
            setPreviewCases((response.testCases ?? []).map(normalizeGeneratedCase));
            if (!response.testCases?.length) {
                setError('AI chưa trả về test case hợp lệ.');
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Không thể tạo bộ test với AI. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const updatePreviewCase = (id, field, value) => {
        setPreviewCases(prev => prev.map(testCase => (
            testCase.id === id ? {...testCase, [field]: value} : testCase
        )));
    };

    const deletePreviewCase = (id) => {
        setPreviewCases(prev => prev.filter(testCase => testCase.id !== id));
    };

    const handleApply = () => {
        onApply(previewCases);
        onClose();
    };

    return (
        <div className={`ai-test-modal-overlay ${!canClose ? 'locked' : ''}`} onClick={canClose ? onClose : undefined}>
            <div
                className="ai-test-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="ai-test-modal-title"
                onClick={(event) => event.stopPropagation()}
            >
                {loading && (
                    <div className="ai-test-generating-lock" role="status" aria-live="polite">
                        <div className="ai-test-generating-spinner"></div>
                        <p>AI đang tạo test cases...</p>
                    </div>
                )}
                <div className="ai-test-modal-header">
                    <div className="ai-test-modal-title-group">
                        <span className="ai-test-modal-icon">
                            <span className="material-symbols-outlined">auto_awesome</span>
                        </span>
                        <div>
                            <span className="ai-test-modal-kicker">AI test case studio</span>
                            <h2 id="ai-test-modal-title">Tạo bộ test với AI</h2>
                            <p>AI dùng mô tả bài toán và cấu hình chấm để đề xuất test cases có thể chỉnh sửa trước khi thêm vào form.</p>
                        </div>
                    </div>
                    <button
                        className="ai-test-modal-close"
                        type="button"
                        onClick={onClose}
                        disabled={!canClose}
                        title={hasPreview ? 'Vui lòng review test cases trước khi đóng' : 'Đóng'}
                    >
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                <div className="ai-test-modal-body">
                    <section className="ai-test-config-panel">
                        <div className="ai-test-warning">
                            <span className="material-symbols-outlined">warning</span>
                            <span>{REVIEW_WARNING}</span>
                        </div>

                        <label className="ai-test-field ai-test-count-field">
                            <span>Số lượng test cases</span>
                            <input
                                className="option-input"
                                type="number"
                                min="1"
                                max={MAX_COUNT}
                                step="1"
                                value={count}
                                onChange={(event) => setCount(event.target.value)}
                            />
                            <small>Tối đa {MAX_COUNT} test cases mỗi lần tạo.</small>
                        </label>

                        <div className="ai-test-summary-grid">
                            {summaryItems.map((item) => (
                                <div className="ai-test-summary-item" key={item.label}>
                                    <span className="material-symbols-outlined">{item.icon}</span>
                                    <div>
                                        <small>{item.label}</small>
                                        <strong>{item.value}</strong>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {error && <div className="ai-test-error">{error}</div>}

                        <div className="ai-test-modal-actions">
                            {!hasPreview && (
                                <button type="button" className="ai-test-secondary" onClick={onClose} disabled={loading}>
                                    Hủy
                                </button>
                            )}
                            <button
                                type="button"
                                className="ai-test-generate"
                                onClick={handleGenerate}
                                disabled={loading || !canGenerate}
                            >
                                <span className={`material-symbols-outlined ${loading ? 'spin-anim' : ''}`}>
                                    {loading ? 'sync' : 'auto_awesome'}
                                </span>
                                {loading ? 'Đang tạo...' : 'Tạo preview'}
                            </button>
                        </div>
                    </section>

                    <section className="ai-test-context-panel">
                        <label className="ai-test-field">
                            <span>Tiêu đề bài toán</span>
                            <input className="option-input" value={title} readOnly/>
                        </label>

                        <label className="ai-test-field">
                            <span>Mô tả bài toán</span>
                            <textarea className="option-input ai-test-contextarea" value={description} readOnly rows={5}/>
                        </label>

                        <label className="ai-test-field">
                            <span>Ràng buộc và cấu hình chấm bài</span>
                            <textarea
                                className="option-input ai-test-contextarea"
                                value={constraints}
                                rows={6}
                                onChange={(event) => setConstraints(event.target.value)}
                            />
                        </label>
                    </section>
                </div>

                {hasPreview && (
                    <div className="ai-test-preview">
                        <div className="ai-test-preview-heading">
                            <div>
                                <h3>Preview test cases</h3>
                                <p>Kiểm tra input, output, thứ tự và trạng thái hiển thị trước khi thêm vào form.</p>
                            </div>
                            <span>{previewCases.length} test cases</span>
                        </div>
                        <div className="ai-test-preview-table-wrap">
                            <table className="ai-test-preview-table">
                                <thead>
                                <tr>
                                    <th>Đầu vào</th>
                                    <th>Đầu ra</th>
                                    <th>Thứ tự</th>
                                    <th>Trọng số</th>
                                    <th>Hiển thị</th>
                                    <th>Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                {previewCases.map((testCase, index) => (
                                    <tr key={testCase.id}>
                                        <td>
                        <textarea
                            className="option-input ai-test-case-textarea"
                            value={testCase.input}
                            rows={3}
                            onChange={(event) => updatePreviewCase(testCase.id, 'input', event.target.value)}
                        />
                                        </td>
                                        <td>
                        <textarea
                            className="option-input ai-test-case-textarea"
                            value={testCase.output}
                            rows={3}
                            onChange={(event) => updatePreviewCase(testCase.id, 'output', event.target.value)}
                        />
                                        </td>
                                        <td>
                                            <input
                                                className="option-input ai-test-number-input"
                                                type="number"
                                                min="0"
                                                step="1"
                                                value={testCase.orderIndex ?? index}
                                                onChange={(event) => updatePreviewCase(testCase.id, 'orderIndex', event.target.value)}
                                            />
                                        </td>
                                        <td>
                                            <input
                                                className="option-input ai-test-number-input"
                                                type="number"
                                                min="0.01"
                                                step="0.01"
                                                value={testCase.scoreWeight ?? 1}
                                                onChange={(event) => updatePreviewCase(testCase.id, 'scoreWeight', event.target.value)}
                                            />
                                        </td>
                                        <td>
                                            <select
                                                className="type-select ai-test-visibility"
                                                value={testCase.hidden ? 'hidden' : 'public'}
                                                onChange={(event) => updatePreviewCase(testCase.id, 'hidden', event.target.value === 'hidden')}
                                            >
                                                <option value="public">Công khai</option>
                                                <option value="hidden">Ẩn</option>
                                            </select>
                                        </td>
                                        <td>
                                            <button className="ai-test-delete" type="button"
                                                    onClick={() => deletePreviewCase(testCase.id)}>
                                                <span className="material-symbols-outlined">delete</span>
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="ai-test-preview-actions">
                            <button type="button" className="ai-test-secondary" onClick={handleGenerate} disabled={loading || !canGenerate}>
                                <span className={`material-symbols-outlined ${loading ? 'spin-anim' : ''}`}>
                                    {loading ? 'sync' : 'refresh'}
                                </span>
                                Tạo lại
                            </button>
                            <button type="button" className="ai-test-apply" onClick={handleApply}>
                                <span className="material-symbols-outlined">add_task</span>
                                Thêm vào form test cases
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
