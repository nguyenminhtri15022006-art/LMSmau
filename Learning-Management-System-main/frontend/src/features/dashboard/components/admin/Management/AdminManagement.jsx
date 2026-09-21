import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { useToast } from '../../../../../components/ui/Toast';
import useAuth from '../../../../../hooks/useAuth';
import dashboardApi from '../../../../../services/dashboard.api';
import { formatDateVN } from '../../../../../utils/dateTime';
import UserInfoModal from '../../../../../components/ui/UserInfoModal';

const PAGE_SIZE = 10;

const SORT_COLUMNS = [
  { key: 'fullname', label: 'Quản trị viên' },
  { key: 'email', label: 'Email' },
  { key: 'createdAt', label: 'Ngày tạo' },
  { key: 'active', label: 'Trạng thái' },
  { key: null, label: 'Hành động' },
];

const selectStyle = {
  background: 'var(--admin-card-hover)',
  border: '1px solid var(--admin-border)',
  borderRadius: '8px',
  padding: '8px 12px',
  color: 'var(--admin-text)',
  fontSize: '0.85rem',
  cursor: 'pointer',
};

const inputStyle = {
  width: '100%',
  padding: '8px 12px',
  borderRadius: '8px',
  border: '1px solid var(--admin-border)',
  background: 'var(--admin-card-hover)',
  color: 'var(--admin-text)',
  fontSize: '0.85rem',
  marginBottom: '2px',
};

const fieldStyle = { marginBottom: '14px' };
const labelStyle = { display: 'block', marginBottom: '4px', fontSize: '0.82rem', color: 'var(--admin-muted)' };
const errorStyle = { color: 'var(--admin-red)', fontSize: '0.75rem', marginTop: '2px' };

const EMPTY_CREATE = { username: '', password: '', fullname: '', email: '', phone: '' };
const EMPTY_EDIT = { fullname: '', phone: '', dob: '', bio: '' };

const toDateInputValue = (dob) => {
  if (!dob) return '';
  try { return new Date(dob).toISOString().split('T')[0]; } catch { return ''; }
};
const EMPTY_PWD = { newPassword: '', confirmPassword: '' };

const getInitials = (name) => {
  if (!name) return 'AD';
  return name.trim().split(/\s+/).slice(-2).map((p) => p[0]?.toUpperCase() || '').join('');
};

const formatDate = (value) => {
  if (!value) return '—';
  return formatDateVN(value) || '—';
};

const validate = {
  username: (v) => {
    if (!v.trim()) return 'Tên đăng nhập không được để trống';
    if (v.trim().length < 5) return 'Tối thiểu 5 ký tự';
    if (!/^[a-zA-Z0-9_]+$/.test(v.trim())) return 'Chỉ chứa chữ cái, số và dấu gạch dưới';
    return '';
  },
  password: (v) => {
    if (!v) return 'Mật khẩu không được để trống';
    if (v.length < 8) return 'Tối thiểu 8 ký tự';
    if (/\s/.test(v)) return 'Không được chứa khoảng trắng';
    return '';
  },
  fullname: (v) => {
    if (!v.trim()) return 'Họ tên không được để trống';
    if (v.trim().length < 3) return 'Tối thiểu 3 ký tự';
    return '';
  },
  email: (v) => {
    if (!v.trim()) return 'Email không được để trống';
    if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(v.trim())) return 'Email không đúng định dạng';
    return '';
  },
  phone: (v) => {
    if (!v) return '';
    if (!/^0[0-9]{9}$/.test(v)) return 'SĐT phải 10 số, bắt đầu bằng 0';
    return '';
  },
};

const Overlay = ({ children, onClose }) => (
  <div
    style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px' }}
    onClick={onClose}
  >
    <div
      style={{ background: 'var(--admin-card)', borderRadius: '12px', padding: '28px', width: '100%', maxWidth: '480px', boxShadow: '0 20px 60px rgba(0,0,0,0.4)', border: '1px solid var(--admin-border)' }}
      onClick={(e) => e.stopPropagation()}
    >
      {children}
    </div>
  </div>
);

const ModalHeader = ({ icon, title, onClose }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
    <h3 style={{ margin: 0, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
      <i className={`ti ${icon}`} style={{ color: 'var(--admin-accent)' }}></i>
      {title}
    </h3>
    <button className="admin-btn" style={{ padding: '4px 8px' }} onClick={onClose}>
      <i className="ti ti-x"></i>
    </button>
  </div>
);

const AdminManagement = () => {
  const toast = useToast();
  const { user: currentUser, updateUser } = useAuth();
  const currentUserId = currentUser?.userId || currentUser?.id || '';

  const [admins, setAdmins] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [sortField, setSortField] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');

  const [createModal, setCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState(EMPTY_CREATE);
  const [createErrors, setCreateErrors] = useState({});
  const [createLoading, setCreateLoading] = useState(false);

  const [editTarget, setEditTarget] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_EDIT);
  const [editErrors, setEditErrors] = useState({});
  const [editLoading, setEditLoading] = useState(false);
  const [editFetching, setEditFetching] = useState(false);

  const [pwdTarget, setPwdTarget] = useState(null);
  const [pwdForm, setPwdForm] = useState(EMPTY_PWD);
  const [pwdErrors, setPwdErrors] = useState({});
  const [pwdLoading, setPwdLoading] = useState(false);

  const [toggleLoadingId, setToggleLoadingId] = useState('');
  const [viewUser, setViewUser] = useState(null);
  const [viewLoading, setViewLoading] = useState(false);

  const fetchAdmins = useCallback(async (pageIndex = 0, keyword = search) => {
    setLoading(true);
    try {
      const data = await dashboardApi.getUsers({
        page: pageIndex,
        size: PAGE_SIZE,
        role: 'ADMIN',
        keyword: keyword.trim() || undefined,
      });
      setAdmins(data?.content || []);
      setTotalElements(Number(data?.totalElements || 0));
      setTotalPages(Number(data?.totalPages || 0));
      setPage(Number(data?.number ?? pageIndex));
    } catch {
      toast.error('Không thể tải danh sách quản trị viên.');
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchAdmins(0);
  }, [fetchAdmins]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchAdmins(0, search);
  };

  const handleSort = (field) => {
    if (!field) return;
    setSortDir((d) => (sortField === field ? (d === 'asc' ? 'desc' : 'asc') : 'asc'));
    setSortField(field);
  };

  const sortIndicator = (field) => {
    if (sortField !== field) return <span style={{ marginLeft: '4px', opacity: 0.3, fontSize: '0.7rem' }}>↕</span>;
    return <span style={{ marginLeft: '4px', fontSize: '0.7rem' }}>{sortDir === 'asc' ? '▲' : '▼'}</span>;
  };

  const displayedAdmins = useMemo(() => {
    const result = [...admins];
    if (sortField) {
      result.sort((a, b) => {
        let va = a[sortField], vb = b[sortField];
        if (sortField === 'fullname') { va = (a.fullname || a.username || '').toLowerCase(); vb = (b.fullname || b.username || '').toLowerCase(); }
        else if (sortField === 'email') { va = (a.email || '').toLowerCase(); vb = (b.email || '').toLowerCase(); }
        else if (sortField === 'createdAt') { va = new Date(a.createdAt || 0).getTime(); vb = new Date(b.createdAt || 0).getTime(); }
        else if (sortField === 'active') { va = a.active ? 1 : 0; vb = b.active ? 1 : 0; }
        if (va < vb) return sortDir === 'asc' ? -1 : 1;
        if (va > vb) return sortDir === 'asc' ? 1 : -1;
        return 0;
      });
    }
    return result;
  }, [admins, sortField, sortDir]);

  // ─── View ─────────────────────────────────────────────
  const handleViewAdmin = async (admin) => {
    setViewUser({});
    setViewLoading(true);
    try {
      const data = await dashboardApi.getUser(admin.userId);
      setViewUser(data);
    } catch {
      setViewUser(null);
    } finally {
      setViewLoading(false);
    }
  };

  // ─── Create ───────────────────────────────────────────
  const openCreate = () => { setCreateForm(EMPTY_CREATE); setCreateErrors({}); setCreateModal(true); };
  const closeCreate = () => { setCreateModal(false); };

  const validateCreate = () => {
    const e = {
      username: validate.username(createForm.username),
      password: validate.password(createForm.password),
      fullname: validate.fullname(createForm.fullname),
      email: validate.email(createForm.email),
      phone: validate.phone(createForm.phone),
    };
    setCreateErrors(e);
    return !Object.values(e).some(Boolean);
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!validateCreate()) return;
    setCreateLoading(true);
    try {
      const newAdmin = await dashboardApi.createAdmin({
        username: createForm.username.trim(),
        password: createForm.password,
        fullname: createForm.fullname.trim(),
        email: createForm.email.trim(),
        phone: createForm.phone.trim() || undefined,
      });
      setAdmins((prev) => [newAdmin, ...prev]);
      setTotalElements((n) => n + 1);
      closeCreate();
      toast.success('Tạo tài khoản admin thành công!');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể tạo tài khoản. Vui lòng thử lại.';
      setCreateErrors((prev) => ({ ...prev, _api: msg }));
    } finally {
      setCreateLoading(false);
    }
  };

  // ─── Edit ─────────────────────────────────────────────
  const openEdit = async (admin) => {
    setEditTarget(admin);
    setEditForm({ fullname: admin.fullname || '', phone: admin.phone || '', dob: '', bio: '' });
    setEditErrors({});
    setEditFetching(true);
    try {
      const full = await dashboardApi.getUser(admin.userId);
      setEditForm({
        fullname: full?.fullname || admin.fullname || '',
        phone: full?.phone || admin.phone || '',
        dob: toDateInputValue(full?.dob),
        bio: full?.bio || '',
      });
    } catch {
      // Dùng dữ liệu summary nếu fetch thất bại
    } finally {
      setEditFetching(false);
    }
  };
  const closeEdit = () => setEditTarget(null);

  const validateEdit = () => {
    const e = {
      fullname: validate.fullname(editForm.fullname),
      phone: validate.phone(editForm.phone),
    };
    setEditErrors(e);
    return !Object.values(e).some(Boolean);
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    if (!validateEdit()) return;
    setEditLoading(true);
    try {
      const updated = await dashboardApi.updateUser(editTarget.userId, {
        fullname: editForm.fullname.trim(),
        phone: editForm.phone.trim() || undefined,
        dob: editForm.dob || undefined,
        bio: editForm.bio.trim() || undefined,
      });
      setAdmins((prev) => prev.map((a) => (a.userId === editTarget.userId ? { ...a, ...updated } : a)));
      if (editTarget.userId === currentUserId) {
        updateUser({ ...currentUser, ...updated });
      }
      closeEdit();
      toast.success('Cập nhật thông tin thành công!');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể cập nhật. Vui lòng thử lại.';
      setEditErrors((prev) => ({ ...prev, _api: msg }));
    } finally {
      setEditLoading(false);
    }
  };

  // ─── Password Reset ────────────────────────────────────
  const openPwd = (admin) => { setPwdTarget(admin); setPwdForm(EMPTY_PWD); setPwdErrors({}); };
  const closePwd = () => setPwdTarget(null);

  const validatePwd = () => {
    const e = {
      newPassword: validate.password(pwdForm.newPassword),
      confirmPassword: pwdForm.confirmPassword !== pwdForm.newPassword ? 'Mật khẩu xác nhận không khớp' : '',
    };
    setPwdErrors(e);
    return !Object.values(e).some(Boolean);
  };

  const handlePwd = async (e) => {
    e.preventDefault();
    if (!validatePwd()) return;
    setPwdLoading(true);
    try {
      await dashboardApi.resetUserPassword(pwdTarget.userId, pwdForm.newPassword);
      closePwd();
      toast.success('Đổi mật khẩu thành công!');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể đổi mật khẩu. Vui lòng thử lại.';
      setPwdErrors((prev) => ({ ...prev, _api: msg }));
    } finally {
      setPwdLoading(false);
    }
  };

  // ─── Toggle Active ─────────────────────────────────────
  const handleToggle = async (admin) => {
    if (admin.userId === currentUserId) return;
    setToggleLoadingId(admin.userId);
    try {
      const updated = await dashboardApi.toggleAdminActive(admin.userId);
      setAdmins((prev) => prev.map((a) => (a.userId === admin.userId ? { ...a, ...updated } : a)));
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể cập nhật trạng thái.';
      toast.error(msg);
    } finally {
      setToggleLoadingId('');
    }
  };

  return (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.3 }}>

      {/* Toolbar */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <button className="admin-btn admin-btn-primary" onClick={openCreate}>
          <i className="ti ti-plus"></i> Thêm Admin mới
        </button>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '8px', marginLeft: 'auto' }}>
          <div className="admin-search-box" style={{ width: '260px' }}>
            <i className="ti ti-search"></i>
            <input
              style={{ background: 'none', border: 'none', color: 'inherit', outline: 'none', width: '100%' }}
              placeholder="Tìm tên, username, email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <button type="submit" className="admin-btn" disabled={loading}>Tìm</button>
        </form>
      </div>

      {/* Table */}
      <div className="admin-card">
        <div className="admin-card-hd">
          <i className="ti ti-shield"></i>
          Danh sách Quản trị viên ({totalElements.toLocaleString()})
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                {SORT_COLUMNS.map((col) => (
                  <th
                    key={col.label}
                    style={col.key ? { cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' } : undefined}
                    onClick={col.key ? () => handleSort(col.key) : undefined}
                  >
                    {col.label}{col.key ? sortIndicator(col.key) : null}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={5} style={{ color: 'var(--admin-muted)', textAlign: 'center', padding: '32px' }}>Đang tải...</td></tr>
              )}
              {!loading && displayedAdmins.length === 0 && (
                <tr><td colSpan={5} style={{ color: 'var(--admin-muted)', textAlign: 'center', padding: '32px' }}>Không tìm thấy quản trị viên nào.</td></tr>
              )}
              {!loading && displayedAdmins.map((admin) => {
                const isSelf = admin.userId === currentUserId;
                return (
                  <tr key={admin.userId}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <span className="admin-avatar" style={{ flexShrink: 0 }}>
                          {getInitials(admin.fullname || admin.username)}
                        </span>
                        <div>
                          <div style={{ fontWeight: 600 }}>{admin.fullname || admin.username}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--admin-muted)' }}>@{admin.username}</div>
                        </div>
                        {isSelf && (
                          <span className="admin-status-pill admin-pill-yellow" style={{ fontSize: '0.65rem', padding: '2px 7px' }}>Bạn</span>
                        )}
                      </div>
                    </td>
                    <td style={{ color: 'var(--admin-muted)' }}>{admin.email || '—'}</td>
                    <td style={{ color: 'var(--admin-muted)' }}>{formatDate(admin.createdAt)}</td>
                    <td>
                      <span className={`admin-status-pill ${admin.active ? 'admin-pill-green' : 'admin-pill-red'}`}>
                        {admin.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                        <button
                          className="admin-btn"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          title="Xem thông tin"
                          onClick={() => handleViewAdmin(admin)}
                        >
                          <i className="ti ti-eye"></i>
                        </button>
                        <button
                          className="admin-btn"
                          style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                          onClick={() => openEdit(admin)}
                        >
                          <i className="ti ti-edit"></i> Sửa
                        </button>
                        <button
                          className="admin-btn"
                          style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                          onClick={() => openPwd(admin)}
                        >
                          <i className="ti ti-key"></i> Mật khẩu
                        </button>
                        {!isSelf && (
                          <button
                            className={`admin-btn ${admin.active ? 'admin-btn-danger' : 'admin-btn-primary'}`}
                            style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                            disabled={toggleLoadingId === admin.userId}
                            onClick={() => handleToggle(admin)}
                          >
                            <i className={`ti ${admin.active ? 'ti-lock' : 'ti-lock-open'}`}></i>
                            {admin.active ? ' Khóa' : ' Kích hoạt'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
        <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
          Trang {Math.min(page + 1, Math.max(totalPages, 1))}/{Math.max(totalPages, 1)}
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="admin-btn" disabled={page <= 0 || loading} onClick={() => fetchAdmins(page - 1)}>
            <i className="ti ti-chevron-left"></i> Trước
          </button>
          <button className="admin-btn" disabled={loading || totalPages === 0 || page >= totalPages - 1} onClick={() => fetchAdmins(page + 1)}>
            Sau <i className="ti ti-chevron-right"></i>
          </button>
        </div>
      </div>

      {/* ─── Create Modal ─── */}
      {createModal && (
        <Overlay onClose={closeCreate}>
          <ModalHeader icon="ti-shield-plus" title="Thêm Quản trị viên mới" onClose={closeCreate} />
          <form onSubmit={handleCreate}>
            {createErrors._api && (
              <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px', padding: '10px 14px', marginBottom: '14px', color: 'var(--admin-red)', fontSize: '0.82rem' }}>
                {createErrors._api}
              </div>
            )}
            <div style={fieldStyle}>
              <label style={labelStyle}>Tên đăng nhập *</label>
              <input style={inputStyle} placeholder="VD: admin_nguyen (min 5 ký tự)" value={createForm.username}
                onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))} />
              {createErrors.username && <div style={errorStyle}>{createErrors.username}</div>}
            </div>
            <div style={fieldStyle}>
              <label style={labelStyle}>Mật khẩu *</label>
              <input style={inputStyle} type="password" placeholder="Tối thiểu 8 ký tự" value={createForm.password}
                onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))} />
              {createErrors.password && <div style={errorStyle}>{createErrors.password}</div>}
            </div>
            <div style={fieldStyle}>
              <label style={labelStyle}>Họ và tên *</label>
              <input style={inputStyle} placeholder="VD: Nguyễn Văn A" value={createForm.fullname}
                onChange={(e) => setCreateForm((f) => ({ ...f, fullname: e.target.value }))} />
              {createErrors.fullname && <div style={errorStyle}>{createErrors.fullname}</div>}
            </div>
            <div style={fieldStyle}>
              <label style={labelStyle}>Email *</label>
              <input style={inputStyle} type="email" placeholder="VD: admin@lms.vn" value={createForm.email}
                onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))} />
              {createErrors.email && <div style={errorStyle}>{createErrors.email}</div>}
            </div>
            <div style={fieldStyle}>
              <label style={labelStyle}>Số điện thoại (tuỳ chọn)</label>
              <input style={inputStyle} placeholder="VD: 0912345678" value={createForm.phone}
                onChange={(e) => setCreateForm((f) => ({ ...f, phone: e.target.value }))} />
              {createErrors.phone && <div style={errorStyle}>{createErrors.phone}</div>}
            </div>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '4px' }}>
              <button type="button" className="admin-btn" onClick={closeCreate}>Hủy</button>
              <button type="submit" className="admin-btn admin-btn-primary" disabled={createLoading}>
                {createLoading ? 'Đang tạo...' : 'Tạo tài khoản'}
              </button>
            </div>
          </form>
        </Overlay>
      )}

      {/* ─── Edit Modal ─── */}
      {editTarget && (
        <Overlay onClose={closeEdit}>
          <ModalHeader icon="ti-edit" title={`Sửa thông tin: ${editTarget.username}`} onClose={closeEdit} />
          {editFetching ? (
            <div style={{ textAlign: 'center', padding: '32px', color: 'var(--admin-muted)' }}>
              <i className="ti ti-loader" style={{ fontSize: '1.5rem' }}></i>
              <div style={{ marginTop: '8px', fontSize: '0.85rem' }}>Đang tải thông tin...</div>
            </div>
          ) : (
            <form onSubmit={handleEdit}>
              {editErrors._api && (
                <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px', padding: '10px 14px', marginBottom: '14px', color: 'var(--admin-red)', fontSize: '0.82rem' }}>
                  {editErrors._api}
                </div>
              )}
              <div style={fieldStyle}>
                <label style={labelStyle}>Họ và tên *</label>
                <input style={inputStyle} placeholder="VD: Nguyễn Văn A" value={editForm.fullname}
                  onChange={(e) => setEditForm((f) => ({ ...f, fullname: e.target.value }))} />
                {editErrors.fullname && <div style={errorStyle}>{editErrors.fullname}</div>}
              </div>
              <div style={{ display: 'flex', gap: '12px' }}>
                <div style={{ ...fieldStyle, flex: 1 }}>
                  <label style={labelStyle}>Số điện thoại</label>
                  <input style={inputStyle} placeholder="0912345678" value={editForm.phone}
                    onChange={(e) => setEditForm((f) => ({ ...f, phone: e.target.value }))} />
                  {editErrors.phone && <div style={errorStyle}>{editErrors.phone}</div>}
                </div>
                <div style={{ ...fieldStyle, flex: 1 }}>
                  <label style={labelStyle}>Ngày sinh</label>
                  <input style={inputStyle} type="date" value={editForm.dob}
                    max={new Date(new Date().setFullYear(new Date().getFullYear() - 6)).toISOString().split('T')[0]}
                    onChange={(e) => setEditForm((f) => ({ ...f, dob: e.target.value }))} />
                </div>
              </div>
              <div style={fieldStyle}>
                <label style={labelStyle}>Giới thiệu bản thân</label>
                <textarea
                  style={{ ...inputStyle, resize: 'vertical', minHeight: '72px' }}
                  placeholder="Mô tả ngắn về quản trị viên..."
                  value={editForm.bio}
                  onChange={(e) => setEditForm((f) => ({ ...f, bio: e.target.value }))}
                />
              </div>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '4px' }}>
                <button type="button" className="admin-btn" onClick={closeEdit}>Hủy</button>
                <button type="submit" className="admin-btn admin-btn-primary" disabled={editLoading}>
                  {editLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          )}
        </Overlay>
      )}

      {/* ─── View Info Modal ─── */}
      {(viewUser || viewLoading) && (
        <UserInfoModal
          user={viewUser}
          loading={viewLoading}
          onClose={() => setViewUser(null)}
        />
      )}

      {/* ─── Password Reset Modal ─── */}
      {pwdTarget && (
        <Overlay onClose={closePwd}>
          <ModalHeader icon="ti-key" title={`Đổi mật khẩu: ${pwdTarget.username}`} onClose={closePwd} />
          <form onSubmit={handlePwd}>
            {pwdErrors._api && (
              <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px', padding: '10px 14px', marginBottom: '14px', color: 'var(--admin-red)', fontSize: '0.82rem' }}>
                {pwdErrors._api}
              </div>
            )}
            <div style={fieldStyle}>
              <label style={labelStyle}>Mật khẩu mới *</label>
              <input style={inputStyle} type="password" placeholder="Tối thiểu 8 ký tự" value={pwdForm.newPassword}
                onChange={(e) => setPwdForm((f) => ({ ...f, newPassword: e.target.value }))} />
              {pwdErrors.newPassword && <div style={errorStyle}>{pwdErrors.newPassword}</div>}
            </div>
            <div style={fieldStyle}>
              <label style={labelStyle}>Xác nhận mật khẩu *</label>
              <input style={inputStyle} type="password" placeholder="Nhập lại mật khẩu mới" value={pwdForm.confirmPassword}
                onChange={(e) => setPwdForm((f) => ({ ...f, confirmPassword: e.target.value }))} />
              {pwdErrors.confirmPassword && <div style={errorStyle}>{pwdErrors.confirmPassword}</div>}
            </div>
<div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button type="button" className="admin-btn" onClick={closePwd}>Hủy</button>
              <button type="submit" className="admin-btn admin-btn-danger" disabled={pwdLoading}>
                {pwdLoading ? 'Đang đổi...' : 'Xác nhận đổi mật khẩu'}
              </button>
            </div>
          </form>
        </Overlay>
      )}
    </motion.div>
  );
};

export default AdminManagement;
