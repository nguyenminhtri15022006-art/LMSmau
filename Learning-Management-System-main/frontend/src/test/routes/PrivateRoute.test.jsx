import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import React from 'react';
import { AuthContext } from '../../context/AuthContext';
import PrivateRoute from '../../routes/PrivateRoute';

const renderPrivateRoute = (authValue) => {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<PrivateRoute />}>
            <Route path="/dashboard" element={<div>Protected dashboard</div>} />
          </Route>
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
};

describe('PrivateRoute', () => {
  const authenticatedUser = {
    user: { id: 'u-1', role: 'STUDENT' },
    isAuthenticated: true,
    loading: false,
  };

  const unauthenticatedUser = {
    user: null,
    isAuthenticated: false,
    loading: false,
  };

  it('renders <Outlet /> when authenticated', () => {
    renderPrivateRoute(authenticatedUser);
    expect(screen.getByText('Protected dashboard')).toBeInTheDocument();
  });

  it('redirects to /login when not authenticated', () => {
    renderPrivateRoute(unauthenticatedUser);
    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('shows loading spinner while checking auth', () => {
    const { container } = renderPrivateRoute({ ...unauthenticatedUser, loading: true });
    expect(container.querySelector('.loading-screen__spinner')).toBeInTheDocument();
  });
});
