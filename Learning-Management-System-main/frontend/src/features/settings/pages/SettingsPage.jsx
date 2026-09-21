import React, { useEffect, useState } from 'react';
import useAuth from '../../../hooks/useAuth';
import { identityApi } from '../../../services/identity.api';
import AnimatedPage from '../../../components/ui/AnimatedPage';
import PersonalInfoSection from '../components/PersonalInfoSection';
import SecuritySection from '../components/SecuritySection';

import '../styles/Settings.css';

export default function SettingsPage() {
  const { user, updateUser } = useAuth();
  const [activeTab, setActiveTab] = useState('personal'); // 'personal' or 'security'

  useEffect(() => {
    identityApi.getProfile()
      .then((res) => {
        const fresh = res?.data?.data ?? res?.data ?? res;
        if (fresh?.userId || fresh?.id) {
          updateUser({ ...user, ...fresh });
        }
      })
      .catch(() => {});
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <AnimatedPage>
      <div className="settings-container">
        {/* Sidebar Nav */}
        <aside className="settings-sidebar">
          <div className="settings-nav-card settings-tabs-card" style={{ background: 'white', borderRadius: '20px', padding: '12px', border: '1px solid #e2e8f0' }}>
            <button
              className={`settings-nav-item ${activeTab === 'personal' ? 'active' : ''}`}
              onClick={() => setActiveTab('personal')}
            >
              <span className="material-symbols-outlined">person</span>
              Thông tin cá nhân
            </button>
            <button
              className={`settings-nav-item ${activeTab === 'security' ? 'active' : ''}`}
              onClick={() => setActiveTab('security')}
            >
              <span className="material-symbols-outlined">security</span>
              Bảo mật
            </button>
          </div>

          <div className="settings-nav-card settings-help-card" style={{ background: 'white', borderRadius: '20px', padding: '24px', border: '1px solid #e2e8f0', marginTop: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div className="settings-help-icon" style={{ width: 40, height: 40, borderRadius: '50%', background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <span className="material-symbols-outlined" style={{ color: '#2563eb', fontSize: '20px' }}>settings</span>
              </div>
              <div className="settings-help-copy">
                <h4 style={{ fontSize: '14px', fontWeight: 700, color: '#1e293b' }}>Cài đặt</h4>
                <p style={{ fontSize: '12px', color: '#64748b' }}>Quản lý tài khoản</p>
              </div>
            </div>
            <p className="settings-help-description" style={{ fontSize: '13px', color: '#64748b', lineHeight: 1.5 }}>
              Cấu hình cài đặt và quản lý tài khoản.
            </p>
          </div>
        </aside>

        {/* Dynamic Content */}
        <main className="settings-content">
          {activeTab === 'personal' ? (
            <PersonalInfoSection user={user} />
          ) : (
            <SecuritySection />
          )}
        </main>
      </div>
    </AnimatedPage>
  );
}
