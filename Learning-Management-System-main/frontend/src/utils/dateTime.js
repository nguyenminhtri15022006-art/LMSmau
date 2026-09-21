export const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh';

const VIETNAM_OFFSET = '+07:00';
const BACKEND_OFFSET_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i;

const hasTimeZoneOffset = (value) => BACKEND_OFFSET_PATTERN.test(value.trim());

export function parseBackendUtcDate(value) {
  if (!value) return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;

  const raw = String(value).trim();
  if (!raw) return null;

  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T');
  const date = new Date(hasTimeZoneOffset(normalized) ? normalized : `${normalized}Z`);

  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatDateVN(value, options = {}) {
  const date = parseBackendUtcDate(value);
  if (!date) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    ...options,
  }).format(date);
}

export function formatVN(value, options = {}) {
  const date = parseBackendUtcDate(value);
  if (!date) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    ...options,
  }).format(date);
}

export function formatTimeVN(value, options = {}) {
  const date = parseBackendUtcDate(value);
  if (!date) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    ...options,
  }).format(date);
}

export function formatDateTimeVN(value, options = {}) {
  const date = parseBackendUtcDate(value);
  if (!date) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    ...options,
  }).format(date);
}

export function vietnamLocalInputToUtcIso(value) {
  if (!value) return null;
  const raw = String(value).trim();
  const localWithSeconds = raw.length === 16 ? `${raw}:00` : raw;
  const date = new Date(`${localWithSeconds}${VIETNAM_OFFSET}`);

  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

export function utcIsoToVietnamLocalInput(value) {
  const date = parseBackendUtcDate(value);
  if (!date) return '';

  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: VIETNAM_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    hourCycle: 'h23',
  }).formatToParts(date);

  const get = (type) => parts.find((part) => part.type === type)?.value;
  return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`;
}
