import React, { useState, useEffect } from 'react';
import '../../../styles/student/CourseHub/ChaptersList.css';

const LESSON_ICON = {
  VIDEO:      'play_circle',
  DOCUMENT:   'description',
  QUIZ:       'quiz',
  CODING:     'code',
  LIVE_CLASS: 'live_tv',
};

const CurriculumItem = ({ item, level = 0, index, forceOpen, onLessonClick }) => {
  const [isOpen, setIsOpen] = useState(() =>
    forceOpen !== undefined ? forceOpen : false
  );
  const isFolder = item.type === 'folder';
  const hasChildren = isFolder && item.items?.length > 0;
  const lessonIcon = LESSON_ICON[item.lessonType?.toUpperCase()] ?? 'play_circle';
  const lessonClickable = !isFolder && !!onLessonClick;

  useEffect(() => {
    if (forceOpen !== undefined) setIsOpen(forceOpen);
  }, [forceOpen]);

  return (
    <div className={`ch-item-wrapper ${item.type}-wrapper`} style={{ '--level': level }}>
      <div
        className={`ch-item-row ${isFolder ? 'folder-row' : 'lesson-row'} ${item.completed ? 'completed' : ''} ${lessonClickable ? 'lesson-clickable' : ''}`}
        onClick={() => isFolder ? setIsOpen(prev => !prev) : onLessonClick?.(item)}
      >
        <div className="ch-item-main">
          {isFolder && (
            <span className="material-symbols-outlined ch-folder-toggle-icon">
              {isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
            </span>
          )}

          {!isFolder && (
            <div className={`ch-lesson-icon ${item.lessonType?.toLowerCase() ?? 'video'}`}>
              <span className="material-symbols-outlined">{lessonIcon}</span>
            </div>
          )}

          <div className="ch-item-info">
            <div className="ch-item-title-row">
              <h4 className="ch-item-title">{item.title}</h4>
            </div>
            {!isFolder && item.lessonType && (
              <p className="ch-item-meta">{item.lessonType.toUpperCase()}</p>
            )}
          </div>
        </div>

        {!isFolder && lessonClickable && (
          <div className="ch-item-action">
            <span className="material-symbols-outlined play-icon">chevron_right</span>
          </div>
        )}
      </div>

      {isFolder && isOpen && hasChildren && (
        <div className="ch-item-children">
          {item.items.map((child, idx) => (
            <CurriculumItem key={child.id} item={child} level={level + 1} index={idx} forceOpen={forceOpen} onLessonClick={onLessonClick} />
          ))}
        </div>
      )}
    </div>
  );
};

export default function ChaptersList({ chapters = [], totalLessons = 0, loading = false, onLessonClick }) {
  const [allOpen, setAllOpen] = useState(false);

  return (
    <div className="ch-chapters-container">
      <div className="ch-section-header">
        <div className="ch-section-title">
          <span className="material-symbols-outlined">menu_book</span>
          Nội dung khóa học
          {totalLessons > 0 && (
            <span className="ch-total-units">{totalLessons} bài học</span>
          )}
        </div>
        {chapters.length > 0 && (
          <button className="ch-toggle-all-btn" onClick={() => setAllOpen(prev => !prev)}>
            {allOpen ? 'Thu gọn tất cả' : 'Xem tất cả'}
          </button>
        )}
      </div>

      <div className="ch-chapters-list">
        {loading ? (
          <div className="ch-empty-state">Đang tải nội dung...</div>
        ) : chapters.length > 0 ? (
          chapters.map((chapter, idx) => (
            <CurriculumItem key={chapter.id} item={chapter} index={idx} forceOpen={allOpen} onLessonClick={onLessonClick} />
          ))
        ) : (
          <div className="ch-empty-state">Không có nội dung cho khóa học này.</div>
        )}
      </div>
    </div>
  );
}
