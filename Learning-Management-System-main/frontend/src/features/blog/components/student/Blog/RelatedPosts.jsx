import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import BlogCard from './BlogCard';

export default function RelatedPosts({ posts }) {
  const location = useLocation();
  const basePath = location.pathname.startsWith('/instructor') ? '/instructor/blog' : '/blog';

  if (!posts.length) return null;

  return (
    <section className="related-posts-section">
      <div className="related-posts-header">
        <h3 className="blog-recent-title">
          <span className="material-symbols-outlined">auto_stories</span>
          Bài viết liên quan
        </h3>
        <Link to={basePath} className="view-all-link">
          Xem tất cả bài viết
          <span className="material-symbols-outlined">arrow_forward</span>
        </Link>
      </div>

      <div className="related-posts-grid">
        {posts.slice(0, 3).map(post => (
          <BlogCard key={post.id} post={post} />
        ))}
      </div>
    </section>
  );
}
