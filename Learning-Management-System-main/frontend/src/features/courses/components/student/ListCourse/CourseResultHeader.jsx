import React from "react";
import "../../../styles/student/ListCourse/CourseResultHeader.css";

export default function CourseResultHeader({ selectedCategory, resultsCount }) {
    return (
        <div className="course-results-header">
            <h2 className="results-title">
                {selectedCategory} Khóa học
                <span className="results-count">({resultsCount} kết quả)</span>
            </h2>
        </div>
    );
}