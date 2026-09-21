import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUserPlus, faArrowRight, faSpinner, faUserCircle, faChevronLeft, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import instructorDashboardApi from '../../../../../services/instructorDashboard.api';
import '../../../styles/teacher/InstructorHome/RecentActivity.css';
import { formatDateVN, parseBackendUtcDate } from '../../../../../utils/dateTime';

const formatTimeAgo = (dateStr) => {
  if (!dateStr) return '';
  const date = parseBackendUtcDate(dateStr);
  if (!date) return '';
  const diff = Date.now() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} ngày trước`;
  return formatDateVN(date);
};

const ActivityItem = ({ enrollment, delay }) => {
  const displayName = enrollment.fullname || enrollment.userId;

  return (
    <motion.div
      className="activity-card-premium"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5, delay }}
    >
      <div className="activity-dot-line green">
        <div className="dot"></div>
        <div className="line"></div>
      </div>

      <div className="activity-main-content">
        <div className="activity-header">
          <div className="activity-icon-mini green">
            <FontAwesomeIcon icon={faUserPlus} />
          </div>
          <span className="activity-time-stamp">{formatTimeAgo(enrollment.enrolledAt)}</span>
        </div>
        <div className="activity-body">
          <p className="activity-text">
            Học sinh mới đăng ký{' '}
            <Link to={`/manage/courses/center/${enrollment.courseId}`} className="high-link">
              {enrollment.courseTitle}
            </Link>
          </p>
          <div className="activity-student-info">
            {enrollment.avatarUrl ? (
              <img
                src={enrollment.avatarUrl}
                alt={displayName}
                className="activity-avatar"
                onError={(e) => { e.target.style.display = 'none'; }}
              />
            ) : (
              <div className="activity-avatar-placeholder">
                <FontAwesomeIcon icon={faUserCircle} />
              </div>
            )}
            <span className="activity-student-name">{displayName}</span>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

const PAGE_SIZE = 3;

const RecentActivity = () => {
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    instructorDashboardApi
      .getRecentEnrollments(50)
      .then(setEnrollments)
      .catch(() => setEnrollments([]))
      .finally(() => setLoading(false));
  }, []);

  const totalPages = Math.ceil(enrollments.length / PAGE_SIZE);
  const pageItems = enrollments.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  const handlePrev = () => setCurrentPage((p) => Math.max(0, p - 1));
  const handleNext = () => setCurrentPage((p) => Math.min(totalPages - 1, p + 1));

  return (
    <motion.section
      className="recent-activity-premium glass-card"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6, delay: 0.9 }}
    >
      <div className="section-title-row">
        <h3>Đăng ký gần đây</h3>
        <span className="pulse-dot"></span>
      </div>

      <div className="activity-timeline">
        {loading ? (
          <div className="activity-loading">
            <FontAwesomeIcon icon={faSpinner} spin />
            <span>Đang tải...</span>
          </div>
        ) : enrollments.length === 0 ? (
          <p className="activity-empty">Chưa có học sinh nào đăng ký gần đây.</p>
        ) : (
          <AnimatePresence mode="wait">
            <motion.div
              key={currentPage}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2 }}
            >
              {pageItems.map((enrollment, index) => (
                <ActivityItem
                  key={`${enrollment.courseId}-${enrollment.userId}-${index}`}
                  enrollment={enrollment}
                  delay={index * 0.06}
                />
              ))}
            </motion.div>
          </AnimatePresence>
        )}
      </div>

      {!loading && totalPages > 1 && (
        <div className="activity-pagination">
          <button
            className="pagination-btn"
            onClick={handlePrev}
            disabled={currentPage === 0}
            aria-label="Trang trước"
          >
            <FontAwesomeIcon icon={faChevronLeft} />
          </button>
          <span className="pagination-info">
            {currentPage + 1} / {totalPages}
          </span>
          <button
            className="pagination-btn"
            onClick={handleNext}
            disabled={currentPage === totalPages - 1}
            aria-label="Trang sau"
          >
            <FontAwesomeIcon icon={faChevronRight} />
          </button>
        </div>
      )}

      <Link to="/manage/courses" className="view-more-activity">
        Quản lý khóa học
        <FontAwesomeIcon icon={faArrowRight} />
      </Link>
    </motion.section>
  );
};

export default RecentActivity;
