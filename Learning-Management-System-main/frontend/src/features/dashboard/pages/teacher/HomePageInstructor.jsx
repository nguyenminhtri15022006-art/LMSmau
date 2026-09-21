import React from 'react';
import { motion } from 'framer-motion';
import TeacherHero from '../../components/teacher/InstructorHome/TeacherHero';
import TeacherStats from '../../components/teacher/Common/TeacherStats';
import QuickActions from '../../components/teacher/InstructorHome/QuickActions';
import RecentPerformance from '../../components/teacher/Common/RecentPerformance';
import RecentActivity from '../../components/teacher/InstructorHome/RecentActivity';
import useAuth from '../../../../hooks/useAuth';
import '../../styles/teacher/Common/DashboardTeacher.css';

const HomePageInstructor = () => {
  const { user } = useAuth();
  
  return (
    <div className="dashboard-teacher-container">
      <motion.div 
        className="dashboard-main-content"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        {/* Giữ nguyên cấu trúc cũ: Hero -> Stats -> Actions -> Grid (Performance + Activity) */}
        <TeacherHero teacherName={user?.fullname || "Instructor"} />
        
        <TeacherStats />

        <QuickActions />

        <div className="dashboard-grid-layout">
          <RecentPerformance />
          <RecentActivity />
        </div>
      </motion.div>
    </div>
  );
};

export default HomePageInstructor;
