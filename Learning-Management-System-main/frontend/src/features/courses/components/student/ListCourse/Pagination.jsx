import React from "react";
import "../../../styles/student/ListCourse/Pagination.css";

export default function Pagination({ currentPage, totalPages, onPageChange }) {
    if (totalPages <= 1) return null;

    const getPageNumbers = () => {
        const pages = [];
        const delta = 2; // Number of pages to show on each side of current page

        // Always show first page
        if (1 < currentPage - delta) {
            pages.push(1);
            if (2 < currentPage - delta) {
                pages.push('...');
            }
        }

        // Show pages around current page
        for (let i = Math.max(1, currentPage - delta); i <= Math.min(totalPages, currentPage + delta); i++) {
            pages.push(i);
        }

        // Always show last page
        if (totalPages > currentPage + delta) {
            if (totalPages - 1 > currentPage + delta) {
                pages.push('...');
            }
            pages.push(totalPages);
        }

        return pages;
    };

    const pageNumbers = getPageNumbers();

    return (
        <nav className="pagination" aria-label="Course pagination">
            <button
                className={`pagination__btn pagination__btn--arrow ${currentPage === 1 ? 'pagination__btn--disabled' : ''}`}
                onClick={() => currentPage > 1 && onPageChange(currentPage - 1)}
                disabled={currentPage === 1}
                aria-label="Previous page"
            >
                <span className="material-symbols-outlined">chevron_left</span>
            </button>

            {pageNumbers.map((page, index) => (
                page === '...' ? (
                    <span key={`dots-${index}`} className="pagination__dots">
                        ...
                    </span>
                ) : (
                    <button
                        key={page}
                        className={`pagination__btn ${currentPage === page ? 'pagination__btn--active' : ''}`}
                        onClick={() => onPageChange(page)}
                        aria-label={`Page ${page}`}
                        aria-current={currentPage === page ? 'page' : undefined}
                    >
                        {page}
                    </button>
                )
            ))}

            <button
                className={`pagination__btn pagination__btn--arrow ${currentPage === totalPages ? 'pagination__btn--disabled' : ''}`}
                onClick={() => currentPage < totalPages && onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                aria-label="Next page"
            >
                <span className="material-symbols-outlined">chevron_right</span>
            </button>
        </nav>
    );
}