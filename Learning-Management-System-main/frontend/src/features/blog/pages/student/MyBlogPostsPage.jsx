import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import blogApi from '../../../../services/blog.api';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import { mapBlogPost, unwrapPageContent } from '../../../../utils/blogMappers';
import { attachBlogAuthorProfiles } from '../../../../utils/blogAuthorProfiles';
import AnimatedPage from '../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../components/ui/LockedFeature';
import { useToast } from '../../../../components/ui/Toast';
import useAuth from '../../../../hooks/useAuth';

import '../../styles/student/BlogCommon.css';
import '../../styles/student/MyBlogPosts.css';

export default function MyBlogPostsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const basePath = location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';
  const [myPosts, setMyPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchMyPosts = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await blogApi.getMyPosts({
        page: 0,
        size: 50,
        sort: 'createdAt,desc',
      });
      const postsWithAuthors = await attachBlogAuthorProfiles(unwrapPageContent(response), user);
      setMyPosts(postsWithAuthors.map(mapBlogPost));
      setError(null);
    } catch (err) {
      setMyPosts([]);
      setError(buildAppErrorState(err, {
        title: 'Không thể tải bài viết của bạn',
        fallbackPath: basePath,
      }));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyPosts();
  }, [user]);

  const handleDelete = async (id, title) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa "${title}"?`)) return;

    try {
      await blogApi.deletePost(id);
      setMyPosts((currentPosts) => currentPosts.filter((post) => post.id !== id));
    } catch (_err) {
      toast.error('Xóa bài viết thất bại. Vui lòng thử lại sau.');
    }
  };

  if (error) {
    return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
  }

  return (
    <AnimatedPage>
      <LockedFeature featureName="Blog Management">
        <div className="blog-page">
          <nav className="ch-breadcrumb">
            <Link to={basePath}>Bài viết</Link>
            <span className="ch-breadcrumb-separator">
              <span className="material-symbols-outlined">chevron_right</span>
            </span>
            <span className="breadcrumb-title">Bài viết của tôi</span>
          </nav>

          <motion.div
            className="manage-posts-container"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, ease: 'easeOut' }}
          >
            <header className="manage-posts-header">
              <h1>Bài viết của tôi</h1>
              <button className="read-more-btn" onClick={() => navigate(`${basePath}/create`)}>
                <span className="material-symbols-outlined add-icon">add</span>
                Viết bài mới
              </button>
            </header>

            <div className="blog-manage-table-container">
              <table className="blog-manage-table">
                <thead>
                  <tr>
                    <th>Bài viết</th>
                    <th>Danh mục</th>
                    <th>Ngày</th>
                    <th>Lượt xem</th>
                    <th>Trạng thái</th>
                    <th>Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan="6" className="empty-state-td">
                        Đang tải bài viết...
                      </td>
                    </tr>
                  ) : myPosts.length > 0 ? myPosts.map((post, index) => (
                    <motion.tr
                      key={post.id}
                      initial={{ opacity: 0, x: -18 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.28, delay: Math.min(index * 0.04, 0.32) }}
                    >
                      <td>
                        <div className="manage-post-info">
                          <img src={post.image} alt={post.title} className="manage-post-thumb" />
                          <Link to={`${basePath}/${post.id}`} className="manage-post-title">{post.title}</Link>
                        </div>
                      </td>
                      <td>
                        <span className="trending-cat">{post.category}</span>
                      </td>
                      <td>
                        <span className="trending-date">{post.date}</span>
                      </td>
                      <td>
                        <span
                          className="trending-views"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            color: '#64748b',
                            fontSize: '13px',
                            fontWeight: '600',
                          }}
                        >
                          <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>visibility</span>
                          {post.viewCount || 0}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge ${post.status === 'PUBLISHED' ? 'status-published' : 'status-draft'}`}>
                          {post.status === 'PUBLISHED' ? 'Published' : 'Draft'}
                        </span>
                      </td>
                      <td>
                        <div className="manage-actions">
                          <button
                            className="action-btn edit"
                            title="Edit Post"
                            onClick={() => navigate(`${basePath}/edit/${post.id}`)}
                          >
                            <span className="material-symbols-outlined action-icon-sm">edit</span>
                          </button>
                          <button className="action-btn delete" title="Delete Post" onClick={() => handleDelete(post.id, post.title)}>
                            <span className="material-symbols-outlined action-icon-sm">delete</span>
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  )) : (
                    <tr>
                      <td colSpan="6" className="empty-state-td">
                        Bạn chưa viết bài viết nào.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </motion.div>
        </div>
      </LockedFeature>
    </AnimatedPage>
  );
}
