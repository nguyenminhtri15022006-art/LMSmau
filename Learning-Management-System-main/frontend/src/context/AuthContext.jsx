/**
 * Global auth state — holds current user + JWT tokens.
 *
 * Token storage strategy:
 *   - "Remember me" checked  -> localStorage   (persists across browser restarts)
 *   - "Remember me" unchecked -> sessionStorage (cleared when tab closes)
 *
 * Social login always uses "remember me" (localStorage).
 *
 * The access token is attached to every API request via the Axios
 * interceptor in api.client.js. When a 401 is received, the interceptor
 * attempts a silent refresh using the refresh token.
 */

import React, {createContext, useCallback, useState} from 'react';
import axios from 'axios';
import {gatherDeviceInfo} from '../services/deviceInfo';
import {ENDPOINTS} from '../constants/endpoints';

export const AuthContext = createContext(null);

const normalizeRole = (role) => {
    const value = String(role || '').replace(/^ROLE_/, '').toUpperCase();
    if (value === 'TEACHER') return 'INSTRUCTOR';
    return value;
};

const normalizeUser = (value) => value
    ? {...value, role: normalizeRole(value.role)}
    : value;

const readStoredUser = () => {
    try {
        const stored = localStorage.getItem('user') || sessionStorage.getItem('user');
        return stored ? normalizeUser(JSON.parse(stored)) : null;
    } catch {
        return null;
    }
};

const readStoredAccessToken = () =>
    localStorage.getItem('access_token') || sessionStorage.getItem('access_token');

export const AuthProvider = ({children}) => {
    const [user, setUser] = useState(() => readStoredUser());

    // Tính loading đồng bộ ngay lần render đầu — không delay 1 cycle
    // Tránh AuthContext render: loading=true → loading=false → nháy trắng
    const [loading] = useState(false);

    const currentUser = user ?? readStoredUser();
    const isAuthenticated = Boolean(currentUser && readStoredAccessToken());

    // Persist session tokens + user to the chosen storage.
    // Clears the OTHER storage to avoid stale data.
    const setSession = useCallback((session, remember = true) => {
        if (!session?.accessToken || !session?.refreshToken || !session?.user) {
            throw new Error('Missing authentication session data');
        }

        const storage = remember ? localStorage : sessionStorage;
        const otherStorage = remember ? sessionStorage : localStorage;

        otherStorage.removeItem('access_token');
        otherStorage.removeItem('refresh_token');
        otherStorage.removeItem('user');

        storage.setItem('access_token', session.accessToken);
        storage.setItem('refresh_token', session.refreshToken);
        const normalizedUser = normalizeUser(session.user);

        storage.setItem('user', JSON.stringify(normalizedUser));

        setUser(normalizedUser);
    }, []);

    // Local username/password login
    const login = useCallback(async (username, password, remember) => {
        try {
            const deviceInfo = await gatherDeviceInfo();

            const response = await axios.post(ENDPOINTS.AUTH.LOGIN, {
                username: username,
                password: password,
                ipAddress: deviceInfo.ip,
                deviceInfo: deviceInfo.deviceInfo
            }, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const data = response.data.data;

            setSession(data, remember);
            return data.user;
        } catch (err) {
            console.error('Login error:', err.response?.data || err.message);
            throw new Error(err.response?.data?.message || 'Login failed. Please try again.');
        }
    }, [setSession]);

    const logout = useCallback(async () => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('user');
        sessionStorage.removeItem('access_token');
        sessionStorage.removeItem('refresh_token');
        sessionStorage.removeItem('user');
        setUser(null);
    }, []);

    const register = useCallback(async (userData) => {
        try {
            const response = await axios.post(ENDPOINTS.AUTH.REGISTER, {
                fullname: userData.fullname,
                username: userData.username,
                email: userData.email,
                password: userData.password,
                role: userData.role === 'instructor' ? 'INSTRUCTOR' : 'STUDENT'
            }, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            return response.data;
        } catch (err) {
            console.error('Register error:', err.response?.data || err.message);
            const apiError = err.response?.data;
            const wrappedError = new Error(apiError?.message || err.message || 'Registration failed. Please try again.');
            wrappedError.code = apiError?.code;
            wrappedError.status = apiError?.status;
            wrappedError.response = err.response;
            throw wrappedError;
        }
    }, []);

    const updateUser = useCallback((updatedUser) => {
        const storage = localStorage.getItem('access_token') ? localStorage : sessionStorage;
        const normalizedUser = normalizeUser(updatedUser);
        storage.setItem('user', JSON.stringify(normalizedUser));
        setUser(normalizedUser);
    }, []);

    const hasRole = useCallback(
        (roles) => {
            if (!currentUser) return false;
            const allowed = Array.isArray(roles) ? roles : [roles];
            return allowed.includes(currentUser.role);
        },
        [currentUser]
    );

    const value = {
        user: currentUser,
        currentUser,
        isAuthenticated,
        loading,
        login,
        logout,
        register,
        hasRole,
        setSession,
        updateUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
