import React from 'react';
import useTheme from '../../../../../hooks/useTheme';
import '../../../styles/admin/AdminTopbar/AdminTopbar.css';

const AdminTopbar = ({title}) => {
    const {isDark, toggleTheme} = useTheme();

    return (
        <header className="admin-topbar">
            <div className="admin-topbar-title">{title}</div>

            <div className="admin-search-box">
                <i className="ti ti-search"></i>
                <span>Tìm kiếm nhanh...</span>
            </div>

            <div className="admin-status-pill admin-pill-green">
                <span>● Hệ thống ổn định</span>
            </div>

            <button
                type="button"
                className="admin-theme-toggle"
                onClick={toggleTheme}
                title={isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối"}
                aria-label={isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối"}
            >
                <span className="material-symbols-outlined">{isDark ? "light_mode" : "dark_mode"}</span>
            </button>

        </header>
    );
};

export default AdminTopbar;
