import React, { useEffect } from 'react';
import { Outlet } from 'react-router-dom';

// =============================================
// AuthLayout – dùng cho Login / Register
// =============================================

function AuthLayout() {
  useEffect(() => {
    document.documentElement.dataset.routeSurface = 'auth';
    document.documentElement.classList.add('auth-route');
    document.body.classList.add('auth-route');

    return () => {
      delete document.documentElement.dataset.routeSurface;
      document.documentElement.classList.remove('auth-route');
      document.body.classList.remove('auth-route');
    };
  }, []);

  return (
    <div className="auth-layout-wrapper" style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <Outlet />
    </div>
  );
}

export default AuthLayout;
