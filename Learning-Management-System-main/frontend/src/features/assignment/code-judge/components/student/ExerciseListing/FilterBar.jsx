import React from 'react';
import { Link } from 'react-router-dom';
import '../../../styles/student/ExerciseListing/FilterBar.css';

const FilterBar = ({ activeLevel, onLevelChange, activeStatus, onStatusChange, slug, showLevels = true }) => {
  const levels = [
    { id: 'All', label: 'Tất cả' },
    { id: 'EASY', label: 'Dễ' },
    { id: 'MEDIUM', label: 'Trung bình' },
    { id: 'HARD', label: 'Khó' }
  ];

  return (
    <div className="exercise-filter-bar">
      <div className="filter-group">
        {showLevels && levels.map(level => (
            <button
              key={level.id}
              className={`filter-chip ${activeLevel === level.id ? 'active' : ''}`}
              onClick={() => onLevelChange(level.id)}
            >
              {level.label}
            </button>
          ))}
        <select
          className="filter-select"
          value={activeStatus}
          onChange={(event) => onStatusChange(event.target.value)}
        >
          <option value="All">Trạng thái: Tất cả</option>
          <option value="Completed">Đã hoàn thành</option>
          <option value="Incomplete">Chưa hoàn thành</option>
        </select>
      </div>
      <Link to={`/exercises/leaderboard/${slug || 'all'}`} className="leaderboard-btn">
        <span className="material-symbols-outlined">leaderboard</span>
        Bảng xếp hạng
      </Link>
    </div>
  );
};

export default FilterBar;
