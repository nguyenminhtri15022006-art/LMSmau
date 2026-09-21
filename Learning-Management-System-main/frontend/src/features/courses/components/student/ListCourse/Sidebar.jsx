import React, { useState } from "react";
import "../../../styles/student/ListCourse/Sidebar.css";

const CATEGORY_ICONS = {
    "Development": "code",
    "Design": "brush",
    "Business": "business_center",
    "Marketing": "campaign",
    "Data Science": "analytics"
};

export default function Sidebar({
    onFiltersChange,
    onCategoryChange,
    categories = [],
    courses = [],
    uncategorizedCategoryId = "__uncategorized__",
}) {
    const [filters, setFilters] = useState({
        level: []
    });
    const [activeCategory, setActiveCategory] = useState("Tất cả danh mục");
    const [categorySearchTerm, setCategorySearchTerm] = useState("");

    const levels = [
        { value: "BEGINNER", label: "Cơ bản" },
        { value: "INTERMEDIATE", label: "Trung cấp" },
        { value: "ADVANCED", label: "Nâng cao" },
    ];

    const courseBelongsToCategory = (course, category) => {
        const categoryIds = Array.isArray(course.categoryIds) ? course.categoryIds : [];
        if (categoryIds.length > 0) {
            return categoryIds.some((id) => String(id) === String(category.id));
        }

        const categoryNames = Array.isArray(course.categoryNames)
            ? course.categoryNames
            : [course.category].filter(Boolean);
        return categoryNames.includes(category.name);
    };

    // Tạo danh sách danh mục để hiển thị
    const displayCategories = [
        {
            id: 0,
            name: "Tất cả danh mục",
            icon: "grid_view",
            description: "Xem tất cả khóa học",
            courseCount: courses.length
        },
        ...categories.map(cat => ({
            id: cat.id,
            name: cat.name,
            icon: CATEGORY_ICONS[cat.name] || "folder",
            description: `Khóa học ${cat.name}`,
            courseCount: courses.filter(c => !c.uncategorized && courseBelongsToCategory(c, cat)).length
        })),
        {
            id: uncategorizedCategoryId,
            name: "Khác",
            icon: "folder_off",
            description: "Khóa học chưa chọn danh mục",
            courseCount: courses.filter(c => c.uncategorized).length
        }
    ];

    // Lọc danh mục dựa trên từ khóa tìm kiếm
    const filteredCategories = displayCategories.filter(cat => 
        cat.name.toLowerCase().includes(categorySearchTerm.toLowerCase())
    );

    // Handle level checkbox changes
    const handleLevelChange = (levelValue, checked) => {
        const currentLevels = filters.level || [];
        const newLevels = checked 
            ? [...currentLevels, levelValue]
            : currentLevels.filter(l => l !== levelValue);
            
        const newFilters = { ...filters, level: newLevels };
        setFilters(newFilters);
        onFiltersChange(newFilters);
    };

    // Handle category selection
    const handleCategorySelect = (category) => {
        setActiveCategory(category.name);
        onCategoryChange?.(category);
    };

    // Clear all filters
    const clearAllFilters = () => {
        const defaultFilters = {
            level: []
        };
        setFilters(defaultFilters);
        setActiveCategory("Tất cả danh mục");
        onFiltersChange(defaultFilters);
        onCategoryChange?.(displayCategories[0]);
    };

    const hasActiveFilters = (filters.level && filters.level.length > 0) || activeCategory !== "Tất cả danh mục";

    return (
        <aside className="sidebar list-course-filter-sidebar">
            <div className="sidebar__header">
                <h3 className="sidebar__title">
                    <span className="material-symbols-outlined">filter_list</span>
                    Bộ lọc
                </h3>
                {hasActiveFilters && (
                    <button
                        className="sidebar__clear-btn"
                        onClick={clearAllFilters}
                        title="Xóa tất cả bộ lọc"
                    >
                        <span className="material-symbols-outlined">clear</span>
                        Xóa tất cả
                    </button>
                )}
            </div>

            {/* Category Filter */}
            <div className="filter-group category-group">
                <h4 className="filter-group__title">
                    <span className="material-symbols-outlined">category</span>
                    Danh mục
                </h4>

                {/* Search Input cho Category */}
                {displayCategories.length > 5 && (
                    <div className="sidebar__category-search">
                        <span className="material-symbols-outlined sidebar__category-search-icon">search</span>
                        <input
                            className="sidebar__category-search-input"
                            type="text" 
                            placeholder="Tìm danh mục..." 
                            value={categorySearchTerm}
                            onChange={(e) => setCategorySearchTerm(e.target.value)}
                        />
                    </div>
                )}

                <div className="category-choices category-choices--scroll">
                    {filteredCategories.length > 0 ? filteredCategories.map((category) => (
                        <button
                            key={category.id}
                            className={`category-choice ${activeCategory === category.name ? 'category-choice--active' : ''}`}
                            onClick={() => handleCategorySelect(category)}
                            title={category.description}
                        >
                            <span className="material-symbols-outlined category-choice__icon">
                                {category.icon}
                            </span>
                            <span className="category-choice__text">
                                {category.name}
                            </span>
                            <span className="category-choice__count">{category.courseCount}</span>
                        </button>
                    )) : (
                        <p className="category-choices__empty">Không tìm thấy danh mục "{categorySearchTerm}"</p>
                    )}
                </div>
            </div>

            {/* Level Filter */}
            <div className="filter-group">
                <h4 className="filter-group__title">
                    <span className="material-symbols-outlined">school</span>
                    Trình độ
                </h4>
                <div className="filter-options">
                    {levels.map((level) => (
                        <label key={level.value} className="filter-option">
                            <input
                                className="filter-option__checkbox"
                                type="checkbox"
                                checked={filters.level?.includes(level.value) || false}
                                onChange={(e) => handleLevelChange(level.value, e.target.checked)}
                            />
                            <div className={`level ${filters.level?.includes(level.value) ? 'level--active' : ''}`}>
                                <span className="level__label">{level.label}</span>
                            </div>
                        </label>
                    ))}
                </div>
            </div>

            {/* Active Filters Summary */}
            {hasActiveFilters && (
                <div className="sidebar__summary">
                    <h4 className="sidebar__summary-title">Đang lọc theo:</h4>
                    <div className="sidebar__summary-tags">
                        {filters.level && filters.level.map(level => (
                            <span key={level} className="filter-tag">
                                {level}
                                <button
                                    className="filter-tag__remove"
                                    onClick={() => handleLevelChange(level, false)}
                                    title="Xóa bộ lọc trình độ"
                                >
                                    <span className="material-symbols-outlined">close</span>
                                </button>
                            </span>
                        ))}
                        {activeCategory !== "Tất cả danh mục" && (
                            <span className="filter-tag">
                                {activeCategory}
                                <button
                                    className="filter-tag__remove"
                                    onClick={() => handleCategorySelect(displayCategories[0])}
                                    title="Xóa bộ lọc danh mục"
                                >
                                    <span className="material-symbols-outlined">close</span>
                                </button>
                            </span>
                        )}
                    </div>
                </div>
            )}
        </aside>
    );
}
