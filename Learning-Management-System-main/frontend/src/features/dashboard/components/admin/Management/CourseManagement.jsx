import React, { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import courseApi from '../../../../../services/course.api';
import identityApi from '../../../../../services/identity.api';
import { useToast } from '../../../../../components/ui/Toast';
import { buildAppErrorState, getAppErrorRoute } from '../../../../../utils/appError';
import { formatDateVN } from '../../../../../utils/dateTime';

const PAGE_SIZE = 10;

const SORT_COLUMNS = [
  { key: 'title', label: 'Khóa học' },
  { key: null, label: 'Giảng viên' },
  { key: null, label: 'Danh mục' },
  { key: 'level', label: 'Cấp độ' },
  { key: 'exerciseCount', label: 'Bài tập' },
  { key: 'updatedAt', label: 'Cập nhật' },
  { key: null, label: 'Trạng thái & Hành động' },
];

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PUBLIC', label: 'Công khai' },
  { value: 'PRIVATE', label: 'Riêng tư' },
  { value: 'LOCKED', label: 'Đã khóa' },
];

const LEVEL_OPTIONS = [
  { value: '', label: 'Tất cả cấp độ' },
  { value: 'BEGINNER', label: 'Cơ bản' },
  { value: 'INTERMEDIATE', label: 'Trung cấp' },
  { value: 'ADVANCED', label: 'Nâng cao' },
];

const STATUS_LABELS = {
  PUBLIC: 'Công khai',
  PRIVATE: 'Riêng tư',
  LOCKED: 'Đã khóa',
};

const LEVEL_LABELS = {
  BEGINNER: 'Cơ bản',
  INTERMEDIATE: 'Trung cấp',
  ADVANCED: 'Nâng cao',
};

const getPageTotal = (page) => Number(page?.totalElements || 0);

const formatDate = (value) => {
  if (!value) return '--';
  return formatDateVN(value) || '--';
};

const getStatusPillClass = (status) => {
  if (status === 'PUBLIC') return 'admin-pill-green';
  if (status === 'LOCKED') return 'admin-pill-red';
  return 'admin-pill-yellow';
};

const getInstructorName = (profile) =>
  profile?.fullname || profile?.fullName || profile?.username || profile?.email || '';

const enrichCoursesWithInstructors = async (courseList) => {
  const instructorIds = [...new Set(courseList.map((course) => course.instructorId).filter(Boolean))];
  if (instructorIds.length === 0) return courseList;

  const profileEntries = await Promise.all(
    instructorIds.map(async (instructorId) => {
      try {
        const profile = await identityApi.getPublicProfile(instructorId);
        return [instructorId, getInstructorName(profile)];
      } catch (err) {
        console.warn('Failed to load instructor profile:', instructorId, err?.response?.status);
        return [instructorId, ''];
      }
    }),
  );

  const instructorMap = Object.fromEntries(profileEntries);
  return courseList.map((course) => ({
    ...course,
    instructorName: instructorMap[course.instructorId] || course.instructorId || '--',
  }));
};

const CourseManagement = () => {
  const location = useLocation();
  const toast = useToast();
  const [courses, setCourses] = useState([]);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [level, setLevel] = useState('');
  const [appliedFilters, setAppliedFilters] = useState({});
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [summary, setSummary] = useState({ total: 0, public: 0, private: 0, locked: 0 });
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState('');
  const [error, setError] = useState(null);
  const [lockDialog, setLockDialog] = useState(null); // { course } | null
  const [lockReason, setLockReason] = useState('');
  const [sortField, setSortField] = useState('updatedAt');
  const [sortDir, setSortDir] = useState('desc');

  const fetchSummary = useCallback(async () => {
    const [allResult, publicResult, privateResult, lockedResult] = await Promise.allSettled([
      courseApi.getAdminCourses({ page: 0, size: 1 }),
      courseApi.getAdminCourses({ status: 'PUBLIC', page: 0, size: 1 }),
      courseApi.getAdminCourses({ status: 'PRIVATE', page: 0, size: 1 }),
      courseApi.getAdminCourses({ status: 'LOCKED', page: 0, size: 1 }),
    ]);

    setSummary({
      total: allResult.status === 'fulfilled' ? getPageTotal(allResult.value) : 0,
      public: publicResult.status === 'fulfilled' ? getPageTotal(publicResult.value) : 0,
      private: privateResult.status === 'fulfilled' ? getPageTotal(privateResult.value) : 0,
      locked: lockedResult.status === 'fulfilled' ? getPageTotal(lockedResult.value) : 0,
    });
  }, []);

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
    if (sortField !== field) return <span style={{ marginLeft: '4px', fontSize: '0.7rem', opacity: 0.3 }}>↕</span>;
    return <span style={{ marginLeft: '4px', fontSize: '0.7rem' }}>{sortDir === 'asc' ? '▲' : '▼'}</span>;
  };

  const fetchCourses = useCallback(async (pageIndex = 0) => {
    setLoading(true);

    try {
      const data = await courseApi.getAdminCourses({
        page: pageIndex,
        size: PAGE_SIZE,
        sort: `${sortField},${sortDir}`,
        ...appliedFilters,
      });

      const courseList = data?.content || [];
      const enrichedCourses = await enrichCoursesWithInstructors(courseList);

      setCourses(enrichedCourses);
      setTotalElements(Number(data?.totalElements || 0));
      setTotalPages(Number(data?.totalPages || 0));
      setPage(Number(data?.number ?? pageIndex));
      setError(null);
    } catch (err) {
      setError(buildAppErrorState(err, {
        title: 'Không thể tải danh sách khóa học',
        fallbackPath: '/admin/home',
      }));
      console.error('Failed to load admin courses:', err);
    } finally {
      setLoading(false);
    }
  }, [appliedFilters, sortField, sortDir]);

  useEffect(() => {
    fetchCourses(0);
    fetchSummary();
  }, [fetchCourses, fetchSummary]);

  const handleFilter = (event) => {
    event?.preventDefault();
    setAppliedFilters({
      keyword: search.trim() || undefined,
      status: status || undefined,
      level: level || undefined,
    });
  };

  const refreshCurrentData = async () => {
    await Promise.all([
      fetchCourses(page),
      fetchSummary(),
    ]);
  };

  const handleLockCourse = async (course) => {
    setLockDialog(course);
    setLockReason('');
  };

  const confirmLockCourse = async () => {
    if (!lockDialog) return;
    const trimmedReason = lockReason.trim();
    if (!trimmedReason) {
      toast.info('Vui lòng nhập lý do khóa khóa học.');
      return;
    }

    const course = lockDialog;
    setLockDialog(null);
    setLockReason('');
    setActionLoadingId(course.id);

    try {
      await courseApi.lockCourse(course.id, trimmedReason);
      await refreshCurrentData();
    } catch (err) {
      toast.error('Không thể khóa khóa học. Vui lòng thử lại.');
      console.error('Failed to lock course:', err);
    } finally {
      setActionLoadingId('');
    }
  };

  const handleUnlockCourse = async (course) => {
    setActionLoadingId(course.id);

    try {
      await courseApi.unlockCourse(course.id);
      await refreshCurrentData();
    } catch (err) {
      toast.error('Không thể mở khóa khóa học. Vui lòng thử lại.');
      console.error('Failed to unlock course:', err);
    } finally {
      setActionLoadingId('');
    }
  };

  const getCategoryNames = (course) => {
    const categories = course.categories || [];
    return categories
      .map((category) => {
        if (typeof category === 'string') return category;
        return category.name || category.title || category.slug;
      })
      .filter(Boolean);
  };

  const renderCategories = (course) => {
    const categoryNames = getCategoryNames(course);
    if (categoryNames.length === 0) return '--';
    if (categoryNames.length === 1) return categoryNames[0];

    return `${categoryNames[0]} +${categoryNames.length - 1}`;
  };

  if (error) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
  }

  return (
    <>
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Tổng khóa học</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-accent)' }}>{summary.total}</div>
          <div className="admin-stat-sub">Trong toàn hệ thống</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Công khai</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-green)' }}>{summary.public}</div>
          <div className="admin-stat-sub">Đang hiển thị cho học viên</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Riêng tư</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-yellow)' }}>{summary.private}</div>
          <div className="admin-stat-sub">Chưa công khai</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Đã khóa</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-red)' }}>{summary.locked}</div>
          <div className="admin-stat-sub neg">Cần quản trị xử lý</div>
        </div>
      </div>

      <form
        className="admin-inline-flex"
        onSubmit={handleFilter}
        style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}
      >
        <button className="admin-btn" type="submit" disabled={loading}>
          <i className="ti ti-refresh"></i> Làm mới
        </button>
        <select
          className="admin-field"
          style={{
            width: '170px',
            marginBottom: 0,
            background: 'var(--admin-card-hover)',
            border: '1px solid var(--admin-border)',
            borderRadius: '8px',
            padding: '8px 12px',
            color: 'var(--admin-text)',
          }}
          value={status}
          onChange={(event) => {
            const next = event.target.value;
            setStatus(next);
            setAppliedFilters((prev) => ({ ...prev, status: next || undefined }));
          }}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value || 'all'} value={option.value}>{option.label}</option>
          ))}
        </select>
        <select
          className="admin-field"
          style={{
            width: '160px',
            marginBottom: 0,
            background: 'var(--admin-card-hover)',
            border: '1px solid var(--admin-border)',
            borderRadius: '8px',
            padding: '8px 12px',
            color: 'var(--admin-text)',
          }}
          value={level}
          onChange={(event) => {
            const next = event.target.value;
            setLevel(next);
            setAppliedFilters((prev) => ({ ...prev, level: next || undefined }));
          }}
        >
          {LEVEL_OPTIONS.map((option) => (
            <option key={option.value || 'all'} value={option.value}>{option.label}</option>
          ))}
        </select>
        <div className="admin-search-box" style={{ width: '280px', marginLeft: 'auto' }}>
          <i className="ti ti-search"></i>
          <input
            style={{ background: 'none', border: 'none', color: 'inherit', outline: 'none', width: '100%' }}
            placeholder="Tìm tên hoặc mô tả khóa học..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </form>

      <div className="admin-card">
        <div className="admin-card-hd" style={{ justifyContent: 'space-between' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
            <i className="ti ti-books"></i>
            Danh sách khóa học ({totalElements.toLocaleString('vi-VN')})
          </span>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                {SORT_COLUMNS.map((col) => (
                  <th
                    key={col.label}
                    style={col.key ? { cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap', textAlign: col.label === 'Trạng thái & Hành động' ? 'right' : undefined } : { textAlign: col.label === 'Trạng thái & Hành động' ? 'right' : undefined }}
                    onClick={col.key ? () => handleSort(col.key) : undefined}
                  >
                    {col.label}{col.key ? sortIndicator(col.key) : null}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--admin-muted)' }}>
                    Đang tải danh sách khóa học...
                  </td>
                </tr>
              )}

              {!loading && courses.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--admin-muted)' }}>
                    Không có khóa học phù hợp.
                  </td>
                </tr>
              )}

              {!loading && courses.map((course) => (
                <tr key={course.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: '260px' }}>
                      {course.thumbnailUrl ? (
                        <img
                          src={course.thumbnailUrl}
                          alt=""
                          style={{ width: '52px', height: '36px', borderRadius: '6px', objectFit: 'cover' }}
                        />
                      ) : (
                        <span
                          style={{
                            width: '52px',
                            height: '36px',
                            borderRadius: '6px',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            background: 'rgba(88, 166, 255, 0.1)',
                            color: 'var(--admin-accent)',
                          }}
                        >
                          <i className="ti ti-book"></i>
                        </span>
                      )}
                      <div>
                        <div style={{ fontWeight: 600, maxWidth: '280px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {course.title || 'Untitled course'}
                        </div>
                        <div style={{ color: 'var(--admin-muted)', fontSize: '0.78rem', marginTop: '3px' }}>
                          {course.duration ? `${course.duration} tháng` : 'Chưa có thời lượng'} - Tạo {formatDate(course.createdAt)}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td style={{ color: 'var(--admin-muted)' }}>{course.instructorName || '--'}</td>
                  <td
                    style={{ color: 'var(--admin-muted)', maxWidth: '180px' }}
                    title={getCategoryNames(course).join(', ')}
                  >
                    {renderCategories(course)}
                  </td>
                  <td>{LEVEL_LABELS[course.level] || course.level || '--'}</td>
                  <td>{Number(course.exerciseCount || 0).toLocaleString('vi-VN')}</td>
                  <td>{formatDate(course.updatedAt)}</td>
                  <td>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '8px' }}>
                      <span className={`admin-status-pill ${getStatusPillClass(course.status)}`} style={{ margin: 0 }}>
                        {STATUS_LABELS[course.status] || course.status || '--'}
                      </span>
                      <Link
                        className="admin-btn"
                        style={{ padding: '5px 10px', fontSize: '0.75rem', textDecoration: 'none' }}
                        to={`/admin/all-courses/${course.id}`}
                      >
                        <i className="ti ti-eye"></i>
                        Xem
                      </Link>
                      {course.status === 'LOCKED' ? (
                        <button
                          className="admin-btn admin-btn-primary"
                          style={{ padding: '5px 10px', fontSize: '0.75rem' }}
                          disabled={actionLoadingId === course.id}
                          onClick={() => handleUnlockCourse(course)}
                          type="button"
                        >
                          <i className="ti ti-lock-open"></i>
                          Mở khóa
                        </button>
                      ) : (
                        <button
                          className="admin-btn admin-btn-danger"
                          style={{ padding: '5px 10px', fontSize: '0.75rem' }}
                          disabled={actionLoadingId === course.id}
                          onClick={() => handleLockCourse(course)}
                          type="button"
                        >
                          <i className="ti ti-lock"></i>
                          Khóa
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="admin-inline-flex" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
          Trang {Math.min(page + 1, Math.max(totalPages, 1))}/{Math.max(totalPages, 1)}
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className="admin-btn"
            disabled={page <= 0 || loading}
            onClick={() => fetchCourses(Math.max(page - 1, 0))}
            type="button"
          >
            <i className="ti ti-chevron-left"></i> Trước
          </button>
          <button
            className="admin-btn"
            disabled={loading || totalPages === 0 || page >= totalPages - 1}
            onClick={() => fetchCourses(page + 1)}
            type="button"
          >
            Sau <i className="ti ti-chevron-right"></i>
          </button>
        </div>
      </div>
    </motion.div>

    {/* Lock course dialog */}
      {lockDialog && (
        <div className="confirm-overlay" onClick={() => setLockDialog(null)}>
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="confirm-icon">
              <i className="ti ti-lock"></i>
            </div>
            <h3>Khóa khóa học</h3>
            <p>Nhập lý do khóa "<strong>{lockDialog.title || lockDialog.id}</strong>":</p>
            <input
              className="lock-reason-input"
              type="text"
              placeholder="Nhập lý do..."
              value={lockReason}
              onChange={(e) => setLockReason(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') confirmLockCourse(); }}
              autoFocus
            />
            <div className="confirm-actions">
              <button className="confirm-btn cancel" onClick={() => setLockDialog(null)}>Hủy</button>
              <button className="confirm-btn delete" onClick={confirmLockCourse}>Khóa</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default CourseManagement;
