import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import CourseCard from "../../../../../components/ui/CourseCard";
import "../../../styles/student/MyCourse/MyCourseContent.css"

const tabs = [
    { label: "Tất cả", value: "all" },
    { label: "Đang học", value: "in-progress" },
    { label: "Đã hoàn thành", value: "completed" },
];

const getSubtitle = (courses) => {
    if (courses.length === 0)
        return "Bạn chưa tham gia khóa học nào. Hãy khám phá và đăng ký để bắt đầu hành trình học tập của bạn.";
    const allCompleted = courses.every((c) => c.status === "Completed");
    if (allCompleted)
        return "Tuyệt vời! Bạn đã hoàn thành tất cả khóa học. Hãy tiếp tục khám phá các khóa học mới để nâng cao kỹ năng.";
    return "Tiếp tục hành trình học tập của bạn với các khóa học bạn đã đăng ký. Theo dõi tiến độ, tiếp tục học các bài học và khám phá các cột mốc đã hoàn thành.";
};

export default function MyCoursesContent({ courses, activeTab, onTabChange, isLoading, error }) {
    const filteredCourses = courses.filter((course) => {
        if (activeTab === "all") return true;
        return course.status?.toLowerCase().replace(" ", "-") === activeTab;
    });

    const allCompleted = !isLoading && courses.length > 0 && courses.every((c) => c.status === "Completed");

    return (
        <section className="my-courses-page">
            <motion.div
                className="my-courses-page__hero"
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, ease: 'easeOut' }}
            >
                <div>
                    <p className="my-courses-page__eyebrow">Bảng điều khiển học tập của tôi</p>
                    <h1 className="my-courses-page__title">Khóa học của tôi</h1>
                    <p className="my-courses-page__subtitle">
                        {getSubtitle(courses)}
                    </p>
                </div>

                <div className="my-courses-page__overview">
                    <div className="my-courses-page__overview-card">
                        <span className="my-courses-page__overview-label">Khóa học đang hoạt động</span>
                        <strong>{courses.filter((course) => course.status === "In Progress").length}</strong>
                    </div>
                    <div className="my-courses-page__overview-card">
                        <span className="my-courses-page__overview-label">Khóa học đã hoàn thành</span>
                        <strong>{courses.filter((course) => course.status === "Completed").length}</strong>
                    </div>
                    <div className="my-courses-page__overview-card">
                        <span className="my-courses-page__overview-label">Tổng số khóa học đã đăng ký</span>
                        <strong>{courses.length}</strong>
                    </div>
                </div>
            </motion.div>

            <motion.div
                className="my-courses-page__tabs"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.38, ease: 'easeOut', delay: 0.08 }}
            >
                {tabs.map((tab) => (
                    <button
                        key={tab.value}
                        type="button"
                        className={`my-courses-page__tab ${activeTab === tab.value ? "active" : ""}`}
                        onClick={() => onTabChange(tab.value)}
                    >
                        {tab.label}
                    </button>
                ))}
            </motion.div>

            <div className="my-courses-page__summary">
                <p>{filteredCourses.length} khóa học</p>
            </div>

            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '80px 0', color: 'var(--text-secondary)' }}>
                    <div className="admin-loading-spinner" style={{ margin: '0 auto 16px auto', width: '32px', height: '32px' }}></div>
                    Đang tải khóa học của bạn...
                </div>
            ) : error ? (
                <div style={{ textAlign: 'center', padding: '80px 0', color: '#ef4444' }}>
                    <p>{error}</p>
                </div>
            ) : courses.length === 0 ? (
                <div className="my-courses-page__empty my-courses-page__empty--cta">
                    <span className="material-symbols-outlined my-courses-page__empty-icon">school</span>
                    <h2>Bạn chưa đăng ký khóa học nào</h2>
                    <p>Hãy khám phá danh sách khóa học và bắt đầu hành trình học tập của bạn ngay hôm nay.</p>
                    <Link to="/list-course" className="my-courses-page__browse-btn">
                        <span className="material-symbols-outlined">explore</span>
                        Khám phá khóa học
                    </Link>
                </div>
            ) : filteredCourses.length > 0 ? (
                <div className="my-courses-page__grid">
                    {filteredCourses.map((course, index) => (
                        <CourseCard key={course.id} course={course} linkPath={`/course-hub?id=${course.id}`} index={index} />
                    ))}
                </div>
            ) : allCompleted ? (
                <div className="my-courses-page__empty my-courses-page__empty--cta">
                    <span className="material-symbols-outlined my-courses-page__empty-icon">emoji_events</span>
                    <h2>Bạn đã hoàn thành tất cả khóa học!</h2>
                    <p>Thành tích đáng tự hào! Hãy tiếp tục khám phá các khóa học mới để không ngừng nâng cao kỹ năng của bạn.</p>
                    <Link to="/list-course" className="my-courses-page__browse-btn">
                        <span className="material-symbols-outlined">explore</span>
                        Khám phá khóa học mới
                    </Link>
                </div>
            ) : (
                <div className="my-courses-page__empty">
                    <span className="material-symbols-outlined my-courses-page__empty-icon">filter_list_off</span>
                    <h2>Không có khóa học nào</h2>
                    <p>Bạn chưa có khóa học nào trong mục này.</p>
                </div>
            )}
        </section>
    );
}
