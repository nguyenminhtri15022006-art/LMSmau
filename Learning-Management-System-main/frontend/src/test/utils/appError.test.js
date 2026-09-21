import { describe, it, expect } from 'vitest';
import { buildAppErrorState, getDefaultAppErrorFallbackPath, getAppErrorRoute } from '../../utils/appError';

describe('buildAppErrorState', () => {
  it('returns service_unavailable for code 1405 (gateway fallback)', () => {
    const error = { response: { data: { code: 1405 }, status: 503 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Dịch vụ tạm thời không khả dụng');
    expect(state.variant).toBe('service');
  });

  it('returns service_unavailable for code 2998 (course service unavailable)', () => {
    const error = { response: { data: { code: 2998 }, status: 503 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Dịch vụ tạm thời không khả dụng');
  });

  it('returns service_unavailable for code 3998 (assignment service)', () => {
    const error = { response: { data: { code: 3998 }, status: 503 } };
    const state = buildAppErrorState(error);
    expect(state.variant).toBe('service');
  });

  it('returns locked variant for code 3210 (result not published)', () => {
    const error = { response: { data: { code: 3210 }, status: 400 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Chưa đến thời gian công khai kết quả');
    expect(state.variant).toBe('locked');
  });

  it('returns warning variant for code 3205 (max attempts)', () => {
    const error = { response: { data: { code: 3205 }, status: 400 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Đã hết lượt làm bài');
    expect(state.variant).toBe('warning');
  });

  it('returns locked variant for HTTP 403', () => {
    const error = { response: { status: 403 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Không thể truy cập nội dung này');
    expect(state.variant).toBe('locked');
  });

  it('returns not-found variant for HTTP 404', () => {
    const error = { response: { status: 404 } };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Không tìm thấy nội dung');
    expect(state.variant).toBe('not-found');
  });

  it('returns default error for unknown codes', () => {
    const error = { response: { data: { code: 9999 }, status: 400 } };
    const state = buildAppErrorState(error);
    expect(state.variant).toBe('warning');
  });

  it('returns system_error for network error (no response)', () => {
    const error = { message: 'Network Error' };
    const state = buildAppErrorState(error);
    expect(state.title).toBe('Hệ thống đang gặp sự cố');
    expect(state.variant).toBe('service');
  });

  it('returns system_error for timeout (ECONNABORTED)', () => {
    const error = { code: 'ECONNABORTED', response: { status: 0 } };
    const state = buildAppErrorState(error);
    expect(state.variant).toBe('service');
  });

  it('uses options.title and options.message when provided for non-service errors', () => {
    const error = { response: { data: { code: 3210 }, status: 400 } };
    const state = buildAppErrorState(error, { title: 'Tùy chỉnh', message: 'Thông báo tùy chỉnh' });
    expect(state.title).toBe('Tùy chỉnh');
    expect(state.message).toBe('Thông báo tùy chỉnh');
  });
});

describe('getDefaultAppErrorFallbackPath', () => {
  it('returns /admin/home for admin paths', () => {
    expect(getDefaultAppErrorFallbackPath('/admin/users')).toBe('/admin/home');
  });

  it('returns /instructor/home for instructor paths', () => {
    expect(getDefaultAppErrorFallbackPath('/instructor/exercises')).toBe('/instructor/home');
    expect(getDefaultAppErrorFallbackPath('/manage/courses')).toBe('/instructor/home');
  });

  it('returns /dashboard for student paths', () => {
    expect(getDefaultAppErrorFallbackPath('/dashboard')).toBe('/dashboard');
    expect(getDefaultAppErrorFallbackPath('/my-courses/detail')).toBe('/dashboard');
    expect(getDefaultAppErrorFallbackPath('/exercises/code')).toBe('/dashboard');
  });

  it('returns / for unrecognized paths', () => {
    expect(getDefaultAppErrorFallbackPath('/about-us')).toBe('/');
  });
});

describe('getAppErrorRoute', () => {
  it('returns /admin/error for admin paths', () => {
    expect(getAppErrorRoute('/admin/users')).toBe('/admin/error');
  });

  it('returns /instructor/error for instructor paths', () => {
    expect(getAppErrorRoute('/instructor/home')).toBe('/instructor/error');
    expect(getAppErrorRoute('/manage/courses')).toBe('/instructor/error');
  });

  it('returns /error for all other paths', () => {
    expect(getAppErrorRoute('/dashboard')).toBe('/error');
    expect(getAppErrorRoute('/')).toBe('/error');
  });
});
