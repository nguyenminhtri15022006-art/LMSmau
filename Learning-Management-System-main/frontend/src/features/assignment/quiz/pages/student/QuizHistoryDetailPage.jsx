import React, { useState, useEffect } from 'react';
import { Navigate, useParams, useNavigate, useLocation } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import { mapResultToQuizResult } from '../../../../../utils/assignmentMappers';
import { buildAppErrorState } from '../../../../../utils/appError';
import { formatDateTimeVN } from '../../../../../utils/dateTime';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';

import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/QuizHistoryDetailPage.css';

const LABELS = ['A', 'B', 'C', 'D', 'E', 'F'];
const CIRCUMFERENCE = 2 * Math.PI * 34;

function formatTime(seconds) {
  if (!seconds && seconds !== 0) return 'N/A';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}p ${s}s`;
}

function formatDateTime(isoString) {
  if (!isoString) return null;
  return formatDateTimeVN(isoString) || isoString;
}

export default function QuizHistoryDetailPage() {
  const { id, attemptId } = useParams();
  const quizId = parseInt(id, 10);
  const parsedAttemptId = parseInt(attemptId, 10);
  const navigate = useNavigate();
  const location = useLocation();

  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('all');

  const courseTitle = location.state?.courseTitle || '';
  const courseSlug = location.state?.courseSlug || '';
  const quizTitle = location.state?.quizTitle || '';
  const attemptNumber = location.state?.attemptNumber || '';

  useEffect(() => {
    let cancelled = false;
    const fetchResult = async () => {
      try {
        setLoading(true);
        const data = await assignmentApi.getQuizResult(quizId, parsedAttemptId);
        if (!cancelled) setResult(mapResultToQuizResult(data));
      } catch (err) {
        if (!cancelled) {
          setError(buildAppErrorState(err, {
            fallbackPath: `/exercises/quiz/${quizId}/overview`,
            fallbackState: { courseTitle, courseSlug },
          }));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchResult();
    return () => { cancelled = true; };
  }, [quizId, parsedAttemptId]);

  const handleBackToOverview = () => {
    navigate(`/exercises/quiz/${quizId}/overview`, {
      state: { courseTitle, courseSlug },
    });
  };

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải kết quả...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  if (!result) return null;

  const resolvedAttemptNumber = attemptNumber || result.attemptNumber || '';

  const allQuestions = result.questions || [];
  const correctQuestions = allQuestions.filter(q => q.isCorrect);
  const incorrectQuestions = allQuestions.filter(q => !q.isCorrect);

  const displayedQuestions =
    filter === 'correct' ? correctQuestions :
    filter === 'incorrect' ? incorrectQuestions :
    allQuestions;

  const isPassed = result.passed;
  const scorePercent = result.scorePercentage;

  return (
    <AnimatedPage>
      <div className="exercise-page">
        <ExerciseBreadcrumb items={[
          ...(courseTitle ? [{ label: courseTitle, link: courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises' }] : []),
          { label: quizTitle || result.title, link: `/exercises/quiz/${quizId}/overview`, state: { courseTitle, courseSlug } },
          { label: `Lần #${resolvedAttemptNumber}` },
        ]} />

        <div className="qhd-card">
          {/* Hero */}
          <div className="qhd-hero">
            <div className="qhd-hero-ring">
              <svg viewBox="0 0 80 80">
                <circle className="qhd-hero-ring-bg" cx="40" cy="40" r="34" />
                <circle
                  className={`qhd-hero-ring-fg ${isPassed ? 'qhd-hero-ring-fg--pass' : 'qhd-hero-ring-fg--fail'}`}
                  cx="40" cy="40" r="34"
                  strokeDasharray={CIRCUMFERENCE}
                  strokeDashoffset={CIRCUMFERENCE - (CIRCUMFERENCE * scorePercent) / 100}
                />
              </svg>
              <div className="qhd-hero-ring-text">{scorePercent}%</div>
            </div>
            <div className="qhd-hero-info">
              <h1 className="qhd-hero-title">{quizTitle || result.title}</h1>
              <p className="qhd-hero-subtitle">Lần làm bài #{resolvedAttemptNumber}</p>
              <div className="qhd-hero-meta">
                <span className={`qhd-hero-badge ${isPassed ? 'qhd-hero-badge--pass' : 'qhd-hero-badge--fail'}`}>
                  <span className="material-symbols-outlined">
                    {isPassed ? 'check_circle' : 'cancel'}
                  </span>
                  {isPassed ? 'Đạt' : 'Chưa đạt'}
                </span>
                <span className="qhd-hero-badge qhd-hero-badge--neutral">
                  <span className="material-symbols-outlined">schedule</span>
                  {formatTime(result.timeSpentS)}
                </span>
                <span className="qhd-hero-badge qhd-hero-badge--neutral">
                  <span className="material-symbols-outlined">calendar_today</span>
                  {formatDateTime(result.submittedAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="qhd-filters">
            <button
              className={`qhd-filter-btn ${filter === 'all' ? 'qhd-filter-btn--active' : ''}`}
              onClick={() => setFilter('all')}
            >
              Tất cả
              <span className="qhd-filter-count">{allQuestions.length}</span>
            </button>
            <button
              className={`qhd-filter-btn qhd-filter-btn--correct ${filter === 'correct' ? 'qhd-filter-btn--active' : ''}`}
              onClick={() => setFilter('correct')}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 14 }}>check_circle</span>
              Đúng
              <span className="qhd-filter-count">{correctQuestions.length}</span>
            </button>
            <button
              className={`qhd-filter-btn qhd-filter-btn--incorrect ${filter === 'incorrect' ? 'qhd-filter-btn--active' : ''}`}
              onClick={() => setFilter('incorrect')}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 14 }}>cancel</span>
              Sai
              <span className="qhd-filter-count">{incorrectQuestions.length}</span>
            </button>
          </div>

          {/* Question List */}
          {displayedQuestions.length === 0 ? (
            <div className="qhd-filter-empty">
              <span className="material-symbols-outlined">search_off</span>
              Không có câu hỏi nào phù hợp bộ lọc
            </div>
          ) : (
            <div className="qhd-review-list">
              {displayedQuestions.map((q) => {
                const isCorrect = q.isCorrect;

                return (
                  <div key={q.id} className={`qhd-review-item ${isCorrect ? 'qhd-review-item--correct' : 'qhd-review-item--incorrect'}`}>
                    <div className="qhd-review-header">
                      <span className="qhd-review-num">Câu hỏi #{q.index + 1}</span>
                      <span className={`qhd-review-status ${isCorrect ? 'qhd-review-status--correct' : 'qhd-review-status--incorrect'}`}>
                        <span className="material-symbols-outlined">
                          {isCorrect ? 'check_circle' : 'cancel'}
                        </span>
                        {isCorrect ? 'Đúng' : 'Sai'}
                      </span>
                    </div>

                    <h3 className="qhd-review-question">{q.question}</h3>

                    <div className="qhd-review-options">
                      {(q.options || []).map((opt, oIdx) => {
                        const isChosen = q.selectedAnswerIds.includes(opt.answerId);
                        const isCorrectOpt = opt.isCorrectOption;

                        let optClass = 'qhd-review-opt--normal';
                        if (isChosen && isCorrectOpt) optClass = 'qhd-review-opt--chosen-correct';
                        else if (isChosen && !isCorrectOpt) optClass = 'qhd-review-opt--chosen-incorrect';
                        else if (!isChosen && isCorrectOpt) optClass = 'qhd-review-opt--correct';

                        return (
                          <div key={opt.answerId} className={`qhd-review-opt ${optClass}`}>
                            {LABELS[oIdx] || oIdx}) {opt.label}
                            {isChosen && (
                              <span className="qhd-review-opt-tag qhd-review-opt-tag--chosen">
                                Đã chọn
                              </span>
                            )}
                            {!isChosen && isCorrectOpt && (
                              <span className="qhd-review-opt-tag qhd-review-opt-tag--correct">
                                Đáp án đúng
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>

                    {(q.earnedScore != null || q.questionScore != null) && (
                      <div className="qhd-review-score">
                        <span>Điểm: <span className="qhd-review-score-val">{Number(q.earnedScore).toFixed(1)}/{q.questionScore}</span></span>
                      </div>
                    )}

                    {q.explanation && (
                      <div className="qhd-review-explanation">
                        <div className="qhd-review-explanation-label">Giải thích</div>
                        <p className="qhd-review-explanation-text">{q.explanation}</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Bottom Actions */}
        <div className="qhd-actions">
          <button className="start-btn" onClick={handleBackToOverview}>
            <span className="material-symbols-outlined">arrow_back</span>
            Về trang tổng quan
          </button>
          <button
            className="start-btn qhd-btn-secondary"
            onClick={() => navigate(`/exercises/quiz/${quizId}`, {
              state: { courseTitle, courseSlug },
            })}
          >
            <span className="material-symbols-outlined">refresh</span>
            Làm lại
          </button>
        </div>
      </div>
    </AnimatedPage>
  );
}
