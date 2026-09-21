import React, { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';
import useAuth from '../hooks/useAuth';
import ROLES from '../constants/roles';

const StudentHome = lazy(() => import('../features/home/pages/HomePage'));

function FallbackRoute() {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) return <StudentHome />;

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

export default FallbackRoute;
