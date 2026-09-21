import React, {Suspense} from 'react';
import {Outlet, useLocation, useNavigate} from 'react-router-dom';
import AdminSidebar from '../../features/dashboard/components/admin/Shared/AdminSidebar';
import AdminTopbar from '../../features/dashboard/components/admin/Shared/AdminTopbar';
import '../../features/dashboard/styles/admin/AdminLayout/AdminLayout.css';
import '../../features/dashboard/styles/admin/AdminCommon/AdminCommon.css';

import PageLoader from '../../components/ui/PageLoader';

function AdminLayout() {
    const location = useLocation();
    const navigate = useNavigate();

    const getPageTitle = () => {
        const path = location.pathname;
        if (/^\/admin\/all-courses\/[^/]+/.test(path)) return 'Xem khóa học';
        if (/^\/admin\/blog\/[^/]+/.test(path)) return 'Xem bài viết';
        if (path.includes('/admin/home')) return 'Dashboard Tổng quan';
        if (path.includes('/admin/admins')) return 'Quản lý Quản trị viên';
        if (path.includes('/admin/users')) return 'Quản lý Người dùng';
        if (path.includes('/admin/all-courses')) return 'Quản trị Khóa học';
        if (path.includes('/admin/web-content')) return 'Nội dung Website';
        if (path.includes('/admin/judge')) return 'Giám sát Code Judge';
        if (path.includes('/admin/blog')) return 'Kiểm duyệt Diễn đàn';
        if (path.includes('/admin/settings')) return 'Cài đặt tài khoản';
        return 'Admin Panel';
    };

    const getActivePage = () => {
        const path = location.pathname;
        if (path.includes('/admin/home')) return 'dashboard';
        if (path.includes('/admin/admins')) return 'admins';
        if (path.includes('/admin/users')) return 'users';
        if (path.includes('/admin/all-courses')) return 'courses';
        if (path.includes('/admin/web-content')) return 'webcontent';
        if (path.includes('/admin/judge')) return 'judge';
        if (path.includes('/admin/blog')) return 'blog';
        if (path.includes('/admin/settings')) return 'settings';
        return '';
    };

    const handlePageChange = (id) => {
        switch (id) {
            case 'dashboard':
                navigate('/admin/home');
                break;
            case 'admins':
                navigate('/admin/admins');
                break;
            case 'users':
                navigate('/admin/users');
                break;
            case 'courses':
                navigate('/admin/all-courses');
                break;
            case 'webcontent':
                navigate('/admin/web-content');
                break;
            case 'judge':
                navigate('/admin/judge');
                break;
            case 'blog':
                navigate('/admin/blog');
                break;
            case 'settings':
                navigate('/admin/settings');
                break;
            default:
                break;
        }
    };

    return (
        <div className="admin-shell">
            <AdminSidebar activePage={getActivePage()} onPageChange={handlePageChange}/>

            <main className="admin-main">
                <AdminTopbar title={getPageTitle()}/>

                <div className="admin-content-area">
                    <Suspense fallback={<PageLoader/>}>
                        <Outlet/>
                    </Suspense>
                </div>
            </main>
        </div>
    );
}

export default AdminLayout;
