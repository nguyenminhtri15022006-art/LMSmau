import React from 'react';

/**
 * PageLoader - Một component hiển thị spinner trung tâm
 * Thường dùng làm Suspense fallback cho các page lazy-load.
 */
const PageLoader = ({ fullScreen = false }) => {
  return (
    <div className={fullScreen ? "loading-screen" : "page-loader"}>
      <div className="loading-screen__spinner" />
      <span className="loading-screen__text">Đang tải nội dung...</span>
    </div>
  );
};

export default PageLoader;
