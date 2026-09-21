// components/AuthTabs.jsx
// ==================================
// Tabs chuyển đổi Login / Sign Up
// Props:
//   activeTab: 'login' | 'signup'
//   onTabChange: (tab) => void
// ==================================

import React from 'react';

const TABS = [
    { key: 'login', label: 'Đăng Nhập' },
    { key: 'register', label: 'Đăng Ký' },
];

function AuthTabs({ activeTab, onTabChange }) {
    return (
        <div className="auth-tabs">
            {TABS.map((tab) => (
                <button
                    key={tab.key}
                    className={`auth-tab ${activeTab === tab.key ? 'auth-tab--active' : ''}`}
                    onClick={() => onTabChange(tab.key)}
                    type="button"
                >
                    {tab.label}
                </button>
            ))}
        </div>
    );
}

export default AuthTabs;
