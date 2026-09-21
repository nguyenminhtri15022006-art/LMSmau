// pages/AuthPage.jsx
// ==================================
// Trang Auth - ghép LoginForm + HeroPanel
// Đây là "page" duy nhất của flow này
// ==================================

import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import '../styles/AuthPage.css';
import LoginForm from '../components/LoginForm';
import HeroPanel from '../components/HeroPanel';
import SignUp from '../components/SignUpForm';
import useAuth from '../../../hooks/useAuth';
import ROLES from '../../../constants/roles';

const AUTH_ROUTES = ['/login', '/register', '/forgot-password', '/authenticate'];

const getPathFromLocation = (value) => {
    if (!value) return null;
    if (typeof value === 'string') return value;
    const pathname = value.pathname || '';
    if (!pathname) return null;
    return `${pathname}${value.search || ''}${value.hash || ''}`;
};

const getDefaultPathByRole = (role) => {
    switch (role) {
        case ROLES.ADMIN:
            return '/admin/home';
        case ROLES.INSTRUCTOR:
            return '/instructor/home';
        case ROLES.STUDENT:
        default:
            return '/dashboard';
    }
};

const isAllowedReturnPath = (path, role) => {
    if (!path || !path.startsWith('/') || path.startsWith('//')) return false;
    if (AUTH_ROUTES.some(route => path === route || path.startsWith(`${route}?`))) return false;

    if (role === ROLES.ADMIN) {
        return path.startsWith('/admin') || path.startsWith('/manage') || !path.startsWith('/instructor');
    }

    if (role === ROLES.INSTRUCTOR) {
        return !path.startsWith('/admin') && !path.startsWith('/dashboard') && !path.startsWith('/my-courses') && !path.startsWith('/exercises');
    }

    return !path.startsWith('/admin') && !path.startsWith('/instructor') && !path.startsWith('/manage');
};

function AuthPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const { login, register } = useAuth();

    const [activeTab, setActiveTab] = useState(location.pathname === '/register' ? 'register' : 'login');
    const [loginError, setLoginError] = useState('');
    const [loginSuccess, setLoginSuccess] = useState('');
    const [isLoggingIn, setIsLoggingIn] = useState(false);
    const [resetData, setResetData] = useState(null);
    const [signupFieldErrors, setSignupFieldErrors] = useState({});
    const [signupMessage, setSignupMessage] = useState(null);

    const getRedirectPathAfterLogin = (role) => {
        const requestedPath = getPathFromLocation(location.state?.from) || sessionStorage.getItem('auth_return_to');
        sessionStorage.removeItem('auth_return_to');
        return isAllowedReturnPath(requestedPath, role)
            ? requestedPath
            : getDefaultPathByRole(role);
    };

    useEffect(() => {
        if (location.pathname === '/register') {
            setActiveTab('register');
        } else if (location.pathname === '/login') {
            setActiveTab('login');
        }
        setSignupMessage(null);
        setSignupFieldErrors({});
    }, [location.pathname]);

    // ─── Login: gọi AuthContext.login → redirect theo role ────────────────────
    async function handleLogin(username, password, remember) {
        setLoginError('');
        setLoginSuccess('');
        setIsLoggingIn(true);
        try {
            const user = await login(username, password, remember)
            navigate(getRedirectPathAfterLogin(user.role), { replace: true });
        } catch (err) {
            setLoginError(err.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
        } finally {
            setIsLoggingIn(false);
        }
    }

    // ─── SignUp: gọi API đăng ký rồi chuyển về login ────────────────────────────
    async function handleSignUp(formData) {
        setIsLoggingIn(true);
        setSignupFieldErrors({});
        setSignupMessage(null);

        try {
            await register(formData);

            // Switch to login and show success message there
            setLoginSuccess('Đăng ký thành công! Hãy đăng nhập để tiếp tục.');
            setActiveTab('login');
            setLoginError('');
            setSignupFieldErrors({});

        } catch (error) {
            console.error('Signup error:', error.message);

            // Parse error code from backend response
            const errorCode = error.code ?? error.response?.data?.code;
            const errorMsg = error.message;

            // Map error codes to field-specific errors
            const fieldErrors = {};

            if (errorCode === 1004) {
                // USER_EXISTED - show error under username field
                fieldErrors.username = 'Tên đăng nhập đã được sử dụng';
            } else if (errorCode === 1005) {
                // EMAIL_EXISTED - show error under email field
                fieldErrors.email = 'Email đã được sử dụng';
            } else {
                setSignupMessage({ type: 'error', text: errorMsg || 'Lỗi đăng ký. Vui lòng thử lại.' });
            }

            setSignupFieldErrors(fieldErrors);
        } finally {
            setIsLoggingIn(false);
        }
    }

    // Tăng tốc độ cuộn cho form side
    const handleWheel = (e) => {
        const multiplier = 1.3;
        e.currentTarget.scrollTop += e.deltaY * multiplier;
    };

    return (
        <div className="auth-page">
            <main className="auth-layout">

                {/* Cột trái: form */}
                <section className="auth-form-side" onWheel={handleWheel}>
                    {activeTab === 'login' && (
                        <>
                            {loginSuccess && (
                                <div style={{
                                    margin: '0 32px 8px',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    background: '#f0fdf4',
                                    border: '1px solid #86efac',
                                    color: '#16a34a',
                                    fontSize: '0.875rem',
                                    fontWeight: '500',
                                }}>
                                    {loginSuccess}
                                </div>
                            )}
                            {loginError && (
                                <div style={{
                                    margin: '0 32px 8px',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    background: '#fef2f2',
                                    border: '1px solid #fca5a5',
                                    color: '#dc2626',
                                    fontSize: '0.875rem',
                                    fontWeight: '500',
                                }}>
                                    {loginError}
                                </div>
                            )}
                            <LoginForm
                                onSubmit={handleLogin}
                                onTabChange={setActiveTab}
                                isLoading={isLoggingIn}
                            />
                        </>
                    )}
                    {activeTab === 'register' && (
                        <>
                            {signupMessage && (
                                <div style={{
                                    margin: '0 32px 8px',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    background: signupMessage.type === 'success' ? '#f0fdf4' : '#fef2f2',
                                    border: signupMessage.type === 'success' ? '1px solid #86efac' : '1px solid #fca5a5',
                                    color: signupMessage.type === 'success' ? '#16a34a' : '#dc2626',
                                    fontSize: '0.875rem',
                                    fontWeight: '500',
                                }}>
                                    {signupMessage.text}
                                </div>
                            )}
                            <SignUp onSubmit={handleSignUp} onTabChange={setActiveTab} isLoading={isLoggingIn}
                                fieldErrors={signupFieldErrors} />
                        </>
                    )}
                </section>

                {/* Cột phải: hero (ẩn trên mobile) */}
                <aside className="auth-hero-side">
                    <HeroPanel activeTab={activeTab} />
                </aside>

            </main>
        </div>
    );
}

export default AuthPage;
