import React, { useEffect, useState } from 'react';
import { courseApi } from '../../../../../../services/course.api';
import { formatDateVN } from '../../../../../../utils/dateTime';
import '../../../../styles/admin/WebContentMgmt/CourseCategoriesTab.css';

const INITIAL_CATEGORY_FORM = { name: '', slug: '' };
const CAT_PAGE_SIZE = 10;

const CourseCategoriesTab = ({ showToast }) => {
  const [categories, setCategories] = useState([]);
  const [catLoading, setCatLoading] = useState(false);
  const [catSaving, setCatSaving] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [catForm, setCatForm] = useState(INITIAL_CATEGORY_FORM);
  const [catSearch, setCatSearch] = useState('');
  const [catPage, setCatPage] = useState(0);

  const fetchCategories = async () => {
    setCatLoading(true);
    try {
      const data = await courseApi.getCategories();
      setCategories(data);
    } catch (error) {
      console.error('Failed to load categories:', error);
      showToast('Không thể tải danh mục. Vui lòng thử lại.', 'danger');
    } finally {
      setCatLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const generateSlug = (name) =>
    name
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D')
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');

  const handleCatNameChange = (value) => {
    setCatForm({ name: value, slug: generateSlug(value) });
  };

  const resetCategoryForm = () => {
    setEditingCategory(null);
    setCatForm(INITIAL_CATEGORY_FORM);
  };

  const handleSaveCategorySubmit = async (event) => {
    event.preventDefault();

    if (!catForm.name.trim()) {
      showToast('Vui lòng nhập tên thể loại', 'danger');
      return;
    }

    setCatSaving(true);
    try {
      if (editingCategory) {
        const updatedCategory = await courseApi.updateCategory(editingCategory.id, catForm);
        setCategories((prev) =>
          prev.map((category) => (category.id === editingCategory.id ? updatedCategory : category)),
        );
        showToast('Đã cập nhật thể loại thành công');
        resetCategoryForm();
      } else {
        const createdCategory = await courseApi.createCategory(catForm);
        setCategories((prev) => [...prev, createdCategory]);
        showToast('Đã thêm thể loại mới thành công');
        setCatForm(INITIAL_CATEGORY_FORM);
      }
    } catch (error) {
      console.error('Failed to save category:', error);
      const message = error?.response?.data?.message || 'Lỗi khi lưu thể loại. Vui lòng thử lại.';
      showToast(message, 'danger');
    } finally {
      setCatSaving(false);
    }
  };

  const handleEditCategory = (category) => {
    setEditingCategory(category);
    setCatForm({ name: category.name, slug: category.slug || '' });
  };

  const handleDeleteCategory = async (categoryId) => {
    if (
      !window.confirm(
        'Bạn có chắc chắn muốn xóa thể loại này? Các khóa học thuộc thể loại này sẽ không còn phân loại.',
      )
    ) {
      return;
    }

    try {
      await courseApi.deleteCategory(categoryId);
      setCategories((prev) => prev.filter((category) => category.id !== categoryId));
      showToast('Đã xóa thể loại thành công');

      if (editingCategory?.id === categoryId) {
        resetCategoryForm();
      }
    } catch (error) {
      console.error('Failed to delete category:', error);
      showToast('Lỗi khi xóa thể loại. Có thể vẫn còn khóa học thuộc danh mục này.', 'danger');
    }
  };

  const filteredCategories = categories.filter(
    (category) =>
      category.name.toLowerCase().includes(catSearch.toLowerCase()) ||
      (category.slug || '').toLowerCase().includes(catSearch.toLowerCase()),
  );

  const catTotalPages = Math.ceil(filteredCategories.length / CAT_PAGE_SIZE);
  const pagedCategories = filteredCategories.slice(catPage * CAT_PAGE_SIZE, (catPage + 1) * CAT_PAGE_SIZE);

  const formatDate = (dateStr) => {
    if (!dateStr) return '--';
    return formatDateVN(dateStr) || '--';
  };

  return (
    <div className="admin-fade-in">
      <div className="web-content-section-note">
        Quản lý các thể loại (danh mục) khóa học. Thể loại giúp phân nhóm và lọc khóa học trên trang công khai.
      </div>

      <div className="admin-grid-2 web-content-categories-grid">
        <div className="web-content-categories-list-column">
          <div className="web-content-categories-toolbar">
            <div className="web-content-categories-search">
              <i className="ti ti-search web-content-categories-search-icon"></i>
              <input
                aria-label="Tìm kiếm thể loại khóa học"
                placeholder="Tìm kiếm thể loại..."
                value={catSearch}
                onChange={(event) => { setCatSearch(event.target.value); setCatPage(0); }}
              />
              {catSearch && (
                <>
                  <span className="web-content-categories-search-count">{filteredCategories.length}</span>
                  <button
                    type="button"
                    className="web-content-categories-search-clear"
                    onClick={() => { setCatSearch(''); setCatPage(0); }}
                    aria-label="Xóa tìm kiếm thể loại khóa học"
                    title="Xóa tìm kiếm"
                  >
                    <i className="ti ti-x"></i>
                  </button>
                </>
              )}
            </div>

            <button
              type="button"
              className="admin-btn admin-btn-primary web-content-categories-refresh"
              onClick={fetchCategories}
              disabled={catLoading}
            >
              <i className={`ti ti-refresh ${catLoading ? 'web-content-spin' : ''}`}></i>
              Tải lại
            </button>
          </div>

          <div className="web-content-categories-stats">
            <div className="web-content-categories-stat-item">
              <i className="ti ti-category"></i>
              <span className="web-content-categories-stat-value">{categories.length}</span>
              <span className="web-content-categories-stat-label">thể loại</span>
            </div>

            {catSearch && (
              <div className="web-content-categories-stat-item">
                <i className="ti ti-filter"></i>
                <span className="web-content-categories-stat-label">
                  Hiển thị {filteredCategories.length} kết quả
                </span>
              </div>
            )}
          </div>

          {catLoading && (
            <div className="web-content-categories-empty">
              <div className="web-content-categories-spinner"></div>
              <span>Đang tải danh sách thể loại...</span>
            </div>
          )}

          {!catLoading && filteredCategories.length === 0 && (
            <div className="admin-card web-content-categories-empty-card">
              <div className="web-content-categories-empty">
                <i className="ti ti-folder-off"></i>
                <span>
                  {catSearch
                    ? 'Không tìm thấy thể loại phù hợp.'
                    : 'Chưa có thể loại nào. Hãy tạo thể loại đầu tiên!'}
                </span>
              </div>
            </div>
          )}

          {!catLoading &&
            pagedCategories.map((category) => (
              <div
                key={category.id}
                className={`admin-card web-content-category-item ${
                  editingCategory?.id === category.id ? 'web-content-category-item--active' : ''
                }`}
              >
                <div className="web-content-category-item-body">
                  <div className="web-content-category-badge">
                    <i className="ti ti-tag"></i>
                  </div>

                  <div className="web-content-category-meta">
                    <div className="web-content-category-name">{category.name}</div>
                    <div className="web-content-category-tags">
                      <span className="web-content-category-slug">/{category.slug || '--'}</span>
                      <span className="web-content-category-muted">ID: {category.id}</span>
                      <span className="web-content-category-muted">{formatDate(category.createdAt)}</span>
                    </div>
                  </div>

                  <div className="web-content-category-actions">
                    <button type="button" className="icon-btn" title="Chỉnh sửa" onClick={() => handleEditCategory(category)}>
                      <i className="ti ti-edit"></i>
                    </button>
                    <button
                      type="button"
                      className="icon-btn web-content-danger-action"
                      title="Xóa"
                      onClick={() => handleDeleteCategory(category.id)}
                    >
                      <i className="ti ti-trash"></i>
                    </button>
                  </div>
                </div>
              </div>
            ))}

          {!catLoading && catTotalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
              <span style={{ fontSize: '0.82rem', color: 'var(--admin-muted)' }}>
                Trang {catPage + 1}/{catTotalPages} · {filteredCategories.length} thể loại
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="admin-btn" disabled={catPage <= 0} onClick={() => setCatPage((p) => p - 1)}>
                  <i className="ti ti-chevron-left"></i> Trước
                </button>
                <button className="admin-btn" disabled={catPage >= catTotalPages - 1} onClick={() => setCatPage((p) => p + 1)}>
                  Sau <i className="ti ti-chevron-right"></i>
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="admin-card">
          <div className="admin-card-hd">
            <i className={`ti ti-${editingCategory ? 'edit' : 'plus'}`}></i>
            {editingCategory ? 'Chỉnh sửa thể loại' : 'Thêm thể loại mới'}
          </div>

          <form className="admin-card-body" onSubmit={handleSaveCategorySubmit}>
            <div className="admin-field">
              <label>Tên thể loại *</label>
              <input
                placeholder="VD: Lập trình Web, Trí tuệ nhân tạo, Data Science..."
                value={catForm.name}
                onChange={(event) => handleCatNameChange(event.target.value)}
                maxLength={200}
              />
            </div>

            <div className="admin-field">
              <label>Slug (URL-friendly, tự động tạo)</label>
              <input
                className="web-content-category-slug-input"
                placeholder="vd: lap-trinh-web"
                value={catForm.slug}
                onChange={(event) => setCatForm({ ...catForm, slug: event.target.value })}
                maxLength={200}
              />
              <span className="web-content-category-hint">
                Slug được tự tạo từ tên. Bạn có thể chỉnh sửa thủ công nếu cần.
              </span>
            </div>

            {catForm.name && (
              <div className="web-content-category-preview">
                <span className="web-content-category-preview-label">Xem trước</span>
                <div className="web-content-category-preview-row">
                  <div className="web-content-category-preview-icon">
                    <i className="ti ti-tag"></i>
                  </div>
                  <div>
                    <div className="web-content-category-preview-name">{catForm.name}</div>
                    <div className="web-content-category-preview-slug">/{catForm.slug || '...'}</div>
                  </div>
                </div>
              </div>
            )}

            <div className="web-content-form-actions">
              <button className="admin-btn admin-btn-primary" type="submit" disabled={catSaving}>
                {catSaving ? (
                  <>
                    <span className="web-content-inline-spinner"></span>
                    Đang lưu...
                  </>
                ) : (
                  <>
                    <i className="ti ti-device-floppy"></i>
                    {editingCategory ? 'Cập nhật' : 'Thêm thể loại'}
                  </>
                )}
              </button>

              {editingCategory && (
                <button className="admin-btn" type="button" onClick={resetCategoryForm}>
                  Hủy bỏ
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CourseCategoriesTab;
