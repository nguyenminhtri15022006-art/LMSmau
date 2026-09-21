import React from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUsers, faBookOpen, faStar } from '@fortawesome/free-solid-svg-icons';
import '../../../styles/teacher/MainDashboard/managementStats.css';

const ManagementStats = ({ totalStudents, enrollments, avgRating, statsLabels }) => {
  const secondLabel = statsLabels?.second ?? 'Tổng đăng ký';
  const thirdLabel  = statsLabels?.third  ?? 'Đánh giá trung bình';
  const stats = [
    { label: 'Tổng học viên', value: totalStudents ?? '—', icon: faUsers },
    { label: secondLabel,     value: enrollments   ?? '—', icon: faBookOpen },
    { label: thirdLabel,      value: avgRating     ?? '—', icon: faStar },
  ];

  return (
    <div className="management-stats-row">
      {stats.map((stat, index) => (
        <motion.div
          key={stat.label}
          className="m-stat-card"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: 0.4 + index * 0.1 }}
        >
          <div className="m-stat-header">
            <div className="m-stat-icon-box">
              <FontAwesomeIcon icon={stat.icon} />
            </div>
            <span>{stat.label}</span>
          </div>
          <div className="m-stat-value">{stat.value}</div>
        </motion.div>
      ))}
    </div>
  );
};

export default ManagementStats;
