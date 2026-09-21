import React from 'react';
import { motion } from 'framer-motion';
import '../../styles/student/DashboardStats.css';

export default function DashboardStats({ stats }) {
  if (!stats) return null;

  return (
    <div className="stats-grid">
      {stats.map((stat, idx) => (
        <motion.div
          key={idx}
          className="stat-card"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.38, ease: 'easeOut', delay: Math.min(idx * 0.06, 0.24) }}
          whileHover={{ y: -6 }}
        >
          <div className="stat-icon" style={{ backgroundColor: stat.color, color: stat.textColor }}>
            <span className="material-symbols-outlined">{stat.icon}</span>
          </div>
          <div className="stat-info">
            <p className="label">{stat.label}</p>
            <p className="value">{stat.value}</p>
          </div>
        </motion.div>
      ))}
    </div>
  );
}
