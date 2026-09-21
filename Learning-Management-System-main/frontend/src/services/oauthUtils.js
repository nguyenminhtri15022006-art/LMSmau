/**
 * OAuth flow step 1 — Initiate social login redirect.
 *
 * Flow overview (Google OAuth):
 *   [LoginForm] --click--> handleSocialLoginClick()
 *       --> startSocialOAuth()
 *       --> save state/provider to sessionStorage
 *       --> redirect browser to accounts.google.com
 *       --> user approves consent
 *       --> Google redirects back to /authenticate
 *       --> [SocialCallbackPage] handles the rest
 *
 * sessionStorage is used (not localStorage) so OAuth state is
 * scoped to the browser tab and auto-cleared when the tab closes.
 */

import { OAuthConfig } from '../configurations/configuration';

export function createStateToken() {
    if (window.crypto?.randomUUID) {
        return window.crypto.randomUUID();
    }
    return `state_${Math.random().toString(36).slice(2)}_${Date.now()}`;
}

export function startSocialOAuth(provider, returnTo = null) {
    if (!provider) {
        return;
    }

    // Save CSRF state + provider to sessionStorage for validation on callback
    const state = createStateToken();
    sessionStorage.setItem('social_oauth_state', state);
    sessionStorage.setItem('social_oauth_provider', provider);
    if (returnTo && returnTo.startsWith('/') && !returnTo.startsWith('//')) {
        sessionStorage.setItem('auth_return_to', returnTo);
    }

    const redirectUri = OAuthConfig.redirectUri;

    if (provider === 'google') {
        const params = new URLSearchParams({
            client_id: OAuthConfig.clientId,
            redirect_uri: redirectUri,
            response_type: 'code',
            scope: 'openid email profile',
            access_type: 'offline',
            prompt: 'select_account',
            state,
        });
        window.location.href = `${OAuthConfig.authUri}?${params.toString()}`;
    }
}

export function handleSocialLoginClick(provider, returnTo = null) {
    startSocialOAuth(provider, returnTo);
}
