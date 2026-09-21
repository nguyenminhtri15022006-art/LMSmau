import { formatDateTimeVN } from './dateTime';

export function formatLimit(value, unit) {
  if (value === null || value === undefined || value === '') return 'Chưa thiết lập';
  return `${value} ${unit}`;
}

export function formatSubmittedAt(value) {
  if (!value) return 'Chưa có thời gian';

  return formatDateTimeVN(value) || value;
}

export function formatSubmissionStatus(status) {
  const statusMap = {
    AC: 'Accepted',
    ACCEPTED: 'Accepted',
    WA: 'Wrong answer',
    WRONG_ANSWER: 'Wrong answer',
    TLE: 'Time limit',
    TIME_LIMIT_EXCEEDED: 'Time limit',
    MLE: 'Memory limit',
    MEMORY_LIMIT_EXCEEDED: 'Memory limit',
    CE: 'Compile error',
    COMPILATION_ERROR: 'Compile error',
    RE: 'Runtime error',
    RUNTIME_ERROR: 'Runtime error',
    IE: 'Internal error',
    INTERNAL_ERROR: 'Internal error',
    PENDING: 'Pending',
    JUDGING: 'Judging',
  };

  return statusMap[status] || status || 'Unknown';
}
