import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import blogApi from '../../../../../services/blog.api';
import {
  mapBlogPost,
  mapBlogTagToCategory,
  unwrapApiData,
} from '../../../../../utils/blogMappers';

const PAGE_SIZE = 15;

const STATUS_LABELS = {
  PUBLISHED: 'Đang hiển thị',
  HIDDEN: 'Đã ẩn',
  DRAFT: 'Bản nháp',
};

const buildPostUpdateFormData = (post, status) => {
  const formData = new FormData();
  formData.append('title', post.title || '');
  formData.append('summary', post.summary || '');
  formData.append('content', post.content || '');
  formData.append('status', status);

  if (post.tags?.length) {
    post.tags.forEach(tag => formData.append('tagIds', tag.id));
  } else {
    formData.append('clearTags', 'true');
  }

  return formData;
};

const getPageTotal = (response) => {
  const payload = unwrapApiData(response);
  return Number(payload?.totalElements ?? payload?.page?.totalElements ?? 0);
};

const BlogMgmt = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [tagIdFilter, setTagIdFilter] = useState('');
  const [appliedFilters, setAppliedFilters] = useState({});
  const [posts, setPosts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [summary, setSummary] = useState({ total: 0, published: 0, hidden: 0, draft: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchSummary = useCallback(async () => {
    const [allRes, pubRes, hidRes, draftRes] = await Promise.allSettled([
      blogApi.getAdminPosts({ page: 0, size: 1 }),
      blogApi.getAdminPosts({ page: 0, size: 1, status: 'PUBLISHED' }),
      blogApi.getAdminPosts({ page: 0, size: 1, status: 'HIDDEN' }),
      blogApi.getAdminPosts({ page: 0, size: 1, status: 'DRAFT' }),
    ]);
    setSummary({
      total: allRes.status === 'fulfilled' ? getPageTotal(allRes.value) : 0,
      published: pubRes.status === 'fulfilled' ? getPageTotal(pubRes.value) : 0,
      hidden: hidRes.status === 'fulfilled' ? getPageTotal(hidRes.value) : 0,
      draft: draftRes.status === 'fulfilled' ? getPageTotal(draftRes.value) : 0,
    });
  }, []);

  const fetchPosts = useCallback(async (pageIndex = 0) => {
    setLoading(true);
    setError('');
    try {
      const response = await blogApi.getAdminPosts({
        page: pageIndex,
        size: PAGE_SIZE,
        sort: 'createdAt,desc',
        ...appliedFilters,
      });
      const payload = unwrapApiData(response);
      setPosts((payload?.content || []).map(mapBlogPost));
      setTotalElements(Number(payload?.totalElements || 0));
      setTotalPages(Number(payload?.totalPages || 0));
      setPage(Number(payload?.number ?? pageIndex));
    } catch (_err) {
      setError('Không thể tải dữ liệu blog từ server.');
      setPosts([]);
    } finally {
      setLoading(false);
    }
  }, [appliedFilters]);

  useEffect(() => {
    blogApi.getTags().then((res) => {
      setCategories((unwrapApiData(res) || []).map(mapBlogTagToCategory));
    }).catch(() => {});
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchPosts(0);
  }, [fetchPosts]);

  const handleFilter = (event) => {
    event?.preventDefault();
    setAppliedFilters({
      keyword: searchQuery.trim() || undefined,
      status: statusFilter || undefined,
      tagId: tagIdFilter || undefined,
    });
  };

  const handleStatusCardClick = (status) => {
    const next = statusFilter === status && status !== '' ? '' : status;
    setStatusFilter(next);
    setAppliedFilters((prev) => ({ ...prev, status: next || undefined }));
  };

  const toggleStatus = async (post) => {
    const nextStatus = post.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED';
    try {
      const detailResponse = await blogApi.getPost(post.id);
      const detailPost = unwrapApiData(detailResponse);
      const updateResponse = await blogApi.updatePost(post.id, buildPostUpdateFormData(detailPost, nextStatus));
      const updatedPost = mapBlogPost(unwrapApiData(updateResponse));
      setPosts(current => current.map(item => item.id === post.id ? updatedPost : item));
      fetchSummary();
      setError('');
    } catch (_err) {
      setError('Không thể cập nhật trạng thái bài viết. Vui lòng thử lại.');
    }
  };

  const getStatusCardStyle = (targetStatus) => ({
    cursor: 'pointer',
    border: statusFilter === targetStatus ? '1px solid var(--admin-accent)' : undefined,
    boxShadow: statusFilter === targetStatus ? '0 0 0 3px rgba(99, 102, 241, 0.14)' : undefined,
  });

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="admin-stats-grid">
        <div className="admin-stat-card" role="button" tabIndex={0}
          onClick={() => handleStatusCardClick('')}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleStatusCardClick('')}
          style={getStatusCardStyle('')}>
          <div className="admin-stat-label">Tổng bài viết</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-accent)' }}>{summary.total}</div>
          <div className="admin-stat-sub">Trong toàn hệ thống</div>
        </div>
        <div className="admin-stat-card" role="button" tabIndex={0}
          onClick={() => handleStatusCardClick('PUBLISHED')}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleStatusCardClick('PUBLISHED')}
          style={getStatusCardStyle('PUBLISHED')}>
          <div className="admin-stat-label">Đang hiển thị</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-green)' }}>{summary.published}</div>
          <div className="admin-stat-sub">Công khai trên blog</div>
        </div>
        <div className="admin-stat-card" role="button" tabIndex={0}
          onClick={() => handleStatusCardClick('HIDDEN')}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleStatusCardClick('HIDDEN')}
          style={getStatusCardStyle('HIDDEN')}>
          <div className="admin-stat-label">Đã bị ẩn</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-red)' }}>{summary.hidden}</div>
          <div className="admin-stat-sub neg">Không hiển thị công khai</div>
        </div>
        <div className="admin-stat-card" role="button" tabIndex={0}
          onClick={() => handleStatusCardClick('DRAFT')}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleStatusCardClick('DRAFT')}
          style={getStatusCardStyle('DRAFT')}>
          <div className="admin-stat-label">Bản nháp</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-purple)' }}>{summary.draft}</div>
          <div className="admin-stat-sub">Chưa xuất bản</div>
        </div>
      </div>

      <div className="admin-card">
        <div className="admin-card-hd" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <i className="ti ti-message-circle"></i>
            Quản lý Diễn đàn & Blog ({totalElements.toLocaleString('vi-VN')})
          </div>
          <form onSubmit={handleFilter} style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative' }}>
              <i className="ti ti-search" style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--admin-muted)' }}></i>
              <input
                type="text"
                placeholder="Tìm tiêu đề, tác giả..."
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                style={{
                  padding: '6px 12px 6px 32px',
                  borderRadius: '6px',
                  border: '1px solid var(--admin-card-hover)',
                  background: 'var(--admin-bg)',
                  color: 'var(--admin-text)',
                  fontSize: '0.85rem',
                  width: '200px',
                }}
              />
            </div>
            <select
              value={statusFilter}
              onChange={(event) => {
                const next = event.target.value;
                setStatusFilter(next);
                setAppliedFilters((prev) => ({ ...prev, status: next || undefined }));
              }}
              style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid var(--admin-card-hover)', background: 'var(--admin-bg)', color: 'var(--admin-text)', fontSize: '0.85rem' }}
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PUBLISHED">Đang hiển thị</option>
              <option value="HIDDEN">Đã ẩn</option>
              <option value="DRAFT">Bản nháp</option>
            </select>
            <select
              value={tagIdFilter}
              onChange={(event) => {
                const next = event.target.value;
                setTagIdFilter(next);
                setAppliedFilters((prev) => ({ ...prev, tagId: next || undefined }));
              }}
              style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid var(--admin-card-hover)', background: 'var(--admin-bg)', color: 'var(--admin-text)', fontSize: '0.85rem' }}
            >
              <option value="">Tất cả danh mục</option>
              {categories.map(category => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
            <button type="submit" className="admin-btn" disabled={loading}>
              <i className="ti ti-search"></i> Tìm
            </button>
          </form>
        </div>

        {error && (
          <div style={{ padding: '12px 16px', color: 'var(--admin-red)', fontSize: '0.85rem' }}>{error}</div>
        )}

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Bài viết</th>
                <th>Tác giả</th>
                <th>Ngày đăng</th>
                <th>Danh mục</th>
                <th>Lượt xem</th>
                <th>Trạng thái</th>
                <th style={{ textAlign: 'right' }}>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '40px', color: 'var(--admin-muted)' }}>Đang tải dữ liệu blog...</td>
                </tr>
              ) : posts.length > 0 ? posts.map(post => (
                <tr key={post.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <img src={post.image} alt="" style={{ width: '40px', height: '28px', borderRadius: '4px', objectFit: 'cover' }} />
                      <div style={{ fontWeight: 500, maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{post.title}</div>
                    </div>
                  </td>
                  <td>{post.author}</td>
                  <td>{post.date}</td>
                  <td><span style={{ fontSize: '0.8rem', color: 'var(--admin-muted)' }}>{post.category}</span></td>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', color: 'var(--admin-muted)' }}>
                      <span className="material-symbols-outlined" style={{ fontSize: '15px' }}>visibility</span>
                      {post.viewCount || 0}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      padding: '4px 10px', borderRadius: '20px', fontSize: '0.7rem',
                      background: post.status === 'PUBLISHED' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                      color: post.status === 'PUBLISHED' ? '#10b981' : 'var(--admin-red)',
                      fontWeight: 600,
                    }}>
                      {STATUS_LABELS[post.status] || post.status}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                      <Link to={`/admin/blog/${post.id}`} className="admin-btn"
                        style={{ padding: '4px 10px', fontSize: '0.75rem', textDecoration: 'none' }}>
                        <i className="ti ti-eye"></i> Xem
                      </Link>
                      <button
                        onClick={() => toggleStatus(post)}
                        className="admin-btn"
                        style={{
                          padding: '4px 10px', fontSize: '0.75rem',
                          background: post.status === 'PUBLISHED' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                          color: post.status === 'PUBLISHED' ? 'var(--admin-red)' : '#10b981',
                          border: post.status === 'PUBLISHED' ? '1px solid rgba(239, 68, 68, 0.2)' : '1px solid rgba(16, 185, 129, 0.2)',
                        }}
                      >
                        <i className={post.status === 'PUBLISHED' ? 'ti ti-eye-off' : 'ti ti-eye'}></i>
                        {post.status === 'PUBLISHED' ? ' Ẩn' : ' Hiện'}
                      </button>
                    </div>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '40px', color: 'var(--admin-muted)' }}>Không tìm thấy bài viết nào phù hợp.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
        <div style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
          Trang {Math.min(page + 1, Math.max(totalPages, 1))}/{Math.max(totalPages, 1)} · {totalElements.toLocaleString('vi-VN')} bài viết
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="admin-btn" disabled={page <= 0 || loading} onClick={() => fetchPosts(page - 1)}>
            <i className="ti ti-chevron-left"></i> Trước
          </button>
          <button className="admin-btn" disabled={loading || totalPages === 0 || page >= totalPages - 1} onClick={() => fetchPosts(page + 1)}>
            Sau <i className="ti ti-chevron-right"></i>
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default BlogMgmt;
