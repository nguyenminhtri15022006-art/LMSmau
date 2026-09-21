import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { courseApi } from "../../../../services/course.api";
import { identityApi } from "../../../../services/identity.api";
import { assignmentApi } from "../../../../services/assignment.api";
import { useToast } from "../../../../components/ui/Toast";
import { buildAppErrorState, getAppErrorRoute } from "../../../../utils/appError";

import CourseHero from "../../components/student/CourseHub/CourseHero";
import ChaptersList from "../../components/student/CourseHub/ChaptersList";
import TeacherAnnouncements from "../../components/student/CourseHub/TeacherAnnouncements";
import CourseSidebar from "../../components/student/CourseHub/CourseSidebar";

import "../../styles/student/CourseHub/CourseHubPage.css";

function buildTree(nodes, completedIds, parentId = null) {
  return nodes
    .filter((node) => (node.parentId ?? null) === parentId)
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    .map((node) => {
      if (node.type === "folder") {
        return {
          id: node.id,
          type: "folder",
          title: node.title,
          items: buildTree(nodes, completedIds, node.id),
        };
      }
      return {
        id: node.id,
        type: "lesson",
        title: node.title,
        lessonId: node.lessonId,
        lessonType: node.lessonType ?? null,
        completed: completedIds.has(node.lessonId),
        items: [],
      };
    });
}

function CourseHubSkeleton() {
  return (
    <div className="course-hub-container">
      <div className="ch-skeleton-breadcrumb" />
      <div className="ch-skeleton-hero" />
      <div className="ch-content-grid" style={{ marginTop: 32 }}>
        <div className="ch-main-column">
          <div className="ch-skeleton-card ch-skeleton-card--tall" />
          <div className="ch-skeleton-card" />
        </div>
        <div>
          <div className="ch-skeleton-card ch-skeleton-card--short" />
          <div className="ch-skeleton-card ch-skeleton-card--short" />
        </div>
      </div>
    </div>
  );
}

const Breadcrumbs = ({ courseTitle }) => (
  <nav className="ch-breadcrumb">
    <Link to="/my-courses">Khóa học của tôi</Link>
    <span className="ch-breadcrumb-separator" style={{ margin: "0 8px", display: "inline-flex", alignItems: "center" }}>
      <span className="material-symbols-outlined" style={{ fontSize: "16px" }}>chevron_right</span>
    </span>
    <span className="ch-breadcrumb-current">{courseTitle ?? "..."}</span>
  </nav>
);

export default function CourseHubPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const courseId = new URLSearchParams(location.search).get("id");

  const [course, setCourse] = useState(null);
  const [tree, setTree] = useState([]);
  const [totalLessons, setTotalLessons] = useState(0);
  const [progress, setProgress] = useState(null);
  const [schedules, setSchedules] = useState([]);
  const [instructor, setInstructor] = useState(null);
  const [loading, setLoading] = useState(true);
  const [schedulesError, setSchedulesError] = useState(false);
  const [blockingError, setBlockingError] = useState(null);

  const [announcements, setAnnouncements] = useState([]);
  const [announcementsLoading, setAnnouncementsLoading] = useState(true);
  const [announcementsPage, setAnnouncementsPage] = useState(0);
  const [announcementsTotalPages, setAnnouncementsTotalPages] = useState(0);
  const ANNOUNCEMENTS_PAGE_SIZE = 5;

  const [quizzes, setQuizzes] = useState([]);
  const [completedQuizCount, setCompletedQuizCount] = useState(0);
  const [problems, setProblems] = useState([]);
  const [completedProblemCount, setCompletedProblemCount] = useState(0);
  const [assignmentsLoading, setAssignmentsLoading] = useState(true);

  useEffect(() => {
    if (!courseId) {
      setBlockingError(buildAppErrorState(null, {
        title: "Không tìm thấy khóa học",
        message: "Liên kết khóa học không hợp lệ hoặc nội dung không còn tồn tại.",
        variant: "not-found",
        icon: "travel_explore",
        fallbackPath: "/my-courses",
      }));
      setLoading(false);
      return;
    }

    const fetchAll = async () => {
      setLoading(true);
      setBlockingError(null);
      try {
        const [courseRes, structure, progressData, schedulesResult] = await Promise.all([
          courseApi.getById(courseId),
          courseApi.getStructure(courseId).catch(() => null),
          courseApi.getProgress(courseId).catch(() => null),
          courseApi.getSchedules(courseId)
            .then((data) => ({ ok: true, data }))
            .catch((err) => {
              console.error("[CourseHub] schedules error:", err?.response?.status);
              return { ok: false, data: [] };
            }),
        ]);

        const courseData = courseRes?.data?.data ?? courseRes?.data;
        if (!courseData) throw new Error("Không tìm thấy khóa học.");

        setCourse(courseData);
        setProgress(progressData);
        setSchedulesError(!schedulesResult.ok);
        setSchedules(schedulesResult.data);

        if (courseData.instructorId) {
          identityApi.getPublicProfile(courseData.instructorId)
            .then(setInstructor)
            .catch(() => null);
        }

        if (structure?.nodes?.length) {
          const nodes = structure.nodes;
          const lessonNodes = nodes.filter((node) => node.type === "lesson" && node.lessonId);
          setTotalLessons(lessonNodes.length);

          const completedIds = new Set(progressData?.completedLessonIds ?? []);
          setTree(buildTree(nodes, completedIds));
        } else {
          setTotalLessons(0);
          setTree([]);
        }
      } catch (err) {
        console.error("[CourseHub] fetch failed:", err);
        setBlockingError(buildAppErrorState(err, {
          title: "Không thể tải trung tâm khóa học",
          fallbackPath: "/my-courses",
        }));
      } finally {
        setLoading(false);
      }
    };

    fetchAll();
  }, [courseId]);

  useEffect(() => {
    if (!courseId) return;
    setAnnouncementsLoading(true);
    courseApi.getAnnouncements(courseId, announcementsPage, ANNOUNCEMENTS_PAGE_SIZE)
      .then((pageData) => {
        setAnnouncements(pageData?.content ?? []);
        setAnnouncementsTotalPages(pageData?.totalPages ?? 0);
      })
      .catch(() => {
        setAnnouncements([]);
        setAnnouncementsTotalPages(0);
        toast.error("Không thể tải thông báo khóa học. Bạn vẫn có thể tiếp tục sử dụng các phần khác.");
      })
      .finally(() => setAnnouncementsLoading(false));
  }, [courseId, announcementsPage, toast]);

  useEffect(() => {
    if (!courseId) return;
    setAssignmentsLoading(true);

    Promise.all([
      assignmentApi.getQuizzesForStudent(courseId, { size: 100 }).catch(() => []),
      assignmentApi.getProblems({ courseId, size: 100 }).catch(() => []),
    ]).then(async ([quizResult, problemResult]) => {
      const quizList = Array.isArray(quizResult) ? quizResult : (quizResult?.content ?? []);
      const problemList = Array.isArray(problemResult) ? problemResult : (problemResult?.content ?? []);

      setQuizzes(quizList);
      setProblems(problemList);

      if (quizList.length > 0) {
        let done = 0;
        await Promise.all(quizList.map((quiz) =>
          assignmentApi.getAttemptHistory(quiz.quizId, { size: 10 })
            .then((history) => {
              const attempts = Array.isArray(history) ? history : (history?.content ?? []);
              if (attempts.some((attempt) => attempt.status === "SUBMITTED")) done++;
            })
            .catch(() => {})
        ));
        setCompletedQuizCount(done);
      } else {
        setCompletedQuizCount(0);
      }

      if (problemList.length > 0) {
        let done = 0;
        await Promise.all(problemList.map((problem) => {
          const problemId = problem.id ?? problem.problemId;
          return assignmentApi.getSubmissionHistory({ problemId, size: 1, status: "AC" })
            .then((history) => {
              const list = Array.isArray(history) ? history : (history?.content ?? []);
              if (list.length > 0) done++;
            })
            .catch(() => {});
        }));
        setCompletedProblemCount(done);
      } else {
        setCompletedProblemCount(0);
      }
    }).finally(() => setAssignmentsLoading(false));
  }, [courseId]);

  if (blockingError) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={blockingError} />;
  }

  if (loading) return <CourseHubSkeleton />;

  const instructorName = instructor
    ? `${instructor.firstName ?? ""} ${instructor.lastName ?? ""}`.trim() || instructor.username
    : null;
  const instructorAvatar = instructorName
    ? instructorName.split(" ").map((word) => word[0]).slice(0, 2).join("").toUpperCase()
    : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="course-hub-container"
    >
      <Breadcrumbs courseTitle={course?.title} />

      <CourseHero
        title={course?.title}
        instructorName={instructorName}
        instructorAvatar={instructorAvatar}
        meetingUrl={course?.meetingUrl}
        schedules={schedules}
        schedulesError={schedulesError}
      />

      <div className="ch-content-grid">
        <div className="ch-main-column">
          <ChaptersList
            chapters={tree}
            totalLessons={totalLessons}
            loading={loading}
            onLessonClick={(lesson) => navigate("/my-courses/detail", { state: { courseId, lessonId: lesson.lessonId } })}
          />
          <TeacherAnnouncements
            announcements={announcements}
            loading={announcementsLoading}
            page={announcementsPage}
            totalPages={announcementsTotalPages}
            onPageChange={setAnnouncementsPage}
          />
        </div>

        <CourseSidebar
          courseId={courseId}
          progress={progress}
          quizCount={quizzes.length}
          completedQuizCount={completedQuizCount}
          problemCount={problems.length}
          completedProblemCount={completedProblemCount}
          assignmentsLoading={assignmentsLoading}
        />
      </div>
    </motion.div>
  );
}
