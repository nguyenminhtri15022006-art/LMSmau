import React from 'react';
import '../../../styles/student/CourseHub/Announcements.css';
import { formatDateVN, parseBackendUtcDate } from '../../../../../utils/dateTime';

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const date = parseBackendUtcDate(dateStr);
  if (!date) return '';
  const diff = Date.now() - date.getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Vừa xong';
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'Hôm qua';
  if (days < 7) return `${days} ngày trước`;
  return formatDateVN(date);
}

export default function TeacherAnnouncements({
  announcements = [],
  loading = false,
  page = 0,
  totalPages = 0,
  onPageChange,
}) {
  return (
    <div className="ch-announcements-container">
      <div className="ch-section-title">
        <span className="material-symbols-outlined emoji-icon">campaign</span>
        Thông báo từ giảng viên
      </div>

      <div className="ch-announcements-list">
        {loading ? (
          <>
            <div className="ch-announcement-skeleton" />
            <div className="ch-announcement-skeleton" />
            <div className="ch-announcement-skeleton ch-announcement-skeleton--short" />
          </>
        ) : announcements.length === 0 ? (
          <div className="ch-announcement-empty">
            <span className="material-symbols-outlined">notifications_off</span>
            Chưa có thông báo nào
          </div>
        ) : (
          announcements.map((item) => (
            <div key={item.id} className="ch-announcement-card">
              <div className="ch-announcement-header">
                <span className="ch-announcement-badge update">{item.title || 'Thông báo'}</span>
                <span className="ch-announcement-time">{timeAgo(item.createdAt)}</span>
              </div>
              <div className="ch-announcement-content">{item.content}</div>
            </div>
          ))
        )}
      </div>

      {/* Phân trang */}
      {totalPages > 1 && (
        <div className="ch-announcements-pagination">
          <button
            className="ch-page-btn"
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0 || loading}
          >
            <span className="material-symbols-outlined">chevron_left</span>
          </button>

          <span className="ch-page-info">
            {page + 1} / {totalPages}
          </span>

          <button
            className="ch-page-btn"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1 || loading}
          >
            <span className="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      )}
    </div>
  );
}
