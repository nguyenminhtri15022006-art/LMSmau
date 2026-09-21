import React, { useState, useEffect, useCallback } from 'react';
import { Navigate, useParams, useNavigate, useLocation } from 'react-router-dom';
import { useProblemContext } from '../../../../../context/ProblemContext';
import { assignmentApi } from '../../../../../services/assignment.api';
import { courseApi } from '../../../../../services/course.api';
import { mapProblemToCodeChallenge } from '../../../../../utils/assignmentMappers';
import { formatSubmittedAt, formatSubmissionStatus } from '../../../../../utils/codeChallengeFormatters';
import { buildAppErrorState } from '../../../../../utils/appError';
import { getCourseRouteSlug } from '../../../../../utils/courseSlug';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import { useToast } from '../../../../../components/ui/Toast';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import ExerciseHeader from '../../components/student/ExerciseCode/ExerciseHeader';
import ProblemPanel from '../../components/student/ExerciseCode/ProblemPanel';
import EditorPanel from '../../components/student/ExerciseCode/EditorPanel';
import { getBoilerplateCode, POPULAR_LANGUAGES } from '../../constants/languageBoilerplates';

import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/ExerciseCodePage/ExerciseCodePage.css';

const DEFAULT_LANGUAGE = 'python';
const NUMERIC_ID_PATTERN = /^\d+$/;

function getShortSubmissionId(submissionId) {
  if (!submissionId) return 'N/A';
  return submissionId.length > 8 ? submissionId.slice(-8) : submissionId;
}

function normalizeEditorLanguage(language) {
  const normalized = String(language || '').toLowerCase();
  const aliases = {
    'c++': 'cpp',
    node: 'javascript',
    nodejs: 'javascript',
  };
  const languageId = aliases[normalized] || normalized;
  return POPULAR_LANGUAGES.some((item) => item.id === languageId) ? languageId : DEFAULT_LANGUAGE;
}

function mapSubmissionHistoryRows(response) {
  const submissions = Array.isArray(response?.content)
    ? response.content
    : Array.isArray(response)
      ? response
      : [];

  return submissions.map((submission, index) => ({
    key: submission.submissionId || `${submission.submittedAt || 'submission'}-${index}`,
    submissionId: submission.submissionId,
    id: getShortSubmissionId(submission.submissionId),
    language: submission.language || '-',
    statusRaw: submission.status,
    status: formatSubmissionStatus(submission.status),
    score: submission.score === null || submission.score === undefined ? '-' : submission.score,
    runtime: submission.execTimeMs === null || submission.execTimeMs === undefined ? '-' : `${submission.execTimeMs} ms`,
    submittedAt: formatSubmittedAt(submission.submittedAt),
  }));
}

function getCompileErrorFromResponse(err) {
  const data = err.response?.data?.data;
  return data?.compileError || null;
}

export default function ExerciseCodePage() {
  const { slugOrId, id: legacyId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { setProblemId } = useProblemContext() ?? {};
  const routeValue = legacyId || slugOrId;
  const isLegacyIdRoute = !!legacyId || NUMERIC_ID_PATTERN.test(slugOrId || '');
  const stateCourseTitle = location.state?.courseTitle || '';
  const stateCourseSlug = location.state?.courseSlug || '';

  const [loading, setLoading] = useState(true);
  const [challenge, setChallenge] = useState(null);
  const [courseContext, setCourseContext] = useState({
    title: stateCourseTitle,
    slug: stateCourseSlug,
  });
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('problem');
  const [isRunning, setIsRunning] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [runResult, setRunResult] = useState(null);
  const [runError, setRunError] = useState(null);
  const [code, setCode] = useState('');
  const [isCodeDirty, setIsCodeDirty] = useState(false);
  const [language, setLanguage] = useState(DEFAULT_LANGUAGE);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingSubmissionId, setLoadingSubmissionId] = useState(null);
  const [submissionHistory, setSubmissionHistory] = useState({
    loading: false,
    rows: [],
    error: null,
  });

  useEffect(() => {
    let cancelled = false;
    const fetchProblem = async () => {
      try {
        setLoading(true);
        const problem = isLegacyIdRoute
          ? await assignmentApi.getProblemDetail(Number(routeValue))
          : await assignmentApi.getProblemDetailBySlug(routeValue);

        if (cancelled) return;
        const mapped = mapProblemToCodeChallenge(problem);

        let nextCourseTitle = stateCourseTitle;
        let nextCourseSlug = stateCourseSlug;
        if ((!nextCourseTitle || !nextCourseSlug) && mapped.courseId) {
          try {
            const courseResponse = await courseApi.getById(mapped.courseId);
            const course = courseResponse?.data?.data ?? courseResponse?.data ?? {};
            nextCourseTitle = nextCourseTitle || course.title || course.name || '';
            nextCourseSlug = nextCourseSlug || getCourseRouteSlug(course) || mapped.courseId;
          } catch {
            nextCourseSlug = nextCourseSlug || mapped.courseId;
          }
        }

        if (cancelled) return;
        setChallenge(mapped);
        setCourseContext({
          title: nextCourseTitle,
          slug: nextCourseSlug,
        });

        let editorLanguage = DEFAULT_LANGUAGE;
        let editorCode = mapped.initialCode || getBoilerplateCode(DEFAULT_LANGUAGE);
        try {
          const latestAccepted = mapped.id
            ? await assignmentApi.getLatestAcceptedSubmission(mapped.id)
            : null;
          if (!cancelled && latestAccepted?.sourceCode) {
            editorLanguage = normalizeEditorLanguage(latestAccepted.language);
            editorCode = latestAccepted.sourceCode;
          }
        } catch {
          // Starter code remains the fallback if no accepted submission can be loaded.
        }

        if (cancelled) return;
        setLanguage(editorLanguage);
        setCode(editorCode);
        setIsCodeDirty(false);

        if (mapped.slug && (isLegacyIdRoute || routeValue !== mapped.slug)) {
          navigate(`/exercises/challenge/${mapped.slug}`, {
            replace: true,
            state: { courseTitle: nextCourseTitle, courseSlug: nextCourseSlug },
          });
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            buildAppErrorState(err, {
              title: 'Không thể tải bài tập',
              fallbackPath: stateCourseSlug ? `/exercises/code/${stateCourseSlug}?tab=coding` : '/exercises',
            }),
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchProblem();
    return () => {
      cancelled = true;
    };
  }, [isLegacyIdRoute, navigate, routeValue, stateCourseSlug, stateCourseTitle]);

  useEffect(() => {
    if (challenge?.id && setProblemId) {
      setProblemId(challenge.id);
    }
    return () => {
      if (setProblemId) setProblemId(null);
    };
  }, [challenge?.id, setProblemId]);

  useEffect(() => {
    if (!challenge?.id) return undefined;

    let cancelled = false;

    const fetchSubmissionHistory = async () => {
      setSubmissionHistory({
        loading: true,
        rows: [],
        error: null,
      });

      try {
        const history = await assignmentApi.getSubmissionHistory({
          problemId: challenge.id,
          page: 0,
          size: 10,
        });

        if (cancelled) return;
        setSubmissionHistory({
          loading: false,
          rows: mapSubmissionHistoryRows(history),
          error: null,
        });
      } catch (err) {
        if (cancelled) return;
        setSubmissionHistory({
          loading: false,
          rows: [],
          error: err.response?.data?.message || err.message || 'Không thể tải lịch sử nộp bài.',
        });
      }
    };

    fetchSubmissionHistory();

    return () => {
      cancelled = true;
    };
  }, [challenge?.id]);

  const handleCodeChange = useCallback((nextCode) => {
    setCode(nextCode);
    setIsCodeDirty(true);
  }, []);

  const handleLanguageChange = useCallback(
    (newLang) => {
      if (newLang === language) return;
      setLanguage(newLang);
      if (!isCodeDirty) {
        setCode(getBoilerplateCode(newLang));
      }
    },
    [isCodeDirty, language],
  );

  const handleSubmissionSelect = useCallback(async (submissionId) => {
    if (!submissionId) return;

    setLoadingSubmissionId(submissionId);
    try {
      const submission = await assignmentApi.getSubmission(submissionId);
      if (!submission?.sourceCode) {
        toast.info('Không tìm thấy mã nguồn của bài nộp này.');
        return;
      }

      setLanguage(normalizeEditorLanguage(submission.language));
      setCode(submission.sourceCode);
      setIsCodeDirty(false);
    } catch (err) {
      toast.error(`Không thể tải mã nguồn bài nộp: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoadingSubmissionId(null);
    }
  }, [toast]);

  const handleRunCode = async () => {
    if (!challenge?.id) return;

    const testCases = (challenge.examples || []).map((example) => ({
      input: String(example.input ?? ''),
      expectedOutput: String(example.output ?? ''),
    }));

    setIsRunning(true);
    setShowResults(false);
    setRunResult(null);
    setRunError(null);

    if (!testCases.length) {
      setRunError('Chưa có ví dụ công khai để chạy thử.');
      setIsRunning(false);
      setShowResults(true);
      return;
    }

    try {
      const result = await assignmentApi.runCode({
        problemId: challenge.id,
        language,
        sourceCode: code,
        testCases,
      });
      setRunResult(result);
    } catch (err) {
      setRunError(err.response?.data?.message || err.message || 'Không thể chạy thử mã.');
    } finally {
      setIsRunning(false);
      setShowResults(true);
    }
  };

  const handleSubmitSolution = async () => {
    if (!challenge?.id) return;

    setIsSubmitting(true);
    setShowResults(false);
    setRunResult(null);
    setRunError(null);

    try {
      const submission = await assignmentApi.submitCode({
        problemId: challenge.id,
        language,
        sourceCode: code,
      });

      if (!submission?.submissionId) {
        throw new Error('Backend khong tra ve ma bai nop.');
      }

      const resultBasePath = challenge.slug
        ? `/exercises/challenge/${challenge.slug}/result`
        : `/exercises/challenge/${challenge.id}/result`;
      const resultPath = `${resultBasePath}?submissionId=${encodeURIComponent(submission.submissionId)}`;

      navigate(resultPath, {
        state: {
          submission,
          challenge: { ...challenge, courseTitle: courseContext.title, courseSlug: courseContext.slug },
          courseTitle: courseContext.title,
          courseSlug: courseContext.slug,
        },
      });
    } catch (err) {
      const compileError = getCompileErrorFromResponse(err);
      if (err.response?.data?.code === 3313 || compileError) {
        setRunResult({
          status: 'CE',
          allPassed: false,
          score: 0,
          compileError: compileError || err.response?.data?.message || 'Biên dịch thất bại.',
          testCaseResults: [],
        });
        setShowResults(true);
        return;
      }

      setRunError(err.response?.data?.message || err.message || 'Không thể nộp bài.');
      setShowResults(true);
      toast.error(`Lỗi khi nộp bài: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải bài tập...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  if (!challenge) return null;

  return (
    <AnimatedPage>
      <div className="exercise-detail-container">
        <div className="exercise-detail-top">
          <ExerciseBreadcrumb
            items={[
              ...(courseContext.title
                ? [{ label: courseContext.title, link: courseContext.slug ? `/exercises/code/${courseContext.slug}?tab=coding` : '/exercises' }]
                : []),
              { label: challenge.title },
            ]}
          />
          <ExerciseHeader challenge={challenge} />
        </div>

        <div className="exercise-workspace">
          <ProblemPanel
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            challenge={challenge}
            submissionHistory={submissionHistory}
            onSubmissionSelect={handleSubmissionSelect}
            loadingSubmissionId={loadingSubmissionId}
          />
          <EditorPanel
            code={code}
            setCode={handleCodeChange}
            language={language}
            onLanguageChange={handleLanguageChange}
            isRunning={isRunning}
            isSubmitting={isSubmitting}
            showResults={showResults}
            runResult={runResult}
            runError={runError}
            handleRunCode={handleRunCode}
            handleSubmitSolution={handleSubmitSolution}
          />
        </div>
      </div>
    </AnimatedPage>
  );
}
