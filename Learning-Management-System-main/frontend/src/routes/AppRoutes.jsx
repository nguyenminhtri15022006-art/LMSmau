import React, {lazy, Suspense} from 'react';
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import PrivateRoute from './PrivateRoute';
import RoleRoute from './RoleRoute';
import FallbackRoute from './FallbackRoute';
import ROLES from '../constants/roles';
import AppErrorPage from '../features/errors/pages/AppErrorPage';


// Layouts
const AuthLayout = lazy(() => import('../layouts/AuthLayout/AuthLayout'));
const StudentLayout = lazy(() => import('../layouts/StudentLayout/StudentLayout'));
const InstructorLayout = lazy(() => import('../layouts/InstructorLayout/InstructorLayout'));
const AdminLayout = lazy(() => import('../layouts/AdminLayout/AdminLayout'));

// Public Pages
const AboutUsPage = lazy(() => import('../features/aboutus/pages/AboutUsPage'));
const AuthPage = lazy(() => import('../features/auth/pages/AuthPage'));
const ForgotPasswordPage = lazy(() => import('../features/auth/pages/ForgotPasswordPage'));
const SocialCallbackPage = lazy(() => import('../features/auth/pages/SocialCallbackPage'));

// Student Pages
const StudentHome = lazy(() => import('../features/home/pages/HomePage'));
const ListCoursePage = lazy(() => import('../features/courses/pages/student/ListCoursePage'));
const DetailCoursePage = lazy(() => import('../features/courses/pages/student/DetailCoursePage'));
const MyCoursePage = lazy(() => import('../features/courses/pages/student/MyCoursePage'));
const CourseHubPage = lazy(() => import('../features/courses/pages/student/CourseHubPage'));
const DetailMyCoursePage = lazy(() => import('../features/courses/pages/student/DetailMyCoursePage'));
const BlogHubPage = lazy(() => import('../features/blog/pages/student/BlogHubPage'));
const ExerciseResultsPage = lazy(() => import('../features/assignment/Exercise/pages/teacher/ExerciseResultsPage'));
const BlogDetailPage = lazy(() => import('../features/blog/pages/student/BlogDetailPage'));
const CreateBlogPostPage = lazy(() => import('../features/blog/pages/student/CreateBlogPostPage'));
const EditBlogPostPage = lazy(() => import('../features/blog/pages/student/EditBlogPostPage'));
const MyBlogPostsPage = lazy(() => import('../features/blog/pages/student/MyBlogPostsPage'));
const ExerciseHubPage = lazy(() => import('../features/assignment/Exercise/pages/student/ExerciseHubPage'));
const ExerciseListingPage = lazy(() => import('../features/assignment/code-judge/pages/student/ExerciseListingPage'));
const QuizListingPage = lazy(() => import('../features/assignment/quiz/pages/student/QuizListingPage'));
const ExerciseCodePage = lazy(() => import('../features/assignment/code-judge/pages/student/ExerciseCodePage'));
const DashboardStudent = lazy(() => import('../features/dashboard/pages/student/DashboardStudent'));
const QuizOverviewPage = lazy(() => import('../features/assignment/quiz/pages/student/QuizOverviewPage'));
const ExerciseQuizPage = lazy(() => import('../features/assignment/quiz/pages/student/ExerciseQuizPage'));
const ExerciseQuizResultPage = lazy(() => import('../features/assignment/quiz/pages/student/ExerciseQuizResultPage'));
const QuizHistoryDetailPage = lazy(() => import('../features/assignment/quiz/pages/student/QuizHistoryDetailPage'));
const ExerciseCodeResultPage = lazy(() => import('../features/assignment/code-judge/pages/student/ExerciseCodeResultPage'));
const LeaderboardPage = lazy(() => import('../features/assignment/leaderboard/pages/student/LeaderboardPage'));
const SettingsPage = lazy(() => import('../features/settings/pages/SettingsPage'));


// Instructor Pages
const InstructorHome = lazy(() => import('../features/dashboard/pages/teacher/HomePageInstructor'));
const CourseManagement = lazy(() => import('../features/courses/pages/teacher/CourseManagement'));
const CourseManagementCenter = lazy(() => import('../features/courses/pages/teacher/CourseManagementCenter'));
const CreateCoursePage = lazy(() => import('../features/courses/pages/teacher/CreateCoursePage'));
const EditCoursePage = lazy(() => import('../features/courses/pages/teacher/EditCoursePage'));
const AssignmentMgmt = lazy(() => import('../features/aboutus/pages/AboutUsPage'));
const InstructorExerciseHub = lazy(() => import('../features/assignment/Exercise/pages/teacher/InstructorExerciseHub'));
const InstructorExerciseDetail = lazy(() => import('../features/assignment/Exercise/pages/teacher/InstructorExerciseDetail'));
const CreateQuizPage = lazy(() => import('../features/assignment/quiz/pages/teacher/CreateQuizPage'));
const EditQuizPage = lazy(() => import('../features/assignment/quiz/pages/teacher/EditQuizPage'));
const CreateCodingPage = lazy(() => import('../features/assignment/code-judge/pages/teacher/CreateCodingPage'));
const PreviewImportPage = lazy(() => import('../features/assignment/code-judge/pages/teacher/PreviewImportPage'));

// Admin Pages
const AdminHome = lazy(() => import('../features/dashboard/pages/admin/HomePageAdmin'));
const UserMgmt = lazy(() => import('../features/dashboard/components/admin/Management/UserManagement'));
const AdminCourseMgmt = lazy(() => import('../features/dashboard/components/admin/Management/CourseManagement'));
const WebContentMgmt = lazy(() => import('../features/dashboard/components/admin/Management/WebContentMgmt'));
const CodeJudgeMgmt = lazy(() => import('../features/dashboard/components/admin/Management/CodeJudgeMgmt'));
const BlogMgmt = lazy(() => import('../features/dashboard/components/admin/Management/BlogMgmt'));
const AdminMgmt = lazy(() => import('../features/dashboard/components/admin/Management/AdminManagement'));

// =============================================
// Application Routes with RBAC (Refactored)
// =============================================

function AppRoutes() {
    return (
        <BrowserRouter>
            <Suspense fallback={
                <div className="loading-screen">
                    <div className="loading-screen__spinner"/>
                    <span className="loading-screen__text">Đang tải...</span>
                </div>
            }>
                <Routes>

                    {/* 1. Auth & Public Entry */}
                    <Route element={<AuthLayout/>}>
                        <Route path="/login" element={<AuthPage/>}/>
                        <Route path="/register" element={<AuthPage/>}/>
                        <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>
                        <Route path="/authenticate" element={<SocialCallbackPage/>}/>
                    </Route>

                    {/* 2. Public / Guest Accessible Routes */}
                    <Route element={<StudentLayout/>}>
                        <Route path="/" element={<FallbackRoute/>}/>
                        <Route path="/home" element={<Navigate to="/" replace/>}/>
                        <Route path="/about-us" element={<AboutUsPage/>}/>
                        <Route path="/list-course" element={<ListCoursePage/>}/>
                        <Route path="/list-course/detail-course" element={<DetailCoursePage/>}/>
                        <Route path="/list-course/detail-course/:courseId" element={<DetailCoursePage/>}/>

                        <Route path="/blog" element={<BlogHubPage/>}/>
                        <Route path="/blog/:id" element={<BlogDetailPage/>}/>
                        <Route path="/error" element={<AppErrorPage/>}/>
                    </Route>

                    {/* 3. Protected Routes (Student, Instructor & Admin) */}
                    <Route element={<PrivateRoute/>}>
                        <Route element={<StudentLayout/>}>
                            <Route path="/settings" element={<SettingsPage/>}/>
                        </Route>

                        {/* --- Student Branch --- */}
                        <Route element={<RoleRoute allowedRoles={[ROLES.STUDENT]}/>}>
                            <Route element={<StudentLayout/>}>
                                <Route path="/dashboard" element={<DashboardStudent/>}/>
                                <Route path="/my-courses" element={<MyCoursePage/>}/>
                                <Route path="/my-courses/detail" element={<DetailMyCoursePage/>}/>
                                <Route path="/course-hub" element={<CourseHubPage/>}/>

                                <Route path="/blog/create" element={<CreateBlogPostPage/>}/>
                                <Route path="/blog/edit/:id" element={<EditBlogPostPage/>}/>
                                <Route path="/blog/my-posts" element={<MyBlogPostsPage/>}/>

                                <Route path="/exercises" element={<ExerciseHubPage/>}/>
                                <Route path="/exercises/code/:slug" element={<ExerciseListingPage/>}/>
                                <Route path="/exercises/quizzes/:courseId" element={<QuizListingPage/>}/>
                                <Route path="/exercises/challenge/:slugOrId" element={<ExerciseCodePage/>}/>
                                <Route path="/exercises/challenge/:id/:legacySlug" element={<ExerciseCodePage/>}/>
                                <Route path="/exercises/quiz/:id/overview" element={<QuizOverviewPage/>}/>
                                <Route path="/exercises/quiz/:id" element={<ExerciseQuizPage/>}/>
                                <Route path="/exercises/quiz/:id/result" element={<ExerciseQuizResultPage/>}/>
                                <Route path="/exercises/quiz/:id/history/:attemptId"
                                       element={<QuizHistoryDetailPage/>}/>
                                <Route path="/exercises/challenge/:slug/result" element={<ExerciseCodeResultPage/>}/>
                                <Route path="/exercises/challenge/:id/:legacySlug/result"
                                       element={<ExerciseCodeResultPage/>}/>
                                <Route path="/exercises/leaderboard/:slug" element={<LeaderboardPage/>}/>
                            </Route>
                        </Route>

                        {/* --- Instructor Branch --- */}
                        <Route element={<RoleRoute allowedRoles={[ROLES.INSTRUCTOR]}/>}>
                            <Route element={<InstructorLayout/>}>
                                <Route path="/instructor/home" element={<InstructorHome/>}/>
                                <Route path="/manage/courses" element={<CourseManagement/>}/>
                                <Route path="/manage/courses/create" element={<CreateCoursePage/>}/>
                                <Route path="/manage/courses/edit/:courseId" element={<EditCoursePage/>}/>
                                <Route path="/manage/courses/center/:courseId" element={<CourseManagementCenter/>}/>
                                <Route path="/manage/assignments" element={<AssignmentMgmt/>}/>

                                {/* Instructor Exercise Routes */}
                                <Route path="/instructor/exercises" element={<InstructorExerciseHub/>}/>
                                <Route path="/instructor/exercises/:courseId" element={<InstructorExerciseDetail/>}/>
                                <Route path="/instructor/exercises/:courseId/create-quiz" element={<CreateQuizPage/>}/>
                                <Route path="/instructor/exercises/:courseId/manual-quiz" element={<EditQuizPage/>}/>
                                <Route path="/instructor/exercises/:courseId/quiz/edit/:quizId"
                                       element={<EditQuizPage/>}/>
                                <Route path="/instructor/exercises/:courseId/create-coding"
                                       element={<CreateCodingPage/>}/>
                                <Route path="/instructor/exercises/:courseId/code-judge/edit/:slugOrId"
                                       element={<CreateCodingPage/>}/>
                                <Route path="/instructor/exercises/:courseId/code-judge/preview"
                                       element={<PreviewImportPage/>}/>
                                <Route path="/instructor/exercises/:courseId/results" element={<ExerciseResultsPage/>}/>

                                {/* Instructor Blog Routes (Reusing Student Components) */}
                                <Route path="/instructor/blog" element={<BlogHubPage/>}/>
                                <Route path="/instructor/blog/create" element={<CreateBlogPostPage/>}/>
                                <Route path="/instructor/blog/edit/:id" element={<EditBlogPostPage/>}/>
                                <Route path="/instructor/blog/my-posts" element={<MyBlogPostsPage/>}/>
                                <Route path="/instructor/blog/:id" element={<BlogDetailPage/>}/>
                                <Route path="/instructor/error" element={<AppErrorPage/>}/>
                            </Route>
                        </Route>

                        {/* --- Admin Branch --- */}
                        <Route element={<RoleRoute allowedRoles={[ROLES.ADMIN]}/>}>
                            <Route element={<AdminLayout/>}>
                                <Route path="/admin/home" element={<AdminHome/>}/>
                                <Route path="/admin/users" element={<UserMgmt/>}/>
                                <Route path="/admin/admins" element={<AdminMgmt/>}/>
                                <Route path="/admin/all-courses" element={<AdminCourseMgmt/>}/>
                                <Route path="/admin/all-courses/:courseId" element={<DetailCoursePage adminPreview/>}/>
                                <Route path="/admin/web-content" element={<WebContentMgmt/>}/>
                                <Route path="/admin/judge" element={<CodeJudgeMgmt/>}/>
                                <Route path="/admin/blog" element={<BlogMgmt/>}/>
                                <Route path="/admin/blog/:id" element={<BlogDetailPage adminPreview/>}/>
                                <Route path="/admin/settings" element={<SettingsPage/>}/>
                                <Route path="/admin/error" element={<AppErrorPage/>}/>
                            </Route>
                        </Route>

                    </Route>

                    {/* 3. Global Fallback */}
                    <Route path="*" element={<Navigate to="/" replace/>}/>
                </Routes>
            </Suspense>
        </BrowserRouter>
    );
}

export default AppRoutes;
