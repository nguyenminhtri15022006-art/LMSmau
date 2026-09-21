import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createStateToken, handleSocialLoginClick, startSocialOAuth } from '../../services/oauthUtils';

describe('oauthUtils', () => {
  let originalLocation;

  beforeEach(() => {
    sessionStorage.clear();
    originalLocation = window.location;
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { href: '' },
    });
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
    vi.restoreAllMocks();
  });

  it('creates state token with crypto randomUUID when available', () => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'uuid-1') });
    expect(createStateToken()).toBe('uuid-1');
  });

  it('creates fallback state token when randomUUID is not available', () => {
    vi.stubGlobal('crypto', {});
    vi.spyOn(Math, 'random').mockReturnValue(0.123456);
    vi.spyOn(Date, 'now').mockReturnValue(12345);

    expect(createStateToken()).toMatch(/^state_[a-z0-9]+_12345$/);
  });

  it('does nothing when provider is missing', () => {
    startSocialOAuth();
    expect(sessionStorage.getItem('social_oauth_state')).toBeNull();
    expect(window.location.href).toBe('');
  });

  it('starts google OAuth and stores safe return path', () => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'oauth-state') });

    startSocialOAuth('google', '/dashboard');

    expect(sessionStorage.getItem('social_oauth_state')).toBe('oauth-state');
    expect(sessionStorage.getItem('social_oauth_provider')).toBe('google');
    expect(sessionStorage.getItem('auth_return_to')).toBe('/dashboard');
    expect(window.location.href).toContain('https://accounts.google.com/o/oauth2/v2/auth?');
    expect(window.location.href).toContain('state=oauth-state');
  });

  it('does not store unsafe return path and delegates click handler', () => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'oauth-state') });

    handleSocialLoginClick('google', '//evil.test');

    expect(sessionStorage.getItem('auth_return_to')).toBeNull();
    expect(sessionStorage.getItem('social_oauth_provider')).toBe('google');
  });
});
