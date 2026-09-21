import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { courseApi } from "../../../../../services/course.api";
import useAuth from "../../../../../hooks/useAuth";
import "../../../styles/student/DetailCourse/CourseStickySidebar.css";

const FALLBACK_THUMBNAIL = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=2070&auto=format&fit=crop";

export default function CourseStickySidebar({ course, courseId, isEnrolled, adminPreview = false, backPath = "/list-course" }) {
    const navigate = useNavigate();
    const location = useLocation();
    const { isAuthenticated } = useAuth();
    const [enrolling, setEnrolling] = useState(false);
    const [enrollError, setEnrollError] = useState(null);
    const [thumbnailSrc, setThumbnailSrc] = useState(course?.image || FALLBACK_THUMBNAIL);

    useEffect(() => {
        setThumbnailSrc(course?.image || FALLBACK_THUMBNAIL);
    }, [course?.image]);

    const handleEnroll = async () => {
        if (!isAuthenticated) {
            navigate("/login", { state: { from: location } });
            return;
        }

        try {
            setEnrolling(true);
            setEnrollError(null);
            await courseApi.enroll(courseId);
            navigate(`/course-hub?id=${courseId}`);
        } catch (err) {
            const msg = err?.response?.data?.message || "Đăng ký thất bại. Vui lòng thử lại.";
            setEnrollError(msg);
        } finally {
            setEnrolling(false);
        }
    };

    const handleContinue = () => {
        navigate(`/course-hub?id=${courseId}`);
    };

    const overviewMetrics = [
        Number(course?.duration) > 0 && {
            icon: "calendar_month",
            label: "Thời lượng",
            value: `${course.duration} tháng`,
        },
        course?.lessonsCount != null && {
            icon: "menu_book",
            label: "Bài học",
            value: `${course.lessonsCount} bài`,
        },
        course?.exerciseCount > 0 && {
            icon: "assignment",
            label: "Bài tập",
            value: `${course.exerciseCount} bài`,
        },
        course?.instructorId && {
            icon: "person",
            label: "Giảng viên",
            value: "1 giảng viên",
        },
    ].filter(Boolean);

    const stats = [
        course?.sectionsCount > 0 && {
            icon: "folder_open",
            label: `${course.sectionsCount} chương`,
        },
        course?.students != null && {
            icon: "group",
            label: `${course.students} học viên`,
        },
    ].filter(Boolean);

    return (
        <aside
            className="course-sidebar-wrapper"
            style={{ "--course-sidebar-thumbnail": `url("${thumbnailSrc}")` }}
        >
            <div className="course-sidebar">
                <div className="course-sidebar__preview">
                    <img
                        className="course-sidebar__thumbnail"
                        src={thumbnailSrc}
                        alt={course?.title || "Course thumbnail"}
                        loading="lazy"
                        onError={(event) => {
                            if (event.currentTarget.src !== FALLBACK_THUMBNAIL) {
                                setThumbnailSrc(FALLBACK_THUMBNAIL);
                            }
                        }}
                    />
                </div>

                <div className="course-sidebar__content">
                    {overviewMetrics.length > 0 && (
                        <div className="course-sidebar__overview" aria-label="Tổng quan khóa học">
                            {overviewMetrics.map((item) => (
                                <div key={item.label} className="course-sidebar__metric">
                                    <span className="material-symbols-outlined course-sidebar__metric-icon">
                                        {item.icon}
                                    </span>
                                    <span className="course-sidebar__metric-copy">
                                        <span className="course-sidebar__metric-label">{item.label}</span>
                                        <strong className="course-sidebar__metric-value">{item.value}</strong>
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="course-sidebar__actions">
                        {!adminPreview && (isEnrolled ? (
                            <button
                                className="course-sidebar__btn course-sidebar__btn--primary"
                                onClick={handleContinue}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: "1.1rem", verticalAlign: "middle", marginRight: "0.4rem" }}>play_arrow</span>
                                Tiếp tục học
                            </button>
                        ) : (
                            <button
                                className="course-sidebar__btn course-sidebar__btn--primary"
                                onClick={handleEnroll}
                                disabled={enrolling}
                            >
                                {enrolling ? "Đang đăng ký..." : "Đăng ký ngay"}
                            </button>
                        ))}
                        <button
                            className="course-sidebar__btn course-sidebar__btn--secondary"
                            onClick={() => navigate(backPath)}
                        >
                            Quay lại danh sách
                        </button>
                    </div>

                    {!adminPreview && enrollError && (
                        <p className="course-sidebar__error">{enrollError}</p>
                    )}

                    {stats.length > 0 && (
                        <div className="course-sidebar__includes">
                            <h3 className="course-sidebar__includes-title">Bao gồm</h3>
                            <ul className="course-sidebar__list">
                                {stats.map((stat, i) => (
                                    <li key={i} className="course-sidebar__list-item">
                                        <span className="material-symbols-outlined course-sidebar__list-icon">{stat.icon}</span>
                                        <span>{stat.label}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            </div>
        </aside>
    );
}
