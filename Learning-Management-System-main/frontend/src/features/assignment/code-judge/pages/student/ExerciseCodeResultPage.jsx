import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import { buildAppErrorState } from '../../../../../utils/appError';
import { formatTimeVN } from '../../../../../utils/dateTime';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import ResultsHero from '../../../shared/components/ResultsHero';

import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/ExerciseCodeResultPage.css';

const POLLING_STATUSES = new Set(['PENDING', 'JUDGING']);
const POLL_INTERVAL_MS = 3000;

function normalizeStatus(status) {
  return String(status || '').toUpperCase();
}

function isJudgingStatus(status) {
  return POLLING_STATUSES.has(normalizeStatus(status));
}

function isPassedStatus(status) {
  return normalizeStatus(status) === 'AC';
}

function getStatusLabel(status) {
  const normalized = normalizeStatus(status);
  const labels = {
    AC: 'Passed',
    WA: 'Wrong answer',
    TLE: 'Time limit',
    MLE: 'Memory limit',
    RE: 'Runtime error',
    CE: 'Compile error',
    PENDING: 'Pending',
    JUDGING: 'Judging',
    IE: 'Internal error',
  };
  return labels[normalized] || normalized || 'Unknown';
}

function getVisibleTestCases(submission) {
  if (Array.isArray(submission?.testCaseResults)) return submission.testCaseResults;
  if (Array.isArray(submission?.allTestResults)) return submission.allTestResults;
  if (Array.isArray(submission?.publicTestResults)) return submission.publicTestResults;
  return [];
}

function formatMemory(kb) {
  if (kb === null || kb === undefined) return '-';
  if (kb >= 1024) return `${Math.round(kb / 1024)} MB`;
  return `${kb} KB`;
}

function getResultStatusClass(status) {
  if (isJudgingStatus(status)) return 'is-judging';
  if (isPassedStatus(status)) return 'is-passed';
  return 'is-failed';
}

function getStatusIcon(status) {
  if (isJudgingStatus(status)) return 'hourglass_top';
  if (isPassedStatus(status)) return 'check_circle';
  return 'cancel';
}

function getLineValue(value) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

export default function ExerciseCodeResultPage() {
  const { slug, id: legacyId, legacySlug } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state || {};
  const stateSubmission = state.submission;
  const stateChallenge = state.challenge;
  const routeSlug = slug || legacySlug || legacyId;
  const submissionId = new URLSearchParams(location.search).get('submissionId') || stateSubmission?.submissionId || '';
  const courseTitle = stateChallenge?.courseTitle || state.courseTitle || '';
  const courseSlug = stateChallenge?.courseSlug || state.courseSlug || '';

  const [submission, setSubmission] = useState(null);
  const [challenge, setChallenge] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedCaseId, setExpandedCaseId] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  useEffect(() => {
    let cancelled = false;
    let timeoutId = null;

    const applySubmission = (nextSubmission) => {
      setSubmission(nextSubmission);
      setLastUpdatedAt(new Date());
      setChallenge((currentChallenge) => (
        currentChallenge || stateChallenge || {
          id: nextSubmission?.problemId,
          slug: routeSlug,
          title: 'Bài tập',
        }
      ));
      setLoading(false);
    };

    const fetchLatest = async () => {
      try {
        const latestSubmission = await assignmentApi.getSubmission(submissionId);
        if (cancelled) return;

        applySubmission(latestSubmission);

        if (isJudgingStatus(latestSubmission?.status)) {
          timeoutId = window.setTimeout(fetchLatest, POLL_INTERVAL_MS);
        }
      } catch (err) {
        if (cancelled) return;

        if (stateSubmission) {
          setLoading(false);
          return;
        }

        setError(
          buildAppErrorState(err, {
            title: 'Không thể tải kết quả',
            fallbackPath: courseSlug ? `/exercises/code/${courseSlug}?tab=coding` : '/exercises',
          }),
        );
        setLoading(false);
      }
    };

    if (stateSubmission) {
      applySubmission(stateSubmission);
    }

    if (!submissionId) {
      setError(buildAppErrorState(null, {
        title: 'Không tìm thấy thông tin nộp bài',
        message: 'Trang kết quả cần mã bài nộp để tải dữ liệu.',
        variant: 'not-found',
        icon: 'assignment_late',
        fallbackPath: courseSlug ? `/exercises/code/${courseSlug}?tab=coding` : '/exercises',
      }));
      setLoading(false);
      return () => {
        cancelled = true;
      };
    }

    fetchLatest();

    return () => {
      cancelled = true;
      if (timeoutId) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [courseSlug, routeSlug, stateChallenge, stateSubmission, submissionId]);

  const testCases = useMemo(() => getVisibleTestCases(submission), [submission]);
  const isAccepted = isPassedStatus(submission?.status);
  const isStillJudging = isJudgingStatus(submission?.status);
  const hasCompileError = normalizeStatus(submission?.status) === 'CE' && submission?.compileError;
  const passedCount = testCases.filter((tc) => isPassedStatus(tc.status)).length;
  const scorePercentage = submission?.score ?? (isAccepted ? 100 : 0);
  const statusClass = getResultStatusClass(submission?.status);
  const lastUpdatedText = lastUpdatedAt ? formatTimeVN(lastUpdatedAt) : '-';
  const hasFullTestCaseList = Array.isArray(submission?.testCaseResults);

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page code-result-page code-result-loading">
          <p>Đang tải kết quả...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  return (
    <AnimatedPage>
      <div className="exercise-page code-result-page">
        <ExerciseBreadcrumb
          items={[
            ...(courseTitle
              ? [{ label: courseTitle, link: courseSlug ? `/exercises/code/${courseSlug}?tab=coding` : '/exercises' }]
              : []),
            { label: `Kết quả: ${challenge?.title || 'Bài tập'}` },
          ]}
        />

        <ResultsHero
          scorePercentage={scorePercentage}
          quizTitle={challenge?.title || 'Bài tập'}
          correctCount={passedCount}
          totalQuestions={testCases.length || 1}
          timeTaken={submission?.judgedAt ? formatTimeVN(submission.judgedAt) : lastUpdatedText}
          userName="bạn"
        />

        <section className="code-result-shell" aria-live={isStillJudging ? 'polite' : 'off'}>
          <div className={`code-result-status ${statusClass}`}>
            <div className="code-result-status__main">
              <span className="material-symbols-outlined code-result-status__icon">
                {getStatusIcon(submission?.status)}
              </span>
              <div>
                <span className="code-result-eyebrow">Trạng thái bài nộp</span>
                <h2>
                  {isStillJudging
                    ? 'Đang chấm bài'
                    : isAccepted
                      ? 'Đã vượt qua'
                      : getStatusLabel(submission?.status)}
                </h2>
                <p>
                  {isStillJudging
                    ? `Trang tự cập nhật mỗi ${POLL_INTERVAL_MS / 1000}s. Lần cập nhật gần nhất: ${lastUpdatedText}.`
                    : `Kết quả mới nhất lúc ${lastUpdatedText}.`}
                </p>
              </div>
            </div>

            <div className="code-result-metrics">
              <div>
                <span>Điểm</span>
                <strong>{scorePercentage}</strong>
              </div>
              <div>
                <span>Runtime</span>
                <strong>{submission?.execTimeMs == null ? '-' : `${submission.execTimeMs} ms`}</strong>
              </div>
              <div>
                <span>Memory</span>
                <strong>{formatMemory(submission?.memoryUsedKb)}</strong>
              </div>
            </div>
          </div>

          {hasCompileError && (
            <pre className="code-result-compile-error">{submission.compileError}</pre>
          )}

          <div className="code-result-testcases">
            <div className="code-result-section-heading">
              <div>
                <span className="code-result-eyebrow">Testcase matrix</span>
                <h3>{passedCount}/{testCases.length || 0} testcase passed</h3>
              </div>
              {isStillJudging && (
                <span className="code-result-live">
                  <span className="material-symbols-outlined">sync</span>
                  Đang cập nhật
                </span>
              )}
            </div>

            {testCases.length === 0 ? (
              <div className="code-result-empty">
                {isStillJudging ? 'Chưa có testcase nào được ghi nhận.' : 'Không có testcase để hiển thị.'}
              </div>
            ) : (
              <div className="code-result-case-list">
                {testCases.map((tc, index) => {
                  const caseKey = tc.testId || `case-${index}`;
                  const isPublicCase = hasFullTestCaseList ? tc.hidden === false : !tc.hidden;
                  const isExpanded = expandedCaseId === caseKey;
                  const passed = isPassedStatus(tc.status);
                  const statusLabel = getStatusLabel(tc.status);

                  return (
                    <div
                      className={`code-result-case ${passed ? 'is-passed' : 'is-failed'} ${isExpanded ? 'is-open' : ''}`}
                      key={caseKey}
                    >
                      <button
                        className="code-result-case__summary"
                        type="button"
                        onClick={() => {
                          if (!isPublicCase) return;
                          setExpandedCaseId(isExpanded ? null : caseKey);
                        }}
                        disabled={!isPublicCase}
                        aria-expanded={isPublicCase ? isExpanded : undefined}
                      >
                        <span className="material-symbols-outlined code-result-case__status">
                          {passed ? 'check_circle' : 'cancel'}
                        </span>
                        <span className="code-result-case__name">Test #{index + 1}</span>
                        <span className={`code-result-case__badge ${isPublicCase ? 'is-public' : 'is-hidden'}`}>
                          {isPublicCase ? 'Public' : 'Hidden'}
                        </span>
                        <span className="code-result-case__label">{statusLabel}</span>
                        {isPublicCase && (
                          <>
                            <span className="code-result-case__meta">
                              {tc.timeMs == null ? '-' : `${tc.timeMs} ms`}
                              <span>{formatMemory(tc.memoryKb)}</span>
                            </span>
                            <span className="material-symbols-outlined code-result-case__chevron">
                              expand_more
                            </span>
                          </>
                        )}
                      </button>

                      {isPublicCase && isExpanded && (
                        <div className="code-result-case__details">
                          <div>
                            <span>Input</span>
                            <pre>{getLineValue(tc.input)}</pre>
                          </div>
                          <div>
                            <span>Expected output</span>
                            <pre>{getLineValue(tc.expectedOutput)}</pre>
                          </div>
                          <div className="code-result-case__actual">
                            <span>Actual output</span>
                            <pre>{getLineValue(tc.outputSnippet)}</pre>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        <div className="code-result-actions">
          <button
            className="code-result-action code-result-action--secondary"
            type="button"
            onClick={() => navigate(challenge?.slug ? `/exercises/challenge/${challenge.slug}` : `/exercises/challenge/${challenge?.id || routeSlug}`)}
          >
            <span className="material-symbols-outlined">code</span>
            Xem lại bài code
          </button>
          <button
            className="code-result-action code-result-action--primary"
            type="button"
            onClick={() => navigate(courseSlug ? `/exercises/code/${courseSlug}?tab=coding` : '/exercises')}
          >
            <span className="material-symbols-outlined">list_alt</span>
            Bài tập khác
          </button>
        </div>
      </div>
    </AnimatedPage>
  );
}
