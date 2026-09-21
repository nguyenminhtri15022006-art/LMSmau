import React from 'react';
import '../../../styles/teacher/CreateQuiz/stepSettings.css';

export default function StepSettings({
                                         settings,
                                         lessonOptions = [],
                                         onSettingsChange,
                                         onSaveDraft,
                                         onPublish,
                                         onCancel,
                                         isSaving,
                                     }) {
    const update = (key, value) => onSettingsChange({...settings, [key]: value});
    const lessonEmptyText = 'Khác';

    return (
        <div className="step-3-settings">
            <div className="settings-section">
                <div className="section-title">
                    <span className="material-symbols-outlined">info</span>
                    Thông tin cơ bản
                </div>
                <div className="settings-card">
                    <div className="form-group full-width">
                        <label>Tên bài kiểm tra</label>
                        <input
                            type="text"
                            className="form-input"
                            value={settings.title}
                            onChange={(event) => update('title', event.target.value)}
                            placeholder="Nhập tên bài kiểm tra"
                        />
                    </div>
                    <div className="form-group full-width">
                        <label>Mô tả</label>
                        <textarea
                            className="form-input settings-textarea"
                            value={settings.description}
                            onChange={(event) => update('description', event.target.value)}
                            placeholder="Mô tả ngắn về nội dung bài kiểm tra"
                        />
                    </div>
                    <div className="form-row">
                        <div className="form-group">
                            <label>Gán cho bài học</label>
                            <select
                                className="form-input"
                                value={settings.lessonId}
                                onChange={(event) => update('lessonId', event.target.value)}
                            >
                                <option value="">{lessonEmptyText}</option>
                                {lessonOptions.map(option => (
                                    <option key={option.value} value={option.value}>{option.label}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group">
                            <label>Thời gian làm bài</label>
                            <div className="input-with-suffix">
                                <input
                                    type="number"
                                    min="1"
                                    max="300"
                                    className="form-input"
                                    value={settings.duration}
                                    onChange={(event) => update('duration', event.target.value)}
                                    placeholder="Không giới hạn"
                                />
                                <span className="suffix">phút</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="settings-section">
                <div className="section-title">
                    <span className="material-symbols-outlined">timer</span>
                    Quy tắc làm bài
                </div>
                <div className="settings-card">
                    <div className="settings-grid">
                        <div className="form-group">
                            <label>Tổng điểm</label>
                            <input
                                type="number"
                                min="0"
                                max="1000"
                                className="form-input"
                                value={settings.totalScore}
                                onChange={(event) => update('totalScore', event.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Điểm đạt</label>
                            <div className="input-with-suffix">
                                <input
                                    type="number"
                                    min="0"
                                    max="100"
                                    className="form-input"
                                    value={settings.passScore}
                                    onChange={(event) => update('passScore', event.target.value)}
                                />
                                <span className="suffix">%</span>
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Số lần làm tối đa</label>
                            <input
                                type="number"
                                min="0"
                                max="127"
                                className="form-input"
                                placeholder="0 (Không giới hạn)"
                                value={settings.maxAttempts}
                                onChange={(event) => update('maxAttempts', event.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Hiển thị đáp án</label>
                            <select
                                className="form-input"
                                value={settings.showResult}
                                onChange={(event) => update('showResult', event.target.value)}
                            >
                                <option value="IMMEDIATELY">Ngay lập tức (Luyện tập)</option>
                                <option value="AFTER_SUBMIT">Sau khi nộp (Kiểm tra)</option>
                                <option value="AFTER_DEADLINE">Sau hạn làm bài (Kiểm tra)</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <div className="settings-section">
                <div className="section-title">
                    <span className="material-symbols-outlined">shuffle</span>
                    Tùy chọn trộn
                </div>
                <div className="settings-card rules-list">
                    <div className="rule-item">
                        <div className="rule-info">
                            <h4>Trộn câu hỏi</h4>
                            <p>Xáo trộn thứ tự xuất hiện của các câu hỏi.</p>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                checked={!!settings.shuffleQuestions}
                                onChange={(event) => update('shuffleQuestions', event.target.checked)}
                            />
                            <span className="slider"></span>
                        </label>
                    </div>
                    <div className="rule-item border-none">
                        <div className="rule-info">
                            <h4>Trộn câu trả lời</h4>
                            <p>Xáo trộn các tùy chọn trắc nghiệm cho mỗi sinh viên.</p>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                checked={!!settings.shuffleAnswers}
                                onChange={(event) => update('shuffleAnswers', event.target.checked)}
                            />
                            <span className="slider"></span>
                        </label>
                    </div>
                </div>
            </div>

            <div className="settings-actions">
                <button className="btn-secondary large" onClick={onCancel} disabled={isSaving}>Hủy</button>
                <button className="btn-secondary large" onClick={onSaveDraft} disabled={isSaving}>
                    {isSaving ? 'Đang lưu...' : 'Lưu nháp'}
                </button>
                <button className="btn-primary large" onClick={onPublish} disabled={isSaving}>
                    {isSaving ? 'Đang lưu...' : 'Hoàn tất & Xuất bản'}
                </button>
            </div>
        </div>
    );
}
