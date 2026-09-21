import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';
import axios from 'axios';
import {
  ADMIN_HEALTH_ENDPOINTS,
  ADMIN_HEALTH_IDENTITY_TIMEOUT_MS,
  ADMIN_HEALTH_TIMEOUT_MS,
} from '../configurations/env';
import { parseBackendUtcDate } from '../utils/dateTime';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const getPageTotal = (payload) => {
  if (!payload) return 0;
  if (Number.isFinite(Number(payload.totalElements))) return Number(payload.totalElements);
  if (Number.isFinite(Number(payload.page?.totalElements))) return Number(payload.page.totalElements);
  if (Array.isArray(payload.content)) return payload.content.length;
  if (Array.isArray(payload)) return payload.length;
  return 0;
};

const getPortLabel = (url) => {
  try {
    const port = new URL(url).port;
    return port ? `:${port}` : '';
  } catch {
    return '';
  }
};

const dashboardApi = {
  // Get overview statistics
  getStats: async () => {
    const [statsResult, coursesResult] = await Promise.allSettled([
      apiClient.get(ENDPOINTS.ADMIN.USER_STATS),
      apiClient.get(ENDPOINTS.ADMIN.COURSES, { params: { page: 0, size: 1 } }),
    ]);

    if (statsResult.status === 'rejected') {
      throw statsResult.reason;
    }

    const userStats = unwrapData(statsResult.value) || {};
    if (coursesResult.status === 'rejected') {
      console.warn('Failed to fetch course stats:', coursesResult.reason);
    }

    const coursesData =
      coursesResult.status === 'fulfilled' ? (unwrapData(coursesResult.value) || {}) : {};

    return {
      totalStudents: Number(userStats.totalStudents || 0),
      newStudentsThisWeek: Number(userStats.newStudentsThisWeek || 0),
      totalInstructors: Number(userStats.totalInstructors || 0),
      newInstructorsThisMonth: Number(userStats.newInstructorsThisMonth || 0),
      totalAdmins: Number(userStats.totalAdmins || 0),
      totalCourses: getPageTotal(coursesData),
      judgeSuccessRate: '78%',
    };
  },

  // Monitor microservices health
  getMicroservicesStatus: async () => {
    const services = [
      {
        name: 'Identity Service',
        url: ADMIN_HEALTH_ENDPOINTS.identity,
        timeout: ADMIN_HEALTH_IDENTITY_TIMEOUT_MS,
      },
      { name: 'Course Service', url: ADMIN_HEALTH_ENDPOINTS.course, timeout: ADMIN_HEALTH_TIMEOUT_MS },
      { name: 'Assignment Service', url: ADMIN_HEALTH_ENDPOINTS.assignment, timeout: ADMIN_HEALTH_TIMEOUT_MS },
      { name: 'Blog Service', url: ADMIN_HEALTH_ENDPOINTS.blog, timeout: ADMIN_HEALTH_TIMEOUT_MS },
      { name: 'Chatbot Service', url: ADMIN_HEALTH_ENDPOINTS.chatbot, timeout: ADMIN_HEALTH_TIMEOUT_MS },
      { name: 'API Gateway', url: ADMIN_HEALTH_ENDPOINTS.gateway, timeout: ADMIN_HEALTH_TIMEOUT_MS }
    ].map((svc) => ({ ...svc, port: getPortLabel(svc.url) }));

    const results = await Promise.all(
      services.map(async (svc) => {
        try {
          await axios.get(svc.url, { timeout: svc.timeout });
          return { ...svc, status: 'Online' };
        } catch (error) {
          return { ...svc, status: 'Offline' };
        }
      })
    );

    return results;
  },

  // Build admin notifications from real system data.
  getNotifications: async () => {
    const [
      servicesResult,
      privateCoursesResult,
      lockedCoursesResult,
      draftPostsResult,
      hiddenPostsResult,
    ] = await Promise.allSettled([
      dashboardApi.getMicroservicesStatus(),
      apiClient.get(ENDPOINTS.ADMIN.COURSES, { params: { status: 'PRIVATE', page: 0, size: 1 } }),
      apiClient.get(ENDPOINTS.ADMIN.COURSES, { params: { status: 'LOCKED', page: 0, size: 1 } }),
      apiClient.get(ENDPOINTS.BLOG.ADMIN_POSTS, { params: { status: 'DRAFT', page: 0, size: 1 } }),
      apiClient.get(ENDPOINTS.BLOG.ADMIN_POSTS, { params: { status: 'HIDDEN', page: 0, size: 1 } }),
    ]);

    if (
      servicesResult.status === 'rejected' &&
      privateCoursesResult.status === 'rejected' &&
      lockedCoursesResult.status === 'rejected' &&
      draftPostsResult.status === 'rejected' &&
      hiddenPostsResult.status === 'rejected'
    ) {
      return [
        {
          id: 'system-notifications-unavailable',
          type: 'warning',
          icon: 'ti-alert-circle',
          title: 'Không thể tải thông báo hệ thống',
          desc: 'Cập nhật vừa xong · Admin Dashboard',
        },
      ];
    }

    const notifications = [];
    const addCountNotification = ({ id, count, type, icon, title, source }) => {
      if (count > 0) {
        notifications.push({
          id,
          type,
          icon,
          title: title(count),
          desc: `Cập nhật vừa xong · ${source}`,
        });
      }
    };

    if (servicesResult.status === 'fulfilled') {
      const offlineServices = servicesResult.value.filter((svc) => svc.status === 'Offline');
      if (offlineServices.length > 0) {
        notifications.push({
          id: 'offline-services',
          type: 'danger',
          icon: 'ti-alert-triangle',
          title: `${offlineServices.length} dịch vụ đang offline`,
          desc: `Cập nhật vừa xong · ${offlineServices.map((svc) => svc.name).join(', ')}`,
        });
      }
    }

    if (lockedCoursesResult.status === 'fulfilled') {
      addCountNotification({
        id: 'locked-courses',
        count: getPageTotal(unwrapData(lockedCoursesResult.value)),
        type: 'warning',
        icon: 'ti-lock',
        title: (count) => `${count} khóa học đang bị khóa`,
        source: 'Course Service',
      });
    }

    if (privateCoursesResult.status === 'fulfilled') {
      addCountNotification({
        id: 'private-courses',
        count: getPageTotal(unwrapData(privateCoursesResult.value)),
        type: 'info',
        icon: 'ti-book',
        title: (count) => `${count} khóa học chưa công khai`,
        source: 'Course Service',
      });
    }

    if (hiddenPostsResult.status === 'fulfilled') {
      addCountNotification({
        id: 'hidden-posts',
        count: getPageTotal(unwrapData(hiddenPostsResult.value)),
        type: 'warning',
        icon: 'ti-eye-off',
        title: (count) => `${count} bài blog đang bị ẩn`,
        source: 'Blog Service',
      });
    }

    if (draftPostsResult.status === 'fulfilled') {
      addCountNotification({
        id: 'draft-posts',
        count: getPageTotal(unwrapData(draftPostsResult.value)),
        type: 'info',
        icon: 'ti-file-text',
        title: (count) => `${count} bài blog đang ở bản nháp`,
        source: 'Blog Service',
      });
    }

    return notifications.length > 0
      ? notifications
      : [
          {
            id: 'system-ok',
            type: 'success',
            icon: 'ti-check',
            title: 'Hệ thống ổn định',
            desc: 'Cập nhật vừa xong · Không có cảnh báo mới',
          },
        ];
  },

  getStudentDashboardData: async () => {
    // 1. Lấy danh sách khóa học đã đăng ký
    const enrolledRes = await apiClient.get(ENDPOINTS.COURSES.ENROLLED);
    const courses = unwrapData(enrolledRes) || [];
    if (courses.length === 0) return { courses: [], schedules: [] };

    const courseIds = courses.map(c => c.id);

    // 2. Fetch progress VÀ schedules song song
    const [progressResults, scheduleResults] = await Promise.all([
      Promise.allSettled(courseIds.map(id => apiClient.get(ENDPOINTS.COURSES.PROGRESS(id)))),
      Promise.allSettled(courseIds.map(id => apiClient.get(ENDPOINTS.COURSES.SCHEDULES(id)))),
    ]);

    // 3. Gộp courses + progress
    const coursesWithProgress = courses.map((course, i) => ({
      ...course,
      progressData: progressResults[i].status === 'fulfilled'
        ? (unwrapData(progressResults[i].value) || {})
        : { totalLessons: 0, completedLessons: 0, percentComplete: 0 },
    }));

    // 4. Lọc và sắp xếp schedules
    const now = Date.now();
    const upcoming = [];
    scheduleResults.forEach(result => {
      if (result.status === 'fulfilled') {
        const schedules = unwrapData(result.value) || [];
        schedules.forEach(s => {
          if ((parseBackendUtcDate(s.endTime)?.getTime() ?? 0) > now) upcoming.push(s);
        });
      }
    });
    const schedules = upcoming.sort((a, b) => parseBackendUtcDate(a.startTime) - parseBackendUtcDate(b.startTime));

    return { courses: coursesWithProgress, schedules };
  },

  // Get recent judge activity
  getJudgeActivity: async () => {
    const res = await apiClient.get(ENDPOINTS.ADMIN.CODE_JUDGE_ACTIVITY, { params: { hours: 24 } });
    return unwrapData(res) || [];
  },

  getUsers: async (params = {}) => {
    const res = await apiClient.get(ENDPOINTS.ADMIN.USERS, { params });
    return unwrapData(res);
  },

  toggleUserActive: async (userId) => {
    const res = await apiClient.patch(`${ENDPOINTS.ADMIN.USERS}/${userId}/active`);
    return unwrapData(res);
  },

  getUser: async (userId) => {
    const res = await apiClient.get(`${ENDPOINTS.ADMIN.USERS}/${userId}`);
    return unwrapData(res);
  },

  createAdmin: async (data) => {
    const res = await apiClient.post(`${ENDPOINTS.ADMIN.USERS}/admins`, data);
    return unwrapData(res);
  },

  updateUser: async (userId, data) => {
    const res = await apiClient.put(`${ENDPOINTS.ADMIN.USERS}/${userId}`, data);
    return unwrapData(res);
  },

  resetUserPassword: async (userId, newPassword) => {
    const res = await apiClient.patch(`${ENDPOINTS.ADMIN.USERS}/${userId}/password`, { newPassword });
    return unwrapData(res);
  },

  toggleAdminActive: async (userId) => {
    const res = await apiClient.patch(`${ENDPOINTS.ADMIN.USERS}/${userId}/admin-active`);
    return unwrapData(res);
  },
};

export default dashboardApi;
