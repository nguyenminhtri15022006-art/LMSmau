import React, { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import courseApi from '../../../../../services/course.api';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../../components/ui/LockedFeature';
import useFileUpload from '../../../../../hooks/useFileUpload';
import SidebarQuestionList from '../../components/teacher/EditQuiz/SidebarQuestionList';
import QuizSettingsForm from '../../components/teacher/EditQuiz/QuizSettingsForm';
import QuestionEditor from '../../components/teacher/EditQuiz/QuestionEditor';
import ManualQuizHeader from '../../components/teacher/EditQuiz/ManualQuizHeader';
import ManualQuizFooter from '../../components/teacher/EditQuiz/ManualQuizFooter';
import ReuseQuizQuestionModal from '../../components/teacher/ReuseQuizQuestionModal';
import AssignmentMessageDialog from '../../../shared/components/AssignmentMessageDialog';
import {
  revokePendingQuestionImage,
  stripPendingQuestionImage,
  uploadPendingQuestionImages,
} from '../../utils/questionImageUpload';
import { distributeQuizQuestionScores } from '../../utils/quizScoring';
import '../../styles/teacher/ManualQuiz/manualQuizPage.css';

function normalizeOptionsForType(type, options) {
  const current = options?.length ? options : [
    { text: '', isCorrect: true },
    { text: '', isCorrect: false },
  ];

  if (type === 'TRUE_FALSE') {
    const correctIndex = current.findIndex(option => option.isCorrect);
    return [
      { ...current[0], text: current[0]?.text || 'Đúng', isCorrect: correctIndex <= 0 },
      { ...current[1], text: current[1]?.text || 'Sai', isCorrect: correctIndex > 0 },
    ];
  }

  if (type === 'SINGLE') {
    const correctIndex = Math.max(0, current.findIndex(option => option.isCorrect));
    return current.map((option, index) => ({
      ...option,
      isCorrect: index === correctIndex,
    }));
  }

  const correctCount = current.filter(option => option.isCorrect).length;
  return current.map((option, index) => ({
    ...option,
    isCorrect: correctCount >= 2 ? option.isCorrect : index < 2,
  }));
}

function createBlankQuestion(index = 0) {
  return {
    id: `new-${Date.now()}-${index}`,
    questionId: null,
    index,
    text: '',
    type: 'SINGLE',
    score: 10,
    topic: '',
    explanation: '',
    imageUrl: '',
    options: [
      { text: '', isCorrect: true },
      { text: '', isCorrect: false },
    ],
  };
}

function getResponseItems(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.items)) return response.items;
  if (Array.isArray(response?.questions)) return response.questions;
  return [];
}

function mapQuestionToEditor(question, index) {
  const questionId = question.questionId ?? question.id;
  return {
    id: questionId ?? `loaded-${index}`,
    questionId,
    index,
    text: question.content ?? '',
    type: question.type ?? 'SINGLE',
    score: question.score ?? 10,
    topic: question.topic ?? '',
    explanation: question.explanation ?? '',
    imageUrl: question.imageUrl ?? '',
    options: normalizeOptionsForType(question.type ?? 'SINGLE', getResponseItems(question.answers).map(answer => ({
      answerId: answer.answerId,
      text: answer.content ?? '',
      isCorrect: !!answer.correct,
      orderIndex: answer.orderIndex,
    }))),
  };
}

function mapReusableQuestionToEditor(question, index, sourceQuiz) {
  return {
    id: `reuse-${question.questionId}-${Date.now()}-${index}`,
    questionId: question.questionId,
    reuseExisting: true,
    bankLocked: true,
    sourceQuizTitle: question.sourceQuiz?.title || sourceQuiz?.title || '',
    index,
    text: question.content ?? '',
    type: question.type ?? 'SINGLE',
    score: question.score ?? 10,
    topic: question.topic ?? '',
    explanation: question.explanation ?? '',
    imageUrl: question.imageUrl ?? '',
    options: normalizeOptionsForType(question.type ?? 'SINGLE', (question.answers ?? []).map(answer => ({
      answerId: answer.answerId,
      text: answer.content ?? '',
      isCorrect: !!answer.correct,
      orderIndex: answer.orderIndex,
    }))),
  };
}

function toQuestionPayload(question) {
  const payload = {
    content: question.text.trim(),
    type: question.type ?? 'SINGLE',
    topic: question.topic || null,
    explanation: question.explanation || null,
    imageUrl: question.imageUrl || null,
    score: Number(question.score) || 10,
    answers: question.options.filter(option => option.text.trim()).map((option, index) => ({
      ...(option.answerId ? { answerId: option.answerId } : {}),
      content: option.text.trim(),
      correct: !!option.isCorrect,
      orderIndex: index,
    })),
  };

  if (question.questionId && question.reuseExisting) {
    payload.questionId = question.questionId;
    payload.reuseExisting = true;
  }

  return payload;
}

function toQuestionDraftPayload(question) {
  const payload = toQuestionPayload(question);
  if (question.questionId) {
    payload.questionId = question.questionId;
  }
  return payload;
}

function getErrorDetail(err) {
  return err?.response?.data?.message || err?.message || 'Vui lòng thử lại sau.';
}

export default function EditQuizPage() {
  const { courseId, quizId } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!quizId;

  const pageTitle = isEditMode ? 'Chỉnh sửa bài kiểm tra' : 'Tạo câu hỏi mới';
  const [loading, setLoading] = useState(isEditMode);
  const [courseName, setCourseName] = useState('');
  const [questions, setQuestions] = useState([]);
  const [activeQuestionId, setActiveQuestionId] = useState(null);
  const [lessonOptions, setLessonOptions] = useState([]);
  const [selectedLessonId, setSelectedLessonId] = useState('');
  const [quizInfo, setQuizInfo] = useState({
    title: '',
    description: '',
    published: false,
  });
  const [settings, setSettings] = useState({
    timeLimit: 30,
    passingScore: 70,
    totalScore: 100,
    maxAttempts: 0,
    shuffleQuestions: true,
    shuffleAnswers: true,
    showResult: 'AFTER_SUBMIT',
  });
  const [isSaving, setIsSaving] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [activeTab, setActiveTab] = useState('questions');
  const [messageDialog, setMessageDialog] = useState(null);
  const [reuseModalOpen, setReuseModalOpen] = useState(false);
  const questionRefs = useRef({});
  const questionsRef = useRef([]);
  const { upload } = useFileUpload();

  const showMessageDialog = (dialog) => {
    setMessageDialog({ tone: 'error', ...dialog });
  };

  const buildQuizPayload = () => ({
    title: quizInfo.title.trim(),
    description: quizInfo.description.trim() || null,
    lessonId: selectedLessonId || null,
    duration: settings.timeLimit === '' ? null : Number(settings.timeLimit),
    totalScore: Number(settings.totalScore) || 100,
    passScore: Number(settings.passingScore) || 50,
    maxAttempts: Number(settings.maxAttempts) || 0,
    shuffleQuestions: !!settings.shuffleQuestions,
    shuffleAnswers: !!settings.shuffleAnswers,
    showResult: settings.showResult || 'AFTER_SUBMIT',
  });

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
        const name = course?.title || course?.name || course?.courseTitle || course?.courseName || '';
        if (name) setCourseName(name);
      })
      .catch(err => console.warn('Failed to load course details:', err));

    courseApi.getLessonNodes(courseId)
      .then(nodes => {
        const options = (nodes ?? [])
          .filter(node => node?.lessonId)
          .map(node => ({
            value: String(node.lessonId),
            label: node.title || `Lesson ${node.lessonId}`,
          }));
        setLessonOptions(options);
        if (!isEditMode && options.length > 0) {
          setSelectedLessonId(options[0].value);
        }
      })
      .catch(err => console.warn('Failed to load lesson nodes:', err));
  }, [courseId, isEditMode]);

  useEffect(() => {
    if (!isEditMode) {
      const initialQ = createBlankQuestion(0);
      setQuestions([initialQ]);
      setActiveQuestionId(initialQ.id);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setLoadError('');

    Promise.all([
      assignmentApi.getQuizDetail(quizId),
      assignmentApi.getQuizQuestions(quizId),
    ])
      .then(([quiz, quizQuestions]) => {
        if (cancelled) return;

        const formattedQuestions = getResponseItems(quizQuestions).map(mapQuestionToEditor);
        setQuizInfo({
          title: quiz.title ?? '',
          description: quiz.description ?? '',
          published: !!quiz.published,
        });
        setSelectedLessonId(quiz.lessonId ? String(quiz.lessonId) : '');
        setSettings({
          timeLimit: quiz.duration ?? '',
          passingScore: quiz.passScore ?? 50,
          totalScore: quiz.totalScore ?? 100,
          maxAttempts: quiz.maxAttempts ?? 0,
          shuffleQuestions: quiz.shuffleQuestions ?? true,
          shuffleAnswers: quiz.shuffleAnswers ?? true,
          showResult: quiz.showResult ?? 'AFTER_SUBMIT',
        });
        setQuestions(formattedQuestions);
        setActiveQuestionId(formattedQuestions[0]?.id ?? null);
      })
      .catch(err => {
        if (!cancelled) {
          setLoadError(err.response?.data?.message || 'Không thể tải dữ liệu quiz.');
        }
        console.error('Failed to load quiz:', err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, isEditMode, quizId]);

  const handleUpdateQuestion = (updatedQ) => {
    setQuestions(prev => prev.map((q, index) => (
      q.id === updatedQ.id ? { ...updatedQ, index } : { ...q, index }
    )));
  };

  const handleSelectQuestion = (questionId) => {
    setActiveQuestionId(questionId);
    setActiveTab('questions');
    window.requestAnimationFrame(() => {
      questionRefs.current[questionId]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  const handleQuestionTypeChange = (question, type) => {
    handleUpdateQuestion({
      ...question,
      type,
      options: normalizeOptionsForType(type, question.options),
    });
  };

  const handleAddQuestion = () => {
    const newQuestion = createBlankQuestion(questions.length);
    setQuestions(prev => [...prev, newQuestion]);
    setActiveQuestionId(newQuestion.id);
    setActiveTab('questions');
    window.setTimeout(() => {
      questionRefs.current[newQuestion.id]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const handleReuseQuestions = (selectedQuestions, sourceQuiz) => {
    setQuestions(prev => {
      const existingIds = new Set(prev.map(question => question.questionId).filter(Boolean));
      const reusableQuestions = selectedQuestions
        .filter(question => !existingIds.has(question.questionId))
        .map((question, index) => mapReusableQuestionToEditor(question, prev.length + index, question.sourceQuiz ?? sourceQuiz));

      if (reusableQuestions.length > 0) {
        setActiveQuestionId(reusableQuestions[0].id);
        setActiveTab('questions');
      }

      return [
        ...prev,
        ...reusableQuestions.map((question, offset) => ({ ...question, index: prev.length + offset })),
      ];
    });
    setReuseModalOpen(false);
  };

  const handleDeleteQuestion = (questionId) => {
    const currentQuestions = questionsRef.current;
    const deleteIndex = currentQuestions.findIndex(question => question.id === questionId);
    if (deleteIndex === -1) return;

    const targetQuestion = currentQuestions[deleteIndex];
    revokePendingQuestionImage(targetQuestion);

    const nextQuestions = currentQuestions
      .filter(question => question.id !== questionId)
      .map((question, index) => ({ ...question, index }));
    const nextActiveQuestion = nextQuestions[deleteIndex] ?? nextQuestions[deleteIndex - 1] ?? null;

    setQuestions(nextQuestions);
    setActiveQuestionId(nextActiveQuestion?.id ?? null);
    setActiveTab('questions');
  };

  const handleCloneQuestionForEditing = (question) => {
    handleUpdateQuestion({
      ...question,
      questionId: null,
      reuseExisting: false,
      bankLocked: false,
      sourceQuizTitle: '',
      options: question.options.map(option => ({
        text: option.text,
        isCorrect: option.isCorrect,
        orderIndex: option.orderIndex,
      })),
    });
  };

  const handleAddOptionForQuestion = (question) => {
    if (!question || question.type === 'TRUE_FALSE') return;
    handleUpdateQuestion({
      ...question,
      options: normalizeOptionsForType(question.type, [
        ...question.options,
        { text: '', isCorrect: false },
      ]),
    });
  };

  const handleDeleteOption = (questionId, index) => {
    const targetQuestion = questions.find(q => q.id === questionId);
    if (!targetQuestion || targetQuestion.type === 'TRUE_FALSE' || targetQuestion.options.length <= 2) return;
    handleUpdateQuestion({
      ...targetQuestion,
      options: normalizeOptionsForType(
        targetQuestion.type,
        targetQuestion.options.filter((_, i) => i !== index),
      ),
    });
  };

  const handleSettingChange = (key, value) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

  const validateQuiz = () => {
    if (!quizInfo.title.trim()) {
      showMessageDialog({
        title: 'Thiếu tên bài kiểm tra',
        message: 'Vui lòng nhập tên bài kiểm tra trước khi lưu.',
      });
      return false;
    }

    if (questions.length === 0) {
      showMessageDialog({
        title: 'Thiếu câu hỏi',
        message: 'Vui lòng thêm ít nhất một câu hỏi.',
      });
      return false;
    }

    const invalidQuestion = questions.find(question => {
      const validOptions = question.options.filter(option => option.text.trim());
      const correctCount = validOptions.filter(option => option.isCorrect).length;
      return !question.text.trim()
        || Number(question.score) < 1
        || validOptions.length < 2
        || (question.type === 'MULTIPLE' ? correctCount < 2 : correctCount !== 1)
        || (question.type === 'TRUE_FALSE' && validOptions.length !== 2);
    });

    if (invalidQuestion) {
      showMessageDialog({
        title: 'Câu hỏi chưa hợp lệ',
        message: 'Mỗi câu hỏi cần có nội dung, điểm số hợp lệ, ít nhất 2 đáp án và số đáp án đúng phù hợp với loại câu hỏi.',
      });
      handleSelectQuestion(invalidQuestion.id);
      return false;
    }

    return true;
  };

  const saveManualQuiz = async ({ publish }) => {
    let uploadedQuestionImageUrls = [];
    try {
      const uploadedImageResult = await uploadPendingQuestionImages(questions, upload, courseId, {
        onUploaded: (url) => uploadedQuestionImageUrls.push(url),
      });
      uploadedQuestionImageUrls = uploadedImageResult.uploadedUrls;
      const payload = {
        quiz: buildQuizPayload(),
        questions: uploadedImageResult.questions.map(isEditMode ? toQuestionDraftPayload : toQuestionPayload),
        publish,
      };

      const savedQuiz = !isEditMode
        ? await assignmentApi.createQuizFromImportedQuestions(courseId, payload)
        : await assignmentApi.syncQuiz(quizId, payload);

      uploadedQuestionImageUrls = [];
      return {
        savedQuiz,
        questions: uploadedImageResult.questions,
      };
    } catch (err) {
      if (uploadedQuestionImageUrls.length > 0) {
        await Promise.allSettled(uploadedQuestionImageUrls.map(url => assignmentApi.deleteUploadedFile(url)));
      }
      throw err;
    }
  };

  const navigateAfterSave = (savedQuiz, { publish }) => {
    const savedQuizId = savedQuiz?.quizId;
    const cloned = isEditMode && savedQuizId && String(savedQuizId) !== String(quizId);

    if (cloned || (!isEditMode && !publish)) {
      navigate(`/instructor/exercises/${courseId}/quiz/edit/${savedQuizId}`);
      return;
    }

    if (publish) {
      navigate(`/instructor/exercises/${courseId}`);
    }
  };

  const handleSave = async () => {
    if (!quizInfo.title.trim()) {
      showMessageDialog({
        title: 'Thiếu tên bài kiểm tra',
        message: 'Vui lòng nhập tên bài kiểm tra trước khi lưu.',
      });
      return;
    }

    if (questions.length === 0) {
      showMessageDialog({
        title: 'Thiếu câu hỏi',
        message: 'Vui lòng thêm ít nhất một câu hỏi.',
      });
      return;
    }

    setIsSaving(true);
    try {
      const { savedQuiz, questions: savedQuestions } = await saveManualQuiz({ publish: false });
      questions.forEach(revokePendingQuestionImage);
      setQuestions(savedQuestions.map(stripPendingQuestionImage));
      navigateAfterSave(savedQuiz, { publish: false });
    } catch (err) {
      showMessageDialog({
        title: 'Lỗi khi lưu',
        message: 'Không thể lưu bản nháp quiz.',
        detail: getErrorDetail(err),
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handlePublish = async () => {
    if (!validateQuiz()) return;

    setIsSaving(true);
    try {
      const { savedQuiz, questions: savedQuestions } = await saveManualQuiz({ publish: true });
      questions.forEach(revokePendingQuestionImage);
      setQuestions(savedQuestions.map(stripPendingQuestionImage));
      navigateAfterSave(savedQuiz, { publish: true });
    } catch (err) {
      showMessageDialog({
        title: 'Lỗi khi lưu quiz',
        message: 'Không thể xuất bản quiz.',
        detail: getErrorDetail(err),
      });
    } finally {
      setIsSaving(false);
    }
  };

  const hasLessons = lessonOptions.length > 0;
  const lessonLabel = hasLessons ? 'Gán cho chương' : 'Gán cho bài học';
  const lessonEmptyText = hasLessons ? 'Không gán chương' : 'Khác';

  return (
    <AnimatedPage>
      <LockedFeature featureName={isEditMode ? 'Edit Quiz' : 'Create Quiz'}>
        <div className="manual-quiz-page">
          <main className="quiz-main-content">
            <ManualQuizHeader
              courseId={courseId}
              courseTitle={courseName}
              quizTitle={quizInfo.title}
              isEditMode={isEditMode}
              pageTitle={pageTitle}
              onSave={handleSave}
              onPublish={handlePublish}
              saving={isSaving}
            />

            {loading ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '80px 0' }}>
                <div className="loading-screen__spinner" />
                <p style={{ color: 'var(--text-600)', marginTop: 16 }}>Đang tải dữ liệu quiz...</p>
              </div>
            ) : loadError ? (
              <div style={{ padding: '48px 0', color: 'var(--danger-600)' }}>
                {loadError}
              </div>
            ) : (
              <div className="quiz-editor-layout">
                <SidebarQuestionList
                  questions={questions}
                  activeQuestionId={activeQuestionId}
                  onSelectQuestion={handleSelectQuestion}
                  onAddQuestion={handleAddQuestion}
                  onReuseQuestions={() => setReuseModalOpen(true)}
                />

                <div className="quiz-editor-main">
                  <div className="quiz-tab-bar">
                    <button
                      className={`quiz-tab-btn ${activeTab === 'questions' ? 'active' : ''}`}
                      onClick={() => setActiveTab('questions')}
                    >
                      <span className="material-symbols-outlined">quiz</span>
                      Câu hỏi
                    </button>
                    <button
                      className={`quiz-tab-btn ${activeTab === 'settings' ? 'active' : ''}`}
                      onClick={() => setActiveTab('settings')}
                    >
                      <span className="material-symbols-outlined">settings</span>
                      Cài đặt
                    </button>
                  </div>

                  {activeTab === 'questions' && (
                    <>
                      {questions.length === 0 ? (
                        <div className="manual-quiz-empty-state">
                          <span className="material-symbols-outlined">quiz</span>
                          <p>Chưa có câu hỏi trong quiz. Thêm câu hỏi thủ công hoặc tái sử dụng từ quiz cũ để tiếp tục.</p>
                          <div>
                            <button className="btn-add-another" onClick={handleAddQuestion}>
                              <span className="material-symbols-outlined">library_add</span>
                              Thêm câu hỏi
                            </button>
                            <button className="btn-add-another" onClick={() => setReuseModalOpen(true)}>
                              <span className="material-symbols-outlined">content_copy</span>
                              Tái sử dụng
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="questions-editor-list">
                          {questions.map(question => (
                            <div
                              key={question.id}
                              ref={element => { questionRefs.current[question.id] = element; }}
                              className="question-editor-anchor"
                              onFocus={() => setActiveQuestionId(question.id)}
                            >
                              <QuestionEditor
                                question={question}
                                active={activeQuestionId === question.id}
                                onUpdate={handleUpdateQuestion}
                                onTypeChange={(type) => handleQuestionTypeChange(question, type)}
                                onAddOption={() => handleAddOptionForQuestion(question)}
                                onDeleteOption={(index) => handleDeleteOption(question.id, index)}
                                onDeleteQuestion={() => handleDeleteQuestion(question.id)}
                                onCloneForEditing={() => handleCloneQuestionForEditing(question)}
                              />
                            </div>
                          ))}
                        </div>
                      )}

                      <ManualQuizFooter
                        onAddQuestion={handleAddQuestion}
                        onReuseQuestions={() => setReuseModalOpen(true)}
                        onPublish={handlePublish}
                        saving={isSaving}
                      />
                    </>
                  )}

                  {activeTab === 'settings' && (
                    <div className="settings-tab-panel">
                      <div className="settings-section">
                        <div className="section-header">
                          <span className="material-symbols-outlined">info</span>
                          Thông tin cơ bản
                        </div>
                        <div className="settings-card">
                          <div className="form-group" style={{ marginBottom: 16 }}>
                            <label>Tên bài kiểm tra</label>
                            <input
                              className="form-input-full"
                              value={quizInfo.title}
                              onChange={(event) => {
                                setQuizInfo(prev => ({ ...prev, title: event.target.value }));
                              }}
                              placeholder="Nhập tên bài kiểm tra"
                            />
                          </div>
                          <div className="form-group" style={{ marginBottom: 16 }}>
                            <label>Mô tả</label>
                            <textarea
                              className="form-input-full"
                              value={quizInfo.description}
                              onChange={(event) => {
                                setQuizInfo(prev => ({ ...prev, description: event.target.value }));
                              }}
                              placeholder="Nhập mô tả ngắn cho quiz"
                            />
                          </div>
                          <div className="form-group">
                            <label>{lessonLabel}</label>
                            <div className="select-wrapper">
                              <select
                                value={selectedLessonId}
                                onChange={(e) => {
                                  setSelectedLessonId(e.target.value);
                                }}
                              >
                                <option value="">{lessonEmptyText}</option>
                                {lessonOptions.map(option => (
                                  <option key={option.value} value={option.value}>{option.label}</option>
                                ))}
                              </select>
                            </div>
                          </div>
                        </div>
                      </div>

                      <QuizSettingsForm settings={settings} onChange={handleSettingChange} />
                    </div>
                  )}
                </div>
              </div>
            )}
          </main>
        </div>
        <AssignmentMessageDialog
          open={!!messageDialog}
          title={messageDialog?.title}
          message={messageDialog?.message}
          detail={messageDialog?.detail}
          tone={messageDialog?.tone}
          onClose={() => setMessageDialog(null)}
        />
        <ReuseQuizQuestionModal
          open={reuseModalOpen}
          courseId={courseId}
          excludeQuizId={quizId}
          existingQuestionIds={questions.map(question => question.questionId)}
          onClose={() => setReuseModalOpen(false)}
          onConfirm={handleReuseQuestions}
        />
      </LockedFeature>
    </AnimatedPage>
  );
}
