import React, { useEffect, useState, useMemo } from 'react';
import { Navigate, useParams, useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { assignmentApi } from '../../../../../services/assignment.api';
import { courseApi } from '../../../../../services/course.api';
import { buildAppErrorState } from '../../../../../utils/appError';
import { findCourseByRouteSlug } from '../../../../../utils/courseSlug';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import '../../../shared/styles/ExerciseShared.css';
import '../../styles/student/LeaderboardPage/LeaderboardPage.css';

const MEDAL_COLORS = {
  1: { bg: '#fff9e6', border: '#f59e0b', text: '#b45309', badge: 'gold' },
  2: { bg: '#f8fafc', border: '#94a3b8', text: '#475569', badge: 'silver' },
  3: { bg: '#fff7ed', border: '#d97706', text: '#9a3412', badge: 'bronze' },
};

const RANK_ICON = { 1: 'trophy', 2: 'workspace_premium', 3: 'military_tech' };

function PodiumCard({ rank, student, isCurrentUser }) {
  const c = MEDAL_COLORS[rank] || {};
  return (
    <motion.div
      className={`podium-card podium-rank-${rank} ${isCurrentUser ? 'is-current-user' : ''}`}
      initial={{ opacity: 0, y: 40 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: rank * 0.12 }}
      style={{ borderColor: c.border }}
    >
      {isCurrentUser && <span className="podium-you-badge">Bạn</span>}
      <div className="podium-medal">
        <span className="material-symbols-outlined" style={{ color: c.border, fontVariationSettings: "'FILL' 1" }}>
          {RANK_ICON[rank]}
        </span>
      </div>
      <div className="podium-avatar" style={{ background: c.bg, borderColor: c.border }}>
        {student.avatarUrl ? (
          <img src={student.avatarUrl} alt={student.name} />
        ) : (
          <span style={{ color: c.text, fontWeight: 800, fontSize: 22 }}>
            {student.name?.charAt(0)?.toUpperCase() || '?'}
          </span>
        )}
      </div>
      <div className="podium-info">
        <h3 className="podium-name">{student.name || '—'}</h3>
        <div className="podium-stat">
          <span className="podium-stat-value" style={{ color: c.text }}>{student.score}</span>
          <span className="podium-stat-label">điểm</span>
        </div>
      </div>
    </motion.div>
  );
}

function TableRow({ rank, student, isCurrentUser, type }) {
  const medal = rank <= 3 ? MEDAL_COLORS[rank] : null;
  return (
    <motion.tr
      className={`leaderboard-row ${isCurrentUser ? 'current-user-row' : ''}`}
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3, delay: Math.min(rank * 0.03, 0.6) }}
    >
      <td className="col-rank">
        {rank <= 3 ? (
          <span
            className={`rank-badge rank-badge--${MEDAL_COLORS[rank].badge}`}
          >
            {rank}
          </span>
        ) : (
          <span className="rank-number">{rank}</span>
        )}
      </td>
      <td className="col-user">
        <div className="user-cell">
          <div className="user-avatar-sm" style={medal ? { borderColor: medal.border } : {}}>
            {student.avatarUrl ? (
              <img src={student.avatarUrl} alt={student.name} />
            ) : (
              student.name?.charAt(0)?.toUpperCase() || '?'
            )}
          </div>
          <div className="user-name-group">
            <span className="user-name">{student.name || '—'}</span>
            {isCurrentUser && <span className="you-tag">Bạn</span>}
          </div>
        </div>
      </td>
      {type === 'coding' ? (
        <>
          <td className="col-stat">{student.solvedCount ?? '—'}</td>
          <td className="col-stat highlight">{student.score ?? '—'}</td>
          <td className="col-stat">{student.accuracy != null ? `${student.accuracy}%` : '—'}</td>
        </>
      ) : (
        <>
          <td className="col-stat">{student.completedCount ?? '—'}</td>
          <td className="col-stat highlight">{student.score ?? '—'}</td>
          <td className="col-stat">{student.avgTime ? `${student.avgTime}p` : '—'}</td>
        </>
      )}
    </motion.tr>
  );
}

export default function LeaderboardPage() {
  const { slug } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const initialTab = tabParam === 'quiz' ? 'quiz' : 'coding';
  const [activeTab, setActiveTab] = useState(initialTab);

  const [codingData, setCodingData] = useState([]);
  const [quizData, setQuizData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [courseTitle, setCourseTitle] = useState('');
  const [currentUserId, setCurrentUserId] = useState(null);

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;

    const fetchData = async () => {
      try {
        setLoading(true);
        const enrolled = await courseApi.getEnrolled();
        if (cancelled) return;
        const course = findCourseByRouteSlug(enrolled, slug);
        if (!course) {
          if (!cancelled) {
            setError(buildAppErrorState(null, {
              title: 'Không tìm thấy khoá học',
              message: 'Khoá học này không tồn tại hoặc bạn chưa được ghi danh.',
              variant: 'not-found',
              icon: 'travel_explore',
              fallbackPath: '/exercises',
            }));
          }
          setLoading(false);
          return;
        }
        if (!cancelled) {
          setCourseTitle(course.title || '');
        }

        const courseId = course.id;

        const [codingRes, quizRes] = await Promise.allSettled([
          assignmentApi.getCodingLeaderboard(courseId),
          assignmentApi.getQuizLeaderboard(courseId),
        ]);

        if (cancelled) return;

        const coding = codingRes.status === 'fulfilled' ? codingRes.value : [];
        const quiz = quizRes.status === 'fulfilled' ? quizRes.value : [];

        setCodingData(Array.isArray(coding) ? coding : coding?.content ?? []);
        setQuizData(Array.isArray(quiz) ? quiz : quiz?.content ?? []);

        const allUsers = [...(Array.isArray(coding) ? coding : coding?.content ?? []), ...(Array.isArray(quiz) ? quiz : quiz?.content ?? [])];
        const me = allUsers.find((u) => u.currentUser);
        if (me) setCurrentUserId(me.userId || me.id);
      } catch (err) {
        if (!cancelled) {
          setError(buildAppErrorState(err, {
            title: 'Không thể tải bảng xếp hạng',
            fallbackPath: slug ? `/exercises/code/${slug}` : '/exercises',
          }));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchData();
    return () => {
      cancelled = true;
    };
  }, [slug]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchParams({ tab }, { replace: true });
  };

  const activeData = useMemo(() => {
    const data = activeTab === 'coding' ? codingData : quizData;
    return data.map((s, i) => ({ ...s, rank: i + 1 }));
  }, [activeTab, codingData, quizData]);

  const podium = activeData.slice(0, 3);
  const rest = activeData.slice(3);

  const currentUser = activeData.find((s) => s.currentUser || (currentUserId && (s.userId === currentUserId || s.id === currentUserId)));

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải bảng xếp hạng...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  return (
    <AnimatedPage>
      <div className="exercise-page leaderboard-page">
        <ExerciseBreadcrumb
          items={[
            { label: courseTitle || 'Khoá học', link: `/exercises/code/${slug}` },
            { label: 'Bảng xếp hạng' },
          ]}
        />

        {/* Header */}
        <header className="leaderboard-header">
          <div className="leaderboard-header-content">
            <div className="leaderboard-header-icon">
              <span className="material-symbols-outlined">trophy</span>
            </div>
            <div>
              <h1 className="leaderboard-title">Bảng xếp hạng</h1>
              <p className="leaderboard-subtitle">
                {courseTitle || 'Khoá học'} — Cùng nhau chinh phục thử thách
              </p>
            </div>
          </div>
        </header>

        {/* Tab Switcher */}
        <div className="leaderboard-tabs">
          <button
            className={`leaderboard-tab ${activeTab === 'coding' ? 'active' : ''}`}
            onClick={() => handleTabChange('coding')}
          >
            <span className="material-symbols-outlined">code</span>
            <span>Lập trình</span>
          </button>
          <button
            className={`leaderboard-tab ${activeTab === 'quiz' ? 'active' : ''}`}
            onClick={() => handleTabChange('quiz')}
          >
            <span className="material-symbols-outlined">quiz</span>
            <span>Trắc nghiệm</span>
          </button>
          <div
            className="leaderboard-tab-indicator"
            style={{ left: activeTab === 'coding' ? '5px' : 'calc(50% + 2.5px)' }}
          />
        </div>

        {/* Podium */}
        {podium.length > 0 && (
          <div className="podium-section">
            {/* Reorder for visual: 2nd - 1st - 3rd */}
            <div className="podium-container">
              {[podium[1], podium[0], podium[2]].map(
                (s, i) =>
                  s && (
                    <PodiumCard
                      key={s.userId || s.id || i}
                      rank={s.rank}
                      student={s}
                      isCurrentUser={!!(s.currentUser || (currentUserId && (s.userId === currentUserId || s.id === currentUserId)))}
                    />
                  ),
              )}
            </div>
          </div>
        )}

        {/* Full Table */}
        <div className="leaderboard-table-wrapper">
          <table className="leaderboard-table">
            <thead>
              <tr>
                <th className="col-rank">#</th>
                <th className="col-user">Sinh viên</th>
                {activeTab === 'coding' ? (
                  <>
                    <th className="col-stat">Bài đã giải</th>
                    <th className="col-stat highlight">Điểm số</th>
                    <th className="col-stat">Độ chính xác</th>
                  </>
                ) : (
                  <>
                    <th className="col-stat">Bài đã làm</th>
                    <th className="col-stat highlight">Điểm TB</th>
                    <th className="col-stat">Thời gian TB</th>
                  </>
                )}
              </tr>
            </thead>
            <tbody>
              <AnimatePresence mode="wait">
                {activeData.length === 0 ? (
                  <tr>
                    <td colSpan={5}>
                      <div className="leaderboard-empty">
                        <span className="material-symbols-outlined">person_off</span>
                        <p>Chưa có dữ liệu bảng xếp hạng.</p>
                        <span className="empty-hint">Hãy là người đầu tiên chinh phục thử thách!</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  activeData.map((student) => (
                    <TableRow
                      key={student.userId || student.id || student.rank}
                      rank={student.rank}
                      student={student}
                      type={activeTab}
                      isCurrentUser={!!(student.currentUser || (currentUserId && (student.userId === currentUserId || student.id === currentUserId)))}
                    />
                  ))
                )}
              </AnimatePresence>
            </tbody>
          </table>
        </div>

        {/* Current user floating card (if user is below top 3) */}
        {currentUser && currentUser.rank > 3 && (
          <motion.div
            className="current-user-float"
            initial={{ y: 100, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
          >
            <div className="current-user-float-inner">
              <span className="current-user-float-rank">#{currentUser.rank}</span>
              <div className="user-avatar-sm">
                {currentUser.name?.charAt(0)?.toUpperCase() || '?'}
              </div>
              <span className="current-user-float-name">{currentUser.name}</span>
              <span className="current-user-float-score">{currentUser.score} điểm</span>
              <span className="you-tag">Bạn</span>
            </div>
          </motion.div>
        )}
      </div>
    </AnimatedPage>
  );
}
