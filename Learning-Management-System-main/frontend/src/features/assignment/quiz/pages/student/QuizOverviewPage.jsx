import React, { useState, useEffect } from 'react';
import { Navigate, useParams, useNavigate, useLocation } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import { buildAppErrorState } from '../../../../../utils/appError';
import { formatDateTimeVN, parseBackendUtcDate } from '../../../../../utils/dateTime';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';

import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/QuizOverviewPage.css';

function formatDuration(minutes) {
  if (minutes == null) return 'Không giới hạn';
  if (minutes < 60) return `${minutes} phút`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}p` : `${h} tiếng`;
}

function formatDateTime(isoString) {
  if (!isoString) return null;
  return formatDateTimeVN(isoString) || isoString;
}

function formatDurationSeconds(seconds) {
  if (seconds == null) return 'N/A';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m > 0 && s > 0) return `${m}p ${s}s`;
  if (m > 0) return `${m} phút`;
  return `${s}s`;
}

export default function QuizOverviewPage() {
  const { id } = useParams();
  const quizId = parseInt(id, 10);
  const navigate = useNavigate();
  const location = useLocation();

  const [quiz, setQuiz] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const courseTitle = location.state?.courseTitle || '';
  const courseSlug = location.state?.courseSlug || '';

  useEffect(() => {
    let cancelled = false;
    const fetchQuiz = async () => {
      try {
        setLoading(true);
        const data = await assignmentApi.getQuizDetail(quizId);
        if (!cancelled) setQuiz(data);
      } catch (err) {
        if (!cancelled) {
          setError(buildAppErrorState(err, {
            fallbackPath: courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises',
          }));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchQuiz();
    return () => { cancelled = true; };
  }, [quizId]);

  useEffect(() => {
    let cancelled = false;
    const fetchHistory = async () => {
      try {
        setHistoryLoading(true);
        setCurrentPage(0);
        const data = await assignmentApi.getAttemptHistory(quizId, { page: 0, size: 5 });
        if (!cancelled) {
          setHistory(data);
          setHasMore(data.number < data.totalPages - 1);
        }
      } catch {
        if (!cancelled) setHistory(null);
      } finally {
        if (!cancelled) setHistoryLoading(false);
      }
    };
    fetchHistory();
    return () => { cancelled = true; };
  }, [quizId]);

  const handleLoadMore = async () => {
    try {
      setLoadingMore(true);
      const nextPage = currentPage + 1;
      const data = await assignmentApi.getAttemptHistory(quizId, { page: nextPage, size: 5 });
      setHistory((prev) => ({
        ...data,
        content: [...prev.content, ...data.content],
      }));
      setCurrentPage(nextPage);
      setHasMore(data.number < data.totalPages - 1);
    } catch {
      // silently fail
    } finally {
      setLoadingMore(false);
    }
  };

  const handleStart = () => {
    navigate(`/exercises/quiz/${quizId}`, {
      state: { courseTitle, courseSlug },
    });
  };

  const handleBack = () => {
    if (courseSlug) {
      navigate(`/exercises/code/${courseSlug}?tab=quiz`);
    } else {
      navigate('/exercises');
    }
  };

  const quizListUrl = courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises';

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải thông tin bài quiz...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  if (!quiz) return null;

  const now = Date.now();
  const startAt = parseBackendUtcDate(quiz.startTime);
  const endAt = parseBackendUtcDate(quiz.endTime);
  const notYetOpen = startAt && startAt.getTime() > now;
  const alreadyClosed = endAt && endAt.getTime() < now;

  return (
    <AnimatedPage>
      <div className="exercise-page">
        <ExerciseBreadcrumb items={[
          ...(courseTitle ? [{ label: courseTitle, link: quizListUrl }] : []),
          { label: quiz.title },
        ]} />

        <div className="qo-card">
          {/* Header row: icon + title + actions */}
          <div className="qo-header">
            <div className="qo-header-left">
              <div className="qo-icon-circle">
                <span className="material-symbols-outlined">quiz</span>
              </div>
              <div className="qo-header-text">
                <h1 className="qo-title">{quiz.title}</h1>
                <div className="qo-meta-row">
                  <span className="qo-meta-badge">
                    <span className="material-symbols-outlined">help_outline</span>
                    {quiz.questionCount ?? 0} câu hỏi
                  </span>
                  <span className="qo-meta-badge">
                    <span className="material-symbols-outlined">schedule</span>
                    {formatDuration(quiz.duration)}
                  </span>
                  <span className="qo-meta-badge">
                    <span className="material-symbols-outlined">emoji_events</span>
                    {quiz.totalScore ?? 100} điểm
                  </span>
                  <span className="qo-meta-badge">
                    <span className="material-symbols-outlined">replay</span>
                    {quiz.maxAttempts === 0 ? 'Không giới hạn' : `${quiz.maxAttempts} lần`}
                  </span>
                </div>
              </div>
            </div>
            <div className="qo-header-actions">
              <button
                className="qo-start-btn"
                onClick={handleStart}
                disabled={notYetOpen || alreadyClosed}
              >
                <span className="material-symbols-outlined">play_arrow</span>
                Bắt đầu làm bài
              </button>
              <button className="qo-back-btn" onClick={handleBack}>
                <span className="material-symbols-outlined">arrow_back</span>
                Quay lại
              </button>
            </div>
          </div>

          {/* Status warning */}
          {notYetOpen && (
            <div className="qo-status qo-status--warn">
              <span className="material-symbols-outlined">lock_clock</span>
              Bài quiz sẽ mở vào {formatDateTime(quiz.startTime)}. Bạn chưa thể làm bài trước thời gian này.
            </div>
          )}
          {alreadyClosed && (
            <div className="qo-status qo-status--error">
              <span className="material-symbols-outlined">lock</span>
              Bài quiz đã đóng vào {formatDateTime(quiz.endTime)}.
            </div>
          )}

          {/* Details: description + time window + requirement */}
          <div className="qo-details">
            {/* Time window */}
            <div className="qo-detail-item">
              <span className="qo-detail-icon">
                <span className="material-symbols-outlined">calendar_clock</span>
              </span>
              <div>
                <span className="qo-detail-label">Thời gian mở</span>
                <span className="qo-detail-value">
                  {!quiz.startTime && !quiz.endTime
                    ? 'Không giới hạn'
                    : `${formatDateTime(quiz.startTime) || 'Không giới hạn'} — ${formatDateTime(quiz.endTime) || 'Không giới hạn'}`
                  }
                </span>
              </div>
            </div>

            {/* Pass score */}
            <div className="qo-detail-item">
              <span className="qo-detail-icon">
                <span className="material-symbols-outlined">verified</span>
              </span>
              <div>
                <span className="qo-detail-label">Điểm đạt yêu cầu</span>
                <span className="qo-detail-value">{quiz.passScore ?? 50}% tổng số điểm</span>
              </div>
            </div>

            {/* Description */}
            {quiz.description && (
              <div className="qo-detail-item qo-detail-desc">
                <span className="qo-detail-icon">
                  <span className="material-symbols-outlined">description</span>
                </span>
                <div>
                  <span className="qo-detail-label">Mô tả</span>
                  <p className="qo-detail-value qo-description-text">{quiz.description}</p>
                </div>
              </div>
            )}
          </div>

          {/* ── Attempt History Section ── */}
          <div className="qo-history">
            <div className="qo-history-header">
              <h2 className="qo-history-title">
                <span className="material-symbols-outlined">history</span>
                Lịch sử làm bài
              </h2>
            </div>

            {historyLoading ? (
              <div className="qo-history-loading">
                <span className="material-symbols-outlined" style={{ animation: 'spin 1s linear infinite' }}>progress_activity</span>
                Đang tải lịch sử...
              </div>
            ) : !history || !history.content || history.content.length === 0 ? (
              <div className="qo-history-empty">
                <span className="material-symbols-outlined">assignment</span>
                <p>Bạn chưa có lần làm bài nào.</p>
              </div>
            ) : (
              <>
                <div className="qo-history-table-wrap">
                  <table className="qo-history-table">
                    <thead>
                      <tr>
                        <th>Lần</th>
                        <th>Điểm số</th>
                        <th>Kết quả</th>
                        <th>Thời gian</th>
                        <th>Ngày nộp</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.content.map((attempt) => {
                        const isPassed = attempt.passed;
                        const statusLabel =
                          attempt.status === 'SUBMITTED' ? (isPassed ? 'Đạt' : 'Chưa đạt') :
                          attempt.status === 'IN_PROGRESS' ? 'Đang làm' :
                          attempt.status === 'EXPIRED' ? 'Hết giờ' : attempt.status;

                        const statusClass =
                          attempt.status === 'SUBMITTED' ? (isPassed ? 'qo-badge--pass' : 'qo-badge--fail') :
                          attempt.status === 'IN_PROGRESS' ? 'qo-badge--progress' :
                          'qo-badge--expired';

                        return (
                          <tr key={attempt.attemptId}>
                            <td className="qo-history-attempt-num">#{attempt.attemptNumber}</td>
                            <td className="qo-history-score">
                              <span className="qo-history-score-num">{attempt.score != null ? Number(attempt.score).toFixed(1) : '-'}</span>
                              <span className="qo-history-score-total">/{attempt.totalScore}</span>
                            </td>
                            <td><span className={`qo-badge ${statusClass}`}>{statusLabel}</span></td>
                            <td className="qo-history-time">{formatDurationSeconds(attempt.timeSpentSeconds)}</td>
                            <td className="qo-history-date">{formatDateTime(attempt.submittedAt || attempt.startedAt)}</td>
                            <td className="qo-history-action">
                              {attempt.status === 'SUBMITTED' && (
                                <button
                                  className="qo-detail-btn"
                                  onClick={() => navigate(`/exercises/quiz/${quizId}/history/${attempt.attemptId}`, {
                                    state: { courseTitle, courseSlug, quizTitle: quiz.title, attemptNumber: attempt.attemptNumber },
                                  })}
                                >
                                  <span className="material-symbols-outlined">visibility</span>
                                  Xem chi tiết
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                {hasMore && (
                  <div className="qo-history-load-more">
                    <button
                      className="qo-load-more-btn"
                      onClick={handleLoadMore}
                      disabled={loadingMore}
                    >
                      {loadingMore ? (
                        <>
                          <span className="material-symbols-outlined" style={{ animation: 'spin 1s linear infinite', fontSize: '16px' }}>progress_activity</span>
                          Đang tải thêm...
                        </>
                      ) : (
                        <>
                          <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>expand_more</span>
                          Xem thêm
                        </>
                      )}
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </AnimatedPage>
  );
}
