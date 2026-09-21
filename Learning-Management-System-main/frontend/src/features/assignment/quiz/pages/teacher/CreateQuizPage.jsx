import React, { useEffect, useRef, useState } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import courseApi from '../../../../../services/course.api';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../../components/ui/LockedFeature';
import useFileUpload from '../../../../../hooks/useFileUpload';
import StepUpload from '../../components/teacher/CreateQuiz/StepUpload';
import StepReview from '../../components/teacher/CreateQuiz/StepReview';
import StepSettings from '../../components/teacher/CreateQuiz/StepSettings';
import ReuseQuizQuestionModal from '../../components/teacher/ReuseQuizQuestionModal';
import {
  revokePendingQuestionImage,
  uploadPendingQuestionImages,
} from '../../utils/questionImageUpload';
import { distributeQuizQuestionScores } from '../../utils/quizScoring';
import '../../styles/teacher/CreateQuiz/createQuiz.css';

const DEFAULT_SETTINGS = {
  title: '',
  description: '',
  lessonId: '',
  duration: 45,
  totalScore: 100,
  passScore: 50,
  maxAttempts: '',
  shuffleQuestions: true,
  shuffleAnswers: true,
  showResult: 'AFTER_SUBMIT',
};

function mapImportedQuestion(question, index) {
  return {
    id: `imported-${index}`,
    questionId: question.questionId ?? null,
    reuseExisting: !!question.reuseExisting,
    bankLocked: !!question.reuseExisting,
    index,
    content: question.content ?? '',
    type: question.type ?? 'SINGLE',
    topic: question.topic ?? '',
    explanation: question.explanation ?? '',
    imageUrl: question.imageUrl ?? '',
    score: question.score ?? 10,
    answers: (question.answers ?? []).map((answer, answerIndex) => ({
      answerId: answer.answerId,
      content: answer.content ?? '',
      correct: !!answer.correct,
      orderIndex: answer.orderIndex ?? answerIndex,
    })),
  };
}

function createBlankQuestion(index) {
  return {
    id: `manual-${Date.now()}-${index}`,
    index,
    content: '',
    type: 'SINGLE',
    topic: '',
    explanation: '',
    imageUrl: '',
    score: 10,
    answers: [
      { content: '', correct: true, orderIndex: 0 },
      { content: '', correct: false, orderIndex: 1 },
      { content: '', correct: false, orderIndex: 2 },
      { content: '', correct: false, orderIndex: 3 },
    ],
  };
}

function mapReusableQuestion(question, index, sourceQuiz) {
  return {
    id: `reuse-${question.questionId}-${Date.now()}-${index}`,
    questionId: question.questionId,
    reuseExisting: true,
    bankLocked: true,
    sourceQuizTitle: question.sourceQuiz?.title || sourceQuiz?.title || '',
    index,
    content: question.content ?? '',
    type: question.type ?? 'SINGLE',
    topic: question.topic ?? '',
    explanation: question.explanation ?? '',
    imageUrl: question.imageUrl ?? '',
    score: question.score ?? 10,
    answers: (question.answers ?? []).map((answer, answerIndex) => ({
      answerId: answer.answerId,
      content: answer.content ?? '',
      correct: !!answer.correct,
      orderIndex: answer.orderIndex ?? answerIndex,
    })),
  };
}

function toQuestionPayload(question) {
  const payload = {
    content: question.content.trim(),
    type: question.type,
    topic: question.topic || null,
    explanation: question.explanation || null,
    imageUrl: question.imageUrl || null,
    score: Number(question.score) || 10,
    answers: question.answers
      .filter(answer => answer.content.trim())
      .map((answer, index) => ({
        content: answer.content.trim(),
        correct: !!answer.correct,
        orderIndex: index,
      })),
  };

  if (question.questionId && question.reuseExisting) {
    payload.questionId = question.questionId;
    payload.reuseExisting = true;
  }

  return payload;
}

function toQuizPayload(settings) {
  return {
    title: settings.title.trim(),
    description: settings.description.trim() || null,
    lessonId: settings.lessonId || null,
    duration: settings.duration === '' ? null : Number(settings.duration),
    totalScore: Number(settings.totalScore) || 100,
    passScore: Number(settings.passScore) || 50,
    maxAttempts: Number(settings.maxAttempts) || 0,
    shuffleQuestions: !!settings.shuffleQuestions,
    shuffleAnswers: !!settings.shuffleAnswers,
    showResult: settings.showResult || 'AFTER_SUBMIT',
  };
}

export default function CreateQuizPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const routeCourseName = location.state?.courseTitle || location.state?.courseName || '';
  const [currentStep, setCurrentStep] = useState(1);
  const [importResult, setImportResult] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [lessonOptions, setLessonOptions] = useState([]);
  const [courseName, setCourseName] = useState(routeCourseName);
  const [isSaving, setIsSaving] = useState(false);
  const [validationDialog, setValidationDialog] = useState(null);
  const [reuseModalOpen, setReuseModalOpen] = useState(false);
  const questionsRef = useRef([]);
  const { upload } = useFileUpload();
  const hasImportResult = Boolean(importResult);
  const hasImportedQuestions = questions.length > 0;

  const steps = [
    { num: 1, title: 'Tải lên' },
    { num: 2, title: 'Kiểm tra' },
    { num: 3, title: 'Cài đặt' },
  ];

  useEffect(() => {
    questionsRef.current = questions;
  }, [questions]);



  useEffect(() => () => {
    questionsRef.current.forEach(revokePendingQuestionImage);
  }, []);

  useEffect(() => {
    if (!courseId) return;

    courseApi.getById(courseId)
      .then(res => {
        const course = res?.data?.data ?? res?.data;
        setCourseName(course?.title || course?.name || routeCourseName);
      })
      .catch(err => console.warn('Failed to load course:', err));

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
  }, [courseId]);

  const handleImportSuccess = (result) => {
    const importedQuestions = (result?.questions ?? []).map(mapImportedQuestion);
    setImportResult(result);
    setQuestions(importedQuestions);
    setCurrentStep(2);
  };

  const handleManualStart = () => {
    const firstQuestion = createBlankQuestion(0);
    setImportResult({
      source: 'manual',
      validCount: 1,
      importedCount: 1,
    });
    setQuestions([firstQuestion]);
    setCurrentStep(2);
  };

  const handleAddManualQuestion = () => {
    const nextQuestion = createBlankQuestion(questionsRef.current.length);
    setQuestions(prev => [
      ...prev,
      { ...nextQuestion, index: prev.length },
    ]);
    setImportResult(prev => ({
      ...(prev ?? {}),
      source: prev?.source ?? 'manual',
      validCount: questionsRef.current.length + 1,
      importedCount: questionsRef.current.length + 1,
    }));
    return nextQuestion;
  };

  const handleReuseQuestions = (selectedQuestions, sourceQuiz) => {
    setQuestions(prev => {
      const existingIds = new Set(prev.map(question => question.questionId).filter(Boolean));
      const reusableQuestions = selectedQuestions
        .filter(question => !existingIds.has(question.questionId))
        .map((question, index) => mapReusableQuestion(question, prev.length + index, question.sourceQuiz ?? sourceQuiz));

      return [
        ...prev,
        ...reusableQuestions.map((question, offset) => ({ ...question, index: prev.length + offset })),
      ];
    });
    setImportResult(prev => ({
      ...(prev ?? {}),
      source: prev?.source ?? 'manual',
      validCount: questionsRef.current.length + selectedQuestions.length,
      importedCount: questionsRef.current.length + selectedQuestions.length,
    }));
    setReuseModalOpen(false);
    setCurrentStep(2);
  };

  const handleDeleteQuestion = (questionId) => {
    const currentQuestions = questionsRef.current;
    const deleteIndex = currentQuestions.findIndex(question => question.id === questionId);
    if (deleteIndex === -1) return null;

    const targetQuestion = currentQuestions[deleteIndex];
    revokePendingQuestionImage(targetQuestion);

    const nextQuestions = currentQuestions
      .filter(question => question.id !== questionId)
      .map((question, index) => ({ ...question, index }));
    const nextActiveQuestion = nextQuestions[deleteIndex] ?? nextQuestions[deleteIndex - 1] ?? null;

    setQuestions(nextQuestions);
    setImportResult(prev => {
      if (!prev) return prev;
      return {
        ...prev,
        validCount: nextQuestions.length,
        importedCount: nextQuestions.length,
      };
    });

    return nextActiveQuestion?.id ?? null;
  };

  const canGoToStep = (stepNum) => {
    if (stepNum === 1) return true;
    if (stepNum === 2) return hasImportResult;
    if (stepNum === 3) return hasImportedQuestions;
    return false;
  };

  const validateQuestions = () => {
    if (!questions.length) {
      setValidationDialog({
        title: 'Thiếu câu hỏi',
        message: 'Vui lòng import ít nhất một câu hỏi trước khi lưu bài kiểm tra.',
      });
      return false;
    }

    const invalidQuestion = questions.find(question => {
      const answers = question.answers.filter(answer => answer.content.trim());
      const correctCount = answers.filter(answer => answer.correct).length;
      if (!question.content.trim() || Number(question.score) < 1 || answers.length < 2) return true;
      if (question.type === 'MULTIPLE') return correctCount < 2;
      if (question.type === 'TRUE_FALSE') return answers.length !== 2 || correctCount !== 1;
      return correctCount !== 1;
    });

    if (invalidQuestion) {
      setValidationDialog({
        title: 'Câu hỏi chưa hợp lệ',
        message: 'Vui lòng kiểm tra lại nội dung câu hỏi, điểm số, đáp án và số lượng đáp án đúng.',
      });
      setCurrentStep(2);
      return false;
    }

    return true;
  };

  const validateSettings = () => {
    if (!settings.title.trim()) {
      setValidationDialog({
        title: 'Thiếu thông tin bài kiểm tra',
        message: 'Vui lòng nhập tên bài kiểm tra trong phần Thông tin cơ bản.',
      });
      setCurrentStep(3);
      return false;
    }

    const duration = settings.duration === '' ? null : Number(settings.duration);
    const totalScore = Number(settings.totalScore);
    const passScore = Number(settings.passScore);
    const maxAttempts = Number(settings.maxAttempts);

    if (duration !== null && (!Number.isFinite(duration) || duration < 1)) {
      setValidationDialog({
        title: 'Thời gian chưa hợp lệ',
        message: 'Thời gian làm bài phải lớn hơn hoặc bằng 1 phút, hoặc để trống nếu không giới hạn.',
      });
      setCurrentStep(3);
      return false;
    }

    if (!Number.isFinite(totalScore) || totalScore <= 0) {
      setValidationDialog({
        title: 'Tổng điểm chưa hợp lệ',
        message: 'Vui lòng nhập tổng điểm lớn hơn 0.',
      });
      setCurrentStep(3);
      return false;
    }

    if (!Number.isFinite(passScore) || passScore < 0 || passScore > 100) {
      setValidationDialog({
        title: 'Điểm đạt chưa hợp lệ',
        message: 'Điểm đạt phải nằm trong khoảng 0% đến 100%.',
      });
      setCurrentStep(3);
      return false;
    }

    if (!Number.isFinite(maxAttempts) || maxAttempts < 0) {
      setValidationDialog({
        title: 'Số lần làm chưa hợp lệ',
        message: 'Số lần làm tối đa phải lớn hơn hoặc bằng 0. Nhập 0 nếu không giới hạn.',
      });
      setCurrentStep(3);
      return false;
    }

    return true;
  };

  const saveQuiz = async ({ publish }) => {
    if (!validateQuestions() || !validateSettings()) return;

    setIsSaving(true);
    let uploadedQuestionImageUrls = [];
    try {
      const uploadedImageResult = await uploadPendingQuestionImages(questions, upload, courseId, {
        onUploaded: (url) => uploadedQuestionImageUrls.push(url),
      });
      uploadedQuestionImageUrls = uploadedImageResult.uploadedUrls;
      await assignmentApi.createQuizFromImportedQuestions(courseId, {
        quiz: toQuizPayload(settings),
        questions: uploadedImageResult.questions.map(toQuestionPayload),
        publish,
      });
      questions.forEach(revokePendingQuestionImage);
      uploadedQuestionImageUrls = [];
      navigate(`/instructor/exercises/${courseId}`);
    } catch (err) {
      if (uploadedQuestionImageUrls.length > 0) {
        await Promise.allSettled(uploadedQuestionImageUrls.map(url => assignmentApi.deleteUploadedFile(url)));
      }
      setValidationDialog({
        title: 'Không thể lưu quiz',
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleBackToUpload = () => {
    questions.forEach(revokePendingQuestionImage);
    setImportResult(null);
    setQuestions([]);
    setCurrentStep(1);
  };

  const handleNext = () => {
    if (currentStep === 1 && hasImportResult) {
      setCurrentStep(2);
      return;
    }
    if (currentStep === 2 && hasImportedQuestions) {
      setCurrentStep(3);
      return;
    }
    if (currentStep === 3) {
      saveQuiz({ publish: true });
    }
  };

  return (
    <AnimatedPage>
      <LockedFeature featureName="Create Quiz">
        <div className="create-quiz-page">
          <header className="create-quiz-header">
            <div className="header-top">
              <nav className="create-quiz-breadcrumb" aria-label="Điều hướng tạo câu hỏi">
                <Link className="create-quiz-breadcrumb-link" to="/instructor/exercises">Bài tập</Link>
                <span className="create-quiz-breadcrumb-separator" aria-hidden="true"></span>
                <Link
                  className="create-quiz-breadcrumb-link create-quiz-breadcrumb-course"
                  to={`/instructor/exercises/${courseId}`}
                >
                  {courseName || 'Khóa học'}
                </Link>
                <span className="create-quiz-breadcrumb-separator" aria-hidden="true"></span>
                <span className="create-quiz-breadcrumb-current">Tạo câu hỏi mới</span>
              </nav>
            </div>

            <div className="header-main">
              <h1>Tạo câu hỏi mới</h1>
              <div className="header-actions">
                <button
                  className="create-quiz-action-btn create-quiz-action-btn--secondary"
                  onClick={() => saveQuiz({ publish: false })}
                  disabled={currentStep !== 3 || isSaving}
                >
                  {isSaving ? 'Đang lưu...' : 'Lưu nháp'}
                </button>
                <button
                  className="create-quiz-action-btn create-quiz-action-btn--primary"
                  onClick={handleNext}
                  disabled={
                    isSaving
                    || (currentStep === 1 && !hasImportResult)
                    || (currentStep === 2 && !hasImportedQuestions)
                  }
                >
                  {currentStep === 3 ? 'Hoàn tất & Xuất bản' : 'Tiếp tục'}
                </button>
              </div>
            </div>

            <div className="stepper-container">
              {steps.map((step, index) => (
                <React.Fragment key={step.num}>
                  <div
                    className={`step-item ${currentStep >= step.num ? 'active' : ''} ${!canGoToStep(step.num) ? 'disabled' : ''}`}
                    onClick={() => {
                      if (canGoToStep(step.num)) setCurrentStep(step.num);
                    }}
                    style={{ cursor: canGoToStep(step.num) ? 'pointer' : 'not-allowed' }}
                  >
                    <div className="step-circle">
                      {currentStep > step.num ? '✓' : step.num}
                    </div>
                    <span className="step-title">{step.title}</span>
                  </div>
                  {index < steps.length - 1 && (
                    <div className={`step-line ${currentStep > step.num ? 'active' : ''}`}></div>
                  )}
                </React.Fragment>
              ))}
            </div>
          </header>

          <div className="step-content-area">
            {currentStep === 1 && (
              <StepUpload
                courseId={courseId}
                onImportSuccess={handleImportSuccess}
                onManualStart={handleManualStart}
              />
            )}
            {currentStep === 2 && (
              <StepReview
                importResult={importResult}
                questions={questions}
                setQuestions={setQuestions}
                onBack={handleBackToUpload}
                onAddQuestion={handleAddManualQuestion}
                onDeleteQuestion={handleDeleteQuestion}
                onReuseQuestions={() => setReuseModalOpen(true)}
                onNext={() => setCurrentStep(3)}
              />
            )}
            {currentStep === 3 && (
              <StepSettings
                settings={settings}
                lessonOptions={lessonOptions}
                onSettingsChange={setSettings}
                onSaveDraft={() => saveQuiz({ publish: false })}
                onPublish={() => saveQuiz({ publish: true })}
                onCancel={() => navigate(`/instructor/exercises/${courseId}`)}
                isSaving={isSaving}
              />
            )}
          </div>

          {validationDialog && (
            <div className="quiz-validation-dialog-backdrop" role="dialog" aria-modal="true">
              <div className="quiz-validation-dialog">
                <div className="quiz-validation-dialog__icon">
                  <span className="material-symbols-outlined">error</span>
                </div>
                <h2>{validationDialog.title}</h2>
                <p>{validationDialog.message}</p>
                <button
                  type="button"
                  className="quiz-validation-dialog__button"
                  onClick={() => setValidationDialog(null)}
                >
                  Đã hiểu
                </button>
              </div>
            </div>
          )}
          <ReuseQuizQuestionModal
            open={reuseModalOpen}
            courseId={courseId}
            existingQuestionIds={questions.map(question => question.questionId)}
            onClose={() => setReuseModalOpen(false)}
            onConfirm={handleReuseQuestions}
          />
        </div>
      </LockedFeature>
    </AnimatedPage>
  );
}
