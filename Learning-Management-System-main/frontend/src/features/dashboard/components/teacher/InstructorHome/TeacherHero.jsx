import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartPie, faPlus, faMagic } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import instructorDashboardApi from '../../../../../services/instructorDashboard.api';
import '../../../styles/teacher/InstructorHome/TeacherHero.css';

const TeacherHero = ({ teacherName = 'Instructor' }) => {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    instructorDashboardApi
      .getMyStats()
      .then(setStats)
      .catch(() => {});
  }, []);

  return (
    <motion.section
      className="teacher-hero-premium"
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.8, ease: 'easeOut' }}
    >
      <div className="hero-content-wrapper">
        <div className="hero-text-side">
          <motion.div
            className="premium-badge"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5 }}
          >
            <FontAwesomeIcon icon={faMagic} className="sparkle-icon" />
            <span>Dashboard Giáo viên</span>
          </motion.div>

          <h1 className="hero-main-title">
            Rất vui được gặp lại, <br />
            <span className="gradient-text">{teacherName}!</span>
          </h1>

          <p className="hero-subtext">
            {stats ? (
              <>
                Bạn đang có{' '}
                <strong>{stats.totalCourses} khóa học</strong> với tổng{' '}
                <strong>{stats.totalStudents.toLocaleString()} học sinh</strong> đang theo học.
              </>
            ) : (
              'Chào mừng trở lại! Hãy kiểm tra tiến độ các khóa học của bạn.'
            )}
          </p>

          <div className="hero-cta-group">
            <Link to="/manage/courses/create" className="btn-primary-gradient">
              <FontAwesomeIcon icon={faPlus} />
              Tạo khóa học mới
            </Link>
            <Link to="/manage/courses" className="btn-glass-secondary">
              <FontAwesomeIcon icon={faChartPie} />
              Quản lý khóa học
            </Link>
          </div>
        </div>

        <div className="hero-visual-side">
          <div className="visual-abstract-elements">
            <motion.div
              className="floating-card info-card-1"
              animate={{ y: [0, -15, 0], rotate: [0, 2, 0] }}
              transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut' }}
            >
              <div className="icon-box-blue">
                <FontAwesomeIcon icon={faChartPie} />
              </div>
              <div className="card-data">
                <span className="label">Bài tập</span>
                <span className="value">{stats ? `+${stats.totalExercises}` : '...'}</span>
              </div>
            </motion.div>

            <motion.div
              className="floating-card info-card-2"
              animate={{ y: [0, 15, 0], rotate: [0, -2, 0] }}
              transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
            >
              <div className="avatar-stack">
                <img src="https://i.pravatar.cc/32?img=1" alt="Student" />
                <img src="https://i.pravatar.cc/32?img=2" alt="Student" />
                <img src="https://i.pravatar.cc/32?img=3" alt="Student" />
              </div>
              <span className="avatar-text">
                {stats ? `${stats.totalStudents.toLocaleString()} students` : '...'}
              </span>
            </motion.div>

            <div className="main-sphere" aria-hidden="true">
              <div className="sphere-inner"></div>
              <svg viewBox="0 0 200 200" className="rotating-svg">
                <defs>
                  <radialGradient id="sunCore" cx="35%" cy="30%" r="70%">
                    <stop offset="0%" stopColor="#fff7ad" />
                    <stop offset="48%" stopColor="#fbbf24" />
                    <stop offset="100%" stopColor="#f97316" />
                  </radialGradient>
                  <radialGradient id="moonCore" cx="36%" cy="28%" r="72%">
                    <stop offset="0%" stopColor="#f8fafc" />
                    <stop offset="56%" stopColor="#c7d2fe" />
                    <stop offset="100%" stopColor="#64748b" />
                  </radialGradient>
                  <linearGradient id="sunOrbit" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#fef3c7" />
                    <stop offset="58%" stopColor="#38bdf8" />
                    <stop offset="100%" stopColor="#2563eb" />
                  </linearGradient>
                  <linearGradient id="moonOrbit" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#bfdbfe" />
                    <stop offset="52%" stopColor="#818cf8" />
                    <stop offset="100%" stopColor="#22d3ee" />
                  </linearGradient>
                  <filter id="celestialGlow" x="-45%" y="-45%" width="190%" height="190%">
                    <feGaussianBlur stdDeviation="5" result="blur" />
                    <feMerge>
                      <feMergeNode in="blur" />
                      <feMergeNode in="SourceGraphic" />
                    </feMerge>
                  </filter>
                </defs>
                <g className="orbit-ring">
                  <circle cx="100" cy="100" r="78" />
                  <circle cx="100" cy="100" r="54" />
                </g>
                <g className="sun-scene" filter="url(#celestialGlow)">
                  <g className="sun-rays">
                    {Array.from({ length: 12 }).map((_, index) => (
                      <line
                        key={index}
                        x1="100"
                        y1="14"
                        x2="100"
                        y2="31"
                        transform={`rotate(${index * 30} 100 100)`}
                      />
                    ))}
                  </g>
                  <circle className="sun-core" cx="100" cy="100" r="42" />
                  <path
                    className="sun-shine"
                    d="M72 88c10-20 42-23 56-2 7 10 5 26-3 35 2-12-2-24-12-31-12-9-28-9-41-2Z"
                  />
                </g>
                <g className="moon-scene" filter="url(#celestialGlow)">
                  <circle className="moon-core" cx="102" cy="100" r="43" />
                  <circle className="moon-cutout" cx="119" cy="83" r="43" />
                  <circle className="moon-crater moon-crater--large" cx="91" cy="92" r="5" />
                  <circle className="moon-crater" cx="106" cy="118" r="3.5" />
                  <circle className="moon-crater" cx="80" cy="112" r="2.8" />
                  <g className="moon-stars">
                    <path d="M51 67l4 8 8 4-8 4-4 8-4-8-8-4 8-4Z" />
                    <path d="M147 51l3 6 6 3-6 3-3 6-3-6-6-3 6-3Z" />
                    <circle cx="150" cy="137" r="2.6" />
                  </g>
                </g>
              </svg>
            </div>
          </div>
        </div>
      </div>
    </motion.section>
  );
};

export default TeacherHero;
