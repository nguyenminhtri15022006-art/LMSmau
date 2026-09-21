import React, {useEffect, useMemo, useState} from 'react';
import useAuth from '../../../../../hooks/useAuth';
import '../../../styles/admin/AdminSidebar/AdminSidebar.css';

const NAV_GROUPS = [
    {
        title: 'Tổng quan',
        items: [
            {id: 'dashboard', label: 'Dashboard', icon: 'ti-dashboard'},
        ]
    },
    {
        title: 'Quản lý',
        items: [
            {id: 'users', label: 'Người dùng', icon: 'ti-users'},
            {id: 'admins', label: 'Quản trị viên', icon: 'ti-shield'},
            {id: 'courses', label: 'Khóa học', icon: 'ti-book'},
            {id: 'judge', label: 'Code Judge', icon: 'ti-code'},
            {id: 'blog', label: 'Diễn đàn', icon: 'ti-message-circle'},
        ]
    },
    {
        title: 'Nội dung Web',
        items: [
            {id: 'webcontent', label: 'Hình & Thẻ', icon: 'ti-photo'},
        ]
    },
    {
        title: 'Cài đặt',
        items: [
            {id: 'settings', label: 'Cài đặt tài khoản', icon: 'ti-settings'},
        ]
    }
];

const AdminSidebar = ({activePage, onPageChange}) => {
    const {user, logout} = useAuth();
    const [imgError, setImgError] = useState(false);
    const avatarSrc = useMemo(() => user?.avatarUrl || user?.avatar || user?.image || '', [user]);
    const avatarFallback = user?.fullname?.charAt(0) || user?.username?.charAt(0) || 'A';

    useEffect(() => {
        setImgError(false);
    }, [avatarSrc]);

    return (
        <aside className="admin-sidebar">
            <div className="admin-sidebar-logo">
                <img className="admin-logo-img" src="/LMSicon.png" alt="LMSicon Logo"/>
                <div className="admin-logo-txt">DevNest</div>
            </div>

            <nav style={{flex: 1}}>
                {NAV_GROUPS.map((group) => (
                    <div key={group.title}>
                        <div className="admin-nav-section">{group.title}</div>
                        {group.items.map((item) => (
                            <div
                                key={item.id}
                                className={`admin-nav-item ${activePage === item.id ? 'active' : ''}`}
                                onClick={() => onPageChange(item.id)}
                            >
                                <i className={`ti ${item.icon}`}></i>
                                <span>{item.label}</span>
                                {item.badge && <span className="admin-badge-dot"></span>}
                            </div>
                        ))}
                    </div>
                ))}
            </nav>

            {/* Improved Logout Section */}
            <div style={{padding: '16px', borderTop: '1px solid var(--admin-border)', background: 'rgba(0,0,0,0.1)'}}>
                <div style={{display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px'}}>
                    <div className="admin-avatar"
                         style={{overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                        {avatarSrc && !imgError ? (
                            <img
                                src={avatarSrc}
                                alt="Avatar"
                                referrerPolicy="no-referrer"
                                crossOrigin="anonymous"
                                onError={() => setImgError(true)}
                                style={{width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%'}}
                            />
                        ) : (
                            avatarFallback
                        )}
                    </div>
                    <div style={{flex: 1, minWidth: 0}}>
                        <div style={{
                            fontSize: '0.85rem',
                            fontWeight: '600',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                        }}>
                            {user?.fullname || 'Administrator'}
                        </div>
                        <div style={{
                            fontSize: '0.7rem',
                            color: 'var(--admin-muted)',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                        }}>
                            {user?.email || 'admin@edu.vn'}
                        </div>
                    </div>
                </div>
                <button
                    className="admin-btn admin-btn-danger"
                    style={{width: '100%', justifyContent: 'center', padding: '6px'}}
                    onClick={logout}
                >
                    <i className="ti ti-logout"></i>
                    Đăng xuất
                </button>
            </div>
        </aside>
    );
};

export default AdminSidebar;
