import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Navigate, useLocation, useNavigate, useOutletContext, useParams} from 'react-router-dom';
import {assignmentApi} from '../../../../../services/assignment.api';
import {buildAppErrorState} from '../../../../../utils/appError';
import {parseBackendUtcDate} from '../../../../../utils/dateTime';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import QuizProgressSidebar from '../../components/student/QuizProgressSidebar';
import QuizTimer from '../../components/student/QuizTimer';

import '../../../shared/styles/ExerciseShared.css';

const LABELS = ['A', 'B', 'C', 'D', 'E', 'F'];

function QuestionPromptMedia({question}) {
    if (!question?.imageUrl) return null;

    return (
        <div className="question-image">
            <img src={question.imageUrl} alt="Ảnh minh họa câu hỏi"/>
        </div>
    );
}

function QuestionTopic({topic}) {
    if (!topic) return null;

    return (
        <span className="question-topic">
            <span className="material-symbols-outlined">sell</span>
            {topic}
        </span>
    );
}

export default function ExerciseQuizPage() {
    const {id} = useParams();
    const quizId = parseInt(id, 10);
    const navigate = useNavigate();
    const location = useLocation();
    const outletContext = useOutletContext();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [session, setSession] = useState(null);
    const [answers, setAnswers] = useState({});
    const [markedForReview, setMarkedForReview] = useState(new Set());
    const [timeLeft, setTimeLeft] = useState(7200);
    const [dialog, setDialog] = useState(null);
    const [showAllAnsweredBanner, setShowAllAnsweredBanner] = useState(false);
    const [practiceIdx, setPracticeIdx] = useState(0);
    const [practiceCompleted, setPracticeCompleted] = useState(new Set());
    const [practiceFeedback, setPracticeFeedback] = useState(null);
    const [practiceChecking, setPracticeChecking] = useState(false);
    const [practiceCopyStatus, setPracticeCopyStatus] = useState('idle');

    const questionRefs = useRef([]);
    const autoSaveTimer = useRef(null);
    const practiceAdvanceTimerRef = useRef(null);
    const practiceCopyTimerRef = useRef(null);
    const startAttemptRef = useRef({quizId: null, promise: null});
    const isSubmitting = useRef(false);
    const autoSubmitTriggeredRef = useRef(false);
    const timeOffsetRef = useRef(0);
    const courseTitle = location.state?.courseTitle || '';
    const courseSlug = location.state?.courseSlug || '';
    const setChatbotVisibilityOverride = outletContext?.setChatbotVisibilityOverride;

    const getServerNow = () => Date.now() + timeOffsetRef.current;
    const isPracticeMode = session?.showResult === 'IMMEDIATELY';
    const practiceStorageKey = session?.attemptId ? `quiz-practice-${session.attemptId}` : null;

    const clearAutoSaveTimer = useCallback(() => {
        if (autoSaveTimer.current) {
            clearInterval(autoSaveTimer.current);
            autoSaveTimer.current = null;
        }
    }, []);

    const clearPracticeAdvanceTimer = useCallback(() => {
        if (practiceAdvanceTimerRef.current) {
            clearTimeout(practiceAdvanceTimerRef.current);
            practiceAdvanceTimerRef.current = null;
        }
    }, []);

    const clearPracticeCopyTimer = useCallback(() => {
        if (practiceCopyTimerRef.current) {
            clearTimeout(practiceCopyTimerRef.current);
            practiceCopyTimerRef.current = null;
        }
    }, []);

    useEffect(() => {
        let cancelled = false;
        const start = async () => {
            try {
                setLoading(true);
                if (startAttemptRef.current.quizId !== quizId || !startAttemptRef.current.promise) {
                    startAttemptRef.current = {
                        quizId,
                        promise: assignmentApi.startOrResumeAttempt(quizId),
                    };
                }

                const attempt = await startAttemptRef.current.promise;
                if (cancelled) return;
                setSession(attempt);

                if (attempt.serverTime) {
                    timeOffsetRef.current = parseBackendUtcDate(attempt.serverTime).getTime() - Date.now();
                }

                const initialAnswers = {};
                (attempt.questions || []).forEach(q => {
                    if (q.selectedAnswerIds && q.selectedAnswerIds.length > 0) {
                        initialAnswers[q.questionId] = q.selectedAnswerIds;
                    }
                });
                setAnswers(initialAnswers);

                if (attempt.expiresAt) {
                    const remaining = Math.max(0, Math.floor((parseBackendUtcDate(attempt.expiresAt).getTime() - getServerNow()) / 1000));
                    setTimeLeft(remaining || 7200);
                } else if (attempt.duration) {
                    setTimeLeft(attempt.duration * 60);
                } else {
                    setTimeLeft(7200);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(buildAppErrorState(err, {
                        fallbackPath: `/exercises/quiz/${quizId}/overview`,
                        fallbackState: {courseTitle, courseSlug},
                    }));
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        };
        start();
        return () => {
            cancelled = true;
        };
    }, [quizId]);

    useEffect(() => {
        if (!session || !session.attemptId || isPracticeMode) return;
        autoSaveTimer.current = setInterval(() => {
            const payload = {
                answers: Object.entries(answers).map(([questionId, selectedAnswerIds]) => ({
                    questionId: Number(questionId),
                    selectedAnswerIds,
                })),
            };
            assignmentApi.autoSaveAttempt(session.quizId, session.attemptId, payload).catch(() => {
            });
        }, 30000);
        return () => {
            clearAutoSaveTimer();
        };
    }, [session, answers, isPracticeMode, clearAutoSaveTimer]);

    useEffect(() => {
        if (!session?.questions?.length || isPracticeMode) return;
        const allAnswered = session.questions.every(q => (answers[q.questionId] || []).length > 0);
        setShowAllAnsweredBanner(allAnswered);
    }, [answers, session?.questions, isPracticeMode]);

    useEffect(() => {
        if (!practiceStorageKey) return;
        try {
            const saved = JSON.parse(localStorage.getItem(practiceStorageKey) || '[]');
            setPracticeCompleted(new Set(saved));
        } catch {
            setPracticeCompleted(new Set());
        }
    }, [practiceStorageKey]);

    const savePracticeCompleted = useCallback((nextSet) => {
        setPracticeCompleted(nextSet);
        if (practiceStorageKey) {
            localStorage.setItem(practiceStorageKey, JSON.stringify([...nextSet]));
        }
    }, [practiceStorageKey]);

    const scrollToQuestion = (idx) => {
        questionRefs.current[idx]?.scrollIntoView({behavior: 'smooth', block: 'center'});
    };

    const countUnanswered = useCallback(() => {
        if (!session?.questions) return 0;
        return session.questions.filter(q => (answers[q.questionId] || []).length === 0).length;
    }, [session?.questions, answers]);

    const countIncompletePracticeQuestions = useCallback((completedSet = practiceCompleted) => {
        if (!session?.questions?.length) return 0;
        return session.questions.filter(question => !completedSet.has(question.questionId)).length;
    }, [practiceCompleted, session]);

    const isPracticeComplete = useCallback(
        (completedSet = practiceCompleted) => countIncompletePracticeQuestions(completedSet) === 0,
        [countIncompletePracticeQuestions, practiceCompleted]
    );

    const handleSubmit = useCallback(async () => {
        if (!session || !session.attemptId || isSubmitting.current) return;
        isSubmitting.current = true;
        clearAutoSaveTimer();
        setDialog(null);

        const payload = {
            answers: Object.entries(answers).map(([questionId, selectedAnswerIds]) => ({
                questionId: Number(questionId),
                selectedAnswerIds,
            })),
        };
        try {
            const result = await assignmentApi.submitAttempt(session.quizId, session.attemptId, payload);
            if (practiceStorageKey) localStorage.removeItem(practiceStorageKey);
            navigate(`/exercises/quiz/${quizId}/result`, {
                state: {quizId, attemptId: session.attemptId, result, courseTitle, courseSlug},
            });
        } catch (err) {
            isSubmitting.current = false;
            setDialog({type: 'submit-error', message: err.response?.data?.message || err.message});
        }
    }, [navigate, session, quizId, answers, courseTitle, courseSlug, clearAutoSaveTimer, practiceStorageKey]);

    const handleSubmitClick = useCallback(() => {
        if (!session || !session.attemptId) return;
        if (isPracticeMode) {
            if (isPracticeComplete()) {
                handleSubmit();
                return;
            }
            setDialog({
                type: 'practice-incomplete',
                unanswered: countIncompletePracticeQuestions(),
            });
            return;
        }
        const unanswered = countUnanswered();
        setDialog(unanswered === 0 ? {type: 'all-answered'} : {type: 'has-unanswered', unanswered});
    }, [session, countUnanswered, countIncompletePracticeQuestions, handleSubmit, isPracticeComplete, isPracticeMode]);

    const handleAutoSubmit = useCallback(async () => {
        if (!session || !session.attemptId || isSubmitting.current || autoSubmitTriggeredRef.current) return;
        autoSubmitTriggeredRef.current = true;
        isSubmitting.current = true;
        clearAutoSaveTimer();

        try {
            setDialog({type: 'timeout'});
            const payload = {
                answers: Object.entries(answers).map(([questionId, selectedAnswerIds]) => ({
                    questionId: Number(questionId),
                    selectedAnswerIds,
                })),
            };
            const result = await assignmentApi.submitAttempt(session.quizId, session.attemptId, payload);
            await new Promise(resolve => setTimeout(resolve, 1500));
            navigate(`/exercises/quiz/${quizId}/result`, {
                state: {quizId, attemptId: session.attemptId, result, courseTitle, courseSlug},
            });
        } catch (err) {
            setDialog({
                type: 'auto-submit-error',
                message: err.response?.data?.message || err.message || 'Không thể nộp bài tự động.',
            });
        } finally {
            isSubmitting.current = false;
        }
    }, [navigate, session, quizId, answers, courseTitle, courseSlug, clearAutoSaveTimer]);

    useEffect(() => {
        if (!session?.expiresAt) return;
        const expiresAtMs = parseBackendUtcDate(session.expiresAt).getTime();
        const tick = () => {
            const remaining = Math.max(0, Math.floor((expiresAtMs - getServerNow()) / 1000));
            setTimeLeft(remaining);
        };
        tick();
        const timer = setInterval(tick, 1000);
        return () => clearInterval(timer);
    }, [session]);

    useEffect(() => {
        if (timeLeft <= 0 && session && !autoSubmitTriggeredRef.current) {
            handleAutoSubmit();
        }
    }, [timeLeft, session, handleAutoSubmit]);

    const handleSelectOption = (questionId, answerId, type) => {
        setAnswers(prev => {
            if (type === 'MULTIPLE') {
                const current = prev[questionId] || [];
                const idx = current.indexOf(answerId);
                if (idx >= 0) {
                    return {...prev, [questionId]: current.filter(id => id !== answerId)};
                }
                return {...prev, [questionId]: [...current, answerId]};
            }
            return {...prev, [questionId]: [answerId]};
        });
    };

    const isSelected = (questionId, answerId) => (answers[questionId] || []).includes(answerId);

    const copyTextToClipboard = useCallback(async (text) => {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(text);
            return;
        }

        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.setAttribute('readonly', '');
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }, []);

    const handleCopyPracticeQuestion = useCallback(async () => {
        const question = session?.questions?.[practiceIdx];
        if (!question) return;

        const lines = [
            `Câu hỏi ${practiceIdx + 1}: ${question.content || ''}`,
            '',
            ...(question.answers || []).slice(0, 4).map((option, idx) => (
                `${LABELS[idx] || idx}. ${option.content || ''}`
            )),
        ];

        try {
            await copyTextToClipboard(lines.join('\n'));
            setPracticeCopyStatus('copied');
        } catch {
            setPracticeCopyStatus('error');
        } finally {
            clearPracticeCopyTimer();
            practiceCopyTimerRef.current = setTimeout(() => {
                setPracticeCopyStatus('idle');
                practiceCopyTimerRef.current = null;
            }, 1400);
        }
    }, [clearPracticeCopyTimer, copyTextToClipboard, practiceIdx, session]);

    const getPracticeFeedbackForQuestion = (questionId) => (
        practiceFeedback && (practiceFeedback.questionId == null || practiceFeedback.questionId === questionId)
            ? practiceFeedback
            : null
    );

    const getPracticeOptionState = (question, option) => {
        const feedback = getPracticeFeedbackForQuestion(question.questionId);
        if (!feedback || !['correct', 'incorrect'].includes(feedback.type)) return '';
        if (!isSelected(question.questionId, option.answerId)) return '';
        return feedback.type === 'correct' ? 'practice-correct' : 'practice-incorrect';
    };

    const toggleReview = (idx) => {
        setMarkedForReview(prev => {
            const newSet = new Set(prev);
            if (newSet.has(idx)) newSet.delete(idx);
            else newSet.add(idx);
            return newSet;
        });
    };

    const findNextIncompletePracticeIndex = useCallback((completedSet = practiceCompleted) => {
        if (!session?.questions?.length) return -1;

        for (let offset = 1; offset < session.questions.length; offset += 1) {
            const nextIndex = (practiceIdx + offset) % session.questions.length;
            const nextQuestion = session.questions[nextIndex];
            if (nextQuestion && !completedSet.has(nextQuestion.questionId)) {
                return nextIndex;
            }
        }

        return -1;
    }, [practiceCompleted, practiceIdx, session]);

    const goToNextPracticeQuestion = useCallback((completedSet = practiceCompleted) => {
        if (!session?.questions?.length) return;

        clearPracticeAdvanceTimer();
        setPracticeFeedback(null);

        const nextIndex = findNextIncompletePracticeIndex(completedSet);
        if (nextIndex >= 0) {
            setPracticeIdx(nextIndex);
            return;
        }

        if (isPracticeComplete(completedSet)) return;
    }, [clearPracticeAdvanceTimer, findNextIncompletePracticeIndex, isPracticeComplete, practiceCompleted, session]);

    const handlePracticeQuestionSelect = useCallback((idx) => {
        clearPracticeAdvanceTimer();
        setPracticeFeedback(null);
        setPracticeIdx(idx);
    }, [clearPracticeAdvanceTimer]);

    const checkPracticeAnswer = useCallback(async (question = session.questions[practiceIdx], selectedOverride = null) => {
        if (!question || practiceChecking) return;
        const selectedAnswerIds = selectedOverride || answers[question.questionId] || [];
        if (selectedAnswerIds.length === 0) {
            setPracticeFeedback({type: 'warning', text: 'Hãy chọn đáp án trước khi kiểm tra.'});
            return;
        }

        setPracticeChecking(true);
        try {
            const response = await assignmentApi.checkPracticeAnswer(session.quizId, session.attemptId, {
                questionId: question.questionId,
                selectedAnswerIds,
            });

            if (response.correct) {
                const nextCompleted = new Set(practiceCompleted);
                nextCompleted.add(question.questionId);
                savePracticeCompleted(nextCompleted);
                setPracticeFeedback({
                    questionId: question.questionId,
                    selectedAnswerIds,
                    type: 'correct',
                    text: response.explanation || 'Chính xác.',
                });
                clearPracticeAdvanceTimer();
                if (practiceIdx < session.questions.length - 1) {
                    practiceAdvanceTimerRef.current = setTimeout(() => {
                        goToNextPracticeQuestion(nextCompleted);
                    }, 5000);
                }
            } else {
                setPracticeFeedback({type: 'incorrect', text: 'Chưa đúng. Hãy chọn lại đáp án khác.'});
            }
        } catch (err) {
            setPracticeFeedback({
                type: 'warning',
                text: err.response?.data?.message || 'Không thể kiểm tra đáp án.',
            });
        } finally {
            setPracticeChecking(false);
        }
    }, [
        answers,
        clearPracticeAdvanceTimer,
        goToNextPracticeQuestion,
        practiceChecking,
        practiceCompleted,
        practiceIdx,
        savePracticeCompleted,
        session,
    ]);

    const handlePracticeNext = useCallback(() => {
        if (!isPracticeMode || !session?.questions?.length) return;
        const question = session.questions[practiceIdx];
        if (!question) return;

        if (practiceCompleted.has(question.questionId)) {
            if (isPracticeComplete()) {
                handleSubmit();
                return;
            }
            goToNextPracticeQuestion();
            return;
        }

        checkPracticeAnswer(question);
    }, [checkPracticeAnswer, goToNextPracticeQuestion, handleSubmit, isPracticeComplete, isPracticeMode, practiceCompleted, practiceIdx, session]);

    const handlePracticeEnter = useCallback(() => {
        if (!isPracticeMode || !session?.questions?.length) return;
        const question = session.questions[practiceIdx];
        if (!question) return;

        if (practiceCompleted.has(question.questionId) && isPracticeComplete()) {
            handleSubmit();
            return;
        }

        handlePracticeNext();
    }, [handlePracticeNext, handleSubmit, isPracticeComplete, isPracticeMode, practiceCompleted, practiceIdx, session]);

    useEffect(() => {
        return () => {
            clearPracticeAdvanceTimer();
            clearPracticeCopyTimer();
        };
    }, [clearPracticeAdvanceTimer, clearPracticeCopyTimer]);

    useEffect(() => {
        setPracticeCopyStatus('idle');
        clearPracticeCopyTimer();
    }, [clearPracticeCopyTimer, practiceIdx]);

    useEffect(() => {
        if (!setChatbotVisibilityOverride || !session?.showResult) return undefined;
        setChatbotVisibilityOverride(isPracticeMode);
        return () => setChatbotVisibilityOverride(null);
    }, [isPracticeMode, session?.showResult, setChatbotVisibilityOverride]);

    useEffect(() => {
        if (!isPracticeMode || !session?.questions?.length || dialog || practiceChecking) return;

        const handleKeyDown = (event) => {
            const tagName = event.target?.tagName?.toLowerCase();
            if (tagName === 'input' || tagName === 'textarea' || tagName === 'select' || tagName === 'button' || event.target?.isContentEditable) {
                return;
            }

            const question = session.questions[practiceIdx];
            if (!question) return;

            if (['1', '2', '3', '4'].includes(event.key)) {
                const optionIndex = Number(event.key) - 1;
                const option = question.answers?.[optionIndex];
                if (!option) return;
                event.preventDefault();
                handleSelectOption(question.questionId, option.answerId, question.type);
                setPracticeFeedback(null);
                return;
            }

            if (event.key === 'Enter') {
                event.preventDefault();
                handlePracticeEnter();
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [dialog, handlePracticeEnter, isPracticeMode, practiceChecking, practiceIdx, session]);

    if (loading) {
        return (
            <AnimatedPage>
                <div className="exercise-page"
                     style={{display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh'}}>
                    <p style={{color: '#64748b'}}>Đang chuẩn bị bài quiz...</p>
                </div>
            </AnimatedPage>
        );
    }

    if (error) {
        return <Navigate to="/error" replace state={error}/>;
    }

    if (!session) return null;

    const answersForSidebar = {};
    session.questions.forEach((q, idx) => {
        if (isPracticeMode) {
            if (practiceCompleted.has(q.questionId)) answersForSidebar[idx] = ['done'];
        } else if ((answers[q.questionId] || []).length > 0) {
            answersForSidebar[idx] = answers[q.questionId];
        }
    });

    const currentPracticeQuestion = session.questions[practiceIdx];
    const currentPracticeCompleted = isPracticeMode && currentPracticeQuestion
        ? practiceCompleted.has(currentPracticeQuestion.questionId)
        : false;
    const currentPracticeFeedback = isPracticeMode && currentPracticeQuestion
        ? getPracticeFeedbackForQuestion(currentPracticeQuestion.questionId)
        : null;
    const currentPracticeExplanation = currentPracticeFeedback?.type === 'correct'
        ? (currentPracticeQuestion.explanation || currentPracticeFeedback.text || '')
        : '';
    const shouldShowPracticeExplanation = currentPracticeExplanation
        && currentPracticeExplanation !== 'Ch\u00ednh x\u00e1c.';
    const nextIncompletePracticeIndex = currentPracticeCompleted ? findNextIncompletePracticeIndex() : -1;
    const practiceNextLabel = currentPracticeCompleted
        ? (nextIncompletePracticeIndex >= 0 ? 'Câu tiếp theo' : 'Nộp bài')
        : 'Kiểm tra đáp án';

    return (
        <AnimatedPage>
            <div className="exercise-page quiz-attempt-page">
                <ExerciseBreadcrumb items={[
                    ...(courseTitle ? [{
                        label: courseTitle,
                        link: courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises'
                    }] : []),
                    {label: session.quizTitle}
                ]}/>

                {showAllAnsweredBanner && (
                    <div className="quiz-all-answered-banner" style={{
                        background: '#dcfce7',
                        border: '1px solid #bbf7d0',
                        borderRadius: '12px',
                        padding: '12px 20px',
                        marginBottom: '24px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        color: '#166534',
                        fontWeight: 600,
                        fontSize: '14px',
                    }}>
                        <span className="material-symbols-outlined" style={{fontSize: '20px'}}>check_circle</span>
                        Bạn đã trả lời tất cả câu hỏi. Có thể nộp bài.
                    </div>
                )}

                <div className="quiz-container"
                     style={{display: 'grid', gridTemplateColumns: '300px 1fr', gap: '32px', alignItems: 'start'}}>
                    <div className="quiz-sidebar-stack" style={{position: 'sticky', top: '100px', paddingBottom: '60px'}}>
                        <QuizProgressSidebar
                            questions={session.questions}
                            currentIdx={isPracticeMode ? practiceIdx : null}
                            answers={answersForSidebar}
                            markedForReview={markedForReview}
                            onQuestionSelect={isPracticeMode ? handlePracticeQuestionSelect : scrollToQuestion}
                            onToggleReview={() => {
                            }}
                            isPracticeMode={isPracticeMode}
                        />
                        <div className="question-card quiz-submit-card" style={{marginTop: '24px', padding: '20px'}}>
                            <QuizTimer timeLeft={timeLeft}/>
                            <button
                                className="start-btn"
                                style={{background: '#10b981', width: '100%', marginTop: '20px'}}
                                onClick={handleSubmitClick}
                            >
                                {isPracticeMode ? 'Nộp bài luyện tập' : 'Nộp bài trắc nghiệm'}
                                <span className="material-symbols-outlined"
                                      style={{verticalAlign: 'middle', marginLeft: '8px'}}>check_circle</span>
                            </button>
                        </div>
                    </div>

                    {isPracticeMode ? (
                        <main className="quiz-main">
                            <div className="question-card" style={{minHeight: 420}}>
                                <header className="question-info">
                                    <span
                                        className="question-badge">Câu hỏi {practiceIdx + 1}/{session.questions.length}</span>
                                    <span className="practice-score-inline" style={{color: '#64748b', fontWeight: 700, fontSize: 13}}>
                    Đúng {practiceCompleted.size}/{session.questions.length}
                  </span>
                                </header>

                                <QuestionTopic topic={currentPracticeQuestion.topic}/>
                                <h2 className="question-text">{currentPracticeQuestion.content}</h2>
                                <QuestionPromptMedia question={currentPracticeQuestion}/>

                                <div className="options-group">
                                    {(currentPracticeQuestion.answers || []).map((option, oIdx) => {
                                        const selected = isSelected(currentPracticeQuestion.questionId, option.answerId);
                                        const optionState = getPracticeOptionState(currentPracticeQuestion, option);

                                        return (
                                            <button
                                                type="button"
                                                key={option.answerId}
                                                className={[
                                                    'option-item',
                                                    selected ? 'selected' : '',
                                                    optionState ? `option-item--${optionState}` : '',
                                                ].filter(Boolean).join(' ')}
                                                onClick={() => {
                                                    handleSelectOption(currentPracticeQuestion.questionId, option.answerId, currentPracticeQuestion.type);
                                                    setPracticeFeedback(null);
                                                }}
                                                style={{width: '100%', textAlign: 'left'}}
                                            >
                                                <div className="radio-mock"></div>
                                                <span
                                                    className="option-label">{LABELS[oIdx] || oIdx}. {option.content}</span>
                                                <span
                                                    className={[
                                                        'option-status-icon',
                                                        optionState ? `option-status-icon--${optionState}` : 'option-status-icon--empty',
                                                    ].join(' ')}
                                                    aria-hidden="true"
                                                >
                                                    {optionState && (
                                                        <span className="material-symbols-outlined">
                                                            {optionState === 'practice-correct' ? 'check_circle' : 'cancel'}
                                                        </span>
                                                    )}
                                                </span>
                                            </button>
                                        );
                                    })}
                                </div>

                                <div className="practice-nav-actions">
                                    <button
                                        type="button"
                                        className={`solve-btn practice-copy-btn ${practiceCopyStatus === 'copied' ? 'copied' : ''}`}
                                        onClick={handleCopyPracticeQuestion}
                                    >
                                        <span className="material-symbols-outlined practice-nav-icon">
                                            {practiceCopyStatus === 'copied' ? 'done' : 'content_copy'}
                                        </span>
                                        <span>
                                            {practiceCopyStatus === 'copied'
                                                ? 'Đã copy'
                                                : practiceCopyStatus === 'error'
                                                    ? 'Lỗi copy'
                                                    : 'Copy câu hỏi'}
                                        </span>
                                    </button>
                                    <button
                                        className="start-btn practice-next-btn"
                                        disabled={practiceChecking}
                                        onClick={handlePracticeNext}
                                    >
                                        <span>{practiceNextLabel}</span>
                                        <kbd className="practice-enter-key">Enter</kbd>
                                    </button>
                                </div>
                            </div>
                            {shouldShowPracticeExplanation && (
                                <div className="practice-explanation practice-explanation--correct practice-explanation--detached">
                                    <div className="practice-explanation__header">
                                        <span className="material-symbols-outlined">lightbulb</span>
                                        <span>Giải thích</span>
                                    </div>
                                    <p>{currentPracticeExplanation}</p>
                                </div>
                            )}
                        </main>
                    ) : (
                        <main className="quiz-main" style={{display: 'flex', flexDirection: 'column', gap: '24px'}}>
                            {session.questions && session.questions.map((q, idx) => (
                                <div
                                    key={q.questionId}
                                    className="question-card"
                                    ref={el => questionRefs.current[idx] = el}
                                    style={{scrollMarginTop: '100px'}}
                                >
                                    <header className="question-info">
                                        <span className="question-badge">Câu hỏi {idx + 1}</span>
                                        <button
                                            className={`review-toggle-btn ${markedForReview.has(idx) ? 'active' : ''}`}
                                            onClick={() => toggleReview(idx)}
                                            style={{
                                                background: 'transparent',
                                                border: 'none',
                                                color: markedForReview.has(idx) ? '#f59e0b' : '#94a3b8',
                                                cursor: 'pointer'
                                            }}
                                        >
                      <span className="material-symbols-outlined">
                        {markedForReview.has(idx) ? 'bookmark_added' : 'bookmark'}
                      </span>
                                        </button>
                                    </header>

                                    <QuestionTopic topic={q.topic}/>
                                    <h2 className="question-text">{q.content}</h2>
                                    <QuestionPromptMedia question={q}/>

                                    <div className="options-group">
                                        {(q.answers || []).map((option, oIdx) => (
                                            <div
                                                key={option.answerId}
                                                className={`option-item ${isSelected(q.questionId, option.answerId) ? 'selected' : ''}`}
                                                onClick={() => handleSelectOption(q.questionId, option.answerId, q.type)}
                                            >
                                                <div className="radio-mock"></div>
                                                <span
                                                    className="option-label">{LABELS[oIdx] || oIdx}. {option.content}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))}

                            <div className="quiz-complete-panel" style={{padding: '40px', textAlign: 'center'}}>
                                <p className="quiz-complete-text" style={{color: '#64748b', marginBottom: '20px'}}>Bạn đã hoàn thành bài trắc
                                    nghiệm.</p>
                                <button className="start-btn" style={{background: '#10b981', padding: '16px 40px'}}
                                        onClick={handleSubmitClick}>
                                    Nộp bài
                                </button>
                            </div>
                        </main>
                    )}
                </div>

                {dialog && (
                    <div
                        className="dialog-overlay"
                        onClick={(dialog.type === 'timeout' || dialog.type === 'auto-submit-error') ? undefined : () => setDialog(null)}
                    >
                        <div className="dialog-card" onClick={e => e.stopPropagation()}>
                            {dialog.type === 'timeout' ? (
                                <>
                                    <div className="dialog-icon warning"><span
                                        className="material-symbols-outlined">timer_off</span></div>
                                    <h3 className="dialog-title">Đã hết thời gian làm bài</h3>
                                    <p className="dialog-body">Bài làm của bạn đang được tự động nộp...</p>
                                </>
                            ) : dialog.type === 'practice-incomplete' ? (
                                <>
                                    <div className="dialog-icon warning"><span
                                        className="material-symbols-outlined">warning</span></div>
                                    <h3 className="dialog-title">Còn câu chưa hoàn thành</h3>
                                    <p className="dialog-body">Bạn còn <strong>{dialog.unanswered} câu</strong> chưa tu
                                        luyện xong.
                                        Bạn vẫn muốn nộp bài?</p>
                                    <div className="dialog-actions">
                                        <button className="dialog-btn cancel" onClick={() => setDialog(null)}>Tiếp tục
                                            tu
                                            luyện
                                        </button>
                                        <button className="dialog-btn danger" onClick={handleSubmit}>Vẫn nộp bài
                                        </button>
                                    </div>
                                </>
                            ) : dialog.type === 'all-answered' ? (
                                <>
                                    <div className="dialog-icon success"><span
                                        className="material-symbols-outlined">check_circle</span></div>
                                    <h3 className="dialog-title">Xác nhận nộp bài</h3>
                                    <p className="dialog-body">Bạn đã hoàn thành tất cả câu hỏi. Bạn có chắc chắn muốn
                                        nộp bài?</p>
                                    <div className="dialog-actions">
                                        <button className="dialog-btn cancel" onClick={() => setDialog(null)}>Hủy
                                        </button>
                                        <button className="dialog-btn confirm" onClick={handleSubmit}>Nộp bài</button>
                                    </div>
                                </>
                            ) : dialog.type === 'has-unanswered' ? (
                                <>
                                    <div className="dialog-icon warning"><span
                                        className="material-symbols-outlined">warning</span></div>
                                    <h3 className="dialog-title">Còn câu hỏi chưa làm</h3>
                                    <p className="dialog-body">Bạn còn <strong>{dialog.unanswered} câu</strong> chưa trả
                                        lời. Bạn vẫn muốn nộp bài?</p>
                                    <div className="dialog-actions">
                                        <button className="dialog-btn cancel" onClick={() => setDialog(null)}>Quay lại
                                            làm bài
                                        </button>
                                        <button className="dialog-btn danger" onClick={handleSubmit}>Vẫn nộp bài
                                        </button>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="dialog-icon warning"><span
                                        className="material-symbols-outlined">error</span></div>
                                    <h3 className="dialog-title">{dialog.type === 'auto-submit-error' ? 'Nộp bài tự động thất bại' : 'Lỗi nộp bài'}</h3>
                                    <p className="dialog-body">{dialog.message}</p>
                                    <div className="dialog-actions">
                                        <button className="dialog-btn cancel" onClick={() => setDialog(null)}>Đóng
                                        </button>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </AnimatedPage>
    );
}
