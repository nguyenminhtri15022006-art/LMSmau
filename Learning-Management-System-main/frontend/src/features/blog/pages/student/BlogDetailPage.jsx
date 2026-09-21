import React, { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import blogApi from '../../../../services/blog.api';
import { mapBlogComment, mapBlogPost, unwrapApiData, unwrapPageContent } from '../../../../utils/blogMappers';
import { attachBlogAuthorProfiles, attachBlogCommentProfiles } from '../../../../utils/blogAuthorProfiles';
import CommentSection from '../../components/student/Blog/CommentSection';
import RelatedPosts from '../../components/student/Blog/RelatedPosts';
import AnimatedPage from '../../../../components/ui/AnimatedPage';
import useAuth from '../../../../hooks/useAuth';

import '../../styles/student/BlogCommon.css';
import '../../styles/student/BlogDetail.css';

export default function BlogDetailPage({ adminPreview = false }) {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const basePath = adminPreview ? '/admin/blog' : location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';
  // Cache the loadPost() Promise (not the resolved value) so StrictMode's second
  // effect run awaits the same in-flight request instead of firing a new one.
  const postPromiseCacheRef = useRef(null);
  const [post, setPost] = useState(null);
  const [relatedPosts, setRelatedPosts] = useState([]);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [commentError, setCommentError] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [voteSubmitting, setVoteSubmitting] = useState(null);
  const [voteError, setVoteError] = useState('');
  const [commentVoteSubmitting, setCommentVoteSubmitting] = useState(null);

  const loadPost = async () => {
    if (adminPreview) {
      return blogApi.getPost(id);
    }

    // Try public endpoint first (slug or public post). If the returned payload
    // doesn't include `content`, fall back to the authenticated detail endpoint.
    const tryPublic = async () => {
      try {
        return await blogApi.getPublicPost(id);
      } catch (err) {
        return null;
      }
    };

    const publicResp = await tryPublic();
    const publicData = publicResp ? unwrapApiData(publicResp) : null;

    if (publicData && (publicData.content || publicData.html || publicData.body)) {
      return publicResp;
    }

    // If public data missing content, try authenticated detail by id
    try {
      return await blogApi.getPost(id);
    } catch (err) {
      // As a last resort, if id looks numeric try public endpoint error handling
      if (/^\d+$/.test(id) && publicResp) return publicResp;
      throw err;
    }
  };

  const loadComments = async (postId) => {
    const commentsResponse = await blogApi.getComments(postId, { page: 0, size: 20, sort: 'createdAt,desc' });
    const commentsWithProfiles = await attachBlogCommentProfiles(unwrapPageContent(commentsResponse), user);
    setComments(commentsWithProfiles.map(mapBlogComment));
  };

  useEffect(() => {
    let ignore = false;

    const fetchPost = async () => {
      setLoading(true);
      setError('');
      setCommentError('');

      try {
        // Store the Promise itself (synchronously, before await) so the second
        // StrictMode effect run reuses the same in-flight request rather than
        // creating a new one. This prevents double view-count increments.
        const cacheKey = `${adminPreview}-${id}`;
        if (!postPromiseCacheRef.current || postPromiseCacheRef.current.key !== cacheKey) {
          postPromiseCacheRef.current = { key: cacheKey, promise: loadPost() };
        }
        const response = await postPromiseCacheRef.current.promise;
        const [postWithAuthor] = await attachBlogAuthorProfiles([unwrapApiData(response)], user);
        const mappedPost = mapBlogPost(postWithAuthor);

        if (ignore) return;

        setPost(mappedPost);

        try {
          await loadComments(mappedPost.id);
        } catch (_err) {
          if (!ignore) {
            setComments([]);
            setCommentError('Không thể tải bình luận. Vui lòng thử lại sau.');
          }
        }

        if (adminPreview) {
          setRelatedPosts([]);
        } else {
          try {
            const relatedResponse = await blogApi.getPublicPosts({ page: 0, size: 4, sort: 'createdAt,desc' });
            if (!ignore) {
              const relatedPostsWithAuthors = await attachBlogAuthorProfiles(unwrapPageContent(relatedResponse), user);
              setRelatedPosts(
                relatedPostsWithAuthors
                  .map(mapBlogPost)
                  .filter(item => item.id !== mappedPost.id),
              );
            }
          } catch (_err) {
            if (!ignore) {
              setRelatedPosts([]);
            }
          }
        }
      } catch (_err) {
        if (!ignore) {
          setPost(null);
          setComments([]);
          setRelatedPosts([]);
          setError('Không thể tải bài viết. Bài viết có thể đã bị xóa hoặc bạn không có quyền xem.');
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    fetchPost();

    return () => {
      ignore = true;
    };
  }, [adminPreview, id, user]);

  const handleSubmitComment = async ({ content, parentId = null }) => {
    if (!post?.id) return;

    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      throw new Error('Login required');
    }

    setCommentSubmitting(true);
    setCommentError('');

    try {
      await blogApi.addComment(post.id, { content, parentId });
      await loadComments(post.id);
    } catch (err) {
      const message = err.response?.status === 401
        ? 'Bạn cần đăng nhập để bình luận.'
        : 'Không thể gửi bình luận. Vui lòng thử lại sau.';
      setCommentError(message);
      throw err;
    } finally {
      setCommentSubmitting(false);
    }
  };

  const updateCommentVoteInTree = (commentList, commentId, voteData) =>
    commentList.map(c => {
      if (c.id === commentId) {
        return {
          ...c,
          upvoteCount: voteData?.upvoteCount ?? c.upvoteCount,
          downvoteCount: voteData?.downvoteCount ?? c.downvoteCount,
          userVote: voteData?.voteType ? voteData.voteType.toLowerCase() : null,
        };
      }
      if (c.replies?.length) {
        return { ...c, replies: updateCommentVoteInTree(c.replies, commentId, voteData) };
      }
      return c;
    });

  const handleCommentVote = async (commentId, type) => {
    if (!post?.id) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }
    try {
      setCommentVoteSubmitting({ commentId, type });
      const voteType = type === 'upvote' ? 'UPVOTE' : 'DOWNVOTE';
      const response = await blogApi.voteComment(post.id, commentId, voteType);
      const voteData = unwrapApiData(response);
      setComments(prev => updateCommentVoteInTree(prev, commentId, voteData));
    } catch (_err) {
      // silently ignore — user can retry by clicking again
    } finally {
      setCommentVoteSubmitting(null);
    }
  };

  const handleVote = async (type) => {
    if (!post?.id) return;

    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    try {
      setVoteSubmitting(type);
      setVoteError('');
      const voteType = type === 'upvote' ? 'UPVOTE' : 'DOWNVOTE';
      const response = await blogApi.vote(post.id, voteType);
      const voteData = unwrapApiData(response);

      setPost(currentPost => currentPost
        ? mapBlogPost({
          ...currentPost,
          upvoteCount: voteData?.upvoteCount ?? currentPost.upvoteCount,
          downvoteCount: voteData?.downvoteCount ?? currentPost.downvoteCount,
          userVote: voteData?.voteType ?? null,
        })
        : currentPost);
    } catch (err) {
      setVoteError(err.response?.data?.message || 'Không thể ghi nhận đánh giá. Vui lòng thử lại sau.');
    } finally {
      setVoteSubmitting(null);
    }
  };

  if (loading) {
    return (
      <AnimatedPage>
        <div className="blog-page">
          <div className="blog-empty-state">Đang tải bài viết...</div>
        </div>
      </AnimatedPage>
    );
  }

  if (error || !post) {
    return (
      <AnimatedPage>
        <div className="blog-page">
          {!adminPreview && (
          <nav className="ch-breadcrumb">
            <Link to={basePath}>Bài viết</Link>
          </nav>
          )}
          <div className="blog-empty-state">{error}</div>
        </div>
      </AnimatedPage>
    );
  }

  return (
    <AnimatedPage>
      <div className="blog-detail-page">
        <div className="blog-detail-shell">
          {!adminPreview && (
          <nav className="ch-breadcrumb blog-detail-breadcrumb">
            <Link to={basePath}>Bài viết</Link>
            <span className="material-symbols-outlined">chevron_right</span>
            <span>{post.title}</span>
          </nav>
          )}

          <article className="blog-detail-article">
            <header className="blog-detail-hero">
              <span className="blog-detail-category">{post.category}</span>
              <h1 className="blog-detail-title">{post.title}</h1>
              {post.excerpt && <p className="blog-detail-excerpt">{post.excerpt}</p>}

              <div className="blog-detail-meta">
                <img
                  src={post.authorAvatar}
                  alt={post.author}
                  className="blog-detail-author-avatar"
                  referrerPolicy="no-referrer"
                />
                <div className="author-info">
                  <h4>{post.author}</h4>
                  <p style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '6px' }}>
                    <span>{post.date}</span>
                    <span>•</span>
                    <span>{post.readTime}</span>
                    <span>•</span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
                      <span className="material-symbols-outlined" style={{ fontSize: '15px' }}>visibility</span>
                      {post.viewCount || 0} lượt xem
                    </span>
                  </p>
                </div>
              </div>
            </header>

            <div className="blog-detail-cover">
              <img src={post.image} alt={post.title} />
            </div>

            <div className="blog-detail-body">
              <div
                className="blog-detail-content"
                dangerouslySetInnerHTML={{ __html: post.content || '<p>Nội dung đang được cập nhật.</p>' }}
              />

              {!adminPreview && (
              <div className="share-article blog-vote-bar">
                <span className="share-label">Đánh giá bài viết</span>
                <div className="share-btns blog-vote-actions">
                  <button
                    className={`share-icon-btn vote-icon-btn ${post.userVote === 'upvote' ? 'active' : ''}`}
                    type="button"
                    aria-label="Thích bài viết"
                    disabled={voteSubmitting === 'upvote'}
                    onClick={() => handleVote('upvote')}
                  >
                    <span className="material-symbols-outlined">thumb_up</span>
                    <span className="vote-count">{post.upvoteCount || 0}</span>
                  </button>
                  <button
                    className={`share-icon-btn vote-icon-btn ${post.userVote === 'downvote' ? 'active' : ''}`}
                    type="button"
                    aria-label="Không thích bài viết"
                    disabled={voteSubmitting === 'downvote'}
                    onClick={() => handleVote('downvote')}
                  >
                    <span className="material-symbols-outlined">thumb_down</span>
                    <span className="vote-count">{post.downvoteCount || 0}</span>
                  </button>
                </div>
                {voteError && <span className="blog-vote-error">{voteError}</span>}
              </div>
              )}

              <CommentSection
                comments={comments}
                currentUser={user}
                isAuthenticated={isAuthenticated}
                onAuthRequired={() => navigate('/login', { state: { from: location } })}
                onSubmit={handleSubmitComment}
                submitting={commentSubmitting}
                error={commentError}
                readOnly={adminPreview}
                onVoteComment={handleCommentVote}
                commentVoteSubmitting={commentVoteSubmitting}
              />
            </div>
          </article>
        </div>
      </div>

      {!adminPreview && <RelatedPosts posts={relatedPosts} />}
    </AnimatedPage>
  );
}
