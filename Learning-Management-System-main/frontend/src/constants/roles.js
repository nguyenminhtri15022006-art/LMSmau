// =============================================
// Role-Based Access Control (RBAC) Constants
// =============================================

export const ROLES = {
  ADMIN: 'ADMIN',
  INSTRUCTOR: 'INSTRUCTOR',
  STUDENT: 'STUDENT',
};

/**
 * Permission matrix per role
 */
export const PERMISSIONS = {
  [ROLES.ADMIN]: [
    'manage:users',
    'manage:courses',
    'manage:assignments',
    'manage:blog',
    'view:analytics',
  ],
  [ROLES.INSTRUCTOR]: [
    'manage:courses',
    'manage:assignments',
    'view:students',
    'grade:assignments',
  ],
  [ROLES.STUDENT]: [
    'view:courses',
    'enroll:courses',
    'submit:assignments',
    'read:blog',
    'write:blog',
  ],
};

export const DEFAULT_ROLE = ROLES.STUDENT;

export default ROLES;
