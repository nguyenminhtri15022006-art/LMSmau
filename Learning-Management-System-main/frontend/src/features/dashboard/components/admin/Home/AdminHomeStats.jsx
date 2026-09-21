import React, {useEffect, useState} from 'react';
import dashboardApi from '../../../../../services/dashboard.api';

const AdminHomeStats = () => {
    const [statsData, setStatsData] = useState({
        totalStudents: '...',
        newStudentsThisWeek: 0,
        totalInstructors: '...',
        newInstructorsThisMonth: 0,
        totalAdmins: '...',
        totalCourses: '...',
        judgeSuccessRate: '...'
    });

    useEffect(() => {
        let isMounted = true;
        const fetchStats = async () => {
            try {
                const data = await dashboardApi.getStats();
                if (isMounted && data) {
                    setStatsData({
                        totalStudents: (data.totalStudents || 0).toLocaleString(),
                        newStudentsThisWeek: data.newStudentsThisWeek || 0,
                        totalInstructors: (data.totalInstructors || 0).toLocaleString(),
                        newInstructorsThisMonth: data.newInstructorsThisMonth || 0,
                        totalAdmins: (data.totalAdmins || 0).toLocaleString(),
                        totalCourses: (data.totalCourses || 0).toLocaleString(),
                        judgeSuccessRate: data.judgeSuccessRate || '0%'
                    });
                }
            } catch (error) {
                console.error('Failed to fetch admin stats:', error);
            }
        };
        fetchStats();
        return () => {
            isMounted = false;
        };
    }, []);

    const stats = [
        {
            label: 'Sinh viên',
            value: statsData.totalStudents,
            sub: statsData.newStudentsThisWeek > 0 ? `↑ +${statsData.newStudentsThisWeek} tuần này` : null,
            color: 'var(--admin-accent)'
        },
        {
            label: 'Giảng viên',
            value: statsData.totalInstructors,
            sub: statsData.newInstructorsThisMonth > 0 ? `↑ +${statsData.newInstructorsThisMonth} tháng này` : null,
            color: 'var(--admin-purple)'
        },
        {
            label: 'Quản trị viên',
            value: statsData.totalAdmins,
            sub: 'Tài khoản quản trị hệ thống',
            color: 'var(--admin-red)'
        },
        {label: 'Khóa học', value: statsData.totalCourses, sub: null, color: 'var(--admin-green)'},
    ];

    return (
        <div className="admin-stats-grid">
            {stats.map((stat, index) => (
                <div key={index} className="admin-stat-card">
                    <div className="admin-stat-label">{stat.label}</div>
                    <div className="admin-stat-val" style={{color: stat.color}}>{stat.value || '...'}</div>
                    {stat.sub && (
                        <div className={`admin-stat-sub ${stat.subNeg ? 'neg' : ''}`}>{stat.sub}</div>
                    )}
                </div>
            ))}
        </div>
    );
};

export default AdminHomeStats;
