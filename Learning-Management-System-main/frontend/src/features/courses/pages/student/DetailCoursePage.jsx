import React, { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import AnimatedPage from "../../../../components/ui/AnimatedPage";
import CourseHero from "../../components/student/DetailCourse/CourseHero";
import CourseStickySidebar from "../../components/student/DetailCourse/CourseStickySidebar";
import LearningPoints from "../../components/student/DetailCourse/LearningPoints";
import ChaptersList from "../../components/student/CourseHub/ChaptersList";
import CourseInfo from "../../components/student/DetailCourse/CourseInfo";
import InstructorSection from "../../components/student/DetailCourse/InstructorSection";
import { courseApi } from "../../../../services/course.api";
import useAuth from "../../../../hooks/useAuth";
import { buildAppErrorState, getAppErrorRoute } from "../../../../utils/appError";
import { formatVN } from "../../../../utils/dateTime";
import "../../styles/student/DetailCourse/DetailCoursePage.css";

const LEVEL_MAP = {
    BEGINNER: "Cơ bản",
    INTERMEDIATE: "Trung cấp",
    ADVANCED: "Nâng cao",
};

const FALLBACK_THUMBNAIL = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=2070&auto=format&fit=crop";

function countLessonsInTree(items) {
    return items.reduce((sum, item) => {
        if (item.type === "lesson") return sum + 1;
        return sum + countLessonsInTree(item.items ?? []);
    }, 0);
}

function buildSections(nodes) {
    function buildTree(parentId = null) {
        return nodes
            .filter((node) => (node.parentId ?? null) === parentId)
            .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
            .map((node) => {
                if (node.type === "folder") {
                    const children = buildTree(node.id);
                    return {
                        id: node.id,
                        type: "folder",
                        title: node.title,
                        lectures: countLessonsInTree(children),
                        items: children,
                    };
                }
                return {
                    id: node.id,
                    type: "lesson",
                    lessonId: node.lessonId,
                    title: node.title,
                    lessonType: node.lessonType ?? null,
                };
            });
    }

    return buildTree(null);
}

export default function DetailCoursePage({ adminPreview = false }) {
    const { courseId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();

    const [course, setCourse] = useState(null);
    const [sections, setSections] = useState([]);
    const [studentsCount, setStudentsCount] = useState(0);
    const [lessonsCount, setLessonsCount] = useState(0);
    const [isEnrolled, setIsEnrolled] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [blockingError, setBlockingError] = useState(null);

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [courseId]);

    useEffect(() => {
        if (!courseId) {
            setBlockingError(buildAppErrorState(null, {
                title: "Không tìm thấy khóa học",
                message: "Liên kết khóa học không hợp lệ hoặc nội dung không còn tồn tại.",
                variant: "not-found",
                icon: "travel_explore",
                fallbackPath: adminPreview ? "/admin/all-courses" : "/list-course",
            }));
            setIsLoading(false);
            return;
        }

        const fetchAll = async () => {
            try {
                setIsLoading(true);
                setBlockingError(null);

                const [courseRes, students, lessons, structure, enrolledList] = await Promise.all([
                    courseApi.getById(courseId),
                    courseApi.getStudentsCount(courseId).catch(() => 0),
                    courseApi.getLessonsCount(courseId).catch(() => 0),
                    courseApi.getStructure(courseId).catch((err) => {
                        console.error("getStructure:", err?.response?.status, err?.response?.data);
                        return null;
                    }),
                    isAuthenticated && !adminPreview ? courseApi.getEnrolled().catch(() => []) : Promise.resolve([]),
                ]);

                const raw = courseRes?.data?.data ?? courseRes?.data;
                if (!raw) throw new Error("Không tìm thấy khóa học.");

                const enrolled = Array.isArray(enrolledList) ? enrolledList : [];
                const isEnrolledCheck = enrolled.some((item) => String(item.id) === String(courseId));

                setCourse(raw);
                setStudentsCount(students);
                setLessonsCount(lessons);
                setIsEnrolled(isEnrolledCheck);
                setSections(structure?.nodes?.length ? buildSections(structure.nodes) : []);
            } catch (err) {
                console.error("Failed to fetch course detail:", err);
                setBlockingError(buildAppErrorState(err, {
                    title: "Không thể tải thông tin khóa học",
                    fallbackPath: adminPreview ? "/admin/all-courses" : "/list-course",
                }));
            } finally {
                setIsLoading(false);
            }
        };

        fetchAll();
    }, [adminPreview, courseId, isAuthenticated]);

    if (blockingError) {
        return <Navigate to={getAppErrorRoute(location.pathname)} replace state={blockingError} />;
    }

    if (isLoading) {
        return (
            <AnimatedPage>
                <div className="detail-course-page">
                    <div className="detail-course-page__skeleton-hero" />
                    <div className="detail-course-page__container">
                        <div className="detail-course-page__grid">
                            <div className="detail-course-page__skeleton-sidebar" />
                            <div className="detail-course-page__main">
                                <div className="detail-course-page__skeleton-block" />
                                <div
                                    className="detail-course-page__skeleton-block detail-course-page__skeleton-block--tall"
                                />
                                <div className="detail-course-page__skeleton-block" />
                            </div>
                        </div>
                    </div>
                </div>
            </AnimatedPage>
        );
    }

    const courseData = {
        title: course.title,
        description: course.description ?? null,
        image: course.thumbnailUrl ?? FALLBACK_THUMBNAIL,
        students: studentsCount > 0 ? studentsCount.toLocaleString("vi-VN") : null,
        lessonsCount: lessonsCount > 0 ? lessonsCount : null,
        lastUpdated: course.updatedAt
            ? formatVN(course.updatedAt, { month: "2-digit", year: "numeric" })
            : null,
        level: LEVEL_MAP[course.level] ?? null,
        categories: course.categories ?? [],
        duration: course.duration ?? null,
        exerciseCount: course.exerciseCount ?? 0,
        sectionsCount: sections.length,
        instructorId: course.instructorId ?? null,
        learningPoints: course.learningPoints ?? [],
        requirements: course.requirements ?? [],
    };

    return (
        <AnimatedPage>
            <div className="detail-course-page">
                <CourseHero course={courseData} showBreadcrumb={!adminPreview} />

                <div className="detail-course-page__container">
                    <div className="detail-course-page__layout">
                        <div className="detail-course-page__main">
                            <LearningPoints points={courseData.learningPoints} />
                            <ChaptersList
                                chapters={sections}
                                totalLessons={countLessonsInTree(sections)}
                                onLessonClick={!adminPreview && isEnrolled
                                    ? (lesson) => navigate("/course-hub", {
                                        state: {
                                            courseId,
                                            lessonId: lesson.lessonId,
                                        },
                                    })
                                    : undefined}
                            />
                            <CourseInfo
                                description={course.description}
                                requirements={courseData.requirements}
                            />
                            <InstructorSection instructorId={courseData.instructorId} />
                        </div>

                        <CourseStickySidebar
                            course={courseData}
                            courseId={courseId}
                            isEnrolled={isEnrolled}
                            adminPreview={adminPreview}
                            backPath={adminPreview ? "/admin/all-courses" : "/list-course"}
                        />
                    </div>
                </div>
            </div>
        </AnimatedPage>
    );
}
