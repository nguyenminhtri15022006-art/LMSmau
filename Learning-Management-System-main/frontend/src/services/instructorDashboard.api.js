import apiClient from './api.client';
import { ENDPOINTS } from '../constants/endpoints';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const instructorDashboardApi = {
  getMyStats: async () => {
    const res = await apiClient.get(ENDPOINTS.INSTRUCTOR.MY_STATS);
    return unwrapData(res) || { totalCourses: 0, totalStudents: 0, totalExercises: 0 };
  },

  getRecentEnrollments: async (limit = 10) => {
    const res = await apiClient.get(ENDPOINTS.INSTRUCTOR.RECENT_ENROLLMENTS, {
      params: { limit },
    });
    return unwrapData(res) || [];
  },

  getTopCourses: async (topN = 3) => {
    const res = await apiClient.get(ENDPOINTS.COURSES.MY_COURSES, {
      params: { page: 0, size: 20 },
    });
    const courses = unwrapData(res)?.content || [];

    const studentCountResults = await Promise.allSettled(
      courses.map((c) => apiClient.get(ENDPOINTS.COURSES.STUDENTS_COUNT(c.id)))
    );

    const averageProgressResults = await Promise.allSettled(
      courses.map((c) => apiClient.get(ENDPOINTS.COURSES.AVERAGE_PROGRESS(c.id)))
    );

    const enriched = courses.map((course, i) => ({
      ...course,
      studentCount:
        studentCountResults[i].status === 'fulfilled'
          ? (unwrapData(studentCountResults[i].value) ?? 0)
          : 0,
      averageProgress:
        averageProgressResults[i].status === 'fulfilled'
          ? Math.round(unwrapData(averageProgressResults[i].value) ?? 0)
          : 0,
    }));

    return enriched
      .sort((a, b) => b.studentCount - a.studentCount)
      .slice(0, topN);
  },
};

export default instructorDashboardApi;
