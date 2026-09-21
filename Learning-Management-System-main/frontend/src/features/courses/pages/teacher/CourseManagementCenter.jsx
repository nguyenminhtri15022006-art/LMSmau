import React, { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGear, faVideo } from '@fortawesome/free-solid-svg-icons';
import { Link, Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import SchedulesCommunication from '../../components/teacher/ManagementCenter/SchedulesCommunication.jsx';
import LearningAnalytics from '../../components/teacher/ManagementCenter/LearningAnalytics.jsx';
import StudentManagementHub from '../../components/teacher/ManagementCenter/StudentManagementHub.jsx';
import AddStudentModal from '../../components/teacher/ManagementCenter/AddStudentModal.jsx';
import { useToast } from '../../../../components/ui/Toast';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import courseApi from '../../../../services/course.api.js';
import '../../styles/teacher/Pages/courseManagementCenter.css';

const CourseManagementCenter = () => {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [isModalOpen,    setIsModalOpen]    = useState(false);
  const [schedules,      setSchedules]      = useState([]);
  const [announcements,  setAnnouncements]  = useState([]);
  const [courseTitle,    setCourseTitle]    = useState('');
  const [meetingUrl,     setMeetingUrl]     = useState('');
  const [studentCount,   setStudentCount]   = useState(0);
  const [lessonCount,    setLessonCount]    = useState(0);
  const [loading,        setLoading]        = useState(true);
  const [error,          setError]          = useState(null);

  const fetchInitialData = useCallback(async () => {
    if (!courseId) return;
    setLoading(true);
    try {
      const [schedData, annPage, courseData, sc, lc] = await Promise.all([
        courseApi.getSchedules(courseId),
        courseApi.getAnnouncements(courseId, 0, 20),
        courseApi.getById(courseId).then(res => res?.data?.data ?? res?.data ?? null),
        courseApi.getStudentsCount(courseId),
        courseApi.getLessonsCount(courseId),
      ]);
      setSchedules(schedData);
      setAnnouncements(annPage.content ?? []);
      setStudentCount(sc ?? 0);
      setLessonCount(lc ?? 0);
      if (courseData) {
        setCourseTitle(courseData.title ?? '');
        setMeetingUrl(courseData.meetingUrl ?? '');
      }
      setError(null);
    } catch (err) {
      console.error('Lỗi tải dữ liệu trung tâm quản lý:', err);
      setError(buildAppErrorState(err, {
        title: 'Không thể tải trung tâm quản lý khóa học',
        fallbackPath: '/manage/courses',
      }));
    } finally {
      setLoading(false);
    }
  }, [courseId]);

  useEffect(() => { fetchInitialData(); }, [fetchInitialData]);

  // --- Schedule handlers ---
  const handleCreateSchedule = async (scheduleData) => {
    try {
      const created = await courseApi.createSchedule(courseId, scheduleData);
      setSchedules(prev => [...prev, created]);
    } catch (err) {
      console.error('Lỗi tạo lịch:', err);
      toast.error(err?.response?.data?.message || 'Không thể tạo lịch học. Vui lòng thử lại.');
    }
  };

  const handleRemoveSchedule = async (scheduleId) => {
    try {
      await courseApi.deleteSchedule(courseId, scheduleId);
      setSchedules(prev => prev.filter(s => s.id !== scheduleId));
    } catch (err) {
      console.error('Lỗi xóa lịch:', err);
      toast.error(err?.response?.data?.message || 'Không thể xóa lịch học.');
    }
  };

  // --- Announcement handlers ---
  const handlePostAnnouncement = async ({ title, content }) => {
    try {
      const created = await courseApi.createAnnouncement(courseId, { title, content });
      setAnnouncements(prev => [created, ...prev]);
    } catch (err) {
      console.error('Lỗi tạo thông báo:', err);
      toast.error(err?.response?.data?.message || 'Không thể đăng thông báo. Vui lòng thử lại.');
    }
  };

  const handleDeleteAnnouncement = async (announcementId) => {
    try {
      await courseApi.deleteAnnouncement(courseId, announcementId);
      setAnnouncements(prev => prev.filter(a => a.id !== announcementId));
    } catch (err) {
      console.error('Lỗi xóa thông báo:', err);
      toast.error(err?.response?.data?.message || 'Không thể xóa thông báo.');
    }
  };

  if (error) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh', color: '#94a3b8' }}>
        Đang tải...
      </div>
    );
  }

  return (
    <div className="management-center-container">
      <motion.div
        className="center-inner"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <header className="center-header">
          <div className="center-header-info">
            <div className="breadcrumbs">
              <Link to="/manage/courses">Khóa học</Link>
              <span className="material-symbols-outlined" style={{ fontSize: '14px', color: '#cbd5e1' }}>chevron_right</span>
              <span>Trung tâm quản lý</span>
            </div>
            <h1>{courseTitle || 'Đang tải...'}</h1>
          </div>
          <div className="center-header-actions">
            <button className="btn-settings" onClick={() => navigate(`/manage/courses/edit/${courseId}`)}>
              <FontAwesomeIcon icon={faGear} />
              Cài đặt
            </button>
            <button
              className="btn-google-meet"
              onClick={() => window.open(meetingUrl || 'https://meet.google.com/new', '_blank')}
            >
              <FontAwesomeIcon icon={faVideo} />
              Tham gia Google Meet
            </button>
          </div>
        </header>

        <div className="center-sections-gap">
          <SchedulesCommunication
            courseId={courseId}
            schedules={schedules}
            announcements={announcements}
            onCreateSchedule={handleCreateSchedule}
            onRemoveSchedule={handleRemoveSchedule}
            onPostAnnouncement={handlePostAnnouncement}
            onDeleteAnnouncement={handleDeleteAnnouncement}
          />
          <LearningAnalytics courseId={courseId} studentCount={studentCount} lessonCount={lessonCount} />
          <StudentManagementHub
            courseId={courseId}
            onAddClick={() => setIsModalOpen(true)}
          />
        </div>

        <AddStudentModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          courseId={courseId}
        />
      </motion.div>
    </div>
  );
};

export default CourseManagementCenter;
