import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';
import { COURSE_DISPLAY_SORT } from '../utils/courseOrder';

const unwrap = (res) => res?.data?.data ?? res?.data;
const publicRequest = {
  skipAuth: true,
  skipAuthRefresh: true,
};

// =============================================
// Course Service API
// =============================================

export const courseApi = {
  getAll: (params) => {
    const sp = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, val]) => {
      if (Array.isArray(val)) val.forEach(v => sp.append(key, String(v)));
      else if (val != null && val !== '') sp.append(key, String(val));
    });
    return apiClient.get(`${ENDPOINTS.COURSES.BASE}?${sp.toString()}`, publicRequest);
  },
  getById: (id) => apiClient.get(ENDPOINTS.COURSES.DETAIL(id), publicRequest),
  enroll: (courseId) => apiClient.post(ENDPOINTS.COURSES.ENROLL(courseId)),
  getEnrolled: () => apiClient.get(ENDPOINTS.COURSES.ENROLLED).then(res => unwrap(res) ?? []),
  getProgress: (courseId) => apiClient.get(ENDPOINTS.COURSES.PROGRESS(courseId)).then(res => unwrap(res) ?? null),

  // --- Instructor: Courses ---
  getMyCourses: (page = 0, size = 50, sort = COURSE_DISPLAY_SORT) =>
    apiClient.get(`${ENDPOINTS.COURSES.MY_COURSES}?page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`)
      .then(res => unwrap(res) ?? { content: [], totalElements: 0, totalPages: 0 }),
  getMyStats: () =>
    apiClient.get(ENDPOINTS.INSTRUCTOR.MY_STATS)
      .then(res => unwrap(res) ?? { totalCourses: 0, totalStudents: 0, totalExercises: 0 }),
  create: (data) => apiClient.post(ENDPOINTS.COURSES.BASE, data),
  update: (id, data) => apiClient.put(ENDPOINTS.COURSES.DETAIL(id), data),
  delete: (id) => apiClient.delete(ENDPOINTS.COURSES.DETAIL(id)),

  // --- Instructor: Students ---
  getStudentsCount: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.STUDENTS_COUNT(courseId)).then(res => unwrap(res) ?? 0),
  getStudents: (courseId, page = 0, size = 20) =>
    apiClient.get(`${ENDPOINTS.COURSES.STUDENTS(courseId)}?page=${page}&size=${size}&sort=enrolledAt,desc`)
      .then(res => unwrap(res) ?? { content: [], totalElements: 0, totalPages: 0 }),
  inviteStudents: (courseId, userIds) =>
    apiClient.post(`${ENDPOINTS.COURSES.BASE}/${courseId}/invite`, { userIds })
      .then(res => unwrap(res)),
  removeStudent: (courseId, userId) =>
    apiClient.delete(ENDPOINTS.COURSES.REMOVE_STUDENT(courseId, userId)),
  searchUserByEmail: (email) =>
    apiClient.get(ENDPOINTS.COURSES.SEARCH_USER, { params: { email } })
      .then(res => unwrap(res)),

  // --- Instructor: Announcements ---
  getAnnouncements: (courseId, page = 0, size = 20) =>
    apiClient.get(`${ENDPOINTS.COURSES.ANNOUNCEMENTS(courseId)}?page=${page}&size=${size}&sort=createdAt,desc`)
      .then(res => unwrap(res) ?? { content: [], totalPages: 0 }),
  createAnnouncement: (courseId, data) =>
    apiClient.post(ENDPOINTS.COURSES.ANNOUNCEMENTS(courseId), data)
      .then(res => unwrap(res)),
  deleteAnnouncement: (courseId, announcementId) =>
    apiClient.delete(`${ENDPOINTS.COURSES.ANNOUNCEMENTS(courseId)}/${announcementId}`),

  // --- Instructor: Schedules ---
  getSchedules: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.SCHEDULES(courseId)).then(res => unwrap(res) ?? []),
  createSchedule: (courseId, data) =>
    apiClient.post(ENDPOINTS.COURSES.SCHEDULES(courseId), data)
      .then(res => unwrap(res)),
  updateSchedule: (courseId, scheduleId, data) =>
    apiClient.put(`${ENDPOINTS.COURSES.SCHEDULES(courseId)}/${scheduleId}`, data)
      .then(res => unwrap(res)),
  deleteSchedule: (courseId, scheduleId) =>
    apiClient.delete(`${ENDPOINTS.COURSES.SCHEDULES(courseId)}/${scheduleId}`),

  // --- Progress ---
  getAverageProgress: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.AVERAGE_PROGRESS(courseId)).then(res => unwrap(res) ?? 0),

  // --- Admin: Courses ---
  getAdminCourses: (params = {}) =>
    apiClient.get(ENDPOINTS.ADMIN.COURSES, { params })
      .then(res => unwrap(res) ?? { content: [], totalElements: 0, totalPages: 0 }),
  lockCourse: (courseId, reason) =>
    apiClient.patch(ENDPOINTS.COURSES.LOCK(courseId), { reason })
      .then(res => unwrap(res)),
  unlockCourse: (courseId) =>
    apiClient.patch(ENDPOINTS.COURSES.UNLOCK(courseId))
      .then(res => unwrap(res)),

  // --- Storage ---
  getPresignedUrl: (fileName, fileType, folder) =>
    apiClient.post(ENDPOINTS.UPLOAD.PRESIGNED, { fileName, fileType, folder })
      .then(res => unwrap(res)),
  deleteStorageFile: (url) =>
    apiClient.delete(`${ENDPOINTS.UPLOAD.PRESIGNED.replace('/presigned-url', '/file')}`, { params: { url } }),

  // --- Course Structure ---
  addStructureNode: (courseId, data) =>
    apiClient.post(`${ENDPOINTS.COURSES.STRUCTURE(courseId)}/nodes`, data)
      .then(res => unwrap(res)),
  deleteStructureNode: (courseId, nodeId) =>
    apiClient.delete(`${ENDPOINTS.COURSES.STRUCTURE(courseId)}/nodes/${nodeId}`),
  reorderStructureNodes: (courseId, nodes) =>
    apiClient.put(`${ENDPOINTS.COURSES.STRUCTURE(courseId)}/reorder`, { nodes })
      .then(res => unwrap(res)),
  updateStructureNodeTitle: (courseId, nodeId, title) =>
    apiClient.patch(`${ENDPOINTS.COURSES.STRUCTURE(courseId)}/nodes/${nodeId}`, { title })
      .then(res => unwrap(res)),
  saveLessonContent: (courseId, lessonId, lessonType, content) =>
    apiClient.put(ENDPOINTS.COURSES.LESSON_DETAIL(courseId, lessonId), { lessonType, content })
      .then(res => unwrap(res)),

  // --- Misc ---
  getCategories: () => apiClient.get(ENDPOINTS.COURSES.CATEGORIES, publicRequest).then(res => unwrap(res) ?? []),
  createCategory: (data) => apiClient.post(ENDPOINTS.COURSES.CATEGORIES, data).then(res => unwrap(res)),
  updateCategory: (id, data) => apiClient.put(`${ENDPOINTS.COURSES.CATEGORIES}/${id}`, data).then(res => unwrap(res)),
  deleteCategory: (id) => apiClient.delete(`${ENDPOINTS.COURSES.CATEGORIES}/${id}`),
  getLessonsCount: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.LESSONS_COUNT(courseId), publicRequest).then(res => unwrap(res) ?? 0),
  getStructure: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.STRUCTURE(courseId), publicRequest).then(res => unwrap(res) ?? null),
  getLessonNodes: (courseId) =>
    apiClient.get(ENDPOINTS.COURSES.LESSON_NODES(courseId)).then(res => unwrap(res) ?? []),
  getLessons: (courseId) => apiClient.get(ENDPOINTS.COURSES.LESSONS(courseId)),
  getLessonById: (courseId, lessonId) =>
    apiClient.get(ENDPOINTS.COURSES.LESSON_DETAIL(courseId, lessonId)).then(res => unwrap(res) ?? null),
  completeLesson: (courseId, lessonId, lessonType) =>
    apiClient.post(ENDPOINTS.COURSES.LESSON_COMPLETE(courseId, lessonId), { lessonType }),
};

export default courseApi;
