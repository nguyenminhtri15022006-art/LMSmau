import React, { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faTimes, faSearch, faPaperPlane, faCheckCircle,
  faExclamationTriangle, faUserCircle, faTrash, faSpinner,
} from '@fortawesome/free-solid-svg-icons';
import courseApi from '../../../../../services/course.api.js';
import '../../../styles/teacher/ManagementCenter/addStudentModal.css';

const AddStudentModal = ({ isOpen, onClose, courseId }) => {
  const [email,        setEmail]        = useState('');
  const [searching,    setSearching]    = useState(false);
  const [searchResult, setSearchResult] = useState(null); // { found: true, user } | { found: false }
  const [inviteList,   setInviteList]   = useState([]);   // list of UserInfoDto
  const [submitting,   setSubmitting]   = useState(false);
  const [result,       setResult]       = useState(null);
  const debounceRef = useRef(null);

  const handleSearchEmail = async (val) => {
    setEmail(val);
    setSearchResult(null);
    if (!val.trim() || !val.includes('@')) return;

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const user = await courseApi.searchUserByEmail(val.trim());
        setSearchResult(user ? { found: true, user } : { found: false });
      } catch {
        setSearchResult({ found: false });
      } finally {
        setSearching(false);
      }
    }, 500);
  };

  const addToList = (user) => {
    if (inviteList.some(u => u.userId === user.userId)) return;
    setInviteList(prev => [...prev, user]);
    setEmail('');
    setSearchResult(null);
  };

  const removeFromList = (userId) => {
    setInviteList(prev => prev.filter(u => u.userId !== userId));
  };

  const handleInvite = async () => {
    if (!inviteList.length) return;
    setSubmitting(true);
    setResult(null);
    try {
      const res = await courseApi.inviteStudents(courseId, inviteList.map(u => u.userId));
      setResult(res);
      setInviteList([]);
    } catch (err) {
      console.error('Lỗi mời học viên:', err);
      setResult({ error: 'Có lỗi xảy ra. Vui lòng thử lại.' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    setEmail('');
    setSearchResult(null);
    setInviteList([]);
    setResult(null);
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="modal-overlay" onClick={handleClose}>
          <motion.div
            className="modal-content"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <div className="modal-title-group">
                <h2>Mời học viên</h2>
                <p>Tìm kiếm học viên bằng email, sau đó thêm vào danh sách mời.</p>
              </div>
            </div>

            {result ? (
              /* ── Kết quả gửi lời mời ── */
              <div style={{ padding: '10px 0' }}>
                {result.error ? (
                  <div style={{ color: '#dc2626', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <FontAwesomeIcon icon={faExclamationTriangle} /> {result.error}
                  </div>
                ) : (
                  <div style={{ fontSize: '0.9rem', lineHeight: '2' }}>
                    {result.successIds?.length > 0 && (
                      <div style={{ color: '#166534' }}>
                        <FontAwesomeIcon icon={faCheckCircle} style={{ marginRight: '6px' }} />
                        <strong>Mời thành công: {result.successIds.length} học viên</strong>
                      </div>
                    )}
                    {result.alreadyIds?.length > 0 && (
                      <div style={{ color: '#92400e' }}>
                        <FontAwesomeIcon icon={faExclamationTriangle} style={{ marginRight: '6px' }} />
                        <strong>Đã tham gia: {result.alreadyIds.length} học viên</strong>
                      </div>
                    )}
                    {result.notFoundIds?.length > 0 && (
                      <div style={{ color: '#991b1b' }}>
                        <FontAwesomeIcon icon={faTimes} style={{ marginRight: '6px' }} />
                        <strong>Không tìm thấy: {result.notFoundIds.length} học viên</strong>
                      </div>
                    )}
                  </div>
                )}
                <div className="modal-actions" style={{ marginTop: '20px' }}>
                  <button className="btn-cancel" onClick={handleClose}>Đóng</button>
                  <button className="btn-send-invitation" onClick={() => setResult(null)}>Mời tiếp</button>
                </div>
              </div>
            ) : (
              <div>
                {/* ── Ô tìm kiếm email ── */}
                <div className="modal-form-group">
                  <label>Tìm kiếm bằng email</label>
                  <div style={{ position: 'relative' }}>
                    <div className="modal-input-wrapper">
                      <FontAwesomeIcon icon={faSearch} className="modal-input-icon" />
                      <input
                        type="text"
                        placeholder="Nhập email học viên..."
                        value={email}
                        onChange={(e) => handleSearchEmail(e.target.value)}
                        autoComplete="off"
                      />
                      {searching && (
                        <FontAwesomeIcon icon={faSpinner} spin style={{ position: 'absolute', right: '14px', color: '#94a3b8' }} />
                      )}
                    </div>

                    {/* Kết quả tìm kiếm */}
                    <AnimatePresence>
                      {searchResult && (
                        <motion.div
                          initial={{ opacity: 0, y: -6 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0 }}
                          style={{
                            position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
                            background: 'var(--surface)', borderRadius: '10px', marginTop: '6px',
                            boxShadow: '0 8px 24px rgba(0,0,0,0.12)', border: '1px solid var(--border-color)',
                          }}
                        >
                          {searchResult.found ? (
                            <div
                              onClick={() => addToList(searchResult.user)}
                              style={{
                                display: 'flex', alignItems: 'center', gap: '12px',
                                padding: '12px 16px', cursor: 'pointer', borderRadius: '10px',
                                width: '100%', boxSizing: 'border-box',
                              }}
                              onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-muted)'}
                              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                            >
                              {searchResult.user.avatarUrl
                                ? <img src={searchResult.user.avatarUrl} alt="" style={{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }} />
                                : <FontAwesomeIcon icon={faUserCircle} style={{ fontSize: '2rem', color: 'var(--border-color)', flexShrink: 0 }} />
                              }
                              <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontWeight: 600, color: 'var(--text-dark)', fontSize: '0.9rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {searchResult.user.fullname || searchResult.user.username || searchResult.user.email || '(Không có tên)'}
                                </div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {searchResult.user.email}
                                </div>
                              </div>
                              <div style={{ flexShrink: 0, fontSize: '0.78rem', color: '#3b82f6', fontWeight: 600 }}>
                                + Thêm
                              </div>
                            </div>
                          ) : (
                            <div style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: '0.85rem', textAlign: 'center' }}>
                              Không tìm thấy người dùng với email này.
                            </div>
                          )}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>

                {/* ── Danh sách sẽ mời ── */}
                {inviteList.length > 0 && (
                  <div style={{ marginTop: '20px' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-label)', marginBottom: '8px' }}>
                      Danh sách mời ({inviteList.length})
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                      {inviteList.map(u => (
                        <div key={u.userId} style={{
                          display: 'grid',
                          gridTemplateColumns: '32px 1fr 28px',
                          alignItems: 'center',
                          gap: '10px',
                          padding: '8px 10px',
                          borderRadius: '8px',
                          background: 'var(--surface-muted)',
                          border: '1px solid var(--border-color)',
                        }}>
                          {/* avatar */}
                          <div style={{ width: '32px', height: '32px', borderRadius: '50%', overflow: 'hidden', background: 'var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            {u.avatarUrl
                              ? <img src={u.avatarUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                              : <FontAwesomeIcon icon={faUserCircle} style={{ fontSize: '1.4rem', color: 'var(--text-muted)' }} />
                            }
                          </div>
                          {/* text */}
                          <div style={{ minWidth: 0 }}>
                            <div style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-dark)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {u.fullname || u.username || '(Không có tên)'}
                            </div>
                            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {u.email}
                            </div>
                          </div>
                          {/* remove btn */}
                          <button
                            onClick={() => removeFromList(u.userId)}
                            style={{ width: '28px', height: '28px', border: 'none', background: 'none', color: '#ef4444', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '6px' }}
                          >
                            <FontAwesomeIcon icon={faTrash} style={{ fontSize: '0.75rem' }} />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="modal-actions" style={{ marginTop: '20px' }}>
                  <button className="btn-cancel" onClick={handleClose}>Hủy</button>
                  <button
                    className="btn-send-invitation"
                    onClick={handleInvite}
                    disabled={submitting || !inviteList.length}
                  >
                    {submitting ? 'Đang gửi...' : `Gửi lời mời (${inviteList.length})`}
                    {!submitting && <FontAwesomeIcon icon={faPaperPlane} />}
                  </button>
                </div>
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

export default AddStudentModal;
