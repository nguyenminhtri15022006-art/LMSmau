import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUsers, faUserPlus, faTrash, faChevronLeft, faChevronRight, faUserCircle, faEye,
} from '@fortawesome/free-solid-svg-icons';
import { useToast } from '../../../../../components/ui/Toast';
import courseApi from '../../../../../services/course.api.js';
import UserInfoModal from '../../../../../components/ui/UserInfoModal';
import '../../../styles/teacher/ManagementCenter/studentManagementHub.css';
import { formatDateVN } from '../../../../../utils/dateTime';

const StudentManagementHub = ({ courseId, onAddClick }) => {
  const toast = useToast();
  const [students,    setStudents]    = useState([]);
  const [totalPages,  setTotalPages]  = useState(0);
  const [totalCount,  setTotalCount]  = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading,     setLoading]     = useState(true);
  const [direction,   setDirection]   = useState(0);
  const [confirmDelete, setConfirmDelete] = useState(null); // { userId, name }
  const [viewStudent, setViewStudent] = useState(null);
  const PAGE_SIZE = 10;

  const fetchStudents = useCallback(async (page) => {
    if (!courseId) return;
    setLoading(true);
    try {
      const data = await courseApi.getStudents(courseId, page, PAGE_SIZE);
      setStudents(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
      setTotalCount(data.totalElements ?? 0);
    } catch (err) {
      console.error('Lỗi tải danh sách học viên:', err);
      toast.error('Không thể tải danh sách học viên.');
    } finally {
      setLoading(false);
    }
  }, [courseId, toast]);

  useEffect(() => { fetchStudents(0); }, [fetchStudents]);

  const paginate = (page) => {
    if (page < 0 || page >= totalPages) return;
    setDirection(page > currentPage ? 1 : -1);
    setCurrentPage(page);
    fetchStudents(page);
  };

  const handleRemoveStudent = async (userId, name) => {
    setConfirmDelete({ userId, name });
  };

  const confirmRemoveStudent = async () => {
    if (!confirmDelete) return;
    const { userId } = confirmDelete;
    setConfirmDelete(null);
    try {
      await courseApi.removeStudent(courseId, userId);
      fetchStudents(currentPage);
    } catch (err) {
      console.error('Lỗi xóa học viên:', err);
      toast.error('Không thể xóa học viên. Vui lòng thử lại.');
    }
  };

  const slideVariants = {
    enter:  (d) => ({ x: d > 0 ?  300 : -300, opacity: 0 }),
    center:      () => ({ x: 0,                  opacity: 1 }),
    exit:   (d) => ({ x: d < 0 ?  300 : -300, opacity: 0 }),
  };

  const formatDate = (iso) =>
    iso ? formatDateVN(iso) : '—';

  const getStatusBadge = (status) => {
    if (status === 'ACTIVE')    return { label: 'Đang học' };
    if (status === 'CANCELLED') return { label: 'Đã hủy' };
    return { label: status };
  };

  return (
    <section className="student-management-hub">
      <div className="section-title">
        <FontAwesomeIcon icon={faUsers} />
        Quản lý học viên
      </div>

      <div className="white-card">
        <div className="hub-header">
          <div className="hub-tabs">
            <button className="hub-tab active">
              Tất cả học viên ({totalCount})
            </button>
          </div>
          <button className="btn-add-student" onClick={onAddClick}>
            <FontAwesomeIcon icon={faUserPlus} />
            Mời học viên
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Đang tải...</div>
        ) : (
          <div className="hub-table-container" style={{ overflow: 'hidden', position: 'relative' }}>
            <table className="management-table" style={{ tableLayout: 'fixed', width: '100%' }}>
              <thead>
                <tr>
                  <th style={{ width: '32%', paddingLeft: '25px' }}>Học viên</th>
                  <th style={{ width: '26%' }}>Email</th>
                  <th style={{ width: '18%' }}>Ngày tham gia</th>
                  <th style={{ width: '14%', textAlign: 'center' }}>Trạng thái</th>
                  <th style={{ width: '10%', textAlign: 'center' }}>Hành động</th>
                </tr>
              </thead>
              <tbody style={{ position: 'relative' }}>
                <AnimatePresence mode="wait" custom={direction}>
                  <motion.tr
                    key={currentPage}
                    custom={direction}
                    variants={slideVariants}
                    initial="enter"
                    animate="center"
                    exit="exit"
                    transition={{ x: { type: 'spring', stiffness: 300, damping: 30 }, opacity: { duration: 0.2 } }}
                  >
                    <td colSpan="5" style={{ padding: 0 }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
                        <colgroup>
                          <col style={{ width: '32%' }} />
                          <col style={{ width: '26%' }} />
                          <col style={{ width: '18%' }} />
                          <col style={{ width: '14%' }} />
                          <col style={{ width: '10%' }} />
                        </colgroup>
                        <tbody>
                          {students.length === 0 ? (
                            <tr>
                              <td colSpan="5" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                                Chưa có học viên nào tham gia khóa học này.
                              </td>
                            </tr>
                          ) : students.map((s) => {
                            const badge = getStatusBadge(s.enrollmentStatus);
                            return (
                              <tr key={s.userId}>
                                <td style={{ padding: '16px 25px' }}>
                                  <div className="student-info-mini">
                                    {s.avatarUrl
                                      ? <img src={s.avatarUrl} alt={s.fullname} className="mini-avatar" />
                                      : <FontAwesomeIcon icon={faUserCircle} style={{ fontSize: '2rem', color: '#cbd5e1' }} />
                                    }
                                    <div className="student-text-info">
                                      <span className="student-name">{s.fullname || s.username || 'N/A'}</span>
                                      <span className="student-email">@{s.username || s.userId?.slice(0, 8)}</span>
                                    </div>
                                  </div>
                                </td>
                                <td style={{ padding: '16px 25px', overflow: 'hidden' }}>
                                  <span style={{ fontSize: '0.85rem', color: 'var(--text-label)', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {s.email || '—'}
                                  </span>
                                </td>
                                <td style={{ padding: '16px 25px' }}>
                                  <span className="date-val">{formatDate(s.enrolledAt)}</span>
                                </td>
                                <td style={{ padding: '16px 25px', textAlign: 'center' }}>
                                  <span className={`status-badge status-badge--${(s.enrollmentStatus || 'other').toLowerCase()}`}>
                                    {badge.label}
                                  </span>
                                </td>
                                <td style={{ padding: '16px 25px', textAlign: 'center' }}>
                                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                                    <button
                                      className="icon-btn"
                                      title="Xem thông tin học viên"
                                      onClick={() => setViewStudent({
                                        userId: s.userId,
                                        fullname: s.fullname,
                                        username: s.username,
                                        email: s.email,
                                        avatarUrl: s.avatarUrl,
                                        role: 'STUDENT',
                                        enrolledAt: s.enrolledAt,
                                      })}
                                    >
                                      <FontAwesomeIcon icon={faEye} />
                                    </button>
                                    <button
                                      className="icon-btn delete"
                                      title="Xóa khỏi khóa học"
                                      onClick={() => handleRemoveStudent(s.userId, s.fullname || s.email)}
                                    >
                                      <FontAwesomeIcon icon={faTrash} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </td>
                  </motion.tr>
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination-footer">
            <span className="pagination-info">
              Trang {currentPage + 1} / {totalPages} — {totalCount} học viên
            </span>
            <div className="pagination-controls">
              <button className="chevron-btn" disabled={currentPage === 0} onClick={() => paginate(currentPage - 1)}>
                <FontAwesomeIcon icon={faChevronLeft} />
              </button>
              {[...Array(Math.min(totalPages, 5))].map((_, i) => (
                <button
                  key={i}
                  className={`page-btn ${currentPage === i ? 'active' : ''}`}
                  onClick={() => paginate(i)}
                >
                  {i + 1}
                </button>
              ))}
              <button className="chevron-btn" disabled={currentPage >= totalPages - 1} onClick={() => paginate(currentPage + 1)}>
                <FontAwesomeIcon icon={faChevronRight} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Confirm delete dialog */}
      <AnimatePresence>
        {confirmDelete && (
          <div className="confirm-overlay" onClick={() => setConfirmDelete(null)}>
            <motion.div
              className="confirm-dialog"
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="confirm-icon">
                <FontAwesomeIcon icon={faTrash} />
              </div>
              <h3>Xác nhận xóa học viên</h3>
              <p>Bạn sắp xóa <strong>{confirmDelete.name}</strong> khỏi khóa học. Hành động này không thể hoàn tác.</p>
              <div className="confirm-actions">
                <button className="confirm-btn cancel" onClick={() => setConfirmDelete(null)}>Hủy</button>
                <button className="confirm-btn delete" onClick={confirmRemoveStudent}>Xóa luôn</button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {viewStudent && (
        <UserInfoModal
          user={viewStudent}
          extraFields={[
            { label: 'Ngày tham gia khóa học', value: viewStudent.enrolledAt
                ? new Date(viewStudent.enrolledAt).toLocaleDateString('vi-VN') : '—' },
          ]}
          onClose={() => setViewStudent(null)}
        />
      )}
    </section>
  );
};

export default StudentManagementHub;
