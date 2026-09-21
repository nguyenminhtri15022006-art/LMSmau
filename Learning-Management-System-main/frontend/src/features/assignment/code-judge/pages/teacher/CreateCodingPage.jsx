import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import courseApi from '../../../../../services/course.api';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../../components/ui/LockedFeature';
import CreateCodingHeader from '../../components/teacher/CreateCoding/CreateCodingHeader';
import ProblemDetailsCard from '../../components/teacher/CreateCoding/ProblemDetailsCard';
import TestCasesCard from '../../components/teacher/CreateCoding/TestCasesCard';
import ChallengeSettingsPanel from '../../components/teacher/CreateCoding/ChallengeSettingsPanel';
import AiTestCaseModal from '../../components/teacher/CreateCoding/AiTestCaseModal';
import UploadCard from '../../components/teacher/ImportTestCases/UploadCard';
import FileStatusCard from '../../components/teacher/ImportTestCases/FileStatusCard';
import AssignmentMessageDialog from '../../../shared/components/AssignmentMessageDialog';
import { POPULAR_LANGUAGES } from '../../constants/languageBoilerplates';
import '../../styles/teacher/ImportTestCases/importTestCasesPage.css';
import '../../styles/teacher/CreateCoding/createCodingPage.css';

const LOCKED_PROBLEM_ERROR_CODES = new Set([3220, 3221]);
const MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_IMPORT_EXTENSIONS = ['csv', 'xlsx'];
const DEFAULT_LANGUAGE_IDS = POPULAR_LANGUAGES.map((language) => language.id);
const DEFAULT_SCORE = 100;
const DEFAULT_TIME_LIMIT_MS = 1000;
const DEFAULT_MEMORY_LIMIT_MB = 256;
const OTHER_LESSON_ID = '00000000-0000-0000-0000-000000000000';

function toNonNegativeInteger(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

function toPositiveNumber(value, fallback = 1) {
  const parsed = Number.parseFloat(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function mapProblemTestCase(testCase, index) {
  return {
    id: testCase.testId ?? `loaded-${index}`,
    testId: testCase.testId,
    input: testCase.input ?? '',
    output: testCase.expectedOutput ?? '',
    hidden: !!testCase.hidden,
    orderIndex: testCase.orderIndex ?? index,
    scoreWeight: testCase.scoreWeight ?? 1,
  };
}

function buildTestCasePayload(testCases) {
  return testCases
    .filter(tc => (tc.output ?? '').trim())
    .map((tc, index) => ({
      testId: tc.testId ?? null,
      input: tc.input ?? '',
      expectedOutput: tc.output,
      hidden: tc.hidden,
      orderIndex: toNonNegativeInteger(tc.orderIndex, index),
      scoreWeight: toPositiveNumber(tc.scoreWeight, 1),
    }));
}

function normalizeLockedState(problemFields, testCases) {
  return JSON.stringify({
    timeLimitMs: Number(problemFields.timeLimitMs),
    memoryLimitMb: Number(problemFields.memoryLimitMb),
    score: Number(problemFields.score),
    testCases: buildTestCasePayload(testCases).map(tc => ({
      testId: tc.testId,
      input: tc.input,
      expectedOutput: tc.expectedOutput,
      hidden: !!tc.hidden,
      scoreWeight: Number(tc.scoreWeight ?? 1),
    })),
  });
}

function isLockedProblemError(err) {
  return LOCKED_PROBLEM_ERROR_CODES.has(Number(err?.response?.data?.code));
}

function getErrorDetail(err) {
  return err?.response?.data?.message || err?.message || 'Vui lòng thử lại sau.';
}

function getExtension(filename = '') {
  return filename.split('.').pop()?.toLowerCase() || '';
}

function toSelectedLessonId(lessonId) {
  if (!lessonId || lessonId === OTHER_LESSON_ID) return '';
  return String(lessonId);
}

function toProblemLessonId(selectedLessonId) {
  return selectedLessonId || OTHER_LESSON_ID;
}

export default function CreateCodingPage() {
  const { courseId, slugOrId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const routeCourseName = location.state?.courseTitle || location.state?.courseName || '';
  const [courseName, setCourseName] = useState(routeCourseName);
  const routeProblemRef = slugOrId;
  const isEditMode = !!routeProblemRef;
  const isNumericProblemRef = /^\d+$/.test(routeProblemRef || '');

  const [loading, setLoading] = useState(isEditMode);
  const [saving, setSaving] = useState(false);
  const [problemInfo, setProblemInfo] = useState({
    title: '',
    description: '',
  });
  const [difficulty, setDifficulty] = useState('EASY');
  const [selectedLessonId, setSelectedLessonId] = useState('');
  const [lessonOptions, setLessonOptions] = useState([]);
  const [selectedLanguages, setSelectedLanguages] = useState(DEFAULT_LANGUAGE_IDS);
  const [score, setScore] = useState(DEFAULT_SCORE);
  const [timeLimitMs, setTimeLimitMs] = useState(DEFAULT_TIME_LIMIT_MS);
  const [memoryLimitMb, setMemoryLimitMb] = useState(DEFAULT_MEMORY_LIMIT_MB);
  const [testCases, setTestCases] = useState([
    { id: 1, input: '', output: '', hidden: false, orderIndex: 0, scoreWeight: 1 },
  ]);
  const [originalLockedState, setOriginalLockedState] = useState(null);
  const [pendingCloneSave, setPendingCloneSave] = useState(null);
  const [isCloning, setIsCloning] = useState(false);
  const [cloneError, setCloneError] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importTemplateFormat, setImportTemplateFormat] = useState('xlsx');
  const [downloadingImportTemplate, setDownloadingImportTemplate] = useState(false);
  const [importingTestCases, setImportingTestCases] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [importErrorMessage, setImportErrorMessage] = useState('');
  const [loadError, setLoadError] = useState('');
  const [messageDialog, setMessageDialog] = useState(null);
  const [problemId, setProblemId] = useState(isNumericProblemRef ? routeProblemRef : null);

  const showMessageDialog = (dialog) => {
    setMessageDialog({ tone: 'error', ...dialog });
  };

  useEffect(() => {
    if (!courseId) return;

    if (!courseName) {
      courseApi.getById(courseId)
        .then(res => {
          const course = res?.data?.data ?? res?.data;
          const name = course?.title || course?.name || course?.courseTitle || course?.courseName || '';
          if (name) setCourseName(name);
        })
        .catch(err => console.warn('Failed to load course details:', err));
    }

    courseApi.getLessonNodes(courseId)
      .then(nodes => {
        const options = (nodes ?? [])
          .filter(node => node?.lessonId)
          .map(node => ({
            value: String(node.lessonId),
            label: node.title || `Lesson ${node.lessonId}`,
          }));
        setLessonOptions(options);
      })
      .catch(err => console.warn('Failed to load lesson nodes:', err));
  }, [courseId, courseName]);

  useEffect(() => {
    if (!isEditMode) return;

    let cancelled = false;
    setLoading(true);
    setLoadError('');

    const loadProblem = isNumericProblemRef
      ? assignmentApi.getProblemDetail(Number(routeProblemRef))
      : assignmentApi.getProblemDetailBySlug(routeProblemRef);

    loadProblem
      .then(problem => {
        if (cancelled) return;

        setProblemId(problem.problemId);
        setProblemInfo({
          title: problem.title ?? '',
          description: problem.description ?? '',
        });
        setDifficulty(problem.difficulty ?? 'EASY');
        setSelectedLessonId(toSelectedLessonId(problem.lessonId));
        const loadedLanguages = problem.allowedLangs?.filter((lang) => DEFAULT_LANGUAGE_IDS.includes(lang)) ?? [];
        setSelectedLanguages(loadedLanguages.length ? loadedLanguages : DEFAULT_LANGUAGE_IDS);
        setScore(problem.score ?? DEFAULT_SCORE);
        setTimeLimitMs(problem.timeLimitMs ?? DEFAULT_TIME_LIMIT_MS);
        setMemoryLimitMb(problem.memoryLimitMb ?? DEFAULT_MEMORY_LIMIT_MB);
        const loadedTestCases = problem.testCases?.length
          ? problem.testCases.map(mapProblemTestCase)
          : [{ id: 1, input: '', output: '', hidden: false, orderIndex: 0, scoreWeight: 1 }];
        setTestCases(loadedTestCases);
        setOriginalLockedState({
          hasSubmissions: Number(problem.totalSubmit) > 0,
          snapshot: normalizeLockedState({
            timeLimitMs: problem.timeLimitMs ?? DEFAULT_TIME_LIMIT_MS,
            memoryLimitMb: problem.memoryLimitMb ?? DEFAULT_MEMORY_LIMIT_MB,
            score: problem.score ?? DEFAULT_SCORE,
          }, loadedTestCases),
          testCases: loadedTestCases,
        });
        if (isNumericProblemRef && problem.slug) {
          navigate(`/instructor/exercises/${courseId}/code-judge/edit/${problem.slug}`, { replace: true });
        }
      })
      .catch(err => {
        if (!cancelled) {
          setLoadError(err.response?.data?.message || 'Không thể tải dữ liệu bài tập lập trình.');
        }
        console.error('Failed to load coding problem:', err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, isEditMode, isNumericProblemRef, navigate, routeProblemRef]);

  const handleAddTestCase = () => {
    setTestCases(prev => [
      ...prev,
      {
        id: Date.now(),
        input: '',
        output: '',
        hidden: false,
        orderIndex: prev.length,
        scoreWeight: 1,
      },
    ]);
  };

  const updateTestCase = (id, field, value) => {
    setTestCases(prev => prev.map(tc => (tc.id === id ? { ...tc, [field]: value } : tc)));
  };

  const deleteTestCase = (id) => {
    setTestCases(prev => prev.filter(tc => tc.id !== id));
  };

  const buildAiConstraints = () => [
    'Ràng buộc bài toán: xem trong mô tả bài toán phía trên',
    `Độ khó: ${difficulty}`,
    `Thời gian chạy: ${timeLimitMs}ms`,
    `Bộ nhớ: ${memoryLimitMb}MB`,
    `Điểm: ${score}`,
    `Ngôn ngữ cho phép: ${selectedLanguages.join(', ') || 'Chưa chọn'}`,
  ].join('\n');

  const buildAiProblemContext = () => ({
    problemId: problemId ? Number(problemId) : null,
    courseId,
    title: problemInfo.title,
    description: problemInfo.description,
    constraints: buildAiConstraints(),
    difficulty,
    timeLimitMs: Number(timeLimitMs),
    memoryLimitMb: Number(memoryLimitMb),
    score: Number(score),
    allowedLangs: selectedLanguages,
    existingTestCases: buildTestCasePayload(testCases),
  });

  const handleAIGenerate = () => {
    setIsAiModalOpen(true);
  };

  const handleApplyAiTestCases = (generatedCases) => {
    setTestCases(prev => {
      const startIndex = prev.length;
      const timestamp = Date.now();
      const mappedCases = generatedCases.map((testCase, index) => ({
        id: `${timestamp}-${index}`,
        input: testCase.input ?? '',
        output: testCase.output ?? testCase.expectedOutput ?? '',
        hidden: testCase.hidden ?? true,
        orderIndex: toNonNegativeInteger(testCase.orderIndex, startIndex + index),
        scoreWeight: toPositiveNumber(testCase.scoreWeight, 1),
      }));
      return [...prev, ...mappedCases];
    });
  };

  const resetImportModal = () => {
    setImportFile(null);
    setImportResult(null);
    setImportErrorMessage('');
    setDownloadingImportTemplate(false);
    setImportingTestCases(false);
  };

  const closeImportModal = () => {
    if (importingTestCases) return;
    setIsImportModalOpen(false);
    resetImportModal();
  };

  const openImportModal = () => {
    if (!problemId) {
      showMessageDialog({
        title: 'Chưa thể nhập test cases',
        message: 'Vui lòng lưu bài tập trước khi nhập bộ test cases.',
      });
      return;
    }
    resetImportModal();
    setIsImportModalOpen(true);
  };

  const handleImportFileChange = (selectedFile) => {
    const nextFile = selectedFile?.target?.files?.[0] || selectedFile;
    if (!nextFile) return;

    const extension = getExtension(nextFile.name);
    if (!ALLOWED_IMPORT_EXTENSIONS.includes(extension)) {
      setImportFile(null);
      setImportResult(null);
      setImportErrorMessage('Chỉ hỗ trợ file .xlsx hoặc .csv.');
      return;
    }

    if (nextFile.size > MAX_IMPORT_FILE_SIZE) {
      setImportFile(null);
      setImportResult(null);
      setImportErrorMessage('File không được vượt quá 10MB.');
      return;
    }

    setImportFile(nextFile);
    setImportResult(null);
    setImportErrorMessage('');
  };

  const handleRemoveImportFile = () => {
    setImportFile(null);
    setImportResult(null);
    setImportErrorMessage('');
  };

  const handleDownloadImportTemplate = async () => {
    setDownloadingImportTemplate(true);
    try {
      await assignmentApi.downloadTestCaseTemplate(importTemplateFormat);
    } catch (err) {
      setImportErrorMessage(getErrorDetail(err));
    } finally {
      setDownloadingImportTemplate(false);
    }
  };

  const handleImportTestCases = async () => {
    if (!problemId) {
      setImportErrorMessage('Vui lòng lưu bài tập trước khi nhập bộ test cases.');
      return;
    }
    if (!importFile) {
      setImportErrorMessage('Vui lòng chọn file test cases.');
      return;
    }

    setImportingTestCases(true);
    setImportErrorMessage('');
    setImportResult(null);
    try {
      const result = await assignmentApi.importTestCases(problemId, importFile);
      if (result?.failedCount > 0) {
        setImportResult(result);
        return;
      }
      const problem = await assignmentApi.getProblemDetail(problemId);
      const importedCases = problem.testCases?.length
        ? problem.testCases.map(mapProblemTestCase)
        : [{ id: 1, input: '', output: '', hidden: false, orderIndex: 0, scoreWeight: 1 }];
      setTestCases(importedCases);
      setIsImportModalOpen(false);
      resetImportModal();
    } catch (err) {
      const responseData = err?.response?.data?.data;
      if (responseData?.errors?.length) {
        setImportResult(responseData);
      }
      setImportErrorMessage(getErrorDetail(err));
    } finally {
      setImportingTestCases(false);
    }
  };

  const aiProblemContext = useMemo(
    () => buildAiProblemContext(),
    [problemId, courseId, problemInfo, difficulty, timeLimitMs, memoryLimitMb, score, selectedLanguages, testCases]
  );

  const validateTestCases = () => {
    const hasPartialCase = testCases.some(tc => {
      const hasInput = (tc.input ?? '').trim().length > 0;
      const hasOutput = (tc.output ?? '').trim().length > 0;
      return hasInput && !hasOutput;
    });

    if (hasPartialCase) {
      showMessageDialog({
        title: 'Test case chưa hợp lệ',
        message: 'Mỗi bộ test có đầu vào cần có đầu ra mong đợi.',
      });
      return false;
    }

    const hasInvalidOrderIndex = testCases.some(tc => {
      if (!(tc.output ?? '').trim()) return false;
      const parsed = Number.parseInt(tc.orderIndex, 10);
      return !Number.isInteger(parsed) || parsed < 0;
    });

    if (hasInvalidOrderIndex) {
      showMessageDialog({
        title: 'Thứ tự test case chưa hợp lệ',
        message: 'Thứ tự test case phải là số nguyên không âm.',
      });
      return false;
    }

    const hasInvalidScoreWeight = testCases.some(tc => {
      if (!(tc.output ?? '').trim()) return false;
      const parsed = Number.parseFloat(tc.scoreWeight);
      return !Number.isFinite(parsed) || parsed <= 0;
    });

    if (hasInvalidScoreWeight) {
      showMessageDialog({
        title: 'Trọng số test case chưa hợp lệ',
        message: 'Trọng số test case phải lớn hơn 0.',
      });
      return false;
    }

    return true;
  };

  const syncTestCases = async (targetProblemId, cases = testCases) => {
    await assignmentApi.syncTestCases(targetProblemId, buildTestCasePayload(cases));
  };

  const hasLockedChanges = (payload) => {
    if (!originalLockedState) return false;
    return originalLockedState.snapshot !== normalizeLockedState(payload, testCases);
  };

  const saveProblemAndCases = async (targetProblemId, payload, shouldSyncTestCases, cases = testCases) => {
    await assignmentApi.updateProblem(targetProblemId, payload);
    if (shouldSyncTestCases) {
      await syncTestCases(targetProblemId, cases);
    }
  };

  const buildCloneTestCases = (cloneProblem) => {
    const sourceCases = originalLockedState?.testCases ?? [];
    const cloneCases = cloneProblem.testCases?.map(mapProblemTestCase) ?? [];
    const cloneIdBySourceId = new Map();

    sourceCases.forEach((sourceCase, index) => {
      if (sourceCase.testId && cloneCases[index]?.testId) {
        cloneIdBySourceId.set(sourceCase.testId, cloneCases[index].testId);
      }
    });

    return testCases.map(testCase => ({
      ...testCase,
      testId: testCase.testId ? cloneIdBySourceId.get(testCase.testId) : undefined,
    }));
  };

  const openCloneDialog = (payload, isPublish) => {
    setPendingCloneSave({ payload, isPublish });
    setCloneError('');
  };

  const handleSave = async (isPublish) => {
    const trimmedTitle = problemInfo.title.trim();
    const trimmedDescription = problemInfo.description.trim();

    if (!trimmedTitle || !trimmedDescription) {
      showMessageDialog({
        title: 'Thiếu thông tin bài tập',
        message: 'Vui lòng nhập tên bài tập và mô tả bài tập trước khi lưu.',
      });
      return;
    }

    if (!validateTestCases()) return;

    const numericScore = Number(score);
    const numericTimeLimitMs = Number(timeLimitMs);
    const numericMemoryLimitMb = Number(memoryLimitMb);

    if (!Number.isFinite(numericScore) || numericScore < 1 || numericScore > 1000) {
      showMessageDialog({
        title: 'Điểm chưa hợp lệ',
        message: 'Điểm phải nằm trong khoảng 1 đến 1000.',
      });
      return;
    }

    if (!Number.isFinite(numericTimeLimitMs) || numericTimeLimitMs < 50 || numericTimeLimitMs > 10000) {
      showMessageDialog({
        title: 'Thời gian chạy chưa hợp lệ',
        message: 'Thời gian chạy phải nằm trong khoảng 50ms đến 10000ms.',
      });
      return;
    }

    if (!Number.isFinite(numericMemoryLimitMb) || numericMemoryLimitMb < 16 || numericMemoryLimitMb > 512) {
      showMessageDialog({
        title: 'Bộ nhớ chưa hợp lệ',
        message: 'Bộ nhớ phải nằm trong khoảng 16MB đến 512MB.',
      });
      return;
    }

    if (selectedLanguages.length === 0) {
      showMessageDialog({
        title: 'Thiếu ngôn ngữ lập trình',
        message: 'Vui lòng chọn ít nhất một ngôn ngữ lập trình.',
      });
      return;
    }

    setSaving(true);
    let payload = null;
    try {
      payload = {
        title: trimmedTitle,
        description: trimmedDescription,
        difficulty,
        lessonId: toProblemLessonId(selectedLessonId),
        timeLimitMs: numericTimeLimitMs,
        memoryLimitMb: numericMemoryLimitMb,
        allowedLangs: selectedLanguages,
        score: numericScore,
        isPublic: isPublish,
      };

      if (isEditMode) {
        if (originalLockedState?.hasSubmissions && hasLockedChanges(payload)) {
          openCloneDialog(payload, isPublish);
          return;
        }

        await saveProblemAndCases(problemId, payload, !originalLockedState?.hasSubmissions);
      } else {
        const problem = await assignmentApi.createProblem({
          ...payload,
          courseId,
        });
        await syncTestCases(problem.problemId);
      }

      navigate(`/instructor/exercises/${courseId}?tab=coding&page=1`, {
        state: courseName ? { courseTitle: courseName } : undefined,
      });
    } catch (err) {
      if (isEditMode && isLockedProblemError(err)) {
        openCloneDialog(payload, isPublish);
      } else {
        showMessageDialog({
          title: 'Lỗi khi lưu bài tập',
          message: 'Không thể lưu bài tập lập trình.',
          detail: getErrorDetail(err),
        });
      }
    } finally {
      setSaving(false);
    }
  };

  const closeCloneDialog = () => {
    if (isCloning) return;
    setPendingCloneSave(null);
    setCloneError('');
  };

  const handleConfirmCloneSave = async () => {
    if (!pendingCloneSave) return;

    setIsCloning(true);
    setCloneError('');
    try {
      const cloned = await assignmentApi.cloneProblem(problemId);
      const newProblemId = cloned?.newProblemId;
      const cloneProblem = await assignmentApi.getProblemDetail(newProblemId);
      const cloneTestCases = buildCloneTestCases(cloneProblem);

      await saveProblemAndCases(newProblemId, pendingCloneSave.payload, true, cloneTestCases);
      setPendingCloneSave(null);
      navigate(`/instructor/exercises/${courseId}/code-judge/edit/${cloneProblem?.slug || newProblemId}`, {
        state: courseName ? { courseTitle: courseName } : undefined,
      });
    } catch (err) {
      setCloneError(err.response?.data?.message || 'Không thể tạo bản sao để chỉnh sửa. Vui lòng thử lại.');
      console.error('Failed to clone coding problem for editing:', err);
    } finally {
      setIsCloning(false);
    }
  };

  const cloneDialog = pendingCloneSave && (
    <div className="coding-clone-overlay" onClick={closeCloneDialog}>
      <div
        className="coding-clone-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="coding-clone-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="coding-clone-icon">
          <span className="material-symbols-outlined">content_copy</span>
        </div>
        <div className="coding-clone-body">
          <h2 id="coding-clone-title">Bài tập đã có bài nộp</h2>
          <p>
            Test cases, giới hạn thời gian, bộ nhớ và điểm số ảnh hưởng trực tiếp đến lịch sử chấm bài.
            Hệ thống sẽ tạo một bản nháp mới để chỉnh sửa, còn bài tập cũ vẫn giữ nguyên submissions và điểm.
          </p>
          {cloneError && <div className="coding-clone-error">{cloneError}</div>}
        </div>
        <div className="coding-clone-actions">
          <button
            type="button"
            className="coding-clone-cancel"
            onClick={closeCloneDialog}
            disabled={isCloning}
          >
            Hủy
          </button>
          <button
            type="button"
            className="coding-clone-primary"
            onClick={handleConfirmCloneSave}
            disabled={isCloning}
          >
            {isCloning ? 'Đang tạo bản sao...' : 'Tạo bản sao để chỉnh sửa'}
          </button>
        </div>
      </div>
    </div>
  );

  const importModal = isImportModalOpen && (
    <div className="coding-import-overlay" onClick={closeImportModal}>
      <div
        className="coding-import-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="coding-import-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="coding-import-header">
          <div>
            <h2 id="coding-import-title">Nhập hàng loạt test cases</h2>
            <p>Chọn file .xlsx hoặc .csv để thêm nhanh bộ test vào bài tập này.</p>
          </div>
          <button
            type="button"
            className="coding-import-close"
            onClick={closeImportModal}
            disabled={importingTestCases}
            aria-label="Đóng popup nhập test cases"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className={`import-workbench coding-import-workbench ${importFile ? 'import-workbench--with-file' : ''}`}>
          <UploadCard
            onFileChange={handleImportFileChange}
            templateFormat={importTemplateFormat}
            onTemplateFormatChange={setImportTemplateFormat}
            onDownloadTemplate={handleDownloadImportTemplate}
            isDownloadingTemplate={downloadingImportTemplate}
            hasFile={!!importFile}
          />

          <div className="import-status-panel">
            {importErrorMessage && (
              <div className="import-error-banner">
                <span className="material-symbols-outlined">error</span>
                <span>{importErrorMessage}</span>
              </div>
            )}

            <FileStatusCard
              file={importFile}
              onRemoveFile={handleRemoveImportFile}
              importResult={importResult}
              onImport={handleImportTestCases}
              importing={importingTestCases}
              canImport={!!problemId}
            />

            {!importFile && (
              <div className="coding-import-info-card">
                <span className="material-symbols-outlined">table_rows</span>
                <h3>Chưa chọn file test cases</h3>
                <p>
                  Tải file mẫu rồi điền từng dòng test case. File cần có đầu vào,
                  đầu ra mong đợi, trạng thái ẩn/hiện và trọng số điểm nếu muốn tùy chỉnh.
                </p>
                <div className="coding-import-info-list">
                  <span>.xlsx hoặc .csv</span>
                  <span>Tối đa 10MB</span>
                  <span>Kiểm tra lỗi trước khi lưu</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <AnimatedPage>
      <LockedFeature featureName={isEditMode ? 'Edit Coding Challenge' : 'Create Coding Challenge'}>
        <div className="coding-page create-coding-page">
          <CreateCodingHeader
            courseId={courseId}
            courseTitle={courseName}
            problemTitle={problemInfo.title}
            isEditMode={isEditMode}
            onSaveDraft={() => handleSave(false)}
            onPublish={() => handleSave(true)}
            saving={saving || loading}
          />

          {loading ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '80px 0' }}>
              <div className="loading-screen__spinner" />
              <p style={{ color: 'var(--text-600)', marginTop: 16 }}>Đang tải dữ liệu bài tập...</p>
            </div>
          ) : loadError ? (
            <div style={{ padding: '48px 0', color: 'var(--danger-600)' }}>
              {loadError}
            </div>
          ) : (
            <div className="coding-creator-layout create-coding-layout">
              <div className="main-column create-coding-main-column">
                <ProblemDetailsCard
                  problemInfo={problemInfo}
                  onUpdate={setProblemInfo}
                />
                <TestCasesCard
                  courseId={courseId}
                  testCases={testCases}
                  isGenerating={isGenerating}
                  canBulkImport={isEditMode}
                  onBulkImport={openImportModal}
                  onAddTestCase={handleAddTestCase}
                  onAIGenerate={handleAIGenerate}
                  onUpdateTestCase={updateTestCase}
                  onDeleteTestCase={deleteTestCase}
                />
                <AiTestCaseModal
                  open={isAiModalOpen}
                  problemContext={aiProblemContext}
                  onClose={() => setIsAiModalOpen(false)}
                  onApply={handleApplyAiTestCases}
                  onGeneratingChange={setIsGenerating}
                />
              </div>

              <ChallengeSettingsPanel
                selectedChapter={selectedLessonId}
                lessonOptions={lessonOptions}
                difficulty={difficulty === 'EASY' ? 'Easy' : difficulty === 'MEDIUM' ? 'Medium' : 'Hard'}
                selectedLanguages={selectedLanguages}
                score={score}
                timeLimitMs={timeLimitMs}
                memoryLimitMb={memoryLimitMb}
                onChapterChange={setSelectedLessonId}
                onDifficultyChange={(d) => setDifficulty(d === 'Easy' ? 'EASY' : d === 'Medium' ? 'MEDIUM' : 'HARD')}
                onLanguagesChange={setSelectedLanguages}
                onScoreChange={setScore}
                onTimeLimitChange={setTimeLimitMs}
                onMemoryLimitChange={setMemoryLimitMb}
                onPublish={() => handleSave(true)}
                onSaveDraft={() => handleSave(false)}
                saving={saving}
              />
            </div>
          )}
          {cloneDialog}
          {importModal}
          <AssignmentMessageDialog
            open={!!messageDialog}
            title={messageDialog?.title}
            message={messageDialog?.message}
            detail={messageDialog?.detail}
            tone={messageDialog?.tone}
            onClose={() => setMessageDialog(null)}
          />
        </div>
      </LockedFeature>
    </AnimatedPage>
  );
}
