import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import '../../styles/student/CourseProgressList.css';

const PAGE_SIZE = 4;
const FALLBACK_IMG = 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80';

const STATUS_VI = {
  'Completed': 'Hoàn thành',
  'In Progress': 'Đang học',
};

export default function CourseProgressList({ courses }) {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  useEffect(() => {
    setPage(0);
  }, [courses]);

  if (!courses || courses.length === 0) return null;

  const totalPages = Math.ceil(courses.length / PAGE_SIZE);
  const currentCourses = courses.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div className="progress-section">
      <div className="section-header">
        <h3 className="section-title">Tiến độ khóa học</h3>
      </div>

      <div className="progress-list">
        {currentCourses.map((course, idx) => (
          <motion.div
            key={course.id}
            className="progress-item-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.38, ease: 'easeOut', delay: Math.min(idx * 0.06, 0.24) }}
            whileHover={{ y: -6 }}
            onClick={() => navigate(`/course-hub?id=${course.id}`)}
            style={{ cursor: 'pointer' }}
          >
            <img
              src={course.image}
              alt={course.title}
              className="course-mini-img"
              onError={e => { e.target.onerror = null; e.target.src = FALLBACK_IMG; }}
            />

            <div className="course-info-row">
              <h4>{course.title}</h4>
              <div className="progress-bar-bg" style={{ height: '6px' }}>
                <div
                  className="progress-bar-fill"
                  style={{
                    width: `${course.progress}%`,
                    backgroundColor: course.progress === 100 ? '#10b981' : '#2563eb'
                  }}
                />
              </div>
            </div>

            <div className="progress-item-meta">
              <span className={`status-badge ${course.status === 'Completed' ? 'completed' : 'in-progress'}`}>
                {STATUS_VI[course.status] || course.status}
              </span>
              <p className="progress-item-meta__label">
                {course.status === 'Completed' ? 'Hoàn thành lúc' : 'Lần cuối cập nhật'}
              </p>
              <p className="progress-item-meta__date">{course.date}</p>
            </div>

            <button
              className="progress-item-card__menu"
              onClick={e => e.stopPropagation()}
            >
              <span className="material-symbols-outlined">more_vert</span>
            </button>
          </motion.div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="progress-pagination">
          <button
            onClick={() => setPage(p => p - 1)}
            disabled={page === 0}
            className="progress-pagination__btn"
          >
            ← Trước
          </button>

          <span className="progress-pagination__status">
            {page + 1} / {totalPages}
          </span>

          <button
            onClick={() => setPage(p => p + 1)}
            disabled={page >= totalPages - 1}
            className="progress-pagination__btn"
          >
            Sau →
          </button>
        </div>
      )}
    </div>
  );
}
