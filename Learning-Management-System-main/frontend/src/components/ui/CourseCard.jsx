import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import "../ui/styles/CourseCard.css";

export default function CourseCard({ course, linkPath, index = 0 }) {
    const getLevelColor = (level) => {
        switch (level?.toLowerCase()) {
            case 'beginner':
            case 'cơ bản': return '#48bb78';
            case 'intermediate':
            case 'trung cấp': return '#ed8936';
            case 'advanced':
            case 'nâng cao': return '#e53e3e';
            default: return '#718096';
        }
    };

    return (
        <motion.div
            className="course-card"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.38, ease: "easeOut", delay: Math.min(index * 0.06, 0.36) }}
            whileHover={{ y: -6 }}
        >
            {/* Image Wrap */}
            <div className="course-card__img-wrap">
                <div
                    className="course-card__img"
                    style={{ backgroundImage: `url(${course.image})` }}
                />
                {course.badge && (
                    <div className="course-card__badge">{course.badge}</div>
                )}
                <div
                    className="course-card__level"
                    style={{ backgroundColor: getLevelColor(course.level) }}
                >
                    {course.level}
                </div>
            </div>

            {/* Content */}
            <div className="course-card__content">
                <h3 className="course-card__title">{course.title}</h3>
                <p className="course-card__instructor">
                    {course.instructorAvatar ? (
                        <span
                            className="course-card__instructor-avatar"
                            style={{ backgroundImage: `url(${course.instructorAvatar})` }}
                            aria-hidden="true"
                        />
                    ) : (
                        <span className="course-card__instructor-avatar course-card__instructor-avatar--placeholder">
                            <span className="material-symbols-outlined">person</span>
                        </span>
                    )}
                    {course.instructor}
                </p>

                <div className="course-card__meta">
                    {course.duration && (
                        <div className="course-card__meta-item course-card__duration">
                            <span className="material-symbols-outlined">schedule</span>
                            {course.duration}
                        </div>
                    )}
                    <div className="course-card__meta-item course-card__lessons">
                        <span className="material-symbols-outlined">play_circle</span>
                        {course.lessons || 0} Bài học
                    </div>
                    <div className="course-card__meta-item course-card__exercises">
                        <span className="material-symbols-outlined">assignment</span>
                        {course.exercises || 0} Bài tập
                    </div>
                </div>

                <div className="course-card__footer">
                    <Link to={linkPath || `/list-course/detail-course/${course.id}`} className="course-card__view-more">
                        <span className="material-symbols-outlined">visibility</span>
                        Chi tiết khóa học
                    </Link>
                </div>
            </div>
        </motion.div>
    );
}
