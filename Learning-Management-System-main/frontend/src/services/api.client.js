/**
 * Axios instance with base URL + JWT interceptors.
 *
 * Request interceptor: attaches access token from the active storage.
 *
 * Response interceptor: on 401, queues requests under a single shared
 * refresh to prevent race conditions. The refresh endpoint itself is
 * never retried to avoid infinite loops.
 */

import axios from 'axios';
import { API_BASE_URL, API_TIMEOUT_MS } from '../configurations/env';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Refresh-queue state (module-level — shared across all requests) ──

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

const clearSession = () => {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('user');
  sessionStorage.removeItem('access_token');
  sessionStorage.removeItem('refresh_token');
  sessionStorage.removeItem('user');
};

// ── Request interceptor ──

apiClient.interceptors.request.use(
  (config) => {
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }

    if (config.skipAuth) {
      delete config.headers.Authorization;
      return config;
    }

    const token =
      localStorage.getItem('access_token') ||
      sessionStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor ──

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (originalRequest?.skipAuthRefresh) {
      return Promise.reject(error);
    }

    // Only handle 401 and only if we haven't retried this specific request yet
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    // Never retry the refresh endpoint itself — that leads to infinite loop
    if (originalRequest.url?.includes('/auth/refresh')) {
      clearSession();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // Another refresh is already in flight — queue this request and wait
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`;
        originalRequest._retry = true;
        return apiClient(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const hasLocalRefreshToken = Boolean(localStorage.getItem('refresh_token'));
      const storage = hasLocalRefreshToken ? localStorage : sessionStorage;
      const refreshToken = storage.getItem('refresh_token');

      if (!refreshToken) {
        throw new Error('Missing refresh token');
      }

      const { data } = await axios.post(
        `${API_BASE_URL}/api/identity/auth/refresh`,
        { refreshToken },
      );

      const payload = data?.data ?? data;
      if (!payload?.accessToken) {
        throw new Error('Invalid refresh response');
      }

      storage.setItem('access_token', payload.accessToken);
      if (payload.refreshToken) {
        storage.setItem('refresh_token', payload.refreshToken);
      }

      // Retry all queued requests with the new token
      processQueue(null, payload.accessToken);

      originalRequest.headers.Authorization = `Bearer ${payload.accessToken}`;
      return apiClient(originalRequest);
    } catch (_err) {
      processQueue(_err, null);
      clearSession();
      window.location.href = '/login';
      return Promise.reject(_err);
    } finally {
      isRefreshing = false;
    }
  },
);

export default apiClient;
