import React from 'react';
import { motion } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';

export default function BlogCard({ post, index = 0 }) {
  const location = useLocation();
  const basePath = location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';
  const postPath = `${basePath}/${post.detailParam || post.id}`;

  return (
    <motion.article
      className="blog-card"
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.38, delay: Math.min(index * 0.06, 0.36), ease: 'easeOut' }}
      whileHover={{ y: -6 }}
    >
      <div className="blog-card-image">
        <img src={post.image} alt={post.title} />
        <span className="blog-badge">{post.category}</span>
        <span className="blog-card-image-glow" aria-hidden="true" />
      </div>
      <div className="blog-card-content">
        <div className="blog-card-meta">
          <span className="blog-date">{post.date}</span>
          <span className="blog-card-read-time">{post.readTime}</span>
        </div>
        <h3 className="blog-card-title">
          <Link to={postPath}>{post.title}</Link>
        </h3>
        <p className="blog-card-excerpt">{post.excerpt}</p>
        <div className="blog-card-author">
          <img
            src={post.authorAvatar}
            alt={post.author}
            className="blog-card-author-avatar"
            referrerPolicy="no-referrer"
          />
          <div className="blog-card-author-info">
            <span className="blog-card-author-name">{post.author}</span>
            <span className="blog-card-author-meta">
              <span className="blog-card-views">
                <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>visibility</span>
                {post.viewCount || 0}
              </span>
            </span>
          </div>
        </div>
        <Link to={postPath} className="blog-card-read-more">
          <span>Đọc bài viết</span>
          <span className="material-symbols-outlined">arrow_forward</span>
        </Link>
      </div>
    </motion.article>
  );
}
