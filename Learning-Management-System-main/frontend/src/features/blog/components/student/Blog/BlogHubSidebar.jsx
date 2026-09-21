import React from 'react';
import { motion } from 'framer-motion';
import { useLocation, useNavigate } from 'react-router-dom';

export default function BlogHubSidebar({ popularPosts }) {
  const navigate = useNavigate();
  const location = useLocation();
  const basePath = location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';

  return (
    <motion.aside
      className="blog-hub-sidebar"
      initial={{ opacity: 0, x: 28 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.48, delay: 0.18, ease: 'easeOut' }}
    >
      <button
        className="create-post-btn"
        onClick={() => navigate(`${basePath}/create`)}
      >
        <span className="material-symbols-outlined">add</span>
        Tạo bài viết mới
      </button>

      <button
        className="manage-posts-btn"
        onClick={() => navigate(`${basePath}/my-posts`)}
      >
        <span className="material-symbols-outlined">dashboard_customize</span>
        Quản lý bài viết
      </button>

      <motion.div
        className="blog-sidebar-card"
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.28 }}
      >
        <h4 className="sidebar-title">Bài viết phổ biến</h4>
        <div className="popular-posts-list">
          {popularPosts.map((post, index) => (
            <motion.div
              key={post.id}
              className="popular-post-item"
              initial={{ opacity: 0, x: 16 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: 0.36 + index * 0.08 }}
            >
              <img src={post.image} alt={post.title} className="popular-thumb" />
              <div className="popular-info">
                <span className="popular-cat">{post.category}</span>
                <h5 className="popular-title">{post.title}</h5>
                <div className="popular-author">
                  <div className="popular-author-main">
                    <img
                      src={post.authorAvatar}
                      alt={post.author}
                      className="popular-author-avatar"
                      referrerPolicy="no-referrer"
                    />
                    <span className="popular-author-name">{post.author}</span>
                  </div>
                  <span className="popular-views">
                    <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>visibility</span>
                    {post.viewCount || 0}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.div>
    </motion.aside>
  );
}
