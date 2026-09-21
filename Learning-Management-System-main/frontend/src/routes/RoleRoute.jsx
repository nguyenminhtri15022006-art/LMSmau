import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useAuth from '../hooks/useAuth';
import ROLES from '../constants/roles';

// =============================================
// RoleRoute – restricts access by role
// =============================================

/**
 * @param {string | string[]} allowedRoles - roles that may access this route
 */
function RoleRoute({ allowedRoles }) {
  const { isAuthenticated, user, hasRole, loading } = useAuth();

  if (loading) {
    return <div className="loading-screen">Loading...</div>;
  }

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  if (!hasRole(allowedRoles)) {
    // Redirect to default page based on user role if they try to access wrong area
    switch (user?.role) {
      case ROLES.ADMIN:
        return <Navigate to="/admin/home" replace />;
      case ROLES.INSTRUCTOR:
        return <Navigate to="/instructor/home" replace />;
      case ROLES.STUDENT:
      default:
        return <Navigate to="/dashboard" replace />;
    }
  }

  return <Outlet />;
}

export default RoleRoute;
