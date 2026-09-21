import React from "react";
import "../../../styles/student/DetailCourse/CourseInfo.css";

export default function CourseInfo({ requirements = [], description }) {
    if (!requirements.length && !description) return null;

    return (
        <div className="course-info">
            {requirements.length > 0 && (
                <section className="course-info__section">
                    <h2 className="course-info__title">Điều kiện tiên quyết</h2>
                    <ul className="course-info__list">
                        {requirements.map((req, index) => (
                            <li key={index}>{req}</li>
                        ))}
                    </ul>
                </section>
            )}

            {description && (
                <section className="course-info__section">
                    <h2 className="course-info__title">Tổng quan khóa học</h2>
                    <div className="course-info__description">
                        <p>{description}</p>
                    </div>
                </section>
            )}
        </div>
    );
}
