const readNumberEnv = (value, fallback) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
};

const normalizeGatewayHealthUrl = (url) => url.replace('/api/actuator/', '/actuator/');

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const API_TIMEOUT_MS = readNumberEnv(import.meta.env.VITE_API_TIMEOUT_MS, 15000);

export const PUBLIC_IP_API_URL =
  import.meta.env.VITE_PUBLIC_IP_API_URL || 'https://api.ipify.org?format=json';
export const PUBLIC_IP_TIMEOUT_MS = readNumberEnv(
  import.meta.env.VITE_PUBLIC_IP_TIMEOUT_MS,
  3000,
);

export const GOOGLE_AUTH_URI =
  import.meta.env.VITE_GOOGLE_AUTH_URI || 'https://accounts.google.com/o/oauth2/v2/auth';

export const ADMIN_HEALTH_TIMEOUT_MS = readNumberEnv(
  import.meta.env.VITE_ADMIN_HEALTH_TIMEOUT_MS,
  5000,
);
export const ADMIN_HEALTH_IDENTITY_TIMEOUT_MS = readNumberEnv(
  import.meta.env.VITE_ADMIN_HEALTH_IDENTITY_TIMEOUT_MS,
  12000,
);
export const ADMIN_HEALTH_REFRESH_INTERVAL_MS = readNumberEnv(
  import.meta.env.VITE_ADMIN_HEALTH_REFRESH_INTERVAL_MS,
  30000,
);

export const ADMIN_HEALTH_ENDPOINTS = {
  identity:
    import.meta.env.VITE_IDENTITY_HEALTH_URL ||
    'http://localhost:8081/identity/actuator/health',
  course:
    import.meta.env.VITE_COURSE_HEALTH_URL ||
    'http://localhost:8082/course/actuator/health/liveness',
  assignment:
    import.meta.env.VITE_ASSIGNMENT_HEALTH_URL ||
    'http://localhost:8083/assignment/actuator/health/liveness',
  blog:
    import.meta.env.VITE_BLOG_HEALTH_URL ||
    'http://localhost:8084/blog/actuator/health/liveness',
  chatbot:
    import.meta.env.VITE_CHATBOT_HEALTH_URL ||
    `${API_BASE_URL}/api/chatbot/actuator/health/liveness`,
  gateway: normalizeGatewayHealthUrl(
    import.meta.env.VITE_GATEWAY_HEALTH_URL ||
      'http://localhost:8080/actuator/health/liveness',
  ),
};
