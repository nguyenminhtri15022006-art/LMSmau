import React, { useState } from 'react';
import '../../../styles/student/CourseHub/CourseHero.css';
import { formatDateVN, formatTimeVN, parseBackendUtcDate } from '../../../../../utils/dateTime';

function formatTime(dateStr) {
  if (!dateStr) return '';
  return formatTimeVN(dateStr);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return formatDateVN(dateStr, { weekday: 'long' });
}

function isToday(dateStr) {
  if (!dateStr) return false;
  return formatDateVN(dateStr) === formatDateVN(new Date());
}

function isOngoing(session) {
  const now = Date.now();
  const start = parseBackendUtcDate(session.startTime)?.getTime();
  const end = session.endTime ? parseBackendUtcDate(session.endTime)?.getTime() : null;
  if (!start) return false;
  if (end) return now >= start && now < end;
  return false;
}

const PAGE_SIZE = 4;

const UpcomingSessions = ({ schedules = [], schedulesError = false }) => {
  const now = Date.now();
  const [page, setPage] = useState(0);

  const upcoming = schedules
    .filter(s => {
      if (!s.startTime) return false;
      const end = s.endTime
        ? parseBackendUtcDate(s.endTime)?.getTime()
        : parseBackendUtcDate(s.startTime)?.getTime();
      return end > now;
    })
    .sort((a, b) => parseBackendUtcDate(a.startTime) - parseBackendUtcDate(b.startTime));

  if (schedulesError) {
    return (
      <div className="ch-sessions-empty">
        <span className="material-symbols-outlined">warning</span>
        Không thể tải lịch học
      </div>
    );
  }

  if (!upcoming.length) {
    return (
      <div className="ch-sessions-empty">
        <span className="material-symbols-outlined">event_available</span>
        Chưa có buổi học sắp tới
      </div>
    );
  }

  const totalPages = Math.ceil(upcoming.length / PAGE_SIZE);
  const visible = upcoming.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div className="ch-sessions-wrapper">
      <div className="ch-sessions-list">
        {visible.map(s => {
          const ongoing = isOngoing(s);
          const today = isToday(s.startTime);
          return (
            <div key={s.id} className={`ch-session-card ${ongoing ? 'ongoing' : ''} ${today && !ongoing ? 'today' : ''}`}>
              <div className="ch-session-date">
                <span className="material-symbols-outlined">calendar_month</span>
                {formatDate(s.startTime)}
                {ongoing && <span className="ch-session-live-badge">ĐANG DIỄN RA</span>}
                {today && !ongoing && <span className="ch-session-today-badge">HÔM NAY</span>}
              </div>
              <div className="ch-session-title">{s.title || 'Buổi học'}</div>
              <div className="ch-session-time">
                <span className="material-symbols-outlined">schedule</span>
                {formatTime(s.startTime)}
                {s.endTime && ` – ${formatTime(s.endTime)}`}
              </div>
              {s.note && (
                <div className="ch-session-note">
                  <span className="material-symbols-outlined">notes</span>
                  {s.note}
                </div>
              )}
              {s.meetingUrl && (
                <a href={s.meetingUrl} target="_blank" rel="noopener noreferrer" className="ch-session-meet-btn">
                  <span className="material-symbols-outlined">videocam</span>
                  Tham gia
                </a>
              )}
            </div>
          );
        })}
      </div>
      {totalPages > 1 && (
        <div className="ch-sessions-pagination">
          <button
            className="ch-sessions-nav-btn"
            onClick={() => setPage(p => p - 1)}
            disabled={page === 0}
          >
            <span className="material-symbols-outlined">chevron_left</span>
          </button>
          <span className="ch-sessions-page-info">{page + 1} / {totalPages}</span>
          <button
            className="ch-sessions-nav-btn"
            onClick={() => setPage(p => p + 1)}
            disabled={page >= totalPages - 1}
          >
            <span className="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default function CourseHero({ title, instructorName, instructorAvatar, meetingUrl, schedules = [], schedulesError = false }) {
  return (
    <div className="ch-hero">
      <div className="ch-hero-top">
        <div className="ch-hero-info">
          <h1 className="ch-hero-title">{title}</h1>
          <div className="ch-instructor-row">
            {instructorName && (
              <div className="ch-instructor-badge">
                <span className="ch-instructor-avatar">{instructorAvatar}</span>
                <span className="ch-instructor-name">{instructorName}</span>
              </div>
            )}
          </div>
        </div>

        {meetingUrl && (
          <a href={meetingUrl} target="_blank" rel="noopener noreferrer" className="ch-meet-btn">
            <span className="material-symbols-outlined">videocam</span>
            Tham gia Google Meet
          </a>
        )}
      </div>

      <div className="ch-hero-bottom">
        <div className="ch-sessions-header">
          <span className="material-symbols-outlined">event</span>
          Lịch các buổi học sắp tới
        </div>
        <UpcomingSessions schedules={schedules} schedulesError={schedulesError} />
      </div>
    </div>
  );
}
