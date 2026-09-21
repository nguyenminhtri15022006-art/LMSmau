import React, {useEffect, useMemo, useState} from 'react';
import {Navigate, useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {assignmentApi} from '../../../../../services/assignment.api';
import {courseApi} from '../../../../../services/course.api';
import {mapProblemToChallenge, mapQuizToExerciseHubItem} from '../../../../../utils/assignmentMappers';
import {buildAppErrorState} from '../../../../../utils/appError';
import {findCourseByRouteSlug} from '../../../../../utils/courseSlug';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import FilterBar from '../../components/student/ExerciseListing/FilterBar';
import ChallengeCard from '../../components/student/ExerciseListing/ChallengeCard';
import QuizItem from '../../../Exercise/components/student/ExerciseHub/QuizItem';

import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/ExerciseListing/ExerciseListingPage.css';

const PAGE_SIZE = 10;
const FETCH_PAGE_SIZE = 100;
const VALID_TABS = new Set(['coding', 'quizzes']);

function normalizeTab(tab) {
    if (tab === 'quiz') return 'quizzes';
    return VALID_TABS.has(tab) ? tab : 'coding';
}

function normalizePage(page) {
    const parsed = Number.parseInt(page, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function toContentList(data) {
    return data?.content || (Array.isArray(data) ? data : []);
}

async function fetchAllPages(fetchPage) {
    const firstPage = await fetchPage(0);
    const firstItems = toContentList(firstPage);
    const totalPages = Number(firstPage?.totalPages ?? 1);

    if (!Number.isFinite(totalPages) || totalPages <= 1) {
        return firstItems;
    }

    const restPages = await Promise.all(
        Array.from({length: totalPages - 1}, (_, index) =>
            fetchPage(index + 1).catch(() => ({content: []}))
        )
    );

    return restPages.reduce(
        (items, page) => items.concat(toContentList(page)),
        firstItems
    );
}

function getCountForTab(lesson, tab) {
    return tab === 'coding' ? lesson.codingCount : lesson.quizCount;
}

function matchesStatus(item, activeStatus) {
    if (activeStatus === 'Completed') return item.completed === true;
    if (activeStatus === 'Incomplete') return item.completed !== true;
    return true;
}

function buildPageNumbers(currentPage, totalPages) {
    const safeTotal = Math.max(1, totalPages);
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(safeTotal, start + 4);
    const adjustedStart = Math.max(1, end - 4);

    return Array.from({length: end - adjustedStart + 1}, (_, index) => adjustedStart + index);
}

export default function ExerciseListingPage() {
    const {slug} = useParams();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState(() => normalizeTab(searchParams.get('tab') || 'coding'));
    const [selectedLessonId, setSelectedLessonId] = useState(() => searchParams.get('lesson') || '');
    const [currentPage, setCurrentPage] = useState(() => normalizePage(searchParams.get('page')));
    const [activeLevel, setActiveLevel] = useState('All');
    const [activeStatus, setActiveStatus] = useState('All');
    const [lessonSearch, setLessonSearch] = useState('');
    const [exerciseSearch, setExerciseSearch] = useState('');

    const [courseId, setCourseId] = useState('');
    const [courseTitle, setCourseTitle] = useState('');
    const [courseDescription, setCourseDescription] = useState('');
    const [lessonNodes, setLessonNodes] = useState([]);
    const [lessonCounts, setLessonCounts] = useState([]);
    const [allProblems, setAllProblems] = useState([]);
    const [allQuizzes, setAllQuizzes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [listLoading, setListLoading] = useState(false);
    const [lessonsReady, setLessonsReady] = useState(false);
    const [error, setError] = useState(null);

    const updateRoute = (tab, lessonId, page) => {
        const params = new URLSearchParams();
        params.set('tab', tab);
        if (lessonId) params.set('lesson', lessonId);
        params.set('page', String(page));
        navigate(`/exercises/code/${slug}?${params.toString()}`, {replace: true});
    };

    useEffect(() => {
        if (!slug) return;
        let cancelled = false;

        const fetchCourseContext = async () => {
            try {
                setLoading(true);
                setLessonsReady(false);

                const enrolled = await courseApi.getEnrolled();
                if (cancelled) return;

                const course = findCourseByRouteSlug(enrolled, slug);
                if (!course) {
                    setError(buildAppErrorState(null, {
                        title: 'Không tìm thấy khoá học',
                        message: 'Khoá học này không tồn tại hoặc bạn chưa được ghi danh.',
                        variant: 'not-found',
                        icon: 'travel_explore',
                        fallbackPath: '/exercises',
                    }));
                    return;
                }

                const resolvedCourseId = course.id;
                const [lessonNodesData, lessonCountsData] = await Promise.all([
                    courseApi.getLessonNodes(resolvedCourseId).catch(() => []),
                    assignmentApi.getLessonCounts(resolvedCourseId).catch(() => []),
                ]);
                if (cancelled) return;

                setCourseId(resolvedCourseId);
                setCourseTitle(course.title || '');
                setCourseDescription(course.description || '');
                setLessonNodes(Array.isArray(lessonNodesData) ? lessonNodesData : []);
                setLessonCounts(Array.isArray(lessonCountsData) ? lessonCountsData : []);
                setLessonsReady(true);
            } catch (err) {
                if (!cancelled) {
                    setError(buildAppErrorState(err, {
                        title: 'Không thể tải danh sách bài tập',
                        fallbackPath: '/exercises',
                    }));
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchCourseContext();
        return () => {
            cancelled = true;
        };
    }, [slug]);

    const countByLessonId = useMemo(() => {
        const map = new Map();
        (Array.isArray(lessonCounts) ? lessonCounts : []).forEach((count) => {
            if (!count?.lessonId) return;
            map.set(String(count.lessonId), {
                codingCount: Number(count.codingCount ?? 0),
                quizCount: Number(count.quizCount ?? 0),
            });
        });
        return map;
    }, [lessonCounts]);

    const lessonOptions = useMemo(() => {
        return (Array.isArray(lessonNodes) ? lessonNodes : [])
            .filter(node => node?.lessonId)
            .map((node, index) => {
                const lessonId = String(node.lessonId);
                const legacyLessonId = String(index + 1);
                const aliases = Array.from(new Set([lessonId, legacyLessonId]));
                const assignmentLessonIds = aliases.map(aliasLessonId => {
                    const counts = countByLessonId.get(aliasLessonId) || {};
                    return {
                        lessonId: aliasLessonId,
                        codingCount: counts.codingCount ?? 0,
                        quizCount: counts.quizCount ?? 0,
                    };
                });
                return {
                    lessonId,
                    displayIndex: index + 1,
                    title: node.title || `Bài ${index + 1}`,
                    order: node.order ?? index,
                    assignmentLessonIds,
                    codingCount: assignmentLessonIds.reduce((total, alias) => total + alias.codingCount, 0),
                    quizCount: assignmentLessonIds.reduce((total, alias) => total + alias.quizCount, 0),
                };
            })
            .sort((a, b) => a.order - b.order);
    }, [lessonNodes, countByLessonId]);

    const totalCodingCount = useMemo(
        () => lessonOptions.reduce((total, lesson) => total + lesson.codingCount, 0),
        [lessonOptions]
    );
    const totalQuizCount = useMemo(
        () => lessonOptions.reduce((total, lesson) => total + lesson.quizCount, 0),
        [lessonOptions]
    );

    const selectedLesson = useMemo(
        () => lessonOptions.find(lesson => lesson.lessonId === selectedLessonId) || null,
        [lessonOptions, selectedLessonId]
    );

    useEffect(() => {
        if (!lessonsReady || lessonOptions.length === 0) return;

        const currentLessonExists = lessonOptions.some(lesson => lesson.lessonId === selectedLessonId);
        if (currentLessonExists) return;

        const lessonWithExercises = lessonOptions.find(lesson => getCountForTab(lesson, activeTab) > 0);
        const fallbackLesson = lessonWithExercises || lessonOptions[0];
        setSelectedLessonId(fallbackLesson.lessonId);
        setCurrentPage(1);
        updateRoute(activeTab, fallbackLesson.lessonId, 1);
    }, [lessonsReady, lessonOptions, selectedLessonId, activeTab]);

    useEffect(() => {
        if (!courseId || !lessonsReady || !selectedLesson) return;

        let cancelled = false;
        const fetchLessonAssignments = async () => {
            try {
                setListLoading(true);
                const hasCountData = Array.isArray(lessonCounts) && lessonCounts.length > 0;
                const aliases = selectedLesson.assignmentLessonIds?.map(alias => alias.lessonId).filter(Boolean) || [];
                const preferredAlias = selectedLesson.assignmentLessonIds?.find(alias => getCountForTab(alias, activeTab) > 0)?.lessonId
                    || aliases[0]
                    || selectedLesson.lessonId;
                const orderedAliases = [
                    preferredAlias,
                    ...aliases.filter(alias => alias !== preferredAlias),
                ].filter(Boolean);

                let loadedItems = [];
                for (const lessonId of orderedAliases) {
                    const items = await fetchAllPages((page) => {
                        const params = {
                            page,
                            size: FETCH_PAGE_SIZE,
                            lessonId,
                        };

                        if (activeTab === 'coding') {
                            params.courseId = courseId;
                            if (activeLevel !== 'All') params.difficulty = activeLevel;
                            return assignmentApi.getProblems(params);
                        }

                        return assignmentApi.getQuizzesForStudent(courseId, params);
                    }).catch(() => []);

                    loadedItems = items.filter(item => item?.lessonId != null && String(item.lessonId) === String(lessonId));
                    if (loadedItems.length > 0 || (hasCountData && getCountForTab(selectedLesson, activeTab) === 0)) {
                        break;
                    }
                }

                if (cancelled) return;
                if (activeTab === 'coding') {
                    setAllProblems(loadedItems);
                } else {
                    setAllQuizzes(loadedItems);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(buildAppErrorState(err, {
                        title: 'Không thể tải danh sách bài tập',
                        fallbackPath: '/exercises',
                    }));
                }
            } finally {
                if (!cancelled) setListLoading(false);
            }
        };

        fetchLessonAssignments();
        return () => {
            cancelled = true;
        };
    }, [activeLevel, activeTab, courseId, lessonCounts, lessonsReady, selectedLesson]);

    const filteredLessons = useMemo(() => {
        const keyword = lessonSearch.trim().toLowerCase();
        if (!keyword) return lessonOptions;
        return lessonOptions.filter(lesson => lesson.title.toLowerCase().includes(keyword));
    }, [lessonOptions, lessonSearch]);

    const filteredAssignmentItems = useMemo(() => {
        const aliases = selectedLesson?.assignmentLessonIds?.map(alias => alias.lessonId) || [];
        const aliasSet = new Set(aliases.map(String));
        const keyword = exerciseSearch.trim().toLowerCase();
        const sourceItems = activeTab === 'coding' ? allProblems : allQuizzes;

        return sourceItems
            .filter(item => {
                if (aliasSet.size > 0 && (item?.lessonId == null || !aliasSet.has(String(item.lessonId)))) {
                    return false;
                }
                if (activeTab === 'coding' && activeLevel !== 'All' && item?.difficulty !== activeLevel) {
                    return false;
                }
                return true;
            })
            .map(item => activeTab === 'coding'
                ? mapProblemToChallenge(item)
                : mapQuizToExerciseHubItem(item)
            )
            .filter(item => {
                const matchesKeyword = !keyword || item.title?.toLowerCase().includes(keyword);
                return matchesKeyword && matchesStatus(item, activeStatus);
            });
    }, [activeLevel, activeStatus, activeTab, allProblems, allQuizzes, exerciseSearch, selectedLesson]);

    const pageMeta = useMemo(() => ({
        totalElements: filteredAssignmentItems.length,
        totalPages: Math.max(1, Math.ceil(filteredAssignmentItems.length / PAGE_SIZE)),
    }), [filteredAssignmentItems]);

    const visibleItems = useMemo(() => {
        const start = (currentPage - 1) * PAGE_SIZE;
        return filteredAssignmentItems.slice(start, start + PAGE_SIZE);
    }, [currentPage, filteredAssignmentItems]);

    useEffect(() => {
        if (currentPage <= pageMeta.totalPages) return;
        setCurrentPage(pageMeta.totalPages);
        updateRoute(activeTab, selectedLessonId, pageMeta.totalPages);
    }, [activeTab, currentPage, pageMeta.totalPages, selectedLessonId]);

    const handleTabChange = (tab) => {
        setActiveTab(tab);
        setActiveLevel('All');
        setActiveStatus('All');
        setExerciseSearch('');

        const currentLesson = lessonOptions.find(lesson => lesson.lessonId === selectedLessonId);
        const nextLesson = currentLesson || lessonOptions.find(lesson => getCountForTab(lesson, tab) > 0) || lessonOptions[0];
        const nextLessonId = nextLesson?.lessonId || '';

        setSelectedLessonId(nextLessonId);
        setCurrentPage(1);
        updateRoute(tab, nextLessonId, 1);
    };

    const handleLessonChange = (lessonId) => {
        setSelectedLessonId(lessonId);
        setCurrentPage(1);
        setExerciseSearch('');
        updateRoute(activeTab, lessonId, 1);
    };

    const handlePageChange = (page) => {
        const nextPage = Math.min(Math.max(page, 1), pageMeta.totalPages || 1);
        setCurrentPage(nextPage);
        updateRoute(activeTab, selectedLessonId, nextPage);
    };

    const handleLevelChange = (level) => {
        setActiveLevel(level);
        setCurrentPage(1);
        updateRoute(activeTab, selectedLessonId, 1);
    };

    const handleStatusChange = (status) => {
        setActiveStatus(status);
        setCurrentPage(1);
        updateRoute(activeTab, selectedLessonId, 1);
    };

    if (loading) {
        return (
            <AnimatedPage>
                <div className="exercise-page exercise-loading">
                    <p>Đang tải bài tập...</p>
                </div>
            </AnimatedPage>
        );
    }

    if (error) {
        return <Navigate to="/error" replace state={error}/>;
    }

    const activeCount = pageMeta.totalElements;
    const pageNumbers = buildPageNumbers(currentPage, pageMeta.totalPages);

    return (
        <AnimatedPage>
            <div className="exercise-page exercise-lesson-page">
                <ExerciseBreadcrumb items={[
                    {label: courseTitle || 'Khoá học'}
                ]}/>

                <header className="listing-header">
                    <div className="listing-heading-row">
                        <div>
                            <h1 className="listing-title">{courseTitle || 'Bài tập khoá học'}</h1>
                            <p className="listing-subtitle">
                                {courseDescription || 'Luyện tập với các bài tập lập trình và trắc nghiệm'}
                            </p>
                        </div>
                    </div>

                    <div className="exercise-tabs">
                        <button
                            className={`exercise-tab ${activeTab === 'coding' ? 'active' : ''}`}
                            onClick={() => handleTabChange('coding')}
                        >
                            Bài tập lập trình
                            <span className="tab-badge">{String(totalCodingCount).padStart(2, '0')}</span>
                        </button>
                        <button
                            className={`exercise-tab ${activeTab === 'quizzes' ? 'active' : ''}`}
                            onClick={() => handleTabChange('quizzes')}
                        >
                            Bài tập trắc nghiệm
                            <span className="tab-badge">{String(totalQuizCount).padStart(2, '0')}</span>
                        </button>
                    </div>
                </header>

                <FilterBar
                    activeLevel={activeLevel}
                    onLevelChange={handleLevelChange}
                    activeStatus={activeStatus}
                    onStatusChange={handleStatusChange}
                    slug={slug}
                    showLevels={activeTab === 'coding'}
                />

                <div className="lesson-workspace">
                    <aside className="lesson-sidebar" aria-label="Danh sách bài học">
                        <div className="lesson-sidebar__header">
                            <span className="material-symbols-outlined">menu_book</span>
                            <div>
                                <h2>Bài học</h2>
                                <p>{lessonOptions.length} bài</p>
                            </div>
                        </div>
                        <label className="lesson-search">
                            <span className="material-symbols-outlined">search</span>
                            <input
                                value={lessonSearch}
                                onChange={(event) => setLessonSearch(event.target.value)}
                                placeholder="Tìm bài học"
                            />
                        </label>

                        <div className="lesson-list">
                            {filteredLessons.map((lesson) => {
                                const count = getCountForTab(lesson, activeTab);
                                const selected = lesson.lessonId === selectedLessonId;
                                return (
                                    <button
                                        key={lesson.lessonId}
                                        type="button"
                                        className={`lesson-nav-item ${selected ? 'active' : ''}`}
                                        onClick={() => handleLessonChange(lesson.lessonId)}
                                    >
                                        <span className="lesson-nav-item__index">{lesson.displayIndex}</span>
                                        <span className="lesson-nav-item__body">
                                            <span className="lesson-nav-item__title">{lesson.title}</span>
                                            <span className="lesson-nav-item__meta">{count} bài tập</span>
                                        </span>
                                    </button>
                                );
                            })}
                            {filteredLessons.length === 0 && (
                                <div className="lesson-empty">Không tìm thấy bài học.</div>
                            )}
                        </div>
                    </aside>

                    <main className="lesson-assignment-panel">
                        <div className="lesson-panel-header">
                            <div>
                                <span className="lesson-panel-kicker">
                                    {activeTab === 'coding' ? 'Coding' : 'Quiz'}
                                </span>
                                <h2>{selectedLesson?.title || 'Tất cả bài tập'}</h2>
                                <p>{activeCount} bài tập trong bài học này</p>
                            </div>
                            <label className="exercise-search">
                                <span className="material-symbols-outlined">search</span>
                                <input
                                    value={exerciseSearch}
                                    onChange={(event) => setExerciseSearch(event.target.value)}
                                    placeholder="Tìm bài tập"
                                />
                            </label>
                        </div>

                        {listLoading ? (
                            <div className="assignment-loading">Đang tải danh sách...</div>
                        ) : (
                            <>
                                <div className={activeTab === 'coding' ? 'lesson-assignment-list coding' : 'lesson-assignment-list quiz'}>
                                    {visibleItems.map(item => (
                                        activeTab === 'coding' ? (
                                            <ChallengeCard
                                                key={item.id}
                                                challenge={item}
                                                courseTitle={courseTitle}
                                                courseSlug={slug}
                                            />
                                        ) : (
                                            <QuizItem
                                                key={item.id}
                                                quiz={item}
                                                courseTitle={courseTitle}
                                                courseSlug={slug}
                                            />
                                        )
                                    ))}
                                </div>

                                {visibleItems.length === 0 && (
                                    <div className="empty-state">
                                        Chưa có bài tập phù hợp trong bài học này.
                                    </div>
                                )}
                            </>
                        )}

                        {pageMeta.totalPages > 1 && (
                            <nav className="lesson-pagination" aria-label="Phân trang bài tập">
                                <button
                                    type="button"
                                    onClick={() => handlePageChange(currentPage - 1)}
                                    disabled={currentPage <= 1}
                                >
                                    <span className="material-symbols-outlined">chevron_left</span>
                                    Trước
                                </button>
                                {pageNumbers.map(page => (
                                    <button
                                        key={page}
                                        type="button"
                                        className={page === currentPage ? 'active' : ''}
                                        onClick={() => handlePageChange(page)}
                                    >
                                        {page}
                                    </button>
                                ))}
                                <button
                                    type="button"
                                    onClick={() => handlePageChange(currentPage + 1)}
                                    disabled={currentPage >= pageMeta.totalPages}
                                >
                                    Sau
                                    <span className="material-symbols-outlined">chevron_right</span>
                                </button>
                            </nav>
                        )}
                    </main>
                </div>
            </div>
        </AnimatedPage>
    );
}
