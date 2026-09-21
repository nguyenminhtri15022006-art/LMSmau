import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AnimatedPage from '../../../components/ui/AnimatedPage';
import { buildAppErrorState, getDefaultAppErrorFallbackPath } from '../../../utils/appError';

import '../styles/AppErrorPage.css';

const ERROR_KICKERS = {
  service: 'Dịch vụ tạm thời gián đoạn',
  locked: 'Nội dung chưa sẵn sàng',
  'not-found': 'Không tìm thấy nội dung',
  warning: 'Không thể tiếp tục',
};

export default function AppErrorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const fallbackPath = getDefaultAppErrorFallbackPath(location.pathname);
  const error = location.state || buildAppErrorState(null, { fallbackPath });
  const kicker = ERROR_KICKERS[error.variant] || 'Không thể tiếp tục';

  const handleBack = () => {
    if (window.history.state?.idx > 0) {
      navigate(-1);
      return;
    }
    navigate(error.fallbackPath || '/', {
      replace: true,
      state: error.fallbackState || undefined,
    });
  };

  return (
    <AnimatedPage>
      <div className="app-error-page">
        <nav className="app-error-breadcrumb" aria-label="Breadcrumb">
          <Link to={error.fallbackPath || '/'} state={error.fallbackState || undefined}>Trang trước</Link>
          <span className="material-symbols-outlined">chevron_right</span>
          <span>Thông báo</span>
        </nav>

        <section className={`app-error-card app-error-card--${error.variant || 'warning'}`}>
          <div className="app-error-icon" aria-hidden="true">
            <span className="material-symbols-outlined">{error.icon || 'error'}</span>
          </div>

          <div className="app-error-copy">
            <p className="app-error-kicker">{kicker}</p>
            <h1>{error.title || 'Không thể hoàn tất thao tác'}</h1>
            <p>{error.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.'}</p>
          </div>

          {(error.code || error.status) && (
            <div className="app-error-meta" aria-label="Thông tin lỗi">
              {error.code && <span>Mã lỗi {error.code}</span>}
              {error.status && <span>HTTP {error.status}</span>}
            </div>
          )}

          <button className="app-error-back-btn" type="button" onClick={handleBack}>
            <span className="material-symbols-outlined">arrow_back</span>
            {error.actionLabel || 'Quay lại'}
          </button>
        </section>
      </div>
    </AnimatedPage>
  );
}
