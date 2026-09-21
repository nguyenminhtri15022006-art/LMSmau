import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';

// =============================================
// Identity / Auth Service API
// =============================================

export const identityApi = {
  login: (credentials) => apiClient.post(ENDPOINTS.AUTH.LOGIN, credentials),
  register: (payload) => apiClient.post(ENDPOINTS.AUTH.REGISTER, payload),
  logout: () => apiClient.post(ENDPOINTS.AUTH.LOGOUT),
  refresh: (token) => apiClient.post(ENDPOINTS.AUTH.REFRESH, { refreshToken: token }),
  getProfile: () => apiClient.get(ENDPOINTS.AUTH.PROFILE),
  updateProfile: (data) => apiClient.put(ENDPOINTS.AUTH.PROFILE, data),
  updateRole: (data) => apiClient.patch(`${ENDPOINTS.AUTH.PROFILE}/role`, data),
  forgotPassword: (payload) => apiClient.post(ENDPOINTS.AUTH.FORGOT_PASSWORD, payload),
  resetPassword: (payload) => apiClient.post(ENDPOINTS.AUTH.RESET_PASSWORD, payload),
  sendVerifyEmail: (payload) => apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL_SEND, payload),
  verifyEmail: (payload) => apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL, payload),
  updatePassword: (payload) => apiClient.patch(ENDPOINTS.AUTH.UPDATE_PASSWORD, payload),
  socialLogin: (provider, payload, config = {}) => apiClient.post(ENDPOINTS.AUTH.SOCIAL_LOGIN(provider), payload, config),
  getPublicProfile: (userId) =>
    apiClient
      .get(ENDPOINTS.AUTH.PUBLIC_PROFILE(userId), { skipAuth: true, skipAuthRefresh: true })
      .then(res => res?.data?.data ?? null),
};

export default identityApi;
