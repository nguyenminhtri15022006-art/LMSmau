import { describe, expect, it } from 'vitest';
import {
  formatDateTimeVN,
  formatDateVN,
  formatTimeVN,
  formatVN,
  parseBackendUtcDate,
  utcIsoToVietnamLocalInput,
  vietnamLocalInputToUtcIso,
} from '../../utils/dateTime';

describe('dateTime utilities', () => {
  it('parses backend dates and rejects invalid values', () => {
    expect(parseBackendUtcDate(null)).toBeNull();
    expect(parseBackendUtcDate('')).toBeNull();
    expect(parseBackendUtcDate('not-a-date')).toBeNull();
    expect(parseBackendUtcDate(new Date('invalid'))).toBeNull();
    expect(parseBackendUtcDate('2024-01-02 03:04:05')?.toISOString()).toBe('2024-01-02T03:04:05.000Z');
    expect(parseBackendUtcDate('2024-01-02T03:04:05+07:00')?.toISOString()).toBe('2024-01-01T20:04:05.000Z');
  });

  it('formats date and time in Vietnam timezone', () => {
    expect(formatDateVN('2024-01-02T00:00:00Z')).toContain('02');
    expect(formatTimeVN('2024-01-02T00:30:00Z')).toContain('07');
    expect(formatDateTimeVN(null)).toBe('');
    expect(formatVN('2024-01-02T00:00:00Z', { year: 'numeric' })).toContain('2024');
  });

  it('converts between Vietnam local input and UTC ISO', () => {
    expect(vietnamLocalInputToUtcIso('2024-01-02T08:30')).toBe('2024-01-02T01:30:00.000Z');
    expect(vietnamLocalInputToUtcIso('bad-date')).toBeNull();
    expect(utcIsoToVietnamLocalInput('2024-01-02T01:30:00Z')).toBe('2024-01-02T08:30');
    expect(utcIsoToVietnamLocalInput(null)).toBe('');
  });
});
