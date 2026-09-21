import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../../../styles/student/CourseHub/CourseSidebar.css';

function normalizePercent(value) {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return 0;
  return Math.min(100, Math.max(0, numericValue));
}

function formatPercent(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function ContinueButton({ progress, courseId }) {
  const navigate = useNavigate();
  const completed = progress?.completedLessons ?? 0;
  const total = progress?.totalLessons ?? 0;

  const handleClick = () => navigate('/my-courses/detail', { state: { courseId } });

  if (total > 0 && completed >= total) {
    return (
      <button className="ch-continue-btn ch-continue-btn--done" onClick={handleClick}>
        <span className="material-symbols-outlined">verified</span>
        Đã hoàn thành
      </button>
    );
  }

  if (completed === 0) {
    return (
      <button className="ch-continue-btn ch-continue-btn--start" onClick={handleClick}>
        <span className="material-symbols-outlined">play_arrow</span>
        Bắt đầu học
      </button>
    );
  }

  return (
    <button className="ch-continue-btn" onClick={handleClick}>
      <span className="material-symbols-outlined">arrow_forward</span>
      Tiếp tục học
    </button>
  );
}

function AssignmentSection({ courseId, quizCount, completedQuizCount, problemCount, completedProblemCount, loading }) {
  const navigate = useNavigate();

  if (loading) {
    return (
      <div className="ch-card ch-homework-card">
        <h4 className="ch-sidebar-title">Bài tập</h4>
        <div className="ch-hw-loading">Đang tải...</div>
      </div>
    );
  }

  const hasQuiz = quizCount > 0;
  const hasProblem = problemCount > 0;
  const quizAllDone = hasQuiz && completedQuizCount >= quizCount;
  const problemAllDone = hasProblem && completedProblemCount >= problemCount;

  if (!hasQuiz && !hasProblem) {
    return (
      <div className="ch-card ch-homework-card">
        <h4 className="ch-sidebar-title">Bài tập</h4>
        <div className="ch-hw-empty">
          <span className="material-symbols-outlined">check_circle</span>
          Không có bài tập
        </div>
      </div>
    );
  }

  return (
    <div className="ch-card ch-homework-card">
      <h4 className="ch-sidebar-title">Bài tập</h4>

      {hasQuiz && (
        <div className="ch-hw-section">
          <div className="ch-hw-section-label">
            <span className="material-symbols-outlined">quiz</span>
            Trắc nghiệm
            <span className="ch-hw-count">{completedQuizCount}/{quizCount}</span>
          </div>
          {quizAllDone ? (
            <div className="ch-hw-done">
              <span className="material-symbols-outlined">verified</span>
              Đã hoàn thành tất cả
            </div>
          ) : (
            <button
              className="ch-hw-btn ch-hw-btn--indigo"
              onClick={() => navigate(`/exercises/code/${courseId}?tab=quizzes&page=1`)}
            >
              <span className="material-symbols-outlined">arrow_forward</span>
              Làm bài trắc nghiệm
            </button>
          )}
        </div>
      )}

      {hasProblem && (
        <div className="ch-hw-section">
          <div className="ch-hw-section-label">
            <span className="material-symbols-outlined">code</span>
            Bài tập code
            <span className="ch-hw-count">{completedProblemCount}/{problemCount}</span>
          </div>
          {problemAllDone ? (
            <div className="ch-hw-done">
              <span className="material-symbols-outlined">verified</span>
              Đã hoàn thành tất cả
            </div>
          ) : (
            <button
              className="ch-hw-btn ch-hw-btn--blue"
              onClick={() => navigate(`/exercises/code/${courseId}`)}
            >
              <span className="material-symbols-outlined">arrow_forward</span>
              Làm bài tập code
            </button>
          )}
        </div>
      )}
    </div>
  );
}

export default function CourseSidebar({
  courseId, progress = null,
  quizCount = 0, completedQuizCount = 0,
  problemCount = 0, completedProblemCount = 0,
  assignmentsLoading = false,
}) {
  const completedLessons = progress?.completedLessons ?? 0;
  const totalLessons = progress?.totalLessons ?? 0;
  const overallProgress = normalizePercent(progress?.percentComplete ?? 0);
  const formattedProgress = formatPercent(overallProgress);

  return (
    <aside className="ch-sidebar">
      {/* Progress card */}
      <div className="ch-card ch-progress-card">
        <h4 className="ch-sidebar-title">Tiến độ của bạn</h4>
        <div className="ch-progress-bar-container">
          <div className="ch-progress-bar-bg">
            <div className="ch-progress-bar-fill" style={{ width: `${overallProgress}%` }} />
          </div>
        </div>
        <div className="ch-progress-stats">
          <span className="ch-progress-lessons">{completedLessons}/{totalLessons} Bài học</span>
          <span className="ch-progress-pct">{formattedProgress}% Hoàn thành</span>
        </div>
        <ContinueButton progress={progress} courseId={courseId} />
      </div>

      {/* Assignment card */}
      <AssignmentSection
        courseId={courseId}
        quizCount={quizCount}
        completedQuizCount={completedQuizCount}
        problemCount={problemCount}
        completedProblemCount={completedProblemCount}
        loading={assignmentsLoading}
      />
    </aside>
  );
}
