import React, { useEffect, useState, useMemo } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import dashboardApi from '../../../../../services/dashboard.api';
import { useToast } from '../../../../../components/ui/Toast';
import { buildAppErrorState, getAppErrorRoute } from '../../../../../utils/appError';
import { formatDateVN } from '../../../../../utils/dateTime';
import UserInfoModal from '../../../../../components/ui/UserInfoModal';

const SORT_COLUMNS = [
  { key: 'fullname', label: 'Người dùng' },
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Vai trò' },
  { key: null, label: 'OAuth' },
  { key: 'createdAt', label: 'Ngày tham gia' },
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

const thStyle = {
  cursor: 'pointer',
  userSelect: 'none',
  whiteSpace: 'nowrap',
};

const UserManagement = () => {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterDateRange, setFilterDateRange] = useState('');
  const [sortField, setSortField] = useState('');
  const [sortDir, setSortDir] = useState('asc');
  const [allUsers, setAllUsers] = useState([]);
  const [viewUser, setViewUser] = useState(null);
  const [viewLoading, setViewLoading] = useState(false);
  const [totalAdmins, setTotalAdmins] = useState(0);
  const pageSize = 10;
  const location = useLocation();

  const hasClientFilter = filterStatus !== '' || filterDateRange !== '';

  const fetchUsers = async (pageIndex = page, selectedRole = role, keyword = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await dashboardApi.getUsers({
        page: pageIndex,
        size: pageSize,
        role: selectedRole || undefined,
        keyword: keyword.trim() || undefined,
      });
      setUsers(data?.content || []);
      setTotalElements(Number(data?.totalElements || 0));
      setTotalPages(Number(data?.totalPages || 0));
      setPage(Number(data?.number || pageIndex));
    } catch (err) {
      setError(buildAppErrorState(err, {
        title: 'Không thể tải danh sách người dùng',
        fallbackPath: '/admin/home',
      }));
      console.error('Failed to load users:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchAllUsers = async (selectedRole = role, keyword = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await dashboardApi.getUsers({
        page: 0,
        size: 1000,
        role: selectedRole || undefined,
        keyword: keyword.trim() || undefined,
      });
      setAllUsers(data?.content || []);
      setPage(0);
    } catch (err) {
      setError(buildAppErrorState(err, {
        title: 'Không thể tải danh sách người dùng',
        fallbackPath: '/admin/home',
      }));
      console.error('Failed to load users:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    dashboardApi.getUsers({ page: 0, size: 1, role: 'ADMIN' })
      .then((data) => setTotalAdmins(Number(data?.totalElements || 0)))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (hasClientFilter) {
      fetchAllUsers(role, search);
    } else {
      fetchUsers(0, role, search);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasClientFilter, role]);

  const handleFilter = () => {
    if (hasClientFilter) {
      fetchAllUsers(role, search);
    } else {
      fetchUsers(0, role, search);
    }
  };

  const handleViewUser = async (userId) => {
    setViewUser({});
    setViewLoading(true);
    try {
      const data = await dashboardApi.getUser(userId);
      setViewUser(data);
    } catch {
      setViewUser(null);
    } finally {
      setViewLoading(false);
    }
  };

  const handleToggleActive = async (userId) => {
    try {
      const updatedUser = await dashboardApi.toggleUserActive(userId);
      const updater = (list) =>
        list.map((item) => (item.userId === userId ? { ...item, ...updatedUser } : item));
      setUsers(updater);
      setAllUsers(updater);
    } catch (err) {
      console.error('Failed to toggle user status:', err);
      toast.error('Không thể cập nhật trạng thái người dùng. Vui lòng thử lại.');
    }
  };

  const rolePillClass = (userRole) => {
    if (userRole === 'ADMIN') return 'admin-pill-yellow';
    if (userRole === 'INSTRUCTOR') return '';
    return 'admin-pill-blue';
  };

  const formatDate = (value) => {
    if (!value) return '—';
    return formatDateVN(value) || '—';
  };

  const getInitials = (name) => {
    if (!name) return 'NA';
    return name
      .trim()
      .split(/\s+/)
      .slice(-2)
      .map((part) => part[0]?.toUpperCase() || '')
      .join('');
  };

  const handleSort = (field) => {
    if (!field) return;
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const sortIndicator = (field) => {
    if (sortField !== field) return null;
    return <span style={{ marginLeft: '4px', fontSize: '0.7rem' }}>{sortDir === 'asc' ? '▲' : '▼'}</span>;
  };

  const isDateInRange = (dateStr, range) => {
    if (!range || !dateStr) return true;
    const d = new Date(dateStr);
    const now = new Date();
    if (range === 'today') {
      return d.toDateString() === now.toDateString();
    }
    if (range === '3d') {
      return (now - d) <= 3 * 24 * 60 * 60 * 1000;
    }
    if (range === '7d') {
      return (now - d) <= 7 * 24 * 60 * 60 * 1000;
    }
    if (range === '3m') {
      return (now - d) <= 90 * 24 * 60 * 60 * 1000;
    }
    if (range === '30d') {
      return (now - d) <= 30 * 24 * 60 * 60 * 1000;
    }
    return true;
  };

  const { displayedUsers, clientTotalPages, clientTotalElements } = useMemo(() => {
    const base = hasClientFilter ? allUsers : users;
    let result = [...base].filter((u) => u.role !== 'ADMIN');

    if (filterStatus) {
      const isActive = filterStatus === 'active';
      result = result.filter((u) => u.active === isActive);
    }
    if (filterDateRange) {
      result = result.filter((u) => isDateInRange(u.createdAt, filterDateRange));
    }

    if (sortField) {
      result.sort((a, b) => {
        let va = a[sortField];
        let vb = b[sortField];
        if (sortField === 'fullname') {
          va = (a.fullname || a.username || '').toLowerCase();
          vb = (b.fullname || b.username || '').toLowerCase();
        } else if (sortField === 'role') {
          va = (a.role || '').toLowerCase();
          vb = (b.role || '').toLowerCase();
        } else if (sortField === 'createdAt') {
          va = new Date(a.createdAt || 0).getTime();
          vb = new Date(b.createdAt || 0).getTime();
        } else if (sortField === 'active') {
          va = a.active ? 1 : 0;
          vb = b.active ? 1 : 0;
        }
        if (va < vb) return sortDir === 'asc' ? -1 : 1;
        if (va > vb) return sortDir === 'asc' ? 1 : -1;
        return 0;
      });
    }

    if (hasClientFilter) {
      const cTotalElements = result.length;
      const cTotalPages = Math.ceil(cTotalElements / pageSize);
      const paginated = result.slice(page * pageSize, (page + 1) * pageSize);
      return { displayedUsers: paginated, clientTotalPages: cTotalPages, clientTotalElements: cTotalElements };
    }

    return { displayedUsers: result, clientTotalPages: 0, clientTotalElements: 0 };
  }, [users, allUsers, hasClientFilter, filterStatus, filterDateRange, sortField, sortDir, page]);

  return (
    error ? <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} /> :
    <>
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <button className="admin-btn" onClick={handleFilter}><i className="ti ti-refresh"></i> Làm mới</button>
        <select
          style={{ ...selectStyle, width: '170px' }}
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Tất cả vai trò</option>
          <option value="STUDENT">STUDENT</option>
          <option value="INSTRUCTOR">INSTRUCTOR</option>
        </select>

        <select
          style={selectStyle}
          value={filterStatus}
          onChange={(e) => {
            setFilterStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Tất cả trạng thái</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
        </select>
        <select
          style={selectStyle}
          value={filterDateRange}
          onChange={(e) => {
            setFilterDateRange(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Tất cả ngày</option>
          <option value="today">Hôm nay</option>
          <option value="3d">3 ngày</option>
          <option value="7d">7 ngày</option>
          <option value="30d">30 ngày</option>
          <option value="3m">3 tháng</option>
        </select>

        <div style={{ marginLeft: 'auto' }}>
          <div className="admin-search-box" style={{ width: '250px' }}>
            <i className="ti ti-search"></i>
            <input
              style={{ background: 'none', border: 'none', color: 'inherit', outline: 'none', width: '100%' }}
              placeholder="Tìm tên, username, email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleFilter();
              }}
            />
          </div>
        </div>
      </div>

      <div className="admin-card">
        <div className="admin-card-hd">
          <i className="ti ti-users"></i>
          Danh sách người dùng ({(hasClientFilter ? clientTotalElements : (role ? totalElements : Math.max(0, totalElements - totalAdmins))).toLocaleString()})
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                {SORT_COLUMNS.map((col) => (
                  <th
                    key={col.label}
                    style={col.key ? thStyle : undefined}
                    onClick={col.key ? () => handleSort(col.key) : undefined}
                  >
                    {col.label}{sortIndicator(col.key)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={7} style={{ color: 'var(--admin-muted)' }}>Đang tải dữ liệu...</td>
                </tr>
              )}
              {!loading && !error && displayedUsers.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ color: 'var(--admin-muted)' }}>Không có người dùng phù hợp.</td>
                </tr>
              )}
              {!loading && !error && displayedUsers.map((user) => (
                <tr key={user.userId}>
                  <td>
                    <span className="admin-avatar">{getInitials(user.fullname || user.username)}</span>
                    {user.fullname || user.username || 'N/A'}
                  </td>
                  <td style={{ color: 'var(--admin-muted)' }}>{user.email || '—'}</td>
                  <td>
                    <span
                      className={`admin-status-pill ${rolePillClass(user.role)}`}
                      style={user.role === 'INSTRUCTOR'
                        ? { background: 'rgba(188, 140, 255, 0.1)', color: 'var(--admin-purple)', border: '1px solid rgba(188, 140, 255, 0.2)' }
                        : undefined}
                    >
                      {user.role || '—'}
                    </span>
                  </td>
                  <td>{user.authProvider || 'LOCAL'}</td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td>
                    <span className={`admin-status-pill ${user.active ? 'admin-pill-green' : 'admin-pill-red'}`}>
                      {user.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        className="admin-btn"
                        style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                        onClick={() => handleViewUser(user.userId)}
                      >
                        <i className="ti ti-eye"></i> Xem
                      </button>
                      <button
                        className={`admin-btn ${user.active ? 'admin-btn-danger' : 'admin-btn-primary'}`}
                        style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                        onClick={() => handleToggleActive(user.userId)}
                      >
                        <i className={`ti ${user.active ? 'ti-lock' : 'ti-lock-open'}`}></i>
                        {user.active ? 'Khóa' : 'Kích hoạt'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="admin-inline-flex" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        {hasClientFilter ? (
          <>
            <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
              {clientTotalElements} kết quả · Trang {Math.min(page + 1, Math.max(clientTotalPages, 1))}/{Math.max(clientTotalPages, 1)}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                className="admin-btn"
                disabled={page <= 0 || loading}
                onClick={() => setPage((p) => p - 1)}
              >
                <i className="ti ti-chevron-left"></i> Trước
              </button>
              <button
                className="admin-btn"
                disabled={loading || clientTotalPages === 0 || page >= clientTotalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Sau <i className="ti ti-chevron-right"></i>
              </button>
            </div>
          </>
        ) : (
          <>
            <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
              Trang {Math.min(page + 1, Math.max(totalPages, 1))}/{Math.max(totalPages, 1)}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                className="admin-btn"
                disabled={page <= 0 || loading}
                onClick={() => fetchUsers(Math.max(page - 1, 0), role, search)}
              >
                <i className="ti ti-chevron-left"></i> Trước
              </button>
              <button
                className="admin-btn"
                disabled={loading || totalPages === 0 || page >= totalPages - 1}
                onClick={() => fetchUsers(page + 1, role, search)}
              >
                Sau <i className="ti ti-chevron-right"></i>
              </button>
            </div>
          </>
        )}
      </div>
    </motion.div>
    {(viewUser || viewLoading) && (
      <UserInfoModal
        user={viewUser}
        loading={viewLoading}
        onClose={() => setViewUser(null)}
      />
    )}
    </>
  );
};

export default UserManagement;
