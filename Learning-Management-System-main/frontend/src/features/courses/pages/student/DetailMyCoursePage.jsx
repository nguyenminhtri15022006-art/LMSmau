import React, { useEffect, useState, useCallback } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { courseApi } from '../../../../services/course.api';
import { useToast } from '../../../../components/ui/Toast';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import '../../styles/student/DetailMyCourse/DetailMyCourse.css';

function flattenLessons(nodes) {
  const result = [];
  function walk(items) {
    for (const item of items) {
      const type = item.type?.toLowerCase();
      if (type === 'lesson') result.push(item);
      else if (item.items?.length) walk(item.items);
    }
  }
  walk(nodes);
  return result;
}

function buildTree(nodes, completedIds, parentId = null) {
  return nodes
    .filter((node) => (node.parentId ?? null) === parentId)
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    .map((node) => {
      const type = node.type?.toLowerCase();
      if (type === 'folder') {
        return {
          id: node.id,
          type: 'folder',
          title: node.title,
          items: buildTree(nodes, completedIds, node.id),
        };
      }
      return {
        id: node.id,
        type: 'lesson',
        title: node.title,
        lessonId: node.lessonId,
        lessonType: node.lessonType ?? null,
        completed: completedIds.has(node.lessonId),
      };
    });
}

function VideoPlayer({ url, title }) {
  const isEmbed =
    url &&
    (url.includes('youtube.com') ||
      url.includes('youtu.be') ||
      url.includes('vimeo.com') ||
      url.includes('/embed/'));

  if (isEmbed) {
    return (
      <iframe
        src={url}
        title={title ?? 'Video bài học'}
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowFullScreen
        className="dmc-video-iframe"
      />
    );
  }

  return (
    <video
      key={url}
      controls
      className="dmc-video-native"
      controlsList="nodownload"
      preload="metadata"
    >
      <source src={url} />
      <p>Trình duyệt không hỗ trợ phát video trực tiếp.</p>
    </video>
  );
}

function DocumentViewer({ url, fileType, title }) {
  const mimeIsPdf = fileType && fileType.toLowerCase().includes('pdf');
  const extIsPdf = url && /\.pdf(\?|$)/i.test(url.split('?')[0]);
  const isPdf = mimeIsPdf || extIsPdf;

  if (isPdf) {
    return (
      <iframe
        src={url}
        title={title ?? 'Tài liệu'}
        className="dmc-doc-iframe"
      />
    );
  }

  const ext = url?.split('?')[0].split('.').pop()?.toUpperCase();
  const label = ext ?? 'Tệp';

  return (
    <div className="dmc-doc-download">
      <span className="material-symbols-outlined dmc-doc-icon">description</span>
      <p className="dmc-doc-filename">{title ?? `Tài liệu (${label})`}</p>
      <p className="dmc-doc-hint">
        Tệp {label} không thể hiển thị trực tiếp trong trình duyệt.
      </p>
      <a
        href={url}
        target="_blank"
        rel="noreferrer"
        className="dmc-doc-open-btn"
      >
        <span className="material-symbols-outlined">open_in_new</span>
        Mở tài liệu
      </a>
    </div>
  );
}

function CompletionPanel({ isCompleted, onComplete, completing, hasNext, onNext }) {
  if (isCompleted) {
    return (
      <div className="dmc-completion-panel dmc-completion-panel--done">
        <div className="dmc-completion-status">
          <span className="material-symbols-outlined dmc-done-check fill-icon">check_circle</span>
          <span className="dmc-completion-label">Đã hoàn thành bài học</span>
        </div>
        {hasNext && (
          <button className="dmc-next-btn" onClick={onNext}>
            Bài tiếp theo
            <span className="material-symbols-outlined">arrow_forward</span>
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="dmc-completion-panel">
      <button
        className="dmc-complete-btn"
        onClick={onComplete}
        disabled={completing}
      >
        {completing ? (
          <>
            <span className="dmc-btn-spinner" />
            Đang lưu...
          </>
        ) : (
          <>
            <span className="material-symbols-outlined">check_circle</span>
            Xác nhận hoàn thành
          </>
        )}
      </button>
    </div>
  );
}

function LessonViewer({ lesson, nodeTitle, isCompleted, onComplete, completing, hasNext, onNext }) {
  if (!lesson) return null;

  const content = lesson.content ?? {};
  const type = lesson.lessonType?.toUpperCase();
  const title = nodeTitle || lesson.title || 'Bài học';

  const completionPanel = (
    <CompletionPanel
      isCompleted={isCompleted}
      onComplete={onComplete}
      completing={completing}
      hasNext={hasNext}
      onNext={onNext}
    />
  );

  if (type === 'VIDEO') {
    const videoUrl =
      content.video_url ?? content.url ?? content.videoUrl ?? content.embedUrl ?? null;
    return (
      <div className="dmc-lesson-viewer">
        <div className="dmc-video-wrapper">
          {videoUrl ? (
            <VideoPlayer url={videoUrl} title={title} />
          ) : (
            <div className="dmc-media-placeholder">
              <span className="material-symbols-outlined">videocam_off</span>
              <p>Video chưa được cung cấp.</p>
            </div>
          )}
        </div>
        <div className="dmc-lesson-info">
          <h2 className="dmc-lesson-title">{title}</h2>
          {lesson.description && (
            <p className="dmc-lesson-desc">{lesson.description}</p>
          )}
        </div>
        {completionPanel}
      </div>
    );
  }

  if (type === 'DOCUMENT') {
    const docUrl = content.file_url ?? content.url ?? content.documentUrl ?? null;
    const fileType = content.file_type ?? null;
    const docContent = content.content ?? content.text ?? null;
    return (
      <div className="dmc-lesson-viewer">
        <div className="dmc-document-wrapper">
          {docUrl ? (
            <DocumentViewer url={docUrl} fileType={fileType} title={title} />
          ) : docContent ? (
            <div className="dmc-document-content">
              <pre>{docContent}</pre>
            </div>
          ) : (
            <div className="dmc-media-placeholder">
              <span className="material-symbols-outlined">description</span>
              <p>Tài liệu chưa được cung cấp.</p>
            </div>
          )}
        </div>
        <div className="dmc-lesson-info">
          <h2 className="dmc-lesson-title">{title}</h2>
          {lesson.description && (
            <p className="dmc-lesson-desc">{lesson.description}</p>
          )}
        </div>
        {completionPanel}
      </div>
    );
  }

  if (type === 'LIVE_CLASS') {
    const meetUrl = content.url ?? content.meetingUrl ?? null;
    return (
      <div className="dmc-lesson-viewer">
        <div className="dmc-special-lesson">
          <span className="material-symbols-outlined dmc-special-icon">live_tv</span>
          <h3>Buổi học trực tiếp</h3>
          {meetUrl ? (
            <a href={meetUrl} target="_blank" rel="noreferrer" className="dmc-meeting-btn">
              <span className="material-symbols-outlined">open_in_new</span>
              Tham gia buổi học
            </a>
          ) : (
            <p className="dmc-hint-text">Link buổi học chưa được cung cấp.</p>
          )}
        </div>
        {completionPanel}
      </div>
    );
  }

  return (
    <div className="dmc-lesson-viewer">
      <div className="dmc-special-lesson">
        <span className="material-symbols-outlined dmc-special-icon">school</span>
        <h3>{title}</h3>
        <p className="dmc-hint-text">Loại bài học: <strong>{lesson.lessonType ?? 'Không xác định'}</strong></p>
      </div>
      {completionPanel}
    </div>
  );
}

const LESSON_ICON = {
  VIDEO: 'play_circle',
  DOCUMENT: 'description',
  QUIZ: 'quiz',
  CODING: 'code',
  LIVE_CLASS: 'live_tv',
};

function SidebarNode({ node, currentLessonId, onSelect, level = 0 }) {
  const hasActiveLesson = useCallback((target) => {
    if (target.lessonId === currentLessonId) return true;
    if (target.items) {
      return target.items.some((child) => hasActiveLesson(child));
    }
    return false;
  }, [currentLessonId]);

  const [isOpen, setIsOpen] = useState(() => hasActiveLesson(node));

  useEffect(() => {
    if (hasActiveLesson(node)) {
      setIsOpen(true);
    }
  }, [currentLessonId, node, hasActiveLesson]);

  if (node.type === 'folder') {
    const hasChildren = node.items?.length > 0;
    return (
      <div className="dmc-sidebar-folder">
        <button
          className={`dmc-sidebar-folder-btn ${isOpen ? 'open' : ''}`}
          onClick={() => setIsOpen((prev) => !prev)}
          style={{ '--indent': level }}
        >
          <span className="material-symbols-outlined dmc-folder-chevron">
            {isOpen ? 'expand_more' : 'chevron_right'}
          </span>
          <span className="material-symbols-outlined dmc-folder-icon">
            {isOpen ? 'folder_open' : 'folder'}
          </span>
          <span className="dmc-folder-title">{node.title}</span>
          {hasChildren && (
            <span className="dmc-folder-count">{node.items.length}</span>
          )}
        </button>
        {isOpen && hasChildren && (
          <div className="dmc-folder-children">
            {node.items.map((child) => (
              <SidebarNode
                key={child.id}
                node={child}
                currentLessonId={currentLessonId}
                onSelect={onSelect}
                level={level + 1}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  const icon = LESSON_ICON[node.lessonType?.toUpperCase()] ?? 'play_circle';
  const isActive = node.lessonId === currentLessonId;

  return (
    <button
      className={`dmc-sidebar-lesson ${isActive ? 'active' : ''} ${node.completed ? 'completed' : ''}`}
      onClick={() => onSelect(node)}
      style={{ '--indent': level }}
    >
      <span className={`dmc-lesson-type-icon ${node.lessonType?.toLowerCase() ?? 'video'}`}>
        <span className="material-symbols-outlined">{icon}</span>
      </span>
      <span className="dmc-sidebar-lesson-title">{node.title}</span>
      {node.completed && (
        <span className="material-symbols-outlined dmc-done-icon fill-icon">check_circle</span>
      )}
    </button>
  );
}

function SidebarHeader({ completed, total }) {
  const pct = total > 0 ? Math.round((completed / total) * 100) : 0;
  return (
    <div className="dmc-sidebar-header">
      <div className="dmc-sidebar-header-top">
        <h3>Nội dung khóa học</h3>
        <span className="dmc-progress-badge">{completed}/{total}</span>
      </div>
      <div className="dmc-progress-bar-track">
        <div
          className="dmc-progress-bar-fill"
          style={{ width: `${pct}%` }}
        />
      </div>
      <p className="dmc-progress-pct">{pct}% hoàn thành</p>
    </div>
  );
}

export default function DetailMyCoursePage() {
  const location = useLocation();
  const toast = useToast();
  const { courseId, lessonId: initialLessonId } = location.state ?? {};

  const [lesson, setLesson] = useState(null);
  const [currentLessonId, setCurrentLessonId] = useState(initialLessonId ?? null);
  const [currentLessonTitle, setCurrentLessonTitle] = useState('');
  const [courseTitle, setCourseTitle] = useState('');
  const [tree, setTree] = useState([]);
  const [flatLessons, setFlatLessons] = useState([]);
  const [completedIds, setCompletedIds] = useState(new Set());
  const [structureNodes, setStructureNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lessonLoading, setLessonLoading] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [blockingError, setBlockingError] = useState(null);

  useEffect(() => {
    if (!courseId) {
      setBlockingError(buildAppErrorState(null, {
        title: 'Không tìm thấy thông tin khóa học',
        message: 'Liên kết khóa học không hợp lệ hoặc phần nội dung này không còn tồn tại.',
        variant: 'not-found',
        icon: 'travel_explore',
        fallbackPath: '/my-courses',
      }));
      setLoading(false);
      return;
    }

    const init = async () => {
      setLoading(true);
      try {
        const [courseRes, structure, progress] = await Promise.all([
          courseApi.getById(courseId),
          courseApi.getStructure(courseId).catch(() => null),
          courseApi.getProgress(courseId).catch(() => null),
        ]);

        const courseData = courseRes?.data?.data ?? courseRes?.data;
        setCourseTitle(courseData?.title ?? '');

        const done = new Set(progress?.completedLessonIds ?? []);
        setCompletedIds(done);

        if (structure?.nodes?.length) {
          const nodes = structure.nodes;
          setStructureNodes(nodes);
          const built = buildTree(nodes, done);
          const flat = flattenLessons(built);
          setTree(built);
          setFlatLessons(flat);

          if (initialLessonId) {
            const initNode = flat.find((item) => item.lessonId === initialLessonId);
            if (initNode) setCurrentLessonTitle(initNode.title ?? '');
          }
        } else {
          setStructureNodes([]);
          setTree([]);
          setFlatLessons([]);
        }

        setBlockingError(null);
      } catch (err) {
        console.error('[DetailMyCoursePage] init failed:', err);
        setBlockingError(buildAppErrorState(err, {
          title: 'Không thể tải nội dung khóa học',
          fallbackPath: '/my-courses',
        }));
      } finally {
        setLoading(false);
      }
    };

    init();
  }, [courseId, initialLessonId]);

  const loadLesson = useCallback(async (lessonId) => {
    if (!courseId || !lessonId) return;
    setLessonLoading(true);
    setLesson(null);
    try {
      const data = await courseApi.getLessonById(courseId, lessonId);
      setLesson(data);
    } catch (err) {
      console.error('[DetailMyCoursePage] load lesson failed:', err);
      toast.error('Không thể tải bài học này. Bạn có thể chọn bài khác để tiếp tục.');
    } finally {
      setLessonLoading(false);
    }
  }, [courseId, toast]);

  useEffect(() => {
    if (currentLessonId) loadLesson(currentLessonId);
  }, [currentLessonId, loadLesson]);

  const handleSelectLesson = useCallback((node) => {
    setCurrentLessonId(node.lessonId);
    setCurrentLessonTitle(node.title ?? '');
  }, []);

  const handleComplete = useCallback(async () => {
    if (!courseId || !currentLessonId || completing) return;
    setCompleting(true);
    try {
      await courseApi.completeLesson(courseId, currentLessonId, lesson?.lessonType);
      const updated = new Set([...completedIds, currentLessonId]);
      setCompletedIds(updated);

      if (structureNodes.length) {
        const rebuilt = buildTree(structureNodes, updated);
        setTree(rebuilt);
        setFlatLessons(flattenLessons(rebuilt));
      }

      toast.success('Bài học đã được đánh dấu hoàn thành!');
    } catch (err) {
      console.error('[DetailMyCoursePage] complete lesson failed:', err);
      toast.error('Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setCompleting(false);
    }
  }, [courseId, currentLessonId, completing, lesson, completedIds, structureNodes, toast]);

  const currentIdx = flatLessons.findIndex((item) => item.lessonId === currentLessonId);
  const prevLesson = currentIdx > 0 ? flatLessons[currentIdx - 1] : null;
  const nextLesson = currentIdx >= 0 && currentIdx < flatLessons.length - 1 ? flatLessons[currentIdx + 1] : null;

  const goNext = useCallback(() => {
    if (nextLesson) setCurrentLessonId(nextLesson.lessonId);
  }, [nextLesson]);

  const isCurrentCompleted = completedIds.has(currentLessonId);

  if (blockingError) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={blockingError} />;
  }

  if (loading) {
    return (
      <div className="detail-my-course-page">
        <div className="dmc-skeleton-header" />
        <div className="dmc-skeleton-body">
          <div className="dmc-skeleton-main" />
          <div className="dmc-skeleton-sidebar" />
        </div>
      </div>
    );
  }

  return (
    <div className="detail-my-course-page">
      <header className="detail-my-course-page__header">
        <nav className="ch-breadcrumb">
          <Link to="/my-courses">Khóa học của tôi</Link>
          <span className="ch-breadcrumb-sep">
            <span className="material-symbols-outlined">chevron_right</span>
          </span>
          <Link to={`/course-hub?id=${courseId}`} className="ch-breadcrumb-current">{courseTitle || 'Khóa học'}</Link>
          {currentLessonTitle && (
            <>
              <span className="ch-breadcrumb-sep">
                <span className="material-symbols-outlined">chevron_right</span>
              </span>
              <span className="ch-breadcrumb-current">{currentLessonTitle}</span>
            </>
          )}
        </nav>
      </header>

      <main className="detail-my-course-page__main">
        <div className="detail-my-course-page__layout">
          <div className="detail-my-course-page__content">
            <div className="dmc-viewer-card">
              {lessonLoading ? (
                <div className="dmc-lesson-loading">
                  <div className="admin-loading-spinner" />
                  <p>Đang tải bài học...</p>
                </div>
              ) : !currentLessonId ? (
                <div className="dmc-no-lesson">
                  <span className="material-symbols-outlined">menu_book</span>
                  <p>Chọn một bài học từ danh sách bên phải để bắt đầu.</p>
                </div>
              ) : (
                <LessonViewer
                  lesson={lesson}
                  nodeTitle={currentLessonTitle}
                  isCompleted={isCurrentCompleted}
                  onComplete={handleComplete}
                  completing={completing}
                  hasNext={!!nextLesson}
                  onNext={goNext}
                />
              )}
            </div>

            {flatLessons.length > 0 && (
              <div className="dmc-nav-bar">
                <button
                  className="dmc-nav-btn"
                  disabled={!prevLesson}
                  onClick={() => prevLesson && setCurrentLessonId(prevLesson.lessonId)}
                >
                  <span className="material-symbols-outlined">arrow_back</span>
                  Bài trước
                </button>
                <span className="dmc-nav-count">
                  {currentIdx >= 0 ? currentIdx + 1 : '–'} / {flatLessons.length}
                </span>
                <button
                  className="dmc-nav-btn dmc-nav-btn--next"
                  disabled={!nextLesson}
                  onClick={() => nextLesson && setCurrentLessonId(nextLesson.lessonId)}
                >
                  Bài tiếp theo
                  <span className="material-symbols-outlined">arrow_forward</span>
                </button>
              </div>
            )}
          </div>

          <aside className="detail-my-course-page__sidebar">
            <SidebarHeader
              completed={completedIds.size}
              total={flatLessons.length}
            />
            <div className="dmc-sidebar-list custom-scrollbar">
              {tree.length > 0 ? (
                tree.map((node) => (
                  <SidebarNode
                    key={node.id}
                    node={node}
                    currentLessonId={currentLessonId}
                    onSelect={handleSelectLesson}
                  />
                ))
              ) : (
                <p className="dmc-sidebar-empty">Không có nội dung.</p>
              )}
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
