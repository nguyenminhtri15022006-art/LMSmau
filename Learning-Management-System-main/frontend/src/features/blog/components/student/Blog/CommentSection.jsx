import React, { useState } from 'react';

const countComments = (comments = []) =>
  comments.reduce((total, comment) => total + 1 + countComments(comment.replies || []), 0);

const CommentItem = ({
  comment,
  isReply = false,
  activeReplyId,
  replyContent,
  submitting,
  isAuthenticated,
  onAuthRequired,
  onReplyClick,
  onReplyChange,
  onReplySubmit,
  onCancelReply,
  onVoteComment,
  commentVoteSubmitting,
  readOnly = false,
}) => {
  const isVoting = (type) =>
    commentVoteSubmitting?.commentId === comment.id && commentVoteSubmitting?.type === type;

  const handleVote = (type) => {
    if (!isAuthenticated) {
      onAuthRequired?.();
      return;
    }
    onVoteComment?.(comment.id, type);
  };

  return (
    <div className={`comment-item ${isReply ? 'reply' : ''}`}>
      <img
        src={comment.authorAvatar}
        alt={comment.author}
        className="comment-avatar"
        referrerPolicy="no-referrer"
      />
      <div className="comment-body">
        <div className="comment-header">
          <span className="comment-name">{comment.author}</span>
          {comment.role && <span className="comment-tag">{comment.role}</span>}
          <span className="comment-time">- {comment.time}</span>
        </div>
        <p className="comment-text">{comment.content}</p>
        {!readOnly && (
          <div className="comment-actions">
            <button
              className={`comment-vote-btn ${comment.userVote === 'upvote' ? 'active' : ''}`}
              type="button"
              aria-label="Thích bình luận"
              disabled={isVoting('upvote') || isVoting('downvote')}
              onClick={() => handleVote('upvote')}
            >
              <span className="material-symbols-outlined">thumb_up</span>
              <span className="comment-vote-count">{comment.upvoteCount || 0}</span>
            </button>
            <button
              className={`comment-vote-btn ${comment.userVote === 'downvote' ? 'active' : ''}`}
              type="button"
              aria-label="Không thích bình luận"
              disabled={isVoting('upvote') || isVoting('downvote')}
              onClick={() => handleVote('downvote')}
            >
              <span className="material-symbols-outlined">thumb_down</span>
              <span className="comment-vote-count">{comment.downvoteCount || 0}</span>
            </button>
            {!isReply && (
              <button
                className="comment-action-btn"
                type="button"
                onClick={() => {
                  if (!isAuthenticated) {
                    onAuthRequired?.();
                    return;
                  }
                  onReplyClick(comment.id);
                }}
              >
                Trả lời
              </button>
            )}
            <button className="comment-action-btn" type="button">Báo cáo</button>
          </div>
        )}

        {!readOnly && activeReplyId === comment.id && (
          <form className="comment-reply-form" onSubmit={event => onReplySubmit(event, comment.id)}>
            <textarea
              className="comment-textarea comment-reply-textarea"
              value={replyContent}
              onChange={event => onReplyChange(event.target.value)}
              placeholder={`Trả lời ${comment.author}...`}
              disabled={submitting}
            />
            <div className="comment-form-actions">
              <button className="comment-cancel-btn" type="button" onClick={onCancelReply} disabled={submitting}>
                Hủy
              </button>
              <button className="post-comment-btn" type="submit" disabled={submitting || !replyContent.trim()}>
                {submitting ? 'Đang gửi...' : 'Gửi trả lời'}
              </button>
            </div>
          </form>
        )}

        {comment.replies && comment.replies.length > 0 && (
          <div className="comment-replies">
            {comment.replies.map(reply => (
              <CommentItem
                key={reply.id}
                comment={reply}
                isReply
                activeReplyId={activeReplyId}
                replyContent={replyContent}
                submitting={submitting}
                isAuthenticated={isAuthenticated}
                onAuthRequired={onAuthRequired}
                onReplyClick={onReplyClick}
                onReplyChange={onReplyChange}
                onReplySubmit={onReplySubmit}
                onCancelReply={onCancelReply}
                onVoteComment={onVoteComment}
                commentVoteSubmitting={commentVoteSubmitting}
                readOnly={readOnly}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default function CommentSection({
  comments = [],
  currentUser = null,
  isAuthenticated = false,
  onAuthRequired,
  onSubmit,
  submitting = false,
  error = '',
  readOnly = false,
  onVoteComment,
  commentVoteSubmitting,
}) {
  const [content, setContent] = useState('');
  const [activeReplyId, setActiveReplyId] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const inputAvatar = currentUser?.avatarUrl || currentUser?.avatar;
  const inputName = currentUser?.fullname || currentUser?.username || 'User';

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!isAuthenticated) {
      onAuthRequired?.();
      return;
    }

    const value = content.trim();
    if (!value) return;

    try {
      await onSubmit({ content: value });
      setContent('');
    } catch (_err) {
      // Error message is owned by the parent so the form keeps the user's draft.
    }
  };

  const handleReplySubmit = async (event, parentId) => {
    event.preventDefault();
    if (!isAuthenticated) {
      onAuthRequired?.();
      return;
    }

    const value = replyContent.trim();
    if (!value) return;

    try {
      await onSubmit({ content: value, parentId });
      setReplyContent('');
      setActiveReplyId(null);
    } catch (_err) {
      // Error message is owned by the parent so the form keeps the user's draft.
    }
  };

  const handleReplyClick = (commentId) => {
    setActiveReplyId(commentId);
    setReplyContent('');
  };

  return (
    <div className="blog-detail-footer">
      <h3 className="comments-header">
        Bình luận ({countComments(comments)})
      </h3>

      {!readOnly && (
        <form className="comment-input-area" onSubmit={handleSubmit}>
          {inputAvatar ? (
            <img
              src={inputAvatar}
              alt={inputName}
              className="comment-input-avatar"
              referrerPolicy="no-referrer"
            />
          ) : (
            <span className="comment-input-avatar comment-input-placeholder material-symbols-outlined">person</span>
          )}
          <div className="comment-input-box">
            <textarea
              placeholder="Viết bình luận..."
              className="comment-textarea"
              value={content}
              onChange={event => setContent(event.target.value)}
              onFocus={() => {
                if (!isAuthenticated) onAuthRequired?.();
              }}
              disabled={submitting}
            />
            <div className="comment-form-actions">
              {error && <span className="comment-error">{error}</span>}
              <button className="post-comment-btn" type="submit" disabled={submitting || !content.trim()}>
                {submitting ? 'Đang gửi...' : 'Đăng bình luận'}
              </button>
            </div>
          </div>
        </form>
      )}

      <div className="comments-list">
        {comments.length > 0 ? (
          comments.map(comment => (
            <CommentItem
              key={comment.id}
              comment={comment}
              activeReplyId={activeReplyId}
              replyContent={replyContent}
              submitting={submitting}
              isAuthenticated={isAuthenticated}
              onAuthRequired={onAuthRequired}
              onReplyClick={handleReplyClick}
              onReplyChange={setReplyContent}
              onReplySubmit={handleReplySubmit}
              onCancelReply={() => setActiveReplyId(null)}
              onVoteComment={onVoteComment}
              commentVoteSubmitting={commentVoteSubmitting}
              readOnly={readOnly}
            />
          ))
        ) : (
          <div className="blog-empty-state">Chưa có bình luận nào. Hãy là người đầu tiên phản hồi bài viết này.</div>
        )}
      </div>
    </div>
  );
}
