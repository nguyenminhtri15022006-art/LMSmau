import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUsers, faBookOpen, faPuzzlePiece } from '@fortawesome/free-solid-svg-icons';
import instructorDashboardApi from '../../../../../services/instructorDashboard.api';
import '../../../styles/teacher/Common/TeacherStats.css';

const StatCard = ({ label, value, icon, colorClass, delay, loading }) => (
  <motion.div
    className="stat-card-premium glass-card"
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.5, delay }}
    whileHover={{ y: -5, transition: { duration: 0.2 } }}
  >
    <div className="stat-main">
      <div className={`icon-wrapper ${colorClass}`}>
        <FontAwesomeIcon icon={icon} />
      </div>
      <div className="stat-info">
        <span className="stat-label">{label}</span>
        <div className="stat-value-row">
          {loading ? (
            <span className="stat-value stat-loading">—</span>
          ) : (
            <span className="stat-value">{value}</span>
          )}
        </div>
      </div>
    </div>
  </motion.div>
);

const TeacherStats = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    instructorDashboardApi
      .getMyStats()
      .then(setStats)
      .catch(() => setStats({ totalCourses: 0, totalStudents: 0, totalExercises: 0 }))
      .finally(() => setLoading(false));
  }, []);

  const cards = [
    {
      label: 'Tổng học sinh',
      value: stats ? stats.totalStudents.toLocaleString() : '0',
      icon: faUsers,
      colorClass: 'blue',
    },
    {
      label: 'Khóa học đang dạy',
      value: stats ? stats.totalCourses.toLocaleString() : '0',
      icon: faBookOpen,
      colorClass: 'green',
    },
    {
      label: 'Bài tập đã tạo',
      value: stats ? stats.totalExercises.toLocaleString() : '0',
      icon: faPuzzlePiece,
      colorClass: 'purple',
    },
  ];

  return (
    <section className="teacher-stats-premium">
      {cards.map((card, index) => (
        <StatCard key={index} {...card} delay={index * 0.1 + 0.2} loading={loading} />
      ))}
    </section>
  );
};

export default TeacherStats;
