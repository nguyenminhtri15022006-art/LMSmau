import React, { useState } from "react";
import "../../../styles/student/ListCourse/Search.css";

export default function Search({ onSearch }) {
    const [searchQuery, setSearchQuery] = useState("");
    const [isSearching, setIsSearching] = useState(false);

    const handleInputChange = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleSearch = () => {
        if (searchQuery.trim()) {
            setIsSearching(true);
            onSearch(searchQuery.trim());

            // Simulate search delay for better UX
            setTimeout(() => {
                setIsSearching(false);
            }, 300);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleSearch();
        }
    };

    const handleClear = () => {
        setSearchQuery("");
        onSearch("");
    };

    return (
        <section className="search">
            <div className="search-container">
                <div className="search-bar">
                    <div className="search-bar__input-wrapper">
                        <span className="material-symbols-outlined search-bar__icon">
                            {isSearching ? 'hourglass_top' : 'search'}
                        </span>
                        <input
                            className="search-bar__input"
                            type="text"
                            placeholder="Tìm kiếm khóa học (ví dụ: Lập trình Web, Thiết kế UI/UX...)"
                            value={searchQuery}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                        />
                        {searchQuery && (
                            <button
                                className="search-bar__clear"
                                onClick={handleClear}
                                title="Clear search"
                            >
                                <span className="material-symbols-outlined">close</span>
                            </button>
                        )}
                    </div>
                    <button
                        className={`btn btn--primary search-bar__btn ${isSearching ? 'btn--loading' : ''}`}
                        onClick={handleSearch}
                        disabled={isSearching}
                    >
                        {isSearching ? (
                            <>
                                <span className="material-symbols-outlined">hourglass_top</span>
                                Searching...
                            </>
                        ) : (
                            <>
                                <span className="material-symbols-outlined">search</span>
                                Tìm kiếm
                            </>
                        )}
                    </button>
                </div>

            </div>
        </section>
    );
}