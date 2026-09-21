import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus } from '@fortawesome/free-solid-svg-icons';
import ManagementTable from '../../components/teacher/MainDashboard/ManagementTable.jsx';
import ManagementStats from '../../components/teacher/MainDashboard/ManagementStats.jsx';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import { COURSE_DISPLAY_SORT, sortCoursesByDisplayOrder } from '../../../../utils/courseOrder';
import courseApi from '../../../../services/course.api.js';
import '../../styles/teacher/Pages/courseManagement.css';

const STATUS_LABEL = { PUBLIC: 'Công khai', PRIVATE: 'Riêng tư', LOCKED: 'Bị khóa' };
const STATUS_CSS   = { PUBLIC: 'published', PRIVATE: 'draft',     LOCKED: 'locked'  };

const CourseManagement = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [courses, setCourses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [stats, setStats] = useState({ totalCourses: 0, totalStudents: 0, totalExercises: 0 });
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('All');
  const [activeTab, setActiveTab] = useState('All');
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [page, statsData, cats] = await Promise.all([
        courseApi.getMyCourses(0, 100, COURSE_DISPLAY_SORT),
        courseApi.getMyStats(),
        courseApi.getCategories(),
      ]);
      const rawCourses = sortCoursesByDisplayOrder(page.content ?? []);
      setCourses(rawCourses);
      setStats(statsData);
      setCategories(cats);
      setError(null);

      if (rawCourses.length > 0) {
        const countResults = await Promise.allSettled(
          rawCourses.map((course) => courseApi.getStudentsCount(course.id))
        );
        setCourses((prev) => prev.map((course, index) => ({
          ...course,
          studentCount: countResults[index].status === 'fulfilled' ? countResults[index].value : 0,
        })));
      }
    } catch (err) {
      console.error('Lỗi tải dữ liệu khóa học:', err);
      setError(buildAppErrorState(err, {
        title: 'Không thể tải danh sách khóa học',
        fallbackPath: '/instructor/home',
      }));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const filteredCourses = useMemo(() => {
    return courses.filter((course) => {
      const matchSearch = course.title.toLowerCase().includes(searchTerm.toLowerCase());
      const matchCategory = categoryFilter === 'All' ||
        course.categories?.some((category) => String(category.id) === categoryFilter);
      const matchTab =
        activeTab === 'All' ||
        (activeTab === 'PUBLIC' && course.status === 'PUBLIC') ||
        (activeTab === 'PRIVATE' && course.status === 'PRIVATE') ||
        (activeTab === 'LOCKED' && course.status === 'LOCKED');
      return matchSearch && matchCategory && matchTab;
    });
  }, [courses, searchTerm, categoryFilter, activeTab]);

  const countByStatus = (status) => courses.filter((course) => course.status === status).length;

  if (error) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
  }

  return (
    <div className="course-management-container">
      <motion.div
        className="management-inner"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <header className="management-header">
          <h1>Quản lý khóa học</h1>
          <div className="header-actions">
            <div className="search-bar-wrapper">
              <FontAwesomeIcon icon={faSearch} className="search-icon" />
              <input
                type="text"
                placeholder="Tìm kiếm khóa học..."
                className="search-input"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
              <button className="btn-search">Tìm kiếm</button>
            </div>
            <div className="action-buttons-group">
              <button className="btn-create-course" onClick={() => navigate('/manage/courses/create')}>
                <FontAwesomeIcon icon={faPlus} />
                Tạo khóa học mới
              </button>
            </div>
          </div>
        </header>

        <section className="management-filters">
          <select
            className="filter-select"
            value={categoryFilter}
            onChange={(event) => setCategoryFilter(event.target.value)}
          >
            <option value="All">Tất cả danh mục</option>
            {categories.map((category) => (
              <option key={category.id} value={String(category.id)}>{category.name}</option>
            ))}
          </select>
        </section>

        <nav className="status-tabs">
          {[
            { key: 'All', label: 'Tất cả', count: courses.length },
            { key: 'PUBLIC', label: 'Công khai', count: countByStatus('PUBLIC') },
            { key: 'PRIVATE', label: 'Riêng tư', count: countByStatus('PRIVATE') },
            { key: 'LOCKED', label: 'Bị khóa', count: countByStatus('LOCKED') },
          ].map((tab) => (
            <button
              key={tab.key}
              className={`status-tab ${activeTab === tab.key ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              <span className="tab-count">{tab.count}</span>
            </button>
          ))}
        </nav>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Đang tải...</div>
        ) : (
          <ManagementTable
            courses={filteredCourses}
            statusLabel={STATUS_LABEL}
            statusCss={STATUS_CSS}
          />
        )}

        <ManagementStats
          totalStudents={stats.totalStudents}
          enrollments={stats.totalCourses}
          avgRating={stats.totalExercises}
          statsLabels={{ second: 'Tổng khóa học', third: 'Tổng bài tập' }}
        />
      </motion.div>
    </div>
  );
};

export default CourseManagement;
