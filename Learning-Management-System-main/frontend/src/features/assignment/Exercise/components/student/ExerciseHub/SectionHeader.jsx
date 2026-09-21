import React from 'react';
import '../../../styles/student/ExerciseHub/SectionHeader.css';

const SectionHeader = ({ icon, title, link = null }) => {
  return (
    <div className="exercise-section-header">
      <h2 className="section-title">
        <span className="material-symbols-outlined" style={{ color: '#2563eb' }}>
          {icon}
        </span>
        {title}
      </h2>
      {link && (
        <a href={link.href} className="section-all-link">
          {link.text}
        </a>
      )}
    </div>
  );
};

export default SectionHeader;
