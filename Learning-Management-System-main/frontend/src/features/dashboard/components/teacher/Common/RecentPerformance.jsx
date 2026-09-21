import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faSpinner, faUserGroup } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import instructorDashboardApi from '../../../../../services/instructorDashboard.api';
import '../../../styles/teacher/Common/RecentPerformance.css';

const RANK_COLORS = [
  { bg: '#fef3c7', text: '#d97706', border: '#fde68a' }, // gold
  { bg: '#f1f5f9', text: '#64748b', border: '#e2e8f0' }, // silver
  { bg: '#fdf2ec', text: '#c2410c', border: '#fed7aa' }, // bronze
];

const PROGRESS_COLORS = ['#f59e0b', '#3b82f6', '#10b981'];

const getInitials = (title = '') =>
  title.split(' ').slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');

const RecentPerformance = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    instructorDashboardApi
      .getTopCourses(3)
      .then(setCourses)
      .catch(() => setCourses([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <motion.section
      className="recent-performance-premium glass-card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.7 }}
    >
      <div className="performance-header">
        <div className="title-area">
          <h3>Hiệu suất khóa học hàng đầu</h3>
          <span className="subtitle">Top khóa học theo số lượng học sinh</span>
        </div>
        <Link to="/manage/courses" className="view-all-btn">
          Xem tất cả
          <FontAwesomeIcon icon={faChartLine} />
        </Link>
      </div>

      <div className="performance-list">
        {loading ? (
          <div className="performance-loading">
            <FontAwesomeIcon icon={faSpinner} spin />
            <span>Đang tải dữ liệu...</span>
          </div>
        ) : courses.length === 0 ? (
          <p className="performance-empty">Bạn chưa có khóa học nào.</p>
        ) : (
          courses.map((course, index) => {
            const rank = RANK_COLORS[index] ?? RANK_COLORS[2];
            const progressColor = PROGRESS_COLORS[index % PROGRESS_COLORS.length];
            const progress = course.averageProgress ?? 0;

            return (
              <motion.div
                key={course.id}
                className="performance-item"
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 + 0.9 }}
              >
                {/* Rank badge */}
                <div
                  className="rank-badge"
                  style={{ background: rank.bg, color: rank.text, border: `1.5px solid ${rank.border}` }}
                >
                  {index + 1}
                </div>

                {/* Course initials */}
                <div
                  className="course-badge"
                  style={{ background: `${progressColor}18`, color: progressColor }}
                >
                  {getInitials(course.title)}
                </div>

                {/* Info + progress */}
                <div className="course-info-col">
                  <span className="course-name">{course.title}</span>
                  <div className="course-meta-row">
                    <FontAwesomeIcon icon={faUserGroup} className="meta-icon" />
                    <span className="student-count">{course.studentCount.toLocaleString()} học sinh</span>
                  </div>
                  <div className="progress-row">
                    <div className="progress-bg">
                      <motion.div
                        className="progress-fill"
                        style={{ background: `linear-gradient(90deg, ${progressColor}, ${progressColor}cc)` }}
                        initial={{ width: 0 }}
                        animate={{ width: `${progress}%` }}
                        transition={{ duration: 1.1, delay: index * 0.15 + 1.0, ease: 'easeOut' }}
                      />
                    </div>
                    <span className="progress-text" style={{ color: progressColor }}>{progress}%</span>
                  </div>
                </div>
              </motion.div>
            );
          })
        )}
      </div>
    </motion.section>
  );
};

export default RecentPerformance;
