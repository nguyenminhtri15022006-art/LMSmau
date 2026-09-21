import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import '../../styles/student/ContinueLearning.css';

const FALLBACK_IMG = 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80';

export default function ContinueLearning({ data }) {
  const navigate = useNavigate();
  if (!data) return null;

  return (
    <motion.div
      className="continue-section"
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.42, ease: 'easeOut', delay: 0.18 }}
    >
      <div className="section-header">
        <h3 className="section-title">
          <span className="material-symbols-outlined" style={{ color: '#2563eb' }}>play_circle</span>
          Tiếp tục học
        </h3>
      </div>

      <div className="continue-card">
        <img
          src={data.image}
          alt={data.title}
          className="continue-img"
          onError={e => { e.target.onerror = null; e.target.src = FALLBACK_IMG; }}
        />
        <div className="continue-info">
          <div className="badge-tag">{data.category}</div>
          <span style={{ fontSize: '12px', color: '#64748b', float: 'right' }}>
            Cập nhật: {data.lastActive}
          </span>
          <h3>{data.title}</h3>
          <p>{data.description}</p>

          <div className="progress-container">
            <div className="progress-labels">
              <span>{data.progress}% Hoàn thành</span>
              <span>{data.lessonsDone} / {data.totalLessons} Bài học</span>
            </div>
            <div className="progress-bar-bg">
              <div className="progress-bar-fill" style={{ width: `${data.progress}%` }} />
            </div>
          </div>

          <button
            className="btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            onClick={() => navigate(`/course-hub?id=${data.courseId}`)}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>play_arrow</span>
            Tiếp tục học
          </button>
        </div>
      </div>
    </motion.div>
  );
}