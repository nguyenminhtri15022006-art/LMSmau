import React, { useState, useEffect } from 'react';
import dashboardApi from '../../../../../services/dashboard.api';

const SystemNotifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchNotifications = async () => {
      setLoading(true);
      try {
        const data = await dashboardApi.getNotifications();
        setNotifications(data);
      } catch (error) {
        console.error('Failed to fetch system notifications:', error);
        setNotifications([
          {
            id: 'system-notifications-error',
            type: 'warning',
            icon: 'ti-alert-circle',
            title: 'Không thể tải thông báo hệ thống',
            desc: 'Cập nhật vừa xong · Admin Dashboard',
          },
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchNotifications();
  }, []);

  const getIconColor = (type) => {
    switch (type) {
      case 'danger': return 'var(--admin-red)';
      case 'warning': return 'var(--admin-yellow)';
      case 'info': return 'var(--admin-accent)';
      case 'success': return 'var(--admin-green)';
      default: return 'var(--admin-muted)';
    }
  };

  return (
    <div className="admin-card">
      <div className="admin-card-hd">
        <i className="ti ti-bell"></i>
        Thông báo hệ thống
      </div>
      <div className="admin-card-body">
        {loading ? (
          <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>Đang tải thông báo...</div>
        ) : (
          notifications.map((note) => (
            <div key={note.id} className="admin-service-row">
              <i className={`ti ${note.icon}`} style={{ color: getIconColor(note.type), fontSize: '1.2rem' }}></i>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.9rem', fontWeight: 600 }}>{note.title}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--admin-muted)' }}>{note.desc}</div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default SystemNotifications;
