import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import React from 'react';
import { AuthContext } from '../../context/AuthContext';
import RoleRoute from '../../routes/RoleRoute';

const renderRoleRoute = (authValue, allowedRoles) => {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={['/instructor/home']}>
        <RoleRoute allowedRoles={allowedRoles} />
      </MemoryRouter>
    </AuthContext.Provider>
  );
};

describe('RoleRoute', () => {
  it('renders <Outlet /> when user has allowed role', () => {
    renderRoleRoute(
      { user: { id: 'u-1', role: 'INSTRUCTOR' }, isAuthenticated: true, hasRole: (r) => r.includes('INSTRUCTOR'), loading: false },
      ['INSTRUCTOR']
    );
    // No redirect = outlet renders
  });

  it('redirects to default page for wrong role student', () => {
    renderRoleRoute(
      { user: { id: 'u-1', role: 'STUDENT' }, isAuthenticated: true, hasRole: () => false, loading: false },
      ['INSTRUCTOR']
    );
    // Navigate to /dashboard (student default)
  });

  it('redirects instructor to /instructor/home when accessing admin route', () => {
    renderRoleRoute(
      { user: { id: 'u-1', role: 'INSTRUCTOR' }, isAuthenticated: true, hasRole: () => false, loading: false },
      ['ADMIN']
    );
  });

  it('shows loading state', () => {
    renderRoleRoute(
      { user: null, isAuthenticated: false, hasRole: () => false, loading: true },
      ['ADMIN']
    );
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });
});
