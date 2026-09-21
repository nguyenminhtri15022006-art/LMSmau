import React, { useState, useEffect} from 'react';
import { motion } from 'framer-motion';
import AnimatedPage from "../../../../components/ui/AnimatedPage";
import DashboardStats from "../../components/student/DashboardStats";
import ContinueLearning from "../../components/student/ContinueLearning";
import CourseProgressList from "../../components/student/CourseProgressList";
import DashboardSidebar from "../../components/student/DashboardSidebar";
import useAuth from "../../../../hooks/useAuth";
import dashboardApi from "../../../../services/dashboard.api.js";
import "../../styles/student/DashboardStudent.css";
import { formatDateVN, formatTimeVN, parseBackendUtcDate, VIETNAM_TIME_ZONE } from "../../../../utils/dateTime";

// Chuyển LocalDateTime thành chuỗi tương đối
function formatRelativeDate(dateStr) {
  if (!dateStr) return '—';
  const date = parseBackendUtcDate(dateStr);
  if (!date) return '—';
  const diffDays = Math.floor((Date.now() - date.getTime()) / 86400000);
  if (diffDays < 0) return 'Hôm nay';
  if (diffDays === 0) return 'Hôm nay';
  if (diffDays === 1) return 'Hôm qua';
  if (diffDays < 7) return `${diffDays} ngày trước`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} tuần trước`;
  return formatDateVN(date, { day: 'numeric', month: 'numeric', year: 'numeric' });
}

const FALLBACK_IMG = 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80';

const MONTHS_VI = ['Th1','Th2','Th3','Th4','Th5','Th6','Th7','Th8','Th9','Th10','Th11','Th12'];

function mapScheduleToEvent(schedule) {
  const date = parseBackendUtcDate(schedule.startTime);
  if (!date) {
    return {
      title: schedule.title,
      time: 'Bắt đầu lúc —',
      day: '',
      month: '',
      meetingUrl: schedule.meetingUrl ?? null,
      courseId: schedule.courseId ?? null,
    };
  }
  const parts = new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    day: 'numeric',
    month: 'numeric',
  }).formatToParts(date);
  const day = parts.find((part) => part.type === 'day')?.value ?? '';
  const month = Number(parts.find((part) => part.type === 'month')?.value ?? 1);
  return {
    title: schedule.title,
    time: `Bắt đầu lúc ${formatTimeVN(schedule.startTime)}`,
    day,
    month: MONTHS_VI[month - 1],
    meetingUrl: schedule.meetingUrl ?? null,
    courseId: schedule.courseId ?? null,
  };
}

export default function DashboardStudent() {
  const { user } = useAuth();
  const [courses, setCourses] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    dashboardApi.getStudentDashboardData()
        .then(({ courses, schedules }) => {
          setCourses(courses);
          setSchedules(schedules);
        })
        .catch(err => {
          console.error('Không thể tải dữ liệu dashboard:', err);
          setError(true);
        })
        .finally(() => setLoading(false));
  }, []);

  const completedCount = courses.filter(c => (c.progressData?.percentComplete ?? 0) >= 100).length;
  const inProgressCount = courses.filter(c => {
    const p = c.progressData?.percentComplete ?? 0;
    return p > 0 && p < 100;
  }).length;
  const notCompletedCount = courses.length - completedCount;
  const totalLessonsCompleted = courses.reduce((sum, c) => sum + (c.progressData?.completedLessons ?? 0), 0);

  const stats = [
    { label: 'Đã đăng ký', value: String(courses.length), icon: 'auto_stories', color: '#eff6ff', textColor: '#2563eb'},
    { label: 'Hoàn thành', value: String(completedCount), icon: 'task_alt', color: '#ecfdf5', textColor: '#10b981'},
    { label: 'Đang học', value: String(inProgressCount), icon: 'pending_actions', color: '#fff7ed', textColor: '#f97316'},
    { label: 'Bài đã học', value: String(totalLessonsCompleted), icon: 'menu_book', color: '#faf5ff', textColor: '#a855f7'}
  ];

  const activeCourse = [...courses]
    .sort((a, b) => (b.progressData?.percentComplete ?? 0) - (a.progressData?.percentComplete ?? 0))
    .find(c => {
      const p = c.progressData?.percentComplete ?? 0;
      return p > 0 && p < 100;
    }) ?? null;

  const continueLearningData = activeCourse ? {
    courseId: activeCourse.id,
    title: activeCourse.title,
    category: activeCourse.categories?.[0]?.name ?? 'Khóa học',
    image: activeCourse.thumbnailUrl ?? FALLBACK_IMG,
    description: activeCourse.description ?? '',
    progress: Math.round(activeCourse.progressData?.percentComplete ?? 0),
    lessonsDone: activeCourse.progressData?.completedLessons ?? 0,
    totalLessons: activeCourse.progressData?.totalLessons ?? 0,
    lastActive: formatRelativeDate(activeCourse.updatedAt),
  } : null;

  const coursesProgressData = courses.map(c => ({
    id: c.id,
    title: c.title,
    image: c.thumbnailUrl ?? FALLBACK_IMG,
    progress: Math.round(c.progressData?.percentComplete ?? 0),
    status: (c.progressData?.percentComplete ?? 0) >= 100 ? 'Completed' : 'In Progress',
    date: formatRelativeDate(c.updatedAt),
  }));

  if (loading) {
    return (
        <AnimatedPage>
          <div className="dashboard-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
            <p style={{ color: '#64748b' }}>Đang tải...</p>
          </div>
        </AnimatedPage>
    );
  }

  if (error) {
    return (
        <AnimatedPage>
          <div className="dashboard-container" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', minHeight: '300px', gap: '16px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '48px', color: '#ef4444' }}>error</span>
            <p style={{ color: '#ef4444', fontWeight: 600 }}>Không thể tải dữ liệu. Vui lòng thử lại.</p>
            <button className="btn-primary" onClick={() => window.location.reload()}>
              Tải lại trang
            </button>
          </div>
        </AnimatedPage>
    );
  }

  return (
      <AnimatedPage>
        <div className="dashboard-container">
          <motion.header
            className="dashboard-welcome"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, ease: 'easeOut' }}
          >
            <h1>Chào mừng trở lại, {user?.fullname || user?.username || 'bạn'}!</h1>
            <p>
              {notCompletedCount > 0
                  ? `Bạn đang có ${notCompletedCount} khóa học chưa hoàn thành.`
                  : completedCount > 0
                      ? `Bạn đã hoàn thành ${completedCount} khóa học. Tiếp tục phát huy nhé!`
                      : courses.length > 0
                          ? `Bạn đã đăng ký ${courses.length} khóa học. Hãy bắt đầu bài học đầu tiên ngay!`
                          : 'Hãy đăng ký một khóa học để bắt đầu hành trình học tập!'}
            </p>
          </motion.header>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.42, ease: 'easeOut', delay: 0.08 }}
          >
            <DashboardStats stats={stats} />
          </motion.div>

          <motion.div
            className="dashboard-main-grid"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, ease: 'easeOut', delay: 0.16 }}
          >
            <div className="dashboard-content-left">
              {continueLearningData && <ContinueLearning data={continueLearningData} />}
              {coursesProgressData.length > 0
                  ? <CourseProgressList courses={coursesProgressData} />
                  : <p style={{ color: '#94a3b8', marginTop: '16px' }}>Bạn chưa đăng ký khóa học nào.</p>
              }
            </div>

            <aside className="dashboard-content-right">
              <DashboardSidebar events={schedules.map(mapScheduleToEvent)} />
            </aside>
          </motion.div>
        </div>
      </AnimatedPage>
  );

}
