import React from 'react';
import '../../../styles/teacher/ManualQuiz/quizSettingsForm.css';

export default function QuizSettingsForm({settings, onChange}) {
    return (
        <>
            <div className="settings-section">
                <div className="section-header">
                    <span className="material-symbols-outlined">timer</span>
                    Quy tắc làm bài
                </div>
                <div className="settings-card">
                    <div className="settings-grid">
                        <div className="form-group">
                            <label>Thời gian (phút)</label>
                            <div className="input-with-icon">
                                <span className="material-symbols-outlined">timer</span>
                                <input
                                    type="number"
                                    placeholder="30"
                                    value={settings.timeLimit}
                                    onChange={(e) => onChange('timeLimit', e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Điểm đạt (%)</label>
                            <div className="input-with-icon">
                                <span className="material-symbols-outlined">grade</span>
                                <input
                                    type="number"
                                    placeholder="70"
                                    value={settings.passingScore}
                                    onChange={(e) => onChange('passingScore', e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Tổng điểm</label>
                            <div className="input-with-icon">
                                <span className="material-symbols-outlined">score</span>
                                <input
                                    type="number"
                                    placeholder="100"
                                    value={settings.totalScore}
                                    onChange={(e) => onChange('totalScore', e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Số lần làm tối đa</label>
                            <div className="input-with-icon">
                                <span className="material-symbols-outlined">replay</span>
                                <input
                                    type="number"
                                    placeholder="0 (Không giới hạn)"
                                    value={settings.maxAttempts}
                                    onChange={(e) => onChange('maxAttempts', e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Hiển thị kết quả</label>
                            <div className="input-with-icon">
                                <span className="material-symbols-outlined">visibility</span>
                                <select
                                    value={settings.showResult}
                                    onChange={(e) => onChange('showResult', e.target.value)}
                                >
                                    <option value="IMMEDIATELY">Ngay lập tức (Luyện tập)</option>
                                    <option value="AFTER_SUBMIT">Sau khi nộp (Kiểm tra)</option>
                                    <option value="AFTER_DEADLINE">Sau hạn làm bài (Kiểm tra)</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="settings-section">
                <div className="section-header">
                    <span className="material-symbols-outlined">shuffle</span>
                    Tùy chọn trộn
                </div>
                <div className="settings-card">
                    <div className="rule-item">
                        <div className="rule-info">
                            <h4>Trộn câu hỏi</h4>
                            <p>Xáo trộn thứ tự xuất hiện của các câu hỏi.</p>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                checked={!!settings.shuffleQuestions}
                                onChange={(e) => onChange('shuffleQuestions', e.target.checked)}
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
                                onChange={(e) => onChange('shuffleAnswers', e.target.checked)}
                            />
                            <span className="slider"></span>
                        </label>
                    </div>
                </div>
            </div>
        </>
    );
}
