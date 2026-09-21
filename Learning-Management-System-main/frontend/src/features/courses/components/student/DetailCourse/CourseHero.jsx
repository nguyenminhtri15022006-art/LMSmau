import { Link } from "react-router-dom";
import "../../../styles/student/DetailCourse/CourseHero.css";

export default function CourseHero({ course, showBreadcrumb = true }) {
    return (
        <section className="course-hero">
            <div className="course-hero__container">
                <div className="course-hero__grid">
                    <div className="course-hero__content">
                        {showBreadcrumb && (
                        <nav className="course-hero__breadcrumbs">
                            <Link className="course-hero__breadcrumb-link" to="/list-course">Danh sách khóa học</Link>
                            <span className="material-symbols-outlined text-xs">chevron_right</span>
                            <span className="course-hero__breadcrumb-current">
                                {course?.title || "Chi tiết khóa học"}
                            </span>
                        </nav>
                        )}

                        {course?.categories?.length > 0 && (
                            <div className="course-hero__categories">
                                {course.categories.map(cat => (
                                    <span key={cat.id} className="course-hero__category-badge">{cat.name}</span>
                                ))}
                            </div>
                        )}

                        <h1 className="course-hero__title">{course?.title}</h1>

                        {course?.description && (
                            <p className="course-hero__description">{course.description}</p>
                        )}

                        <div className="course-hero__meta">
                            {course?.rating != null && (
                                <div className="course-hero__rating">
                                    <span className="course-hero__rating-value">{course.rating}</span>
                                    <div className="course-hero__stars">
                                        {[...Array(5)].map((_, i) => (
                                            <span key={i} className="material-symbols-outlined">
                                                {i < Math.floor(course.rating) ? "star" : i < course.rating ? "star_half" : "star_border"}
                                            </span>
                                        ))}
                                    </div>
                                    {course?.reviews != null && (
                                        <span className="course-hero__reviews">({course.reviews} đánh giá)</span>
                                    )}
                                </div>
                            )}

                            {course?.students != null && (
                                <div className="course-hero__info-item">
                                    <span className="material-symbols-outlined">group</span>
                                    <span>{course.students} học viên</span>
                                </div>
                            )}

                            {course?.lessonsCount != null && (
                                <div className="course-hero__info-item">
                                    <span className="material-symbols-outlined">menu_book</span>
                                    <span>{course.lessonsCount} bài học</span>
                                </div>
                            )}

                            {course?.level && (
                                <div className="course-hero__info-item">
                                    <span className="material-symbols-outlined">signal_cellular_alt</span>
                                    <span>{course.level}</span>
                                </div>
                            )}

                            {course?.lastUpdated && (
                                <div className="course-hero__info-item">
                                    <span className="material-symbols-outlined">update</span>
                                    <span>Cập nhật {course.lastUpdated}</span>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
