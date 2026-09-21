import React, { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import HeaderStudent from '../../components/ui/HeaderStudent';
import FooterStudent from '../../components/ui/FooterStudent';
import ChatBot from '../../components/ui/ChatBot';
import PageLoader from '../../components/ui/PageLoader';

// =============================================
// InstructorLayout
// =============================================

function InstructorLayout() {
  return (
    <div className="instructor-layout">
      {/* <Sidebar role="INSTRUCTOR" /> */}
      <HeaderStudent />
      <main className="instructor-main">
        <Suspense fallback={<PageLoader />}>
          <Outlet />
        </Suspense>
      </main>
      <FooterStudent role="instructor" />
      <ChatBot />
    </div>
  );
}

export default InstructorLayout;
