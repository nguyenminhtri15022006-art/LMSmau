import '@testing-library/jest-dom';

// Mock vitest environment variables
import.meta.env.VITE_API_BASE_URL = 'http://localhost:8080';
import.meta.env.VITE_API_TIMEOUT_MS = '15000';
