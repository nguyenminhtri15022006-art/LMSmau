/**
 * OAuth flow step 2 — Handle the redirect back from Google.
 *
 * Route: /authenticate
 * Google redirects here with ?code=...&state=... after user consents.
 *
 * This page:
 *   1. Validates state (CSRF protection) and reads the auth code from URL
 *   2. POSTs the code to backend /auth/social-login/{provider}
 *   3. Backend exchanges code for Google tokens, finds/creates user,
 *      returns JWT access + refresh tokens
 *   4a. Returning user with role selected -> save session -> redirect to dashboard
 *   4b. New user or no role selected      -> show RoleSelectionModal
 *   4c. Error                             -> show message + "Back to login" button
 */

import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/AuthPage.css';
import '../styles/AuthForm.css';
import Logo from '../components/Logo';
import HeroPanel from '../components/HeroPanel';
import { OAuthConfig } from '../../../configurations/configuration';
import identityApi from '../../../services/identity.api';
import { gatherDeviceInfo } from '../../../services/deviceInfo';
import useAuth from '../../../hooks/useAuth';
import ROLES from '../../../constants/roles';
import RoleSelectionModal from '../components/RoleSelectionModal';

const AUTH_ROUTES = ['/login', '/register', '/forgot-password', '/authenticate'];

function defaultPathByRole(role) {
    switch (role) {
        case ROLES.ADMIN:
            return '/admin/home';
        case ROLES.INSTRUCTOR:
            return '/instructor/home';
        case ROLES.STUDENT:
        default:
            return '/dashboard';
    }
}

function isAllowedReturnPath(path, role) {
    if (!path || !path.startsWith('/') || path.startsWith('//')) return false;
    if (AUTH_ROUTES.some(route => path === route || path.startsWith(`${route}?`))) return false;

    if (role === ROLES.ADMIN) {
        return path.startsWith('/admin') || path.startsWith('/manage') || !path.startsWith('/instructor');
    }

    if (role === ROLES.INSTRUCTOR) {
        return !path.startsWith('/admin') && !path.startsWith('/dashboard') && !path.startsWith('/my-courses') && !path.startsWith('/exercises');
    }

    return !path.startsWith('/admin') && !path.startsWith('/instructor') && !path.startsWith('/manage');
}

function getPathAfterLogin(role) {
    const requestedPath = sessionStorage.getItem('auth_return_to');
    sessionStorage.removeItem('auth_return_to');
    return isAllowedReturnPath(requestedPath, role) ? requestedPath : defaultPathByRole(role);
}

function SocialCallbackPage() {
    const navigate = useNavigate();
    const { setSession } = useAuth();


    const [error, setError] = useState('');
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [pendingSession, setPendingSession] = useState(null);
    const [activeProvider, setActiveProvider] = useState('provider');
    const [isProcessing, setIsProcessing] = useState(true);
    const abortControllerRef = useRef(null);

    function clearSocialOAuthData() {
        sessionStorage.removeItem('social_oauth_state');
        sessionStorage.removeItem('social_oauth_provider');
    }

    function navigateAfterLogin(role) {
        navigate(getPathAfterLogin(role), { replace: true });
    }

    useEffect(() => {
        let isMounted = true;
        const abortController = new AbortController();
        abortControllerRef.current = abortController;

        // 20s timeout — if the backend doesn't respond, show an error
        const processingTimeoutId = setTimeout(() => {
            if (!isMounted) return;
            setError('Hết thời gian chờ xác thực. Vui lòng thử lại.');
            setIsProcessing(false);
        }, 20000);

        async function finalizeSocialLogin() {
            const params = new URLSearchParams(window.location.search);
            const code = params.get('code');
            const state = params.get('state');
            const oauthError = params.get('error');

            const storedState = sessionStorage.getItem('social_oauth_state');
            const provider = sessionStorage.getItem('social_oauth_provider');
            setActiveProvider(provider || 'provider');

            // User clicked "Cancel" on Google consent screen
            if (oauthError) {
                if (isMounted) {
                    setError('Xác thực thất bại. Vui lòng thử lại.');
                    setIsProcessing(false);
                }
                return;
            }

            // CSRF check: URL state must match the one we saved before redirect
            if (!code || !state || state !== storedState || !provider) {
                if (isMounted) {
                    setError('Xác thực thất bại. Vui lòng thử lại.');
                    setIsProcessing(false);
                }
                return;
            }

            try {
                // Gather device info for the refresh-token audit record
                const deviceInfo = await gatherDeviceInfo();
                if (abortController.signal.aborted) return;

                // POST code + redirectUri to backend -> backend exchanges with Google
                const response = await identityApi.socialLogin(provider, {
                    code,
                    redirectUri: OAuthConfig.redirectUri,
                    ipAddress: deviceInfo.ip,
                    deviceInfo: deviceInfo.deviceInfo,
                }, {
                    signal: abortController.signal,
                });

                if (abortController.signal.aborted) return;

                // Backend wraps response in ApiResponse<T> -> { data: { data: {...} } }
                const payload = response?.data?.data ?? response?.data;

                if (!payload?.accessToken || !payload?.refreshToken || !payload?.user) {
                    throw new Error('Thông tin xác thực không hợp lệ. Vui lòng thử lại.');
                }

                clearSocialOAuthData();

                // Show role modal for new users or users who haven't selected a role yet
                const shouldShowRoleModal = Boolean(
                    payload.requiresRoleSelection || payload.isNewUser || !payload.user.roleSelected
                );

                if (shouldShowRoleModal) {
                    setPendingSession(payload);
                    setShowRoleModal(true);
                    setIsProcessing(false);
                    return;
                }

                // Returning user with role -> persist session and redirect
                setSession(payload, true);
                navigateAfterLogin(payload.user.role);
            } catch (err) {
                // Aborted silently (component unmounted or timeout)
                if (abortController.signal.aborted || err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError') {
                    if (isMounted) {
                        setIsProcessing(false);
                    }
                    return;
                }

                console.error('Social login failed:', err);
                if (isMounted) {
                    const message = err.response?.data?.message || err.message || 'Xác thực thất bại. Vui lòng thử lại.';
                    setError(message);
                    setIsProcessing(false);
                }
            }
        }

        finalizeSocialLogin();

        return () => {
            isMounted = false;
            abortController.abort();
            clearTimeout(processingTimeoutId);
            abortControllerRef.current = null;
        };
    }, [setSession, navigate]);

    function handleBackToLogin() {
        abortControllerRef.current?.abort();
        clearSocialOAuthData();
        sessionStorage.removeItem('auth_return_to');
        navigate('/login', { replace: true });
    }

    // Called when user picks a role in the modal
    async function handleRoleSelection(role) {
        if (!pendingSession) return;

        // Store session first so axios interceptor can attach the JWT to the role-update request
        setSession(pendingSession, true);

        try {
            const response = await identityApi.updateRole({ role });
            const updatedUser = response?.data?.data || response?.data || {
                ...pendingSession.user,
                role,
                roleSelected: true,
            };
            const refreshResponse = await identityApi.refresh(pendingSession.refreshToken);
            const refreshedSession = refreshResponse?.data?.data || refreshResponse?.data;

            if (!refreshedSession?.accessToken || !refreshedSession?.refreshToken) {
                throw new Error('Thong tin xac thuc khong hop le. Vui long thu lai.');
            }

            const sessionToStore = {
                ...pendingSession,
                accessToken: refreshedSession.accessToken,
                refreshToken: refreshedSession.refreshToken,
                accessTokenExpiry: refreshedSession.accessTokenExpiry,
                user: updatedUser,
            };

            setSession(sessionToStore, true);
            navigateAfterLogin(sessionToStore.user.role);
        } catch (err) {
            console.error('Failed to update role:', err);
            setError(err.response?.data?.message || err.message || 'Xác thực thất bại. Vui lòng thử lại.');
        } finally {
            setShowRoleModal(false);
            setPendingSession(null);
        }
    }

    return (
        <div className="auth-page">
            <main className="auth-layout">
                <section className="auth-form-side">
                    <div className="auth-form-inner">
                        <Logo />
                        <div className="form-heading">
                            <h1>Đang xác thực...</h1>
                            <p>{error ? error : 'Vui lòng chờ...'}</p>
                        </div>
                        {(error || !isProcessing) && (
                            <button className="btn-primary" type="button" onClick={handleBackToLogin}>
                                Quay lại đăng nhập
                            </button>
                        )}
                    </div>
                </section>
                <aside className="auth-hero-side">
                    <HeroPanel activeTab="login" />
                </aside>
            </main>

            {showRoleModal && (
                <RoleSelectionModal
                    provider={activeProvider}
                    onConfirm={(role) => handleRoleSelection(role)}
                    onCancel={() => {
                        setShowRoleModal(false);
                        setPendingSession(null);
                        handleBackToLogin();
                    }}
                />
            )}
        </div>
    );
}

export default SocialCallbackPage;
