import React from "react";
import "../../../styles/student/DetailCourse/LearningPoints.css";

export default function LearningPoints({ points = [] }) {
    if (!points.length) return null;

    return (
        <div className="learning-points">
            <h2 className="learning-points__title">Bạn sẽ học được gì</h2>
            <div className="learning-points__grid">
                {points.map((point, index) => (
                    <div key={index} className="learning-points__item">
                        <span className="material-symbols-outlined learning-points__icon">check</span>
                        <span className="learning-points__text">{point}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}
