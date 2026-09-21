import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

// =============================================
// PrivateRoute – requires authenticated user
// =============================================

function PrivateRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  // Chờ AuthContext đọc xong localStorage trước khi quyết định redirect
  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-screen__spinner" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}

export default PrivateRoute;
