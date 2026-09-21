import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import React from 'react';

// Mock axios
vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
  },
}));

vi.mock('../../services/deviceInfo', () => ({
  gatherDeviceInfo: vi.fn(() => Promise.resolve({ ip: '127.0.0.1', deviceInfo: 'Test Browser' })),
}));

import axios from 'axios';
import { AuthProvider, AuthContext } from '../../context/AuthContext';

/**
 * Helper: A component that reads context and exposes functions + renders current state.
 */
function AuthStateReader({ onReady }) {
  const ctx = React.useContext(AuthContext);
  React.useEffect(() => {
    if (onReady) onReady(ctx);
  }, [ctx, onReady]);
  return React.createElement('div', { 'data-testid': 'auth-state' },
    ctx.user ? `${ctx.user.id}|${ctx.user.role}` : 'null'
  );
}

function renderWithAuth() {
  const ref = {};
  render(
    React.createElement(AuthProvider, null,
      React.createElement(AuthStateReader, { onReady: (ctx) => { Object.assign(ref, ctx); } })
    )
  );
  return ref;
}

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  vi.clearAllMocks();
});

describe('AuthProvider', () => {
  it('renders children without crashing', () => {
    render(
      React.createElement(AuthProvider, null,
        React.createElement('div', { 'data-testid': 'child' }, 'Hello')
      )
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });

  it('initializes with no user when storage is empty', () => {
    const ref = renderWithAuth();
    expect(ref.user).toBeNull();
    expect(ref.isAuthenticated).toBe(false);
  });

  it('restores user from localStorage', () => {
    const user = { id: 'u-1', username: 'john', role: 'STUDENT', email: 'john@test.com' };
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));

    const ref = renderWithAuth();
    expect(ref.user).not.toBeNull();
    expect(ref.user.username).toBe('john');
  });

  it('normalizes TEACHER role to INSTRUCTOR on restore', () => {
    const user = { id: 'u-1', username: 'teach', role: 'TEACHER' };
    localStorage.setItem('access_token', 'token');
    localStorage.setItem('user', JSON.stringify(user));

    const ref = renderWithAuth();
    expect(ref.user.role).toBe('INSTRUCTOR');
  });

  it('ignores invalid stored user JSON', () => {
    localStorage.setItem('access_token', 'token');
    localStorage.setItem('user', '{bad-json');

    const ref = renderWithAuth();
    expect(ref.user).toBeNull();
    expect(ref.isAuthenticated).toBe(false);
  });

  it('throws when setting an incomplete session', () => {
    const ref = renderWithAuth();
    expect(() => ref.setSession({ accessToken: 'access-only' })).toThrow('Missing authentication session data');
  });
});

describe('AuthContext login', () => {
  it('calls login API and stores session', async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        data: {
          accessToken: 'access-1',
          refreshToken: 'refresh-1',
          user: { id: 'u-1', username: 'john', role: 'STUDENT' },
        },
      },
    });

    const ref = renderWithAuth();
    await ref.login('john', 'pass123', true);

    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/login'),
      expect.objectContaining({ username: 'john' }),
      expect.any(Object),
    );
    expect(localStorage.getItem('access_token')).toBe('access-1');
  });

  it('stores in sessionStorage when remember=false', async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        data: {
          accessToken: 'access-2',
          refreshToken: 'refresh-2',
          user: { id: 'u-2', username: 'jane', role: 'STUDENT' },
        },
      },
    });

    const ref = renderWithAuth();
    await ref.login('jane', 'pass456', false);

    expect(sessionStorage.getItem('access_token')).toBe('access-2');
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('throws API error message when login fails', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    axios.post.mockRejectedValueOnce({
      response: { data: { message: 'Invalid credentials' } },
    });

    const ref = renderWithAuth();
    await expect(ref.login('john', 'bad-pass', true)).rejects.toThrow('Invalid credentials');
    consoleSpy.mockRestore();
  });
});

describe('AuthContext logout', () => {
  it('clears all storage and sets user to null', async () => {
    localStorage.setItem('access_token', 'token');
    localStorage.setItem('refresh_token', 'refresh');
    localStorage.setItem('user', JSON.stringify({ id: 'u-1', role: 'STUDENT' }));

    const ref = renderWithAuth();

    await act(async () => {
      await ref.logout();
    });

    expect(localStorage.getItem('access_token')).toBeNull();
    await waitFor(() => {
      expect(screen.getByTestId('auth-state').textContent).toBe('null');
    });
  });
});

describe('AuthContext hasRole', () => {
  it('checks role correctly', () => {
    localStorage.setItem('access_token', 'token');
    localStorage.setItem('user', JSON.stringify({ id: 'u-1', role: 'INSTRUCTOR' }));

    const ref = renderWithAuth();
    expect(ref.hasRole('INSTRUCTOR')).toBe(true);
    expect(ref.hasRole('STUDENT')).toBe(false);
    expect(ref.hasRole(['INSTRUCTOR', 'ADMIN'])).toBe(true);
    expect(ref.hasRole(['STUDENT', 'ADMIN'])).toBe(false);
  });
});

describe('AuthContext register', () => {
  it('calls register API and returns response', async () => {
    axios.post.mockResolvedValueOnce({
      data: { data: { id: 'u-1', username: 'newuser' }, code: '1000', message: 'OK' },
    });

    const ref = renderWithAuth();
    const userData = {
      fullname: 'New User',
      username: 'newuser',
      email: 'new@test.com',
      password: 'pass123',
      role: 'student',
    };
    const response = await ref.register(userData);

    expect(response).toEqual({ data: { id: 'u-1', username: 'newuser' }, code: '1000', message: 'OK' });
    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/register'),
      expect.objectContaining({ username: 'newuser', role: 'STUDENT' }),
      expect.any(Object),
    );
  });

  it('normalizes instructor role to INSTRUCTOR on register', async () => {
    axios.post.mockResolvedValueOnce({ data: {} });

    const ref = renderWithAuth();
    await ref.register({
      fullname: 'Instructor',
      username: 'inst',
      email: 'inst@test.com',
      password: 'pass',
      role: 'instructor',
    });

    expect(axios.post).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ role: 'INSTRUCTOR' }),
      expect.any(Object),
    );
  });

  it('wraps register API errors with metadata', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    axios.post.mockRejectedValueOnce({
      response: {
        data: { message: 'Email already exists', code: 1002, status: 409 },
      },
    });

    const ref = renderWithAuth();
    await expect(ref.register({
      fullname: 'New User',
      username: 'newuser',
      email: 'new@test.com',
      password: 'pass',
      role: 'student',
    })).rejects.toMatchObject({
      message: 'Email already exists',
      code: 1002,
      status: 409,
    });
    consoleSpy.mockRestore();
  });
});

describe('AuthContext updateUser', () => {
  it('updates user in provider and storage', () => {
    localStorage.setItem('access_token', 'token');
    localStorage.setItem('user', JSON.stringify({ id: 'u-1', role: 'STUDENT' }));

    const ref = renderWithAuth();
    act(() => ref.updateUser({ id: 'u-1', role: 'INSTRUCTOR', name: 'Updated' }));

    expect(screen.getByTestId('auth-state').textContent).toBe('u-1|INSTRUCTOR');
    const stored = JSON.parse(localStorage.getItem('user'));
    expect(stored.name).toBe('Updated');
  });

  it('updates user in sessionStorage when session token is active there', () => {
    sessionStorage.setItem('access_token', 'token');
    sessionStorage.setItem('user', JSON.stringify({ id: 'u-2', role: 'STUDENT' }));

    const ref = renderWithAuth();
    act(() => ref.updateUser({ id: 'u-2', role: 'TEACHER', name: 'Session User' }));

    expect(screen.getByTestId('auth-state').textContent).toBe('u-2|INSTRUCTOR');
    expect(JSON.parse(sessionStorage.getItem('user')).name).toBe('Session User');
  });
});
