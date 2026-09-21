import { describe, it, expect } from 'vitest';
import {
  formatLimit,
  formatSubmittedAt,
  formatSubmissionStatus,
} from '../../utils/codeChallengeFormatters';

describe('formatLimit', () => {
  it('formats value with unit', () => {
    expect(formatLimit(100, 'ms')).toBe('100 ms');
    expect(formatLimit(256, 'MB')).toBe('256 MB');
  });

  it('returns fallback for null/undefined/empty', () => {
    expect(formatLimit(null, 'ms')).toBe('Chưa thiết lập');
    expect(formatLimit(undefined, 'ms')).toBe('Chưa thiết lập');
    expect(formatLimit('', 'ms')).toBe('Chưa thiết lập');
  });
});

describe('formatSubmittedAt', () => {
  it('formats a valid ISO date', () => {
    const result = formatSubmittedAt('2024-06-15T10:30:00Z');
    expect(result).toContain('/');
    expect(result).toContain(':');
  });

  it('returns fallback for null/undefined', () => {
    expect(formatSubmittedAt(null)).toBe('Chưa có thời gian');
    expect(formatSubmittedAt(undefined)).toBe('Chưa có thời gian');
  });
});

describe('formatSubmissionStatus', () => {
  it('maps known status codes to display names', () => {
    expect(formatSubmissionStatus('ACCEPTED')).toBe('Accepted');
    expect(formatSubmissionStatus('WRONG_ANSWER')).toBe('Wrong answer');
    expect(formatSubmissionStatus('TIME_LIMIT_EXCEEDED')).toBe('Time limit');
    expect(formatSubmissionStatus('COMPILATION_ERROR')).toBe('Compile error');
    expect(formatSubmissionStatus('RUNTIME_ERROR')).toBe('Runtime error');
    expect(formatSubmissionStatus('PENDING')).toBe('Pending');
    expect(formatSubmissionStatus('JUDGING')).toBe('Judging');
  });

  it('returns raw status for unknown values', () => {
    expect(formatSubmissionStatus('CUSTOM_STATUS')).toBe('CUSTOM_STATUS');
  });

  it('returns Unknown for null/undefined', () => {
    expect(formatSubmissionStatus(null)).toBe('Unknown');
    expect(formatSubmissionStatus(undefined)).toBe('Unknown');
  });
});
