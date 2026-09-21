import React, { useEffect, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { Link, Navigate, useLocation } from 'react-router-dom';
import courseApi from '../../../../../services/course.api';
import { buildAppErrorState, getAppErrorRoute } from '../../../../../utils/appError';
import CourseExerciseCard from '../../components/teacher/InstructorExerciseHub/CourseExerciseCard';
import Pagination from '../../../../courses/components/student/ListCourse/Pagination';
import '../../styles/teacher/InstructorExerciseHub/instructorExerciseHub.css';

const ITEMS_PER_PAGE = 6;
const formatNumber = (value) => Number(value ?? 0).toLocaleString('vi-VN');
const pageTransition = { duration: 0.5 };
const contentTransition = { duration: 0.32, ease: 'easeOut' };
const contentMotion = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 },
};

export default function InstructorExerciseHub() {
  const location = useLocation();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const shouldReduceMotion = useReducedMotion();
  const activePageTransition = shouldReduceMotion ? { duration: 0 } : pageTransition;
  const activeContentTransition = shouldReduceMotion ? { duration: 0 } : contentTransition;
  const activeContentMotion = shouldReduceMotion
    ? { initial: { opacity: 1, y: 0 }, animate: { opacity: 1, y: 0 }, exit: { opacity: 1, y: 0 } }
    : contentMotion;

  useEffect(() => {
    courseApi.getMyCourses()
      .then(data => {
        const list = data?.content ?? data ?? [];
        setCourses(list);
      })
      .catch(err => {
        console.error('Failed to fetch courses:', err);
        setError(buildAppErrorState(err, {
          title: 'Không thể tải danh sách khóa học',
          fallbackPath: '/instructor/home',
        }));
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [courses]);

  const totalPages = Math.ceil(courses.length / ITEMS_PER_PAGE);
  const paginatedCourses = courses.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );
  const hasCourses = courses.length > 0;

  if (error) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
  }

  return (
    <motion.div
      className="instructor-exercise-hub"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={activePageTransition}
    >
        <header className="exercise-hub-header">
          <div className="exercise-hub-heading">
            <h1 className="exercise-hub-title">Quản lý bài tập</h1>
            <p className="exercise-hub-subtitle">
              Chọn khóa học để mở không gian quản lý bài tập, tạo quiz hoặc coding challenge và theo dõi kết quả học viên trong từng nội dung giảng dạy.
            </p>
          </div>

        </header>

        <AnimatePresence mode="wait">
        {loading && (
          <motion.div
            key="exercise-hub-loading"
            className="exercise-hub-loading"
            {...activeContentMotion}
            transition={activeContentTransition}
          >
            <div className="loading-screen__spinner" />
            <p>Đang tải khóa học...</p>
          </motion.div>
        )}

        {!loading && !error && !hasCourses && (
          <motion.div
            key="exercise-hub-empty"
            className="empty-container exercise-empty-state"
            {...activeContentMotion}
            transition={activeContentTransition}
          >
            <div className="exercise-empty-icon">
              <span className="material-symbols-outlined">folder_open</span>
            </div>
            <div className="exercise-empty-copy">
              <h2>Bạn chưa có khóa học nào.</h2>
              <p>
                Hãy tạo khóa học đầu tiên, thêm nội dung học tập, rồi quay lại đây để tạo quiz hoặc coding challenge cho học viên.
              </p>
            </div>
            <div className="exercise-empty-steps" aria-label="Các bước bắt đầu">
              <div className="exercise-empty-step">
                <span>1</span>
                <p>Tạo khóa học và nhập thông tin cơ bản.</p>
              </div>
              <div className="exercise-empty-step">
                <span>2</span>
                <p>Thêm chương, bài học và tài nguyên học tập.</p>
              </div>
              <div className="exercise-empty-step">
                <span>3</span>
                <p>Quay lại trung tâm này để tạo quiz hoặc bài coding.</p>
              </div>
            </div>
            <div className="exercise-empty-actions">
              <Link to="/manage/courses/create" className="exercise-hub-action exercise-hub-action--primary">
                <span className="material-symbols-outlined">add_circle</span>
                Tạo khóa học đầu tiên
              </Link>
              <Link to="/manage/courses" className="exercise-hub-action exercise-hub-action--secondary">
                <span className="material-symbols-outlined">settings</span>
                Mở quản lý khóa học
              </Link>
            </div>
          </motion.div>
        )}

        {!loading && !error && hasCourses && (
          <motion.section
            key="exercise-hub-courses"
            className="exercise-course-section"
            {...activeContentMotion}
            transition={activeContentTransition}
          >
            <div className="exercise-section-heading">
              <div>
                <h2>Khóa học của bạn</h2>
                <p>Chọn một khóa học để quản lý ngân hàng quiz và coding challenge.</p>
              </div>
              <span>{formatNumber(courses.length)} khóa học</span>
            </div>

            <div className="course-grid">
              {paginatedCourses.map(course => (
                <CourseExerciseCard key={course.id ?? course.courseId} course={course} />
              ))}
            </div>

            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </motion.section>
        )}
        </AnimatePresence>
    </motion.div>
  );
}
