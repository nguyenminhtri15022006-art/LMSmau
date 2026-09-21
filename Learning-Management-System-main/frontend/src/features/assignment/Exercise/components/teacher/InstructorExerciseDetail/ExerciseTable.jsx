import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCopy, faLockOpen } from '@fortawesome/free-solid-svg-icons';
import { useNavigate, useParams } from 'react-router-dom';
import assignmentApi from '../../../../../../services/assignment.api';
import '../../../styles/teacher/InstructorExerciseDetail/exerciseTable.css';

function getPageNumbers(currentPage, totalPages) {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = [1];
  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);

  if (start > 2) pages.push('start-ellipsis');
  for (let page = start; page <= end; page += 1) pages.push(page);
  if (end < totalPages - 1) pages.push('end-ellipsis');
  pages.push(totalPages);

  return pages;
}

export default function ExerciseTable({
  currentList = [],
  activeTab,
  courseName = '',
  page = 1,
  pageSize = 5,
  onPageChange,
  onItemDeleted,
}) {
  const navigate = useNavigate();
  const { courseId } = useParams();
  const [collapsedGroups, setCollapsedGroups] = useState({});
  const [pendingDeleteItem, setPendingDeleteItem] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [pendingEditAction, setPendingEditAction] = useState(null);
  const [isResolvingEdit, setIsResolvingEdit] = useState(false);
  const [editActionError, setEditActionError] = useState('');

  const navigateToEdit = (itemOrId) => {
    const navigationState = courseName ? { courseTitle: courseName } : undefined;

    if (activeTab === 'quizzes') {
      const quizId = typeof itemOrId === 'object' ? itemOrId.id : itemOrId;
      navigate(`/instructor/exercises/${courseId}/quiz/edit/${quizId}`, { state: navigationState });
    } else {
      const item = typeof itemOrId === 'object' ? itemOrId : { id: itemOrId };
      navigate(`/instructor/exercises/${courseId}/code-judge/edit/${item.slug || item.id || item.problemId}`, {
        state: navigationState,
      });
    }
  };

  const handleEdit = (item) => {
    const doneCount = Number(item.doneCount) || 0;
    if (doneCount > 0) {
      setPendingEditAction({ type: 'clone', item, activeTab });
      setEditActionError('');
      return;
    }

    if (item.status === 'Published') {
      setPendingEditAction({ type: 'unpublish', item, activeTab });
      setEditActionError('');
      return;
    }

    navigateToEdit(item);
  };

  const closeEditAction = () => {
    if (isResolvingEdit) return;
    setPendingEditAction(null);
    setEditActionError('');
  };

  const handleConfirmEditAction = () => {
    if (!pendingEditAction) return;

    const { type, item, activeTab: actionTab } = pendingEditAction;
    const isQuiz = actionTab === 'quizzes';
    const request = type === 'clone'
      ? (isQuiz ? assignmentApi.cloneQuiz(item.id) : assignmentApi.cloneProblem(item.id))
      : (isQuiz ? assignmentApi.unpublishQuiz(item.id) : assignmentApi.unpublishProblem(item.id));

    setIsResolvingEdit(true);
    setEditActionError('');
    request
      .then(async (response) => {
        const targetId = type === 'clone'
          ? (isQuiz ? response?.newQuizId : response?.newProblemId)
          : item.id;
        let targetItem = type === 'clone' ? targetId : item;

        if (type === 'clone' && !isQuiz && targetId) {
          try {
            targetItem = await assignmentApi.getProblemDetail(targetId);
          } catch (err) {
            console.warn('Failed to resolve cloned problem slug:', err);
          }
        }

        setPendingEditAction(null);
        navigateToEdit(targetItem);
      })
      .catch(err => {
        const label = isQuiz ? 'quiz' : 'coding challenge';
        setEditActionError(err.response?.data?.message || `Không thể chuẩn bị ${label} để chỉnh sửa. Vui lòng thử lại.`);
        console.error(`Failed to prepare ${label} for editing:`, err);
      })
      .finally(() => {
        setIsResolvingEdit(false);
      });
  };

  const openDeleteConfirm = (item) => {
    setPendingDeleteItem(item);
    setDeleteError('');
  };

  const closeDeleteConfirm = () => {
    if (isDeleting) return;
    setPendingDeleteItem(null);
    setDeleteError('');
  };

  const handleDelete = () => {
    if (!pendingDeleteItem) return;

    const label = activeTab === 'quizzes' ? 'quiz' : 'coding challenge';
    const request = activeTab === 'quizzes'
      ? assignmentApi.deleteQuiz(pendingDeleteItem.id)
      : assignmentApi.deleteProblem(pendingDeleteItem.id);

    setIsDeleting(true);
    setDeleteError('');
    request
      .then(() => {
        setPendingDeleteItem(null);
        onItemDeleted?.();
      })
      .catch(err => {
        setDeleteError(err.response?.data?.message || `Không thể xóa ${label}. Vui lòng thử lại.`);
        console.error(`Failed to delete ${label}:`, err);
      })
      .finally(() => {
        setIsDeleting(false);
      });
  };

  const allChapterEntries = useMemo(() => currentList.reduce((acc, item) => {
    const key = item.chapterKey || item.chapter || 'other';
    const existing = acc.find(group => group.key === key);

    if (existing) {
      existing.items.push(item);
      return acc;
    }

    acc.push({
      key,
      title: item.chapter || 'Khác',
      items: [item],
    });
    return acc;
  }, []), [currentList]);

  const totalItems = currentList.length;
  const totalLessons = allChapterEntries.length;
  const lessonsPerPage = Math.max(1, Number(pageSize) || 5);
  const totalPages = Math.max(1, Math.ceil(totalLessons / lessonsPerPage));
  const safePage = Math.min(Math.max(page, 1), totalPages);
  const startIndex = (safePage - 1) * lessonsPerPage;
  const chapterEntries = useMemo(
    () => allChapterEntries.slice(startIndex, startIndex + lessonsPerPage),
    [allChapterEntries, startIndex, lessonsPerPage]
  );
  const endLessonIndex = Math.min(startIndex + chapterEntries.length, totalLessons);
  const visibleItemCount = chapterEntries.reduce((total, group) => total + group.items.length, 0);
  const pageNumbers = useMemo(
    () => getPageNumbers(safePage, totalPages),
    [safePage, totalPages]
  );

  useEffect(() => {
    if (page !== safePage) onPageChange?.(safePage);
  }, [page, safePage, onPageChange]);

  const handleToggleGroup = (groupKey) => {
    const scopedKey = `${activeTab}:${groupKey}`;
    setCollapsedGroups(prev => ({
      ...prev,
      [scopedKey]: prev[scopedKey] === undefined ? false : !prev[scopedKey],
    }));
  };

  const confirmDialog = pendingDeleteItem && (
    <div className="exercise-confirm-overlay" onClick={closeDeleteConfirm}>
      <div
        className="exercise-confirm-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-exercise-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="exercise-confirm-icon">
          <span className="material-symbols-outlined">delete</span>
        </div>
        <div className="exercise-confirm-body">
          <h2 id="delete-exercise-title">Xác nhận xóa bài tập</h2>
          {Number(pendingDeleteItem.doneCount) > 0 ? (
            <p>
              Bài tập <strong>{pendingDeleteItem.title}</strong> đã có <strong>{pendingDeleteItem.doneCount}</strong> lượt làm.
              Sau khi xóa, bài tập sẽ không còn hiển thị trong danh sách. Lịch sử làm bài và điểm sẽ được hệ thống bảo toàn.
              Nếu cần khôi phục, vui lòng liên hệ admin.
            </p>
          ) : (
            <p>
              Bạn sắp xóa <strong>{pendingDeleteItem.title}</strong>. Hành động này không thể hoàn tác.
            </p>
          )}
          {deleteError && <div className="exercise-confirm-error">{deleteError}</div>}
        </div>
        <div className="exercise-confirm-actions">
          <button
            type="button"
            className="exercise-confirm-cancel"
            onClick={closeDeleteConfirm}
            disabled={isDeleting}
          >
            Hủy
          </button>
          <button
            type="button"
            className="exercise-confirm-delete"
            onClick={handleDelete}
            disabled={isDeleting}
          >
            {isDeleting ? 'Đang xóa...' : 'Xóa luôn'}
          </button>
        </div>
      </div>
    </div>
  );

  const editActionLabel = pendingEditAction?.activeTab === 'quizzes' ? 'Quiz' : 'Coding challenge';
  const editActionTitle = pendingEditAction?.type === 'clone'
    ? `${editActionLabel} đã có người làm`
    : `${editActionLabel} đang được xuất bản`;

  const editActionDialog = pendingEditAction && (
    <div className="exercise-confirm-overlay" onClick={closeEditAction}>
      <div
        className="exercise-confirm-dialog exercise-confirm-dialog--edit-action"
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-quiz-action-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className={`exercise-confirm-icon exercise-confirm-icon--edit ${pendingEditAction.type === 'clone' ? 'exercise-confirm-icon--clone' : 'exercise-confirm-icon--unpublish'}`}>
          <FontAwesomeIcon icon={pendingEditAction.type === 'clone' ? faCopy : faLockOpen} />
        </div>
        <div className="exercise-confirm-body">
          <h2 id="edit-quiz-action-title">
            {editActionTitle}
          </h2>
          <p>
            {pendingEditAction.type === 'clone'
              ? <>{editActionLabel} <strong>{pendingEditAction.item.title}</strong> đã có lượt làm. Hãy tạo bản sao để chỉnh sửa mà không ảnh hưởng kết quả cũ.</>
              : <>{editActionLabel} <strong>{pendingEditAction.item.title}</strong> chưa có lượt làm. Hãy ngưng xuất bản trước khi chỉnh sửa.</>}
          </p>
          {editActionError && <div className="exercise-confirm-error">{editActionError}</div>}
        </div>
        <div className="exercise-confirm-actions">
          <button
            type="button"
            className="exercise-confirm-cancel"
            onClick={closeEditAction}
            disabled={isResolvingEdit}
          >
            Hủy
          </button>
          <button
            type="button"
            className="exercise-confirm-primary"
            onClick={handleConfirmEditAction}
            disabled={isResolvingEdit}
          >
            {isResolvingEdit
              ? 'Đang xử lý...'
              : pendingEditAction.type === 'clone'
                ? 'Tạo bản sao để chỉnh sửa'
                : 'Unpublish và chỉnh sửa'}
          </button>
        </div>
      </div>
    </div>
  );

  if (chapterEntries.length === 0) {
    return (
      <div className="exercise-table-container">
        <div style={{ textAlign: 'center', padding: '48px 16px', color: 'var(--text-500)' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', margin: '0 auto 12px' }}>inbox</span>
          <p>Chưa có bài tập nào. Nhấn "Tạo bài tập" để thêm mới.</p>
        </div>
        {confirmDialog}
        {editActionDialog}
      </div>
    );
  }

  return (
    <div className="exercise-table-container">
      <table className="exercise-table">
        <thead>
          <tr>
            <th>TIÊU ĐỀ BÀI TẬP</th>
            <th>ĐỘ KHÓ</th>
            <th>ĐÃ LÀM</th>
            <th>TRẠNG THÁI</th>
            <th style={{ textAlign: 'right' }}>HÀNH ĐỘNG</th>
          </tr>
        </thead>
        <tbody>
          {chapterEntries.map(group => {
            const scopedKey = `${activeTab}:${group.key}`;
            const isCollapsed = collapsedGroups[scopedKey] !== false;

            return (
              <React.Fragment key={group.key}>
                <tr className="chapter-header-row">
                  <td colSpan="5">
                    <div className="chapter-header-content">
                      <span className="material-symbols-outlined chapter-folder-icon">
                        {isCollapsed ? 'folder' : 'folder_open'}
                      </span>
                      <span className="chapter-title">{group.title}</span>
                      <span className="count-badge">{group.items.length} bài</span>
                      <button
                        type="button"
                        className="chapter-toggle-btn"
                        onClick={() => handleToggleGroup(group.key)}
                        aria-label={isCollapsed ? `Hiện ${group.title}` : `Ẩn ${group.title}`}
                      >
                        <span className="material-symbols-outlined">
                          {isCollapsed ? 'expand_more' : 'expand_less'}
                        </span>
                      </button>
                    </div>
                  </td>
                </tr>
                {!isCollapsed && group.items.map(item => (
                  <tr key={item.id} className="exercise-row">
                    <td>
                      <div className="ex-title-cell">
                        <div className="ex-icon-wrapper">
                          <span className="material-symbols-outlined">
                            {activeTab === 'quizzes' ? 'quiz' : 'code_blocks'}
                          </span>
                        </div>
                        <div className="ex-title-info">
                          <h4 className="ex-name">{item.title}</h4>
                          <span className="ex-meta">{item.meta}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`ex-difficulty ${item.difficultyTone ? `difficulty-${item.difficultyTone}` : ''}`}>
                        {item.difficulty ?? '—'}
                      </span>
                    </td>
                    <td className="done-count-cell">
                      {Number.isFinite(Number(item.doneCount)) ? Number(item.doneCount) : 0}
                    </td>
                    <td>
                      <div className={`status-pill ${item.status?.toLowerCase()}`}>
                        <span className="status-dot"></span>
                        {item.status}
                      </div>
                    </td>
                    <td>
                      <div className="action-buttons" style={{ justifyContent: 'flex-end' }}>
                        <button className="icon-btn edit-btn" title="Chỉnh sửa" onClick={() => handleEdit(item)}>
                          <span className="material-symbols-outlined">edit</span>
                        </button>
                        <button className="icon-btn delete-btn" title="Xóa" onClick={() => openDeleteConfirm(item)}>
                          <span className="material-symbols-outlined">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </React.Fragment>
            );
          })}
        </tbody>
      </table>
      <div className="table-footer">
        <span className="showing-text">
          Hiển thị <strong>{startIndex + 1}–{endLessonIndex}</strong> của <strong>{totalLessons}</strong> bài học
          {' '}<span>({visibleItemCount} / {totalItems} bài tập)</span>
        </span>
        <div className="exercise-table-pagination" aria-label="Phân trang bài tập">
          <button
            type="button"
            className="exercise-page-btn"
            disabled={safePage === 1}
            onClick={() => onPageChange?.(safePage - 1)}
          >
            <span className="material-symbols-outlined">chevron_left</span>
          </button>
          {pageNumbers.map(pageNumber => (
            typeof pageNumber === 'number' ? (
              <button
                key={pageNumber}
                type="button"
                className={`exercise-page-btn ${pageNumber === safePage ? 'active' : ''}`}
                onClick={() => onPageChange?.(pageNumber)}
              >
                {pageNumber}
              </button>
            ) : (
              <span key={pageNumber} className="exercise-page-ellipsis">...</span>
            )
          ))}
          <button
            type="button"
            className="exercise-page-btn"
            disabled={safePage === totalPages}
            onClick={() => onPageChange?.(safePage + 1)}
          >
            <span className="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </div>
      {confirmDialog}
      {editActionDialog}
    </div>
  );
}
