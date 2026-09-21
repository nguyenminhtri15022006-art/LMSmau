import React from 'react';

export default function BlogFilters({ categories, activeCategory, onCategoryChange, onSearch, searchQuery }) {
  const selectedCategoryId = activeCategory == null ? '' : String(activeCategory);

  const handleCategoryChange = (event) => {
    const nextValue = event.target.value;
    const nextCategory = categories.find((cat) => String(cat.id ?? '') === nextValue);
    onCategoryChange(nextCategory?.id ?? null);
  };

  return (
    <div className="blog-hub-header">
      <div className="blog-hub-title-row">
        <h1 className="blog-hub-title">Thư viện học tập</h1>
        <div className="blog-search-bar">
          <input
            type="text"
            placeholder="Tìm kiếm bài viết..."
            value={searchQuery}
            onChange={(e) => onSearch(e.target.value)}
          />
          <button className="blog-search-btn">
            <span className="material-symbols-outlined">search</span>
            Tìm kiếm
          </button>
        </div>
      </div>

      <div className="blog-filter-row">
        <div className="blog-filter-meta">
          <span className="material-symbols-outlined">filter_list</span>
          <label className="blog-filter-label" htmlFor="blog-category-filter">
            Danh mục
          </label>
        </div>
        <div className="blog-filter-select-wrap">
          <select
            id="blog-category-filter"
            className="blog-filter-select"
            value={selectedCategoryId}
            onChange={handleCategoryChange}
          >
            {categories.map((cat) => (
              <option key={cat.id ?? 'all'} value={cat.id ?? ''}>
                {cat.name}
              </option>
            ))}
          </select>
          <span className="material-symbols-outlined">keyboard_arrow_down</span>
        </div>
      </div>
    </div>
  );
}
