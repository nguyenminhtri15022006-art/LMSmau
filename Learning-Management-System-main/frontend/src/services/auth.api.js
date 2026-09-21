import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';

// =============================================
// Auth / Identity Service API
// Centralized API calls for authentication flows
// =============================================

export const authApi = {
  /**
   * Login with email/username and password
   * @param {string} identifier - Email or username
   * @param {string} password - User password
   * @returns {Promise} Response with accessToken, refreshToken, and user data
   */
  login: (identifier, password) =>
    apiClient.post(ENDPOINTS.AUTH.LOGIN, {
      identifier,
      password
    }),

  /**
   * Register new user account
   * @param {Object} userData - {name, username, email, password, role}
   * @returns {Promise} Response with user data
   */
  register: (userData) =>
    apiClient.post(ENDPOINTS.AUTH.REGISTER, {
      name: userData.name,
      username: userData.userName,
      email: userData.email,
      password: userData.password,
      role: userData.role === 'instructor' ? 'INSTRUCTOR' : 'STUDENT'
    }),

  /**
   * Request OTP for password reset
   * @param {string} email - User email address
   * @returns {Promise} Response confirming OTP sent
   */
  forgotPassword: (email) =>
    apiClient.post(`${ENDPOINTS.AUTH.LOGIN.split('/auth')[0]}/auth/forgot-password`, {
      email
    }),

  /**
   * Verify OTP code
   * @param {string} email - User email
   * @param {string} otp - OTP code (6 digits)
   * @returns {Promise} Response confirming OTP is valid
   */
  verifyOtp: (email, otp) =>
    apiClient.post(`${ENDPOINTS.AUTH.LOGIN.split('/auth')[0]}/auth/verify-otp`, {
      email,
      otp
    }),

  /**
   * Complete password reset with new password
   * @param {string} email - User email
   * @param {string} otp - OTP code
   * @param {string} newPassword - New password
   * @returns {Promise} Response confirming password reset
   */
  resetPassword: (email, otp, newPassword) =>
    apiClient.post(`${ENDPOINTS.AUTH.LOGIN.split('/auth')[0]}/auth/reset-password`, {
      email,
      otp,
      newPassword
    }),

  /**
   * Logout user (clear server-side session/tokens)
   * @returns {Promise}
   */
  logout: () =>
    apiClient.post(ENDPOINTS.AUTH.LOGOUT),

  /**
   * Refresh access token using refresh token
   * @param {string} refreshToken - Refresh token from localStorage
   * @returns {Promise} Response with new accessToken
   */
  refreshToken: (refreshToken) =>
    apiClient.post(ENDPOINTS.AUTH.REFRESH, {
      refreshToken
    }),

  /**
   * Get current user profile
   * @returns {Promise} Response with user profile data
   */
  getProfile: () =>
    apiClient.get(ENDPOINTS.AUTH.PROFILE),

  /**
   * Update user profile
   * @param {Object} profileData - {name, email, phone, etc}
   * @returns {Promise} Response with updated profile
   */
  updateProfile: (profileData) =>
    apiClient.put(ENDPOINTS.AUTH.PROFILE, profileData),

  /**
   * Change password for authenticated user
   * @param {Object} passwordData - {oldPassword, newPassword, confirmPassword}
   * @returns {Promise} Response with updated user data
   */
  updatePassword: (passwordData) =>
    apiClient.patch(ENDPOINTS.AUTH.UPDATE_PASSWORD, {
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword,
      confirmPassword: passwordData.confirmPassword
    }),

  /**
   * Send email verification OTP
   * @param {string} email - User email address
   * @returns {Promise} Response confirming OTP sent
   */
  sendVerifyEmail: (email) =>
    apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL_SEND, { email }),

  /**
   * Verify email with OTP code
   * @param {string} email - User email
   * @param {string} otp - OTP code (6 digits)
   * @returns {Promise} Response confirming email verified
   */
  verifyEmail: (email, otp) =>
    apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL, { email, otp })
};

export default authApi;

