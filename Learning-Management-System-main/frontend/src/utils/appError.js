const SERVICE_UNAVAILABLE_ERROR = {
  title: 'Dịch vụ tạm thời không khả dụng',
  message: 'Một dịch vụ cần thiết đang gián đoạn. Vui lòng quay lại sau ít phút.',
  variant: 'service',
  icon: 'cloud_off',
};

const SYSTEM_ERROR = {
  title: 'Hệ thống đang gặp sự cố',
  message: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối hoặc thử lại sau.',
  variant: 'service',
  icon: 'dns',
};

const ERROR_BY_CODE = {
  1056: SERVICE_UNAVAILABLE_ERROR,
  1405: SERVICE_UNAVAILABLE_ERROR,
  2998: SERVICE_UNAVAILABLE_ERROR,
  3210: {
    title: 'Chưa đến thời gian công khai kết quả',
    message: 'Kết quả bài quiz này chưa được mở để xem. Vui lòng quay lại sau thời điểm công khai.',
    variant: 'locked',
    icon: 'lock_clock',
  },
  3205: {
    title: 'Đã hết lượt làm bài',
    message: 'Bạn đã sử dụng hết số lần làm bài cho quiz này.',
    variant: 'warning',
    icon: 'block',
  },
  3998: SERVICE_UNAVAILABLE_ERROR,
  4998: SERVICE_UNAVAILABLE_ERROR,
  6998: SERVICE_UNAVAILABLE_ERROR,
};

const ERROR_BY_STATUS = {
  403: {
    title: 'Không thể truy cập nội dung này',
    message: 'Bạn chưa có quyền hoặc nội dung này chưa được mở.',
    variant: 'locked',
    icon: 'lock',
  },
  404: {
    title: 'Không tìm thấy nội dung',
    message: 'Nội dung bạn cần có thể đã bị xóa hoặc đường dẫn không còn hợp lệ.',
    variant: 'not-found',
    icon: 'travel_explore',
  },
  503: SERVICE_UNAVAILABLE_ERROR,
};

const DEFAULT_ERROR = {
  title: 'Không thể hoàn tất thao tác',
  message: 'Đã có lỗi xảy ra. Vui lòng thử lại sau.',
  variant: 'warning',
  icon: 'error',
};

const SERVICE_UNAVAILABLE_CODES = new Set([1056, 1405, 2998, 3998, 4998, 6998]);

function resolveStatus(error) {
  const rawStatus = error?.response?.status || error?.response?.data?.status;
  if (typeof rawStatus === 'number') return rawStatus;
  if (typeof rawStatus === 'string') {
    const parsed = Number(rawStatus);
    return Number.isNaN(parsed) ? rawStatus : parsed;
  }
  return null;
}

export function buildAppErrorState(error, options = {}) {
  const code = error?.response?.data?.code;
  const status = resolveStatus(error);
  const apiMessage = error?.response?.data?.message;
  const isTimeout = error?.code === 'ECONNABORTED';
  const isNetworkError = !error?.response && Boolean(error);

  let preset = ERROR_BY_CODE[code] || ERROR_BY_STATUS[status] || DEFAULT_ERROR;

  if (isTimeout || isNetworkError || (typeof status === 'number' && status >= 500)) {
    preset = status === 503 || SERVICE_UNAVAILABLE_CODES.has(code)
      ? SERVICE_UNAVAILABLE_ERROR
      : SYSTEM_ERROR;
  }

  const usePresetMessage = preset.variant === 'service';

  return {
    title: usePresetMessage ? preset.title : options.title || preset.title,
    message: options.message || (usePresetMessage ? preset.message : apiMessage || preset.message),
    code: options.code || code || null,
    status: options.status || status || null,
    variant: options.variant || preset.variant,
    icon: options.icon || preset.icon,
    actionLabel: options.actionLabel || 'Quay lại',
    fallbackPath: options.fallbackPath || '/dashboard',
    fallbackState: options.fallbackState || null,
  };
}

export function getDefaultAppErrorFallbackPath(pathname = '') {
  if (pathname.startsWith('/admin')) {
    return '/admin/home';
  }
  if (pathname.startsWith('/instructor') || pathname.startsWith('/manage/')) {
    return '/instructor/home';
  }
  if (
    pathname.startsWith('/dashboard') ||
    pathname.startsWith('/my-courses') ||
    pathname.startsWith('/exercises') ||
    pathname.startsWith('/settings')
  ) {
    return '/dashboard';
  }
  return '/';
}

export function getAppErrorRoute(pathname = '') {
  if (pathname.startsWith('/admin')) {
    return '/admin/error';
  }
  if (pathname.startsWith('/instructor') || pathname.startsWith('/manage/')) {
    return '/instructor/error';
  }
  return '/error';
}
