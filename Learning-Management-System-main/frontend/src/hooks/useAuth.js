import { useContext, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import ROLES from '../constants/roles';

// =============================================
// useAuth – React context-based hook
// =============================================

/**
 * Provides authentication state and helpers for JWT management & RBAC.
 *
 * Usage:
 *   const { user, isAuthenticated, login, logout, hasRole } = useAuth();
 */
export function useAuth() {
  const navigate = useNavigate();
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  const { user, isAuthenticated, loading, login, logout: clearSession, register, setSession, updateUser } = context;

  const logout = useCallback(async () => {
    await clearSession();
    navigate('/', { replace: true });
  }, [clearSession, navigate]);

  /**
   * Checks if the current user has one of the specified roles.
   * @param {string | string[]} roles
   * @returns {boolean}
   */
  const hasRole = useCallback(
    (roles) => {
      if (!user) return false;
      const allowed = Array.isArray(roles) ? roles : [roles];
      return allowed.includes(user.role);
    },
    [user],
  );

  const isAdmin = hasRole(ROLES.ADMIN);
  const isInstructor = hasRole(ROLES.INSTRUCTOR);
  const isStudent = hasRole(ROLES.STUDENT);

  return { user, isAuthenticated, loading, login, logout, register, setSession, updateUser, hasRole, isAdmin, isInstructor, isStudent };
}

export default useAuth;
