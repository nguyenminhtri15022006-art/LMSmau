import React, { useEffect, useState } from 'react';
import { useParams, Link, useLocation, useSearchParams } from 'react-router-dom';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import assignmentApi from '../../../../../services/assignment.api';
import courseApi from '../../../../../services/course.api';
import { formatDateTimeVN } from '../../../../../utils/dateTime';
import '../../../shared/styles/ExerciseShared.css';
import '../../styles/teacher/InstructorExerciseDetail/instructorExerciseDetail.css';
import '../../styles/teacher/InstructorExerciseDetail/exerciseTable.css';
import '../../styles/teacher/ExerciseResultsPage/exerciseResultsPage.css';

const PAGE_SIZE = 10;

function normalizeType(value) {
  return value === 'coding' ? 'coding' : 'quiz';
}

function formatDate(value) {
  if (!value) return '-';
  return formatDateTimeVN(value) || '-';
}

function statusClass(status) {
  const normalized = String(status || '').toLowerCase().replaceAll('_', '-');
  if (normalized.includes('accepted') || normalized.includes('submitted')) return 'published';
  if (normalized.includes('pending') || normalized.includes('judging')) return 'draft';
  return 'draft';
}

function getCourseName(course) {
  return course?.title || course?.name || course?.courseTitle || course?.courseName || '';
}

function escapeCsvValue(value) {
  const text = value == null ? '' : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

function downloadCsv(filename, rows) {
  const csv = rows.map(row => row.map(escapeCsvValue).join(',')).join('\r\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');

  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export default function ExerciseResultsPage() {
  const { courseId } = useParams();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const type = normalizeType(searchParams.get('type'));
  const routeCourseName = location.state?.courseTitle || location.state?.courseName || '';
  const [courseName, setCourseName] = useState(routeCourseName);
  const [page, setPage] = useState(0);
  const [resultPage, setResultPage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setPage(0);
  }, [type]);

  useEffect(() => {
    if (!courseId || routeCourseName) return;

    let cancelled = false;
    courseApi.getById(courseId)
      .then(res => {
        if (!cancelled) {
          setCourseName(getCourseName(res?.data?.data ?? res?.data) || '');
        }
      })
      .catch(err => console.warn('Failed to load course name for results breadcrumb:', err));

    return () => {
      cancelled = true;
    };
  }, [courseId, routeCourseName]);

  useEffect(() => {
    if (!courseId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    assignmentApi.getInstructorSubmissions(courseId, { type, page, size: PAGE_SIZE })
      .then(data => {
        if (!cancelled) {
          setResultPage(data);
        }
      })
      .catch(err => {
        if (!cancelled) {
          setError('Không thể tải kết quả nộp bài.');
          console.error('Failed to fetch instructor submissions:', err);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, type, page]);

  const results = resultPage?.content ?? [];
  const totalPages = resultPage?.totalPages ?? 0;
  const totalElements = resultPage?.totalElements ?? 0;

  const handleTypeChange = (nextType) => {
    setPage(0);
    setSearchParams({ type: nextType });
  };

  const handleExportAll = async () => {
    if (!courseId || exporting || totalElements === 0) return;

    setExporting(true);
    try {
      const data = await assignmentApi.getInstructorSubmissions(courseId, {
        type,
        page: 0,
        size: Math.max(totalElements, PAGE_SIZE),
      });
      const exportRows = data?.content ?? [];
      const rows = [
        ['Tên học sinh', 'Bài tập', 'Loại', 'Điểm', 'Ngày nộp', 'Trạng thái'],
        ...exportRows.map(item => [
          item.studentName,
          item.exerciseTitle,
          item.type === 'quiz' ? 'Bài trắc nghiệm' : 'Bài lập trình',
          `${item.scorePercent ?? 0}%`,
          formatDate(item.submittedAt),
          item.status,
        ]),
      ];
      const safeCourse = (courseName || 'course').replace(/[\\/:*?"<>|]+/g, '-');
      downloadCsv(`ket-qua-${safeCourse}-${type}.csv`, rows);
    } catch (err) {
      setError('Không thể xuất kết quả. Vui lòng thử lại.');
      console.error('Failed to export instructor submissions:', err);
    } finally {
      setExporting(false);
    }
  };

  return (
    <AnimatedPage>
      <div className="exercise-detail-page">
        <nav className="ch-breadcrumb" style={{ marginBottom: '32px' }}>
          <Link to="/instructor/exercises">Bài tập</Link>
          <span className="ch-breadcrumb-separator" style={{ margin: '0 8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
          </span>
          <Link
            to={`/instructor/exercises/${courseId}?tab=${type === 'coding' ? 'coding' : 'quiz'}&page=1`}
            state={courseName ? { courseTitle: courseName } : undefined}
            className="ch-breadcrumb-current"
          >
            {courseName || 'Khóa học'}
          </Link>
          <span className="ch-breadcrumb-separator" style={{ margin: '0 8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
          </span>
          <span className="ch-breadcrumb-current">Kết quả học sinh</span>
        </nav>

        <header className="exercise-detail-header" style={{ marginBottom: '32px' }}>
          <div className="header-left">
            <h1>Chi tiết <span className="highlight-text">Kết quả học sinh</span></h1>
            <p>Theo dõi từng lần nộp bài theo quiz hoặc coding challenge trong khóa học.</p>
          </div>
          <div className="results-header-actions">
            <button
              type="button"
              className="btn-secondary results-export-button"
              onClick={handleExportAll}
              disabled={loading || exporting || totalElements === 0}
            >
              <span className={`material-symbols-outlined ${exporting ? 'results-spin' : ''}`}>
                {exporting ? 'sync' : 'download'}
              </span>
              {exporting ? 'Đang xuất...' : 'Xuất tất cả'}
            </button>
          </div>
        </header>

        <div className="exercise-tabs" style={{ marginBottom: '20px' }}>
          <button
            className={`exercise-tab ${type === 'coding' ? 'active' : ''}`}
            onClick={() => handleTypeChange('coding')}
          >
            Bài lập trình
          </button>
          <button
            className={`exercise-tab ${type === 'quiz' ? 'active' : ''}`}
            onClick={() => handleTypeChange('quiz')}
          >
            Bài trắc nghiệm
          </button>
        </div>

        <div className="exercise-table-container">
          <table className="exercise-table">
            <thead>
              <tr>
                <th>TÊN HỌC SINH</th>
                <th>BÀI TẬP</th>
                <th>LOẠI</th>
                <th>ĐIỂM</th>
                <th>NGÀY NỘP</th>
                <th>TRẠNG THÁI</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Đang tải kết quả...
                  </td>
                </tr>
              )}
              {!loading && error && (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#ef4444' }}>
                    {error}
                  </td>
                </tr>
              )}
              {!loading && !error && results.length === 0 && (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Chưa có lần nộp nào.
                  </td>
                </tr>
              )}
              {!loading && !error && results.map((res, index) => (
                <tr key={`${res.submittedAt ?? index}-${res.studentName}-${res.exerciseTitle}`}>
                  <td style={{ fontWeight: 600 }}>{res.studentName}</td>
                  <td>{res.exerciseTitle}</td>
                  <td>
                    <span className={`question-badge ${res.type === 'quiz' ? 'quiz-bg' : 'code-bg'}`} style={{ background: res.type === 'quiz' ? '#eff6ff' : '#f5f3ff', color: res.type === 'quiz' ? '#3b82f6' : '#8b5cf6' }}>
                      {res.type === 'quiz' ? 'Bài trắc nghiệm' : 'Bài lập trình'}
                    </span>
                  </td>
                  <td style={{ fontWeight: 800, color: Number(res.scorePercent) >= 80 ? '#10b981' : '#f59e0b' }}>
                    {res.scorePercent ?? 0}%
                  </td>
                  <td style={{ color: '#64748b', fontSize: '13px' }}>{formatDate(res.submittedAt)}</td>
                  <td>
                    <div className={`status-pill ${statusClass(res.status)}`}>
                      <span className="status-dot"></span>
                      {res.status}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="table-footer" style={{ marginTop: 24, borderRadius: 12, border: '1px solid #e2e8f0' }}>
          <span className="showing-text">
            Hiển thị <strong>{results.length > 0 ? (page * PAGE_SIZE + 1) : 0}–{page * PAGE_SIZE + results.length}</strong> của <strong>{totalElements}</strong> kết quả
          </span>
          <div className="exercise-table-pagination" aria-label="Phân trang kết quả">
            <button
              type="button"
              className="exercise-page-btn"
              disabled={page <= 0 || loading}
              onClick={() => setPage(prev => Math.max(0, prev - 1))}
            >
              <span className="material-symbols-outlined">chevron_left</span>
            </button>
            <span className="showing-text" style={{ margin: '0 8px' }}>
              Trang {totalPages === 0 ? 0 : page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="exercise-page-btn"
              disabled={page >= totalPages - 1 || loading}
              onClick={() => setPage(prev => prev + 1)}
            >
              <span className="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </AnimatedPage>
  );
}
