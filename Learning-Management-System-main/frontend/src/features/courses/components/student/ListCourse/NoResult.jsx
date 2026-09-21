import React from "react";
import "../../../styles/student/ListCourse/NoResult.css";

export default function NoResults() {
    return (
        <div className="no-results">
            <div className="no-results-icon">
                <span className="material-symbols-outlined">search_off</span>
            </div>
            <h3>Không tìm thấy khóa học</h3>
            <p>Thử điều chỉnh tiêu chí tìm kiếm hoặc bộ lọc của bạn</p>
        </div>
    );
}