import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faBullhorn, faCalendarPlus, faPaperPlane,
  faTrash, faHistory, faTimes, faCalendarDay, faClock, faListUl,
} from '@fortawesome/free-solid-svg-icons';
import '../../../styles/teacher/ManagementCenter/schedulesCommunication.css';
import {
  formatDateTimeVN,
  formatTimeVN,
  parseBackendUtcDate,
  utcIsoToVietnamLocalInput,
  vietnamLocalInputToUtcIso,
} from '../../../../../utils/dateTime';

const SchedulesCommunication = ({
  courseId,
  schedules,
  announcements,
  onCreateSchedule,
  onRemoveSchedule,
  onPostAnnouncement,
  onDeleteAnnouncement,
}) => {
  const [announcementTitle,   setAnnouncementTitle]   = useState('');
  const [announcementContent, setAnnouncementContent] = useState('');
  const [submittingAnn,       setSubmittingAnn]       = useState(false);

  const [scheduleForm, setScheduleForm] = useState({
    title:     '',
    startTime: '',
    endTime:   '',
    note:      '',
  });
  const [submittingSched, setSubmittingSched] = useState(false);
  const [schedError,      setSchedError]      = useState('');
  const [sessionsPage,    setSessionsPage]    = useState(0);
  const SESSIONS_PAGE_SIZE = 5;

  const handlePostAnn = async () => {
    if (!announcementTitle.trim() || !announcementContent.trim()) return;
    setSubmittingAnn(true);
    try {
      await onPostAnnouncement({ title: announcementTitle.trim(), content: announcementContent.trim() });
      setAnnouncementTitle('');
      setAnnouncementContent('');
    } finally {
      setSubmittingAnn(false);
    }
  };

  const nowIso = () => utcIsoToVietnamLocalInput(new Date());

  const handleCreateSched = async () => {
    setSchedError('');
    const { title, startTime, endTime } = scheduleForm;
    if (!title.trim() || !startTime || !endTime) {
      setSchedError('Vui lòng điền đầy đủ tiêu đề, thời gian bắt đầu và kết thúc.');
      return;
    }
    const startTimeUtc = vietnamLocalInputToUtcIso(startTime);
    const endTimeUtc = vietnamLocalInputToUtcIso(endTime);
    if (!startTimeUtc || !endTimeUtc) {
      setSchedError('Thời gian lịch học không hợp lệ.');
      return;
    }
    if (parseBackendUtcDate(startTimeUtc) < new Date()) {
      setSchedError('Thời gian bắt đầu không được trong quá khứ.');
      return;
    }
    if (parseBackendUtcDate(endTimeUtc) <= parseBackendUtcDate(startTimeUtc)) {
      setSchedError('Thời gian kết thúc phải sau thời gian bắt đầu.');
      return;
    }
    setSubmittingSched(true);
    try {
      await onCreateSchedule({
        title:     title.trim(),
        startTime: startTimeUtc,
        endTime:   endTimeUtc,
        note:      scheduleForm.note.trim() || null,
      });
      setScheduleForm({ title: '', startTime: '', endTime: '', note: '' });
    } catch {
      setSchedError('Không thể tạo lịch. Vui lòng thử lại.');
    } finally {
      setSubmittingSched(false);
    }
  };

  const sortedSchedules = [...schedules].sort(
    (a, b) => parseBackendUtcDate(a.startTime) - parseBackendUtcDate(b.startTime)
  );
  const totalSessionPages = Math.ceil(sortedSchedules.length / SESSIONS_PAGE_SIZE);
  const safePage = Math.min(sessionsPage, Math.max(0, totalSessionPages - 1));
  const pageItems = sortedSchedules.slice(safePage * SESSIONS_PAGE_SIZE, (safePage + 1) * SESSIONS_PAGE_SIZE);

  return (
    <section className="schedules-communication">
      <div className="section-title">
        <FontAwesomeIcon icon={faCalendarPlus} />
        Lịch học & Thông báo
      </div>

      <div className="schedules-comm-grid">
        {/* ─── Left column: announcement + all sessions ─── */}
        <div className="schedules-left-col">
        <div className="white-card announcement-section">
          <div className="sub-card-title">
            <FontAwesomeIcon icon={faBullhorn} style={{ color: '#3b82f6' }} />
            Thông báo khóa học
          </div>

          {/* History */}
          <div className="announcement-history-wrapper" style={{ marginBottom: '25px' }}>
            <div className="sub-card-title mini">
              <FontAwesomeIcon icon={faHistory} style={{ fontSize: '0.8rem', marginRight: '8px' }} />
              Thông báo gần đây
            </div>
            <div className="announcement-history-list">
              <AnimatePresence initial={false}>
                {announcements.map((ann) => (
                  <motion.div
                    key={ann.id}
                    layout
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                    className="history-item-card"
                  >
                    <div className="history-item-header">
                      <span className="history-date">
                        {ann.createdAt ? formatDateTimeVN(ann.createdAt) : 'Vừa đăng'}
                      </span>
                      <button className="btn-del-ann" onClick={() => onDeleteAnnouncement(ann.id)}>
                        <FontAwesomeIcon icon={faTimes} />
                      </button>
                    </div>
                    <p className="history-text" style={{ fontWeight: 600, marginBottom: '4px' }}>
                      {ann.title}
                    </p>
                    <p className="history-text" style={{ marginTop: 0, color: '#475569' }}>
                      {ann.content}
                    </p>
                  </motion.div>
                ))}
              </AnimatePresence>
              {announcements.length === 0 && (
                <div className="empty-history">Chưa có thông báo nào.</div>
              )}
            </div>
          </div>

          {/* New announcement form */}
          <div className="announcement-form" style={{ borderTop: '1px solid #f1f5f9', paddingTop: '25px' }}>
            <div className="sub-card-title mini">Thông báo mới</div>
            <input
              type="text"
              placeholder="Tiêu đề thông báo..."
              value={announcementTitle}
              onChange={(e) => setAnnouncementTitle(e.target.value)}
              style={{
                width: '100%', padding: '10px 14px', borderRadius: '10px',
                border: '1.5px solid #e2e8f0', fontSize: '0.9rem',
                marginBottom: '10px', boxSizing: 'border-box',
              }}
            />
            <textarea
              placeholder="Nội dung thông báo cho học viên..."
              value={announcementContent}
              onChange={(e) => setAnnouncementContent(e.target.value)}
            />
            <div className="announcement-actions">
              <div />
              <button
                className="post-btn"
                onClick={handlePostAnn}
                disabled={submittingAnn || !announcementTitle.trim() || !announcementContent.trim()}
              >
                {submittingAnn ? 'Đang đăng...' : 'Đăng'}
                <FontAwesomeIcon icon={faPaperPlane} />
              </button>
            </div>
          </div>

        </div>

      {/* ─── All sessions — same width as announcement card ─── */}
      <div className="all-sessions-section white-card">
        <div className="all-sessions-header">
          <div className="sub-card-title" style={{ marginBottom: 0 }}>
            <FontAwesomeIcon icon={faListUl} style={{ color: '#22c55e' }} />
            Tất cả buổi học
          </div>
          <span className="sessions-count-badge">{schedules.length}</span>
        </div>

        {schedules.length === 0 ? (
          <div className="sessions-empty">
            <FontAwesomeIcon icon={faCalendarDay} className="sessions-empty-icon" />
            <span>Chưa có buổi học nào được tạo.</span>
          </div>
        ) : (
          <>
            <div className="sessions-list">
              {pageItems.map((session) => (
                <div key={session.id} className="session-row">
                  <div className="session-row-accent" />
                  <div className="session-row-body">
                    <div className="session-row-title">{session.title}</div>
                    <div className="session-row-meta">
                      <span>
                        <FontAwesomeIcon icon={faClock} style={{ marginRight: 4, opacity: 0.5 }} />
                        {formatDateTimeVN(session.startTime)}
                      </span>
                      <span className="session-row-arrow">→</span>
                      <span>{formatTimeVN(session.endTime)}</span>
                    </div>
                    {session.note && <div className="session-row-note">{session.note}</div>}
                  </div>
                  <button className="session-del-btn" onClick={() => onRemoveSchedule(session.id)}>×</button>
                </div>
              ))}
            </div>
            {totalSessionPages > 1 && (
              <div className="sessions-pagination">
                <button
                  className="sessions-page-btn"
                  onClick={() => setSessionsPage(p => Math.max(0, p - 1))}
                  disabled={safePage === 0}
                >
                  ‹
                </button>
                {Array.from({ length: totalSessionPages }, (_, i) => (
                  <button
                    key={i}
                    className={`sessions-page-btn${safePage === i ? ' active' : ''}`}
                    onClick={() => setSessionsPage(i)}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  className="sessions-page-btn"
                  onClick={() => setSessionsPage(p => Math.min(totalSessionPages - 1, p + 1))}
                  disabled={safePage >= totalSessionPages - 1}
                >
                  ›
                </button>
              </div>
            )}
          </>
        )}
      </div>
        </div>{/* end .schedules-left-col */}

        {/* ─── Sidebar ─── */}
        <div className="sidebar-col">
          <div className="white-card scheduler-section">
            <div className="sub-card-title">Lên lịch buổi học mới</div>
            <form className="scheduler-form" onSubmit={(e) => e.preventDefault()}>
              <div className="form-group">
                <label>Tiêu đề buổi học</label>
                <input
                  type="text"
                  placeholder="VD: Buổi học 01"
                  value={scheduleForm.title}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, title: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Thời gian bắt đầu</label>
                <input
                  type="datetime-local"
                  min={nowIso()}
                  value={scheduleForm.startTime}
                  onChange={(e) => {
                    setSchedError('');
                    setScheduleForm({ ...scheduleForm, startTime: e.target.value });
                  }}
                />
              </div>
              <div className="form-group">
                <label>Thời gian kết thúc</label>
                <input
                  type="datetime-local"
                  min={scheduleForm.startTime || nowIso()}
                  value={scheduleForm.endTime}
                  onChange={(e) => {
                    setSchedError('');
                    setScheduleForm({ ...scheduleForm, endTime: e.target.value });
                  }}
                />
              </div>
              <div className="form-group">
                <label>Ghi chú (tùy chọn)</label>
                <input
                  type="text"
                  placeholder="VD: Học online qua Google Meet"
                  value={scheduleForm.note}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, note: e.target.value })}
                />
              </div>

              {schedError && (
                <div style={{
                  background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px',
                  padding: '10px 14px', fontSize: '0.83rem', color: '#dc2626', marginBottom: '10px',
                }}>
                  {schedError}
                </div>
              )}

              <button
                className="update-cal-btn"
                type="button"
                onClick={handleCreateSched}
                disabled={submittingSched}
              >
                {submittingSched ? 'Đang lưu...' : 'Thêm lịch'}
              </button>
            </form>
          </div>

          {/* Upcoming schedules list */}
          <div className="white-card upcoming-schedules-card" style={{ marginTop: '20px', padding: '20px' }}>
            <div className="sub-card-title" style={{ marginBottom: '15px' }}>Lịch sắp tới</div>
            {schedules.length === 0 ? (
              <div className="upcoming-schedules-empty" style={{ color: '#94a3b8', fontSize: '0.85rem', textAlign: 'center', padding: '20px 0' }}>
                Chưa có lịch học nào.
              </div>
            ) : (
              schedules
                .filter(s => {
                  const visibleUntil = parseBackendUtcDate(s.endTime) ?? parseBackendUtcDate(s.startTime);
                  return visibleUntil >= new Date();
                })
                .sort((a, b) => parseBackendUtcDate(a.startTime) - parseBackendUtcDate(b.startTime))
                .slice(0, 5)
                .map(s => (
                  <div key={s.id} className="upcoming-schedule-item" style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '10px 0', borderBottom: '1px solid #f1f5f9', fontSize: '0.85rem'
                  }}>
                    <div>
                      <div className="upcoming-schedule-title" style={{ fontWeight: 600, color: '#1e293b' }}>{s.title}</div>
                      <div className="upcoming-schedule-time" style={{ color: '#64748b', marginTop: '2px' }}>
                        {formatDateTimeVN(s.startTime)}
                      </div>
                    </div>
                    <button
                      onClick={() => onRemoveSchedule(s.id)}
                      style={{ border: 'none', background: 'none', color: '#ef4444', cursor: 'pointer', padding: '4px' }}
                    >
                      <FontAwesomeIcon icon={faTrash} />
                    </button>
                  </div>
                ))
            )}
          </div>
        </div>
      </div>

    </section>
  );
};

export default SchedulesCommunication;
