import React, { Suspense, useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import HeaderStudent from '../../components/ui/HeaderStudent';
import FooterStudent from '../../components/ui/FooterStudent';
import ChatBot from '../../components/ui/ChatBot';
import PageLoader from '../../components/ui/PageLoader';
import { ProblemProvider } from '../../context/ProblemContext';
import useAuth from '../../hooks/useAuth';

// Các trang không hiện chatbot
const NO_CHATBOT_ROUTES = ['/', '/home', '/dashboard'];
const NO_CHATBOT_PREFIXES = ['/blog', '/'];
const QUIZ_ATTEMPT_ROUTE = /^\/exercises\/quiz\/[^/]+$/;

function useShouldShowChatbot() {
  const { pathname } = useLocation();
  if (NO_CHATBOT_ROUTES.includes(pathname)) return false;
  if (pathname.startsWith('/blog')) return false;
  if (QUIZ_ATTEMPT_ROUTE.test(pathname)) return false;
  return true;
}

// =============================================
// StudentLayout
// =============================================

function StudentLayout() {
  const { pathname } = useLocation();
  const { isAuthenticated } = useAuth();
  const baseShowChatbot = useShouldShowChatbot();
  const [chatbotVisibilityOverride, setChatbotVisibilityOverride] = useState(null);
  const showChatbot = isAuthenticated && (chatbotVisibilityOverride ?? baseShowChatbot);

  useEffect(() => {
    setChatbotVisibilityOverride(null);
  }, [pathname]);

  return (
    <ProblemProvider>
      <div className="student-layout">
        <HeaderStudent />
        <main className="student-main">
          <Suspense fallback={<PageLoader />}>
            <Outlet context={{ setChatbotVisibilityOverride }} />
          </Suspense>
        </main>
        <FooterStudent role="student" />
        {showChatbot && <ChatBot />}
      </div>
    </ProblemProvider>
  );
}

export default StudentLayout;
