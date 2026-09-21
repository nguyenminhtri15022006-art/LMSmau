import React, {lazy, Suspense, useCallback, useEffect, useRef, useState} from 'react';
import ConsoleOutput from '../../../../shared/components/ConsoleOutput';
import {getLanguageById, POPULAR_LANGUAGES} from '../../../constants/languageBoilerplates';

const CodeEditor = lazy(() => import('./CodeEditor'));

export default function EditorPanel({
                                        code,
                                        setCode,
                                        language,
                                        onLanguageChange,
                                        isRunning,
                                        showResults,
                                        runResult,
                                        runError,
                                        isSubmitting = false,
                                        handleRunCode,
                                        handleSubmitSolution,
                                    }) {
    const [consoleHeight, setConsoleHeight] = useState(200);
    const [isConsoleVisible, setIsConsoleVisible] = useState(false);
    const [showLangDropdown, setShowLangDropdown] = useState(false);
    const isDragging = useRef(false);
    const dropdownRef = useRef(null);

    const currentLang = getLanguageById(language);
    const isBusy = isRunning || isSubmitting;

    const startResizing = useCallback(() => {
        isDragging.current = true;
        document.body.style.cursor = 'ns-resize';
        document.body.style.userSelect = 'none';
    }, []);

    const stopResizing = useCallback(() => {
        isDragging.current = false;
        document.body.style.cursor = 'default';
        document.body.style.userSelect = 'auto';
    }, []);

    const resize = useCallback(
        (e) => {
            if (!isDragging.current) return;
            const newHeight = window.innerHeight - e.clientY - 20;
            if (newHeight > 100 && newHeight < 380) {
                setConsoleHeight(newHeight);
            }
        },
        [],
    );

    useEffect(() => {
        window.addEventListener('mousemove', resize);
        window.addEventListener('mouseup', stopResizing);
        return () => {
            window.removeEventListener('mousemove', resize);
            window.removeEventListener('mouseup', stopResizing);
        };
    }, [resize, stopResizing]);

    useEffect(() => {
        function handleClickOutside(e) {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setShowLangDropdown(false);
            }
        }

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSelectLanguage = (langId) => {
        onLanguageChange(langId);
        setShowLangDropdown(false);
    };

    const handleRunClick = () => {
        setIsConsoleVisible(true);
        handleRunCode();
    };

    const handleSubmitClick = () => {
        setIsConsoleVisible(true);
        handleSubmitSolution();
    };

    return (
        <div className="editor-panel">
            <div className="editor-header">
                <div className="lang-selector" ref={dropdownRef}>
                    <button
                        className="lang-badge-btn"
                        onClick={() => setShowLangDropdown(!showLangDropdown)}
                    >
                        <span className="material-symbols-outlined" style={{fontSize: '16px'}}>code</span>
                        <span>{currentLang.icon} {currentLang.name}</span>
                        <span className="lang-version">{currentLang.version}</span>
                        <span
                            className="material-symbols-outlined"
                            style={{
                                fontSize: '14px',
                                transition: 'transform 0.2s',
                                transform: showLangDropdown ? 'rotate(180deg)' : 'rotate(0)'
                            }}
                        >
              expand_more
            </span>
                    </button>

                    {showLangDropdown && (
                        <div className="lang-dropdown">
                            {POPULAR_LANGUAGES.map((lang) => (
                                <button
                                    key={lang.id}
                                    className={`lang-dropdown-item ${lang.id === language ? 'active' : ''}`}
                                    onClick={() => handleSelectLanguage(lang.id)}
                                >
                                    <span>{lang.icon}</span>
                                    <span>{lang.name}</span>
                                    <span className="lang-version-item">{lang.version}</span>
                                    {lang.id === language && (
                                        <span className="material-symbols-outlined" style={{
                                            fontSize: '16px',
                                            color: '#60a5fa',
                                            marginLeft: 'auto'
                                        }}>check</span>
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                <div className="editor-actions">
                    <button className="run-btn" onClick={handleRunClick} disabled={isBusy}>
                        <span className="material-symbols-outlined" style={{fontSize: '16px'}}>play_arrow</span>
                        {isRunning ? 'Đang chạy...' : 'Chạy mã'}
                    </button>
                    <button className="submit-btn" onClick={handleSubmitClick} disabled={isBusy}>
                        <span className="material-symbols-outlined" style={{fontSize: '16px'}}>task_alt</span>
                        {isSubmitting ? 'Đang nộp...' : 'Gửi bài'}
                    </button>
                </div>
            </div>

            <Suspense
                fallback={
                    <div className="code-editor-shell">
                        <div className="code-editor-loading">Loading editor...</div>
                    </div>
                }
            >
                <CodeEditor code={code} setCode={setCode} language={language}/>
            </Suspense>

            {isConsoleVisible && (
                <ConsoleOutput
                    isRunning={isRunning}
                    showResults={showResults}
                    runResult={runResult}
                    runError={runError}
                    height={consoleHeight}
                    onClose={() => setIsConsoleVisible(false)}
                    onResizeStart={startResizing}
                />
            )}
        </div>
    );
}
