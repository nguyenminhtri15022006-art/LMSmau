import React, { useState, useEffect } from 'react';
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import { mapResultToQuizResult } from '../../../../../utils/assignmentMappers';
import { buildAppErrorState } from '../../../../../utils/appError';
import { formatDateTimeVN } from '../../../../../utils/dateTime';
import useAuth from '../../../../../hooks/useAuth';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import ResultsHero from '../../../shared/components/ResultsHero';

import '../../../shared/styles/ExerciseShared.css';

function formatTime(seconds) {
  if (!seconds && seconds !== 0) return 'N/A';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}m ${s}s`;
}

function formatDateTime(isoString) {
  if (!isoString) return null;
  return formatDateTimeVN(isoString) || isoString;
}

export default function ExerciseQuizResultPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const courseTitle = location.state?.courseTitle || '';
  const courseSlug = location.state?.courseSlug || '';

  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const fetchResult = async () => {
      try {
        const state = location.state || {};
        const quizId = state.quizId || parseInt(id, 10);
        const attemptId = state.attemptId;

        if (state.result) {
          if (!cancelled) {
            setResult(mapResultToQuizResult(state.result));
            setLoading(false);
          }
          return;
        }

        if (!attemptId) {
          if (!cancelled) {
            setError(buildAppErrorState(null, {
              title: 'Không tìm thấy thông tin bài làm',
              message: 'Trang kết quả cần thông tin lần làm bài để tải dữ liệu.',
              icon: 'assignment_late',
              variant: 'not-found',
              fallbackPath: `/exercises/quiz/${quizId}/overview`,
              fallbackState: { courseTitle, courseSlug },
            }));
            setLoading(false);
          }
          return;
        }

        setLoading(true);
        const data = await assignmentApi.getQuizResult(quizId, attemptId);
        if (!cancelled) {
          setResult(mapResultToQuizResult(data));
        }
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
  }, [id, location.state?.quizId, location.state?.attemptId, location.state?.result]);

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page quiz-result-page quiz-result-loading" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải kết quả...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  if (!result) return null;

  const questions = result.questions || [];
  const correctCount = questions.filter(q => q.isCorrect).length;
  const detailLocked = result.resultAvailable === false;

  return (
    <AnimatedPage>
      <div className="exercise-page quiz-result-page">
        <ExerciseBreadcrumb items={[
          ...(courseTitle ? [{ label: courseTitle, link: courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises' }] : []),
          { label: result.title, link: `/exercises/quiz/${result.quizId}/overview`, state: { courseTitle, courseSlug } },
          { label: 'Kết quả' }
        ]} />

        <ResultsHero
          scorePercentage={result.scorePercentage}
          quizTitle={result.title}
          correctCount={correctCount}
          totalQuestions={result.totalQuestions}
          timeTaken={formatTime(result.timeSpentS)}
          userName={user?.fullname || 'Học viên'}
        />

        {detailLocked && (
          <div className="result-actions-card quiz-result-lock-card" style={{ marginBottom: 16, padding: 20 }}>
            <div className="quiz-result-lock-notice" style={{ display: 'flex', gap: 12, alignItems: 'center', color: '#92400e', fontWeight: 700 }}>
              <span className="material-symbols-outlined">lock_clock</span>
              Chi tiết đáp án sẽ mở sau {formatDateTime(result.resultAvailableAt) || 'hạn bài quiz'}.
            </div>
          </div>
        )}

        <div className="result-actions-card">
          {!detailLocked && (
            <button
              className="action-btn action-btn--primary"
              onClick={() => navigate(`/exercises/quiz/${result.quizId}/history/${result.attemptId}`, {
                state: { courseTitle, courseSlug, quizTitle: result.title, attemptNumber: result.attemptNumber },
              })}
            >
              <span className="material-symbols-outlined">visibility</span>
              <div className="action-btn__content">
                <span className="action-btn__label">Xem chi tiết</span>
                <span className="action-btn__hint">Đáp án và giải thích từng câu</span>
              </div>
              <span className="material-symbols-outlined action-btn__chevron">chevron_right</span>
            </button>
          )}

          <button
            className="action-btn action-btn--outline"
            onClick={() => navigate(`/exercises/quiz/${result.quizId}`, {
              state: { courseTitle, courseSlug },
            })}
          >
            <span className="material-symbols-outlined">refresh</span>
            <div className="action-btn__content">
              <span className="action-btn__label">Làm lại bài trắc nghiệm</span>
              <span className="action-btn__hint">Thử sức với đề cũ</span>
            </div>
            <span className="material-symbols-outlined action-btn__chevron">chevron_right</span>
          </button>

          <button
            className="action-btn action-btn--ghost"
            onClick={() => navigate(
              courseSlug ? `/exercises/code/${courseSlug}?tab=quiz` : '/exercises',
            )}
          >
            <span className="material-symbols-outlined">category</span>
            <div className="action-btn__content">
              <span className="action-btn__label">Bài tập khác</span>
              <span className="action-btn__hint">Quay lại danh sách bài tập</span>
            </div>
            <span className="material-symbols-outlined action-btn__chevron">chevron_right</span>
          </button>
        </div>
      </div>
    </AnimatedPage>
  );
}
