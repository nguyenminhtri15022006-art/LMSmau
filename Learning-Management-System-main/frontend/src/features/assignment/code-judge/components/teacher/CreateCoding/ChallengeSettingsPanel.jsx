import React from 'react';
import {POPULAR_LANGUAGES} from '../../../constants/languageBoilerplates';
import '../../../styles/teacher/CreateCoding/challengeSettingsPanel.css';

const difficultyLevels = ['Easy', 'Medium', 'Hard'];
const difficultyToneByLevel = {
    Easy: 'easy',
    Medium: 'medium',
    Hard: 'hard',
};
const languageOptions = POPULAR_LANGUAGES.map((language) => ({
    value: language.id,
    label: `${language.name} ${language.version}`,
}));

export default function ChallengeSettingsPanel({
                                                   selectedChapter,
                                                   lessonOptions = [],
                                                   difficulty,
                                                   selectedLanguages = [],
                                                   score,
                                                   timeLimitMs,
                                                   memoryLimitMb,
                                                   onChapterChange,
                                                   onDifficultyChange,
                                                   onLanguagesChange,
                                                   onScoreChange,
                                                   onTimeLimitChange,
                                                   onMemoryLimitChange,
                                                   onPublish,
                                                   onSaveDraft,
                                                   saving,
                                               }) {
    const allLanguageIds = languageOptions.map((option) => option.value);
    const allSelected = selectedLanguages.length === allLanguageIds.length;

    const toggleLanguage = (languageId) => {
        const nextLanguages = selectedLanguages.includes(languageId)
            ? selectedLanguages.filter((item) => item !== languageId)
            : [...selectedLanguages, languageId];
        onLanguagesChange(nextLanguages);
    };

    const toggleAllLanguages = () => {
        onLanguagesChange(allSelected ? [] : allLanguageIds);
    };

    return (
        <div className="sidebar-column create-coding-settings-panel">
            <div className="editor-card">
                <div className="settings-panel-heading">
                    <span className="material-symbols-outlined">tune</span>
                    <div>
                        <h2>Cấu hình chấm bài</h2>
                        <p>Thiết lập độ khó, điểm và giới hạn chạy.</p>
                    </div>
                </div>

                <div className="form-group">
                    <label>Giao cho bài học</label>
                    <select
                        className="option-input settings-select"
                        value={selectedChapter}
                        onChange={(event) => onChapterChange(event.target.value)}
                    >
                        <option value="">Khác</option>
                        {lessonOptions.map((option) => (
                            <option key={option.value} value={option.value}>{option.label}</option>
                        ))}
                    </select>
                </div>

                <div className="form-group settings-form-group">
                    <label>Độ khó</label>
                    <div className="difficulty-selector">
                        {difficultyLevels.map((level) => (
                            <button
                                key={level}
                                type="button"
                                className={`diff-btn diff-btn--${difficultyToneByLevel[level]} ${difficulty === level ? 'active' : ''}`}
                                onClick={() => onDifficultyChange(level)}
                            >
                                {level}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="form-group settings-form-group">
                    <label>Ngôn ngữ</label>
                    <div className="language-picker">
                        <button
                            type="button"
                            className={`language-all-toggle ${allSelected ? 'active' : ''}`}
                            onClick={toggleAllLanguages}
                        >
                            Tất cả ngôn ngữ
                        </button>
                        <div className="language-option-grid">
                            {languageOptions.map((option) => (
                                <label key={option.value} className="language-option">
                                    <input
                                        type="checkbox"
                                        checked={selectedLanguages.includes(option.value)}
                                        onChange={() => toggleLanguage(option.value)}
                                    />
                                    <span>{option.label}</span>
                                </label>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="settings-number-grid">
                    <div className="form-group settings-form-group">
                        <label>Điểm</label>
                        <input
                            type="number"
                            className="option-input settings-select"
                            min="1"
                            max="1000"
                            value={score}
                            onChange={(event) => onScoreChange(event.target.value)}
                        />
                    </div>
                    <div className="form-group settings-form-group">
                        <label>Thời gian (ms)</label>
                        <input
                            type="number"
                            className="option-input settings-select"
                            min="50"
                            max="10000"
                            step="50"
                            value={timeLimitMs}
                            onChange={(event) => onTimeLimitChange(Number(event.target.value))}
                        />
                    </div>
                    <div className="form-group settings-form-group">
                        <label>Bộ nhớ (MB)</label>
                        <input
                            type="number"
                            className="option-input settings-select"
                            min="16"
                            max="512"
                            value={memoryLimitMb}
                            onChange={(event) => onMemoryLimitChange(Number(event.target.value))}
                        />
                    </div>
                </div>
            </div>

            <div className="settings-action-row">
                <button className="btn-draft" onClick={onSaveDraft} disabled={saving}>
                    Lưu nháp
                </button>
                <button className="btn-publish" onClick={onPublish} disabled={saving}>
                    {saving ? 'Đang lưu...' : 'Xuất bản'}
                </button>
            </div>

            <div className="settings-autosave-note">
        <span>
          <span className="material-symbols-outlined">schedule</span>
          Lưu lên hệ thống khi bấm Xuất bản hoặc Lưu bản nháp
        </span>
            </div>
        </div>
    );
}
