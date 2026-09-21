import React, {useEffect, useRef, useState} from 'react';
import {Link, useLocation, useParams, useSearchParams} from 'react-router-dom';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import courseApi from '../../../../../services/course.api';
import assignmentApi from '../../../../../services/assignment.api';
import ExerciseTable from '../../components/teacher/InstructorExerciseDetail/ExerciseTable';
import ExerciseSidebar from '../../components/teacher/InstructorExerciseDetail/ExerciseSidebar';
import CreateExerciseModal from '../../components/teacher/InstructorExerciseDetail/CreateExerciseModal';
import AssignmentMessageDialog from '../../../shared/components/AssignmentMessageDialog';
import '../../../shared/styles/ExerciseShared.css';
import '../../styles/teacher/InstructorExerciseDetail/instructorExerciseDetail.css';

const ASSIGNMENT_FETCH_SIZE = 100;
const ASSIGNMENT_PAGE_SIZE = 5;
const DEFAULT_TAB = 'coding';
const VALID_TABS = new Set(['quizzes', 'coding']);
const TAB_BY_QUERY_TAB = {
    quiz: 'quizzes',
    quizzes: 'quizzes',
    coding: 'coding',
};
const QUERY_TAB_BY_TAB = {
    quizzes: 'quiz',
    coding: 'coding',
};

function normalizeTab(tab) {
    return VALID_TABS.has(tab) ? tab : (TAB_BY_QUERY_TAB[tab] ?? DEFAULT_TAB);
}

function normalizePage(page) {
    const parsedPage = Number(page);
    return Number.isInteger(parsedPage) && parsedPage > 0 ? parsedPage : 1;
}

function tabToQueryValue(tab) {
    return QUERY_TAB_BY_TAB[normalizeTab(tab)];
}

function assignmentTypeFromTab(tab) {
    return tab === 'coding' ? 'coding' : 'quiz';
}

const OTHER_CHAPTER = 'Khác';

function toContentList(data) {
    return data?.content ?? data ?? [];
}

function toSettledValue(result, fallback) {
    return result.status === 'fulfilled' ? result.value : fallback;
}

function getCourseName(course) {
    return course?.title || course?.name || course?.courseTitle || course?.courseName || '';
}

function buildLessonTitleMap(lessonNodes) {
    return new Map(
        (lessonNodes ?? [])
            .filter(node => node?.lessonId)
            .map(node => [String(node.lessonId), node.title || OTHER_CHAPTER])
    );
}

function resolveLessonGroup(lessonId, lessonTitleMap) {
    if (!lessonId) return {chapter: OTHER_CHAPTER, chapterKey: 'other'};

    const key = String(lessonId);
    const title = lessonTitleMap.get(key);
    return title
        ? {chapter: title, chapterKey: `lesson:${key}`}
        : {chapter: OTHER_CHAPTER, chapterKey: 'other'};
}

function mapDifficultyTone(tone) {
    const difficultyMap = {
        easy: {difficulty: 'DỄ', difficultyTone: 'easy'},
        medium: {difficulty: 'TRUNG BÌNH', difficultyTone: 'medium'},
        hard: {difficulty: 'KHÓ', difficultyTone: 'hard'},
    };

    return difficultyMap[tone] ?? {difficulty: null, difficultyTone: null};
}

function mapQuizDifficulty(questionCount = 0) {
    if (questionCount > 50) return mapDifficultyTone('hard');
    if (questionCount > 25) return mapDifficultyTone('medium');
    if (questionCount > 0) return mapDifficultyTone('easy');
    return {difficulty: null, difficultyTone: null};
}

function mapBackendDifficulty(difficulty) {
    const normalized = String(difficulty ?? '').trim().toUpperCase();
    const tone = {
        EASY: 'easy',
        BEGINNER: 'easy',
        DỄ: 'easy',
        MEDIUM: 'medium',
        INTERMEDIATE: 'medium',
        'TRUNG BÌNH': 'medium',
        HARD: 'hard',
        ADVANCED: 'hard',
        KHÓ: 'hard',
    }[normalized];

    return mapDifficultyTone(tone);
}

function mapQuizToRow(quiz, lessonTitleMap) {
    const group = resolveLessonGroup(quiz.lessonId, lessonTitleMap);
    const difficulty = mapQuizDifficulty(quiz.questionCount ?? 0);

    return {
        id: quiz.quizId,
        lessonId: quiz.lessonId,
        title: quiz.title,
        ...group,
        meta: `${quiz.questionCount ?? 0} câu hỏi — ${quiz.duration != null ? quiz.duration + ' phút' : 'Không giới hạn'}`,
        ...difficulty,
        doneCount: quiz.attemptCount ?? 0,
        status: quiz.published ? 'Published' : 'Draft',
    };
}

function mapProblemToRow(problem, lessonTitleMap) {
    const group = resolveLessonGroup(problem.lessonId, lessonTitleMap);
    const difficulty = mapBackendDifficulty(problem.difficulty);

    return {
        id: problem.problemId,
        slug: problem.slug,
        lessonId: problem.lessonId,
        title: problem.title,
        ...group,
        meta: problem.allowedLangs?.join(', ') ?? '',
        ...difficulty,
        doneCount: problem.totalSubmit ?? 0,
        status: problem.isPublic ? 'Published' : 'Draft',
    };
}

export default function InstructorExerciseDetail() {
    const {courseId} = useParams();
    const location = useLocation();
    const [searchParams, setSearchParams] = useSearchParams();
    const routeCourseName = location.state?.courseTitle || location.state?.courseName || '';
    const initialTab = normalizeTab(searchParams.get('tab'));
    const initialPage = normalizePage(searchParams.get('page'));

    const [activeTab, setActiveTab] = useState(initialTab);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    const [courseName, setCourseName] = useState(routeCourseName);
    const [quizzes, setQuizzes] = useState([]);
    const [problems, setProblems] = useState([]);
    const [pages, setPages] = useState({
        quizzes: initialTab === 'quizzes' ? initialPage : 1,
        coding: initialTab === 'coding' ? initialPage : 1,
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [sidebarAnalytics, setSidebarAnalytics] = useState(null);
    const [sidebarLoading, setSidebarLoading] = useState(false);
    const [sidebarError, setSidebarError] = useState(null);
    const [messageDialog, setMessageDialog] = useState(null);
    const exerciseContentTopRef = useRef(null);

    const syncExerciseQuery = (tab, page) => {
        const nextParams = new URLSearchParams(searchParams);
        nextParams.set('tab', tabToQueryValue(tab));
        nextParams.set('page', String(normalizePage(page)));
        setSearchParams(nextParams, {replace: true});
    };

    const fetchData = () => {
        if (!courseId) return;

        setLoading(true);
        setError(null);
        setCourseName(routeCourseName);

        Promise.allSettled([
            courseApi.getById(courseId).then(res => res?.data?.data ?? res?.data),
            courseApi.getLessonNodes(courseId).catch(err => {
                console.warn('Failed to fetch course lesson nodes:', err);
                return [];
            }),
            assignmentApi.getQuizzesForInstructor(courseId, {size: ASSIGNMENT_FETCH_SIZE}).then(toContentList),
            assignmentApi.getProblems({courseId, size: ASSIGNMENT_FETCH_SIZE}).then(toContentList),
        ])
            .then(([courseResult, lessonNodesResult, quizResult, problemResult]) => {
                if (quizResult.status === 'rejected' && problemResult.status === 'rejected') {
                    throw quizResult.reason || problemResult.reason;
                }

                const course = toSettledValue(courseResult, null);
                const lessonNodes = toSettledValue(lessonNodesResult, []);
                const quizData = toSettledValue(quizResult, []);
                const problemData = toSettledValue(problemResult, []);
                const lessonTitleMap = buildLessonTitleMap(lessonNodes);

                setCourseName(getCourseName(course) || routeCourseName);
                setQuizzes(quizData
                    .filter(quiz => !quiz.deleted)
                    .map(quiz => mapQuizToRow(quiz, lessonTitleMap)));
                setProblems(problemData
                    .filter(problem => !problem.deleted)
                    .map(problem => mapProblemToRow(problem, lessonTitleMap)));
                setPages(prev => ({
                    quizzes: prev.quizzes ?? 1,
                    coding: prev.coding ?? 1,
                }));
            })
            .catch(err => {
                setError('Không thể tải dữ liệu bài tập. Vui lòng thử lại sau.');
                console.error('Failed to fetch exercise data:', err);
            })
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchData();
    }, [courseId]);

    useEffect(() => {
        const nextTab = normalizeTab(searchParams.get('tab'));
        const nextPage = normalizePage(searchParams.get('page'));
        const isCanonicalQuery =
            searchParams.get('tab') === tabToQueryValue(nextTab) &&
            searchParams.get('page') === String(nextPage);

        if (!isCanonicalQuery) {
            const nextParams = new URLSearchParams(searchParams);
            nextParams.set('tab', tabToQueryValue(nextTab));
            nextParams.set('page', String(nextPage));
            setSearchParams(nextParams, {replace: true});
            return;
        }

        setActiveTab(nextTab);
        setPages(prev => (
            prev[nextTab] === nextPage
                ? prev
                : {...prev, [nextTab]: nextPage}
        ));
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        if (!courseId) return;

        let cancelled = false;
        const type = assignmentTypeFromTab(activeTab);

        setSidebarLoading(true);
        setSidebarError(null);
        setSidebarAnalytics(null);

        assignmentApi.getInstructorAnalytics(courseId, {type})
            .then(data => {
                if (!cancelled) {
                    setSidebarAnalytics(data);
                }
            })
            .catch(err => {
                if (!cancelled) {
                    setSidebarError('Không thể tải thống kê.');
                    console.error('Failed to fetch assignment analytics:', err);
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setSidebarLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [courseId, activeTab]);

    const currentList = activeTab === 'quizzes' ? quizzes : problems;
    const currentPage = pages[activeTab] ?? 1;
    const sidebarData = {courseName, analytics: sidebarAnalytics};
    const sidebarType = assignmentTypeFromTab(activeTab);

    const scrollToExerciseContentTop = () => {
        window.requestAnimationFrame(() => {
            const target = exerciseContentTopRef.current;
            if (target) {
                target.scrollIntoView({behavior: 'smooth', block: 'start'});
                return;
            }
            window.scrollTo({top: 0, behavior: 'smooth'});
        });
    };

    const handleTabChange = (tab) => {
        const nextTab = normalizeTab(tab);
        setActiveTab(nextTab);
        setPages(prev => ({...prev, [nextTab]: 1}));
        syncExerciseQuery(nextTab, 1);
    };

    const handlePageChange = (nextPage) => {
        const page = normalizePage(nextPage);
        const shouldScroll = page !== currentPage;
        setPages(prev => ({...prev, [activeTab]: page}));
        syncExerciseQuery(activeTab, page);
        if (shouldScroll) {
            scrollToExerciseContentTop();
        }
    };

    return (
        <AnimatedPage>
            <div className="exercise-detail-page">

                <nav className="ch-breadcrumb" style={{marginBottom: '32px'}}>
                    <Link to="/instructor/exercises">Bài tập</Link>
                    <span className="ch-breadcrumb-separator"
                          style={{margin: '0 8px', display: 'inline-flex', alignItems: 'center'}}>
            <span className="material-symbols-outlined" style={{fontSize: '16px'}}>chevron_right</span>
          </span>
                    <span className="ch-breadcrumb-current">{courseName || 'Khóa học'}</span>
                </nav>

                {loading && (
                    <div className="exercise-detail-loading">
                        <div className="loading-screen__spinner"/>
                        <p>Đang tải dữ liệu bài tập...</p>
                    </div>
                )}

                {!loading && error && (
                    <div style={{textAlign: 'center', padding: '80px 0'}}>
                        <span className="material-symbols-outlined"
                              style={{fontSize: 48, color: 'var(--text-400)'}}>error_outline</span>
                        <p style={{color: 'var(--text-600)', marginTop: 12}}>{error}</p>
                    </div>
                )}

                {!loading && !error && (
                    <>
                        <header className="exercise-detail-header">
                            <div className="header-left">
                                <h1>Bài tập cho khóa học: <span className="highlight-text">{courseName}</span></h1>
                            </div>
                        </header>

                        <div className="exercise-main-grid">
                            <div className="exercise-content-area" ref={exerciseContentTopRef}>
                                <div className="exercise-tabs">
                                    <button
                                        className={`exercise-tab ${activeTab === 'coding' ? 'active' : ''}`}
                                        onClick={() => handleTabChange('coding')}
                                    >
                                        Bài tập lập trình <span
                                        className="tab-badge">{String(problems.length).padStart(2, '0')}</span>
                                    </button>
                                    <button
                                        className={`exercise-tab ${activeTab === 'quizzes' ? 'active' : ''}`}
                                        onClick={() => handleTabChange('quizzes')}
                                    >
                                        Bài tập trắc nghiệm <span
                                        className="tab-badge">{String(quizzes.length).padStart(2, '0')}</span>
                                    </button>
                                </div>

                                <ExerciseTable
                                    currentList={currentList}
                                    activeTab={activeTab}
                                    courseName={courseName}
                                    page={currentPage}
                                    pageSize={ASSIGNMENT_PAGE_SIZE}
                                    onPageChange={handlePageChange}
                                    onItemDeleted={fetchData}
                                />
                            </div>

                            <div className="exercise-right-rail">
                                <button className="btn-primary create-exercise-button"
                                        onClick={() => setIsCreateModalOpen(true)}>
                                    <span className="material-symbols-outlined create-exercise-button__icon">add</span>
                                    <span className="create-exercise-button__content">
                    <span className="create-exercise-button__label">Tạo bài tập</span>
                    <span className="create-exercise-button__hint">Thêm quiz hoặc coding challenge</span>
                  </span>
                                </button>
                                <ExerciseSidebar
                                    data={sidebarData}
                                    type={sidebarType}
                                    loading={sidebarLoading}
                                    error={sidebarError}
                                    onAIGenerate={() => setIsCreateModalOpen(true)}
                                    onExport={() => setMessageDialog({
                                        title: 'Chưa thể xuất báo cáo',
                                        message: 'Tính năng xuất báo cáo bài tập chưa được cấu hình ở màn hình này.',
                                        tone: 'info',
                                    })}
                                    onBulkSettings={() => setMessageDialog({
                                        title: 'Chưa thể cấu hình hàng loạt',
                                        message: 'Tính năng cấu hình hàng loạt chưa được hỗ trợ cho danh sách bài tập hiện tại.',
                                        tone: 'info',
                                    })}
                                />
                            </div>
                        </div>
                    </>
                )}

                <CreateExerciseModal
                    isOpen={isCreateModalOpen}
                    onClose={() => setIsCreateModalOpen(false)}
                    courseId={courseId}
                    courseName={courseName}
                />
                <AssignmentMessageDialog
                    open={!!messageDialog}
                    title={messageDialog?.title}
                    message={messageDialog?.message}
                    detail={messageDialog?.detail}
                    tone={messageDialog?.tone}
                    onClose={() => setMessageDialog(null)}
                />
            </div>
        </AnimatedPage>
    );
}
