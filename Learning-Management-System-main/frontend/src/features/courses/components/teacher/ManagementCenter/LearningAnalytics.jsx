import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartBar } from '@fortawesome/free-solid-svg-icons';
import courseApi from '../../../../../services/course.api.js';
import '../../../styles/teacher/ManagementCenter/learningAnalytics.css';

const LearningAnalytics = ({ courseId, studentCount = 0, lessonCount = 0 }) => {
  const [avgProgress, setAvgProgress] = useState(null);
  const [loading,     setLoading]     = useState(true);

  useEffect(() => {
    if (!courseId) return;
    courseApi.getAverageProgress(courseId)
      .then(val => setAvgProgress(Math.round(val ?? 0)))
      .catch(() => setAvgProgress(0))
      .finally(() => setLoading(false));
  }, [courseId]);

  const progressVal = avgProgress ?? 0;

  return (
    <section className="learning-analytics">
      <div className="section-title">
        <FontAwesomeIcon icon={faChartBar} />
        Thống kê học tập
      </div>

      <div>
        <div className="white-card">
          <div className="sub-card-title">Tổng quan tiến độ học viên</div>
          <div className="performance-overview-grid">
            <div className="mini-stat-card">
              <span className="mini-stat-label">Tỷ lệ hoàn thành TB</span>
              <div className="mini-stat-value green">
                {loading ? '...' : `${progressVal}%`}
              </div>
            </div>
            <div className="mini-stat-card">
              <span className="mini-stat-label">Tổng học viên</span>
              <div className="mini-stat-value">{studentCount.toLocaleString()}</div>
            </div>
            <div className="mini-stat-card">
              <span className="mini-stat-label">Tổng bài học</span>
              <div className="mini-stat-value">{lessonCount.toLocaleString()}</div>
            </div>
          </div>

          {/* Progress bar tổng */}
          {!loading && (
            <div style={{ marginTop: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.85rem', color: '#475569' }}>
                <span>Tiến độ trung bình toàn khóa</span>
                <span style={{ fontWeight: 700 }}>{progressVal}%</span>
              </div>
              <div className="progress-bar-track">
                <motion.div
                  style={{ height: '100%', borderRadius: '10px', background: 'linear-gradient(90deg, #3b82f6, #6366f1)' }}
                  initial={{ width: 0 }}
                  animate={{ width: `${progressVal}%` }}
                  transition={{ duration: 1.2, ease: 'easeOut' }}
                />
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
};

export default LearningAnalytics;
