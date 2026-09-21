import React from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faPenNib, faUserGraduate, faBolt, faArrowRight } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import '../../../styles/teacher/InstructorHome/QuickActions.css';

const actions = [
  {
    label: 'Tạo khóa học',
    desc: 'Thêm khóa học mới vào danh mục',
    icon: faPlus,
    color: 'blue-grad',
    to: '/manage/courses/create',
  },
  {
    label: 'Viết bài blog',
    desc: 'Chia sẻ kiến thức với sinh viên',
    icon: faPenNib,
    color: 'purple-grad',
    to: '/instructor/blog/create',
  },
  {
    label: 'Quản lý khóa học',
    desc: 'Theo dõi tiến độ và bài tập học sinh',
    icon: faUserGraduate,
    color: 'green-grad',
    to: '/manage/courses',
  },
];

const QuickActions = () => (
  <section className="quick-actions-premium">
    <div className="section-header">
      <div className="title-with-icon">
        <div className="bolt-icon-wrapper">
          <FontAwesomeIcon icon={faBolt} />
        </div>
        <h3>Thao tác nhanh</h3>
      </div>
    </div>

    <div className="actions-flex">
      {actions.map((action, index) => (
        <motion.div
          key={index}
          className={`action-card ${action.color}`}
          whileHover={{ y: -8, scale: 1.02 }}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1 * index + 0.5 }}
        >
          <div className="action-icon">
            <FontAwesomeIcon icon={action.icon} />
          </div>
          <div className="action-info">
            <h4>{action.label}</h4>
            <p>{action.desc}</p>
          </div>
          <Link to={action.to} className="action-arrow">
            <FontAwesomeIcon icon={faArrowRight} />
          </Link>
        </motion.div>
      ))}
    </div>
  </section>
);

export default QuickActions;
