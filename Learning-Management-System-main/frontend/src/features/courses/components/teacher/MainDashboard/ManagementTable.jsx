import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faEdit,
  faChevronLeft,
  faChevronRight,
  faSearch,
} from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import '../../../styles/teacher/MainDashboard/managementTable.css';
import { formatDateVN } from '../../../../../utils/dateTime';

const STATUS_LABEL_DEFAULT = { PUBLIC: 'Đã xuất bản', PRIVATE: 'Bản nháp', LOCKED: 'Bị khóa' };
const STATUS_CSS_DEFAULT   = { PUBLIC: 'published',    PRIVATE: 'draft',      LOCKED: 'locked'  };
const LEVEL_LABEL          = { BEGINNER: 'Cơ bản', INTERMEDIATE: 'Trung cấp', ADVANCED: 'Nâng cao' };

const ManagementTable = ({ courses, statusLabel, statusCss }) => {
  const getLabel = (status) => (statusLabel ?? STATUS_LABEL_DEFAULT)[status] ?? status;
  const getCss   = (status) => (statusCss   ?? STATUS_CSS_DEFAULT)[status]   ?? status.toLowerCase();

  const [currentPage, setCurrentPage] = useState(1);
  const [direction,   setDirection]   = useState(0);
  const itemsPerPage = 5;

  const totalPages      = Math.ceil(courses.length / itemsPerPage);
  const indexOfLastItem  = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems     = courses.slice(indexOfFirstItem, indexOfLastItem);

  useEffect(() => {
    if (currentPage > 1 && currentItems.length === 0) setCurrentPage(1);
  }, [courses, currentItems.length, currentPage]);

  const paginate = (pageNumber) => {
    if (pageNumber > 0 && pageNumber <= totalPages) {
      setDirection(pageNumber > currentPage ? 1 : -1);
      setCurrentPage(pageNumber);
    }
  };

  const slideVariants = {
    enter:  (d) => ({ x: d > 0 ?  500 : -500, opacity: 0 }),
    center:      { x: 0, opacity: 1 },
    exit:   (d) => ({ x: d < 0 ?  500 : -500, opacity: 0 }),
  };

  return (
    <motion.div
      className="management-table-card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >

      <div className="management-table-container" style={{ overflow: 'hidden', position: 'relative' }}>
        <table className="management-table">
          <thead>
            <tr>
              <th style={{ width: '40%' }}>Thông tin khóa học</th>
              <th style={{ width: '15%' }}>Học viên</th>
              <th style={{ width: '15%' }}>Cấp độ</th>
              <th style={{ width: '20%' }}>Trạng thái</th>
              <th style={{ width: '10%' }}>Hành động</th>
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
                transition={{
                  x: { type: 'spring', stiffness: 300, damping: 30 },
                  opacity: { duration: 0.2 },
                }}
                style={{ width: '100%' }}
              >
                <td colSpan="5" style={{ padding: 0 }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <tbody>
                      {currentItems.map((course) => (
                        <tr key={course.id}>
                          <td style={{ width: '40%', padding: '20px 25px' }}>
                            <div className="course-info-cell">
                              <div className="course-thumb" style={{ backgroundColor: '#6366f1', overflow: 'hidden' }}>
                                {course.thumbnailUrl
                                  ? <img src={course.thumbnailUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                  : <FontAwesomeIcon icon={faSearch} style={{ opacity: 0.5 }} />
                                }
                              </div>
                              <div className="course-text-info">
                                <Link to={`/manage/courses/center/${course.id}`} className="course-name-link">
                                  <span className="course-name">{course.title ?? course.name}</span>
                                </Link>
                                <span className="course-update-date">
                                  {course.updatedAt
                                    ? formatDateVN(course.updatedAt)
                                    : course.updated}
                                </span>
                              </div>
                            </div>
                          </td>

                          <td style={{ width: '15%', padding: '20px 25px' }}>
                            <span className="student-count-val">
                              {(course.studentCount ?? course.students ?? 0).toLocaleString()}
                            </span>
                          </td>

                          <td style={{ width: '15%', padding: '20px 25px' }}>
                            <span style={{ color: '#64748b', fontSize: '0.85rem' }}>
                              {LEVEL_LABEL[course.level] ?? '—'}
                            </span>
                          </td>

                          <td style={{ width: '20%', padding: '20px 25px' }}>
                            <span className={`status-badge status-${getCss(course.status)}`}>
                              {getLabel(course.status)}
                            </span>
                          </td>

                          <td style={{ width: '10%', padding: '20px 25px' }}>
                            <Link to={`/manage/courses/edit/${course.id}`} className="icon-btn" title="Chỉnh sửa">
                              <FontAwesomeIcon icon={faEdit} />
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </td>
              </motion.tr>
            </AnimatePresence>

            {currentItems.length === 0 && (
              <tr style={{ height: '300px' }}>
                <td colSpan="5" style={{ textAlign: 'center', color: '#94a3b8' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '15px' }}>
                    <FontAwesomeIcon icon={faSearch} style={{ fontSize: '2.5rem', opacity: 0.2 }} />
                    <span>Không tìm thấy khóa học nào khớp với bộ lọc của bạn.</span>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination-footer">
        <span className="pagination-info">
          Hiển thị {indexOfFirstItem + 1}–{Math.min(indexOfLastItem, courses.length)} của {courses.length} khóa học
        </span>
        <div className="pagination-controls">
          <button className="chevron-btn" onClick={() => paginate(currentPage - 1)} disabled={currentPage === 1}>
            <FontAwesomeIcon icon={faChevronLeft} />
          </button>
          {[...Array(totalPages)].map((_, i) => (
            <button
              key={i + 1}
              className={`page-btn ${currentPage === i + 1 ? 'active' : ''}`}
              onClick={() => paginate(i + 1)}
            >
              {i + 1}
            </button>
          ))}
          <button className="chevron-btn" onClick={() => paginate(currentPage + 1)} disabled={currentPage === totalPages || totalPages === 0}>
            <FontAwesomeIcon icon={faChevronRight} />
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default ManagementTable;
