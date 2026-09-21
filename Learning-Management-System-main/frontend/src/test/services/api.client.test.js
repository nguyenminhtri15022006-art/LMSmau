import { afterEach, describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import apiClient from '../../services/api.client';

describe('apiClient configuration', () => {
  it('has correct base URL from env', () => {
    expect(apiClient.defaults.baseURL).toBe('http://localhost:8080');
  });

  it('has correct timeout', () => {
    expect(apiClient.defaults.timeout).toBe(15000);
  });

  it('has JSON content-type default', () => {
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json');
  });
});

describe('Request interceptor', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('attaches Bearer token from localStorage', async () => {
    localStorage.setItem('access_token', 'test-token');
    const config = { headers: {} };
    const result = await apiClient.interceptors.request.handlers[0].fulfilled(config);
    expect(result.headers.Authorization).toBe('Bearer test-token');
  });

  it('attaches Bearer token from sessionStorage when localStorage is empty', async () => {
    sessionStorage.setItem('access_token', 'session-token');
    const config = { headers: {} };
    const result = await apiClient.interceptors.request.handlers[0].fulfilled(config);
    expect(result.headers.Authorization).toBe('Bearer session-token');
  });

  it('removes Content-Type for FormData', async () => {
    const formData = new FormData();
    const config = { headers: {}, data: formData };
    const result = await apiClient.interceptors.request.handlers[0].fulfilled(config);
    expect(result.headers['Content-Type']).toBeUndefined();
  });

  it('skips auth when skipAuth flag is set', async () => {
    const config = { headers: {}, skipAuth: true };
    const result = await apiClient.interceptors.request.handlers[0].fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });
});

describe('Response interceptor', () => {
  let originalLocation;

  const rejected = () => apiClient.interceptors.response.handlers[0].rejected;

  const makeRetryConfig = (overrides = {}) => ({
    url: '/api/protected',
    method: 'get',
    headers: {},
    adapter: vi.fn().mockResolvedValue({
      data: { ok: true },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {},
    }),
    ...overrides,
  });

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.restoreAllMocks();
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
  });

  it('passes successful responses through unchanged', () => {
    const response = { data: { ok: true } };
    expect(apiClient.interceptors.response.handlers[0].fulfilled(response)).toBe(response);
  });

  it('rejects non-refreshable errors', async () => {
    const error = { config: makeRetryConfig(), response: { status: 500 } };
    await expect(rejected()(error)).rejects.toBe(error);

    const retriedError = { config: makeRetryConfig({ _retry: true }), response: { status: 401 } };
    await expect(rejected()(retriedError)).rejects.toBe(retriedError);

    const skipError = { config: makeRetryConfig({ skipAuthRefresh: true }), response: { status: 401 } };
    await expect(rejected()(skipError)).rejects.toBe(skipError);
  });

  it('refreshes token from localStorage and retries original request', async () => {
    localStorage.setItem('refresh_token', 'refresh-1');
    const postSpy = vi.spyOn(axios, 'post').mockResolvedValueOnce({
      data: { data: { accessToken: 'access-2', refreshToken: 'refresh-2' } },
    });
    const config = makeRetryConfig();

    const result = await rejected()({ config, response: { status: 401 } });

    expect(postSpy).toHaveBeenCalledWith(
      expect.stringContaining('/api/identity/auth/refresh'),
      { refreshToken: 'refresh-1' },
    );
    expect(localStorage.getItem('access_token')).toBe('access-2');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-2');
    expect(config.headers.Authorization).toBe('Bearer access-2');
    expect(config.adapter).toHaveBeenCalled();
    expect(result.data).toEqual({ ok: true });
  });

  it('uses sessionStorage refresh token when localStorage is empty', async () => {
    sessionStorage.setItem('refresh_token', 'session-refresh');
    vi.spyOn(axios, 'post').mockResolvedValueOnce({
      data: { accessToken: 'session-access' },
    });
    const config = makeRetryConfig();

    await rejected()({ config, response: { status: 401 } });

    expect(sessionStorage.getItem('access_token')).toBe('session-access');
    expect(config.headers.Authorization).toBe('Bearer session-access');
  });

  it('queues concurrent 401 requests behind one refresh call', async () => {
    localStorage.setItem('refresh_token', 'refresh-queue');
    let resolveRefresh;
    vi.spyOn(axios, 'post').mockImplementationOnce(() => new Promise((resolve) => {
      resolveRefresh = resolve;
    }));

    const firstConfig = makeRetryConfig();
    const secondConfig = makeRetryConfig();
    const firstPromise = rejected()({ config: firstConfig, response: { status: 401 } });
    await Promise.resolve();
    const secondPromise = rejected()({ config: secondConfig, response: { status: 401 } });

    resolveRefresh({ data: { data: { accessToken: 'queued-access' } } });
    const results = await Promise.all([firstPromise, secondPromise]);

    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(firstConfig.headers.Authorization).toBe('Bearer queued-access');
    expect(secondConfig.headers.Authorization).toBe('Bearer queued-access');
    expect(results.map((res) => res.data)).toEqual([{ ok: true }, { ok: true }]);
  });

  it('clears session and redirects when refresh is unavailable or invalid', async () => {
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');
    localStorage.setItem('user', JSON.stringify({ id: 'u-1' }));
    vi.spyOn(axios, 'post').mockResolvedValueOnce({ data: { data: {} } });

    await expect(rejected()({
      config: makeRetryConfig(),
      response: { status: 401 },
    })).rejects.toThrow('Invalid refresh response');

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(window.location.href).toBe('/login');
  });

  it('does not retry refresh endpoint failures', async () => {
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');
    const error = {
      config: makeRetryConfig({ url: '/api/identity/auth/refresh' }),
      response: { status: 401 },
    };

    await expect(rejected()(error)).rejects.toBe(error);

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(window.location.href).toBe('/login');
  });
});
