import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../../../styles/teacher/InstructorExerciseHub/courseExerciseCard.css';

function getCategory(course) {
  return course.categories?.[0]?.name || course.category || course.difficulty || 'Khóa học';
}

function getStatusLabel(course) {
  if (course.status) return course.status;
  if (course.published !== undefined) return course.published ? 'Đã xuất bản' : 'Bản nháp';
  if (course.isPublished !== undefined) return course.isPublished ? 'Đã xuất bản' : 'Bản nháp';
  return null;
}

export default function CourseExerciseCard({ course }) {
  const navigate = useNavigate();
  const courseId = course.id ?? course.courseId;
  const courseTitle = course.title ?? course.name ?? 'Khóa học chưa đặt tên';
  const category = getCategory(course);
  const statusLabel = getStatusLabel(course);
  const openCourseExercises = () => {
    navigate(`/instructor/exercises/${courseId}`, { state: { courseTitle } });
  };
  const handleButtonClick = (event) => {
    event.stopPropagation();
    openCourseExercises();
  };

  return (
    <article
      className="course-exercise-card"
      onClick={openCourseExercises}
    >
      <div className="course-image-container">
        <img
          src={course.thumbnailUrl || `https://picsum.photos/seed/course${courseId}/480/280`}
          alt={courseTitle}
          className="course-thumbnail"
        />
        <div className="course-image-overlay" />
        <span className="course-category-badge">{category}</span>
        {statusLabel && <span className="course-status-badge">{statusLabel}</span>}
      </div>

      <div className="course-info">
        <div className="course-title-row">
          <span className="material-symbols-outlined">assignment</span>
          <h3>{courseTitle}</h3>
        </div>
        <p>
          Mở không gian quản lý quiz, coding challenge và kết quả làm bài cho khóa học này.
        </p>
      </div>

      <button className="manage-btn" type="button" onClick={handleButtonClick}>
        Quản lý bài tập
        <span className="material-symbols-outlined">arrow_forward</span>
      </button>
    </article>
  );
}
