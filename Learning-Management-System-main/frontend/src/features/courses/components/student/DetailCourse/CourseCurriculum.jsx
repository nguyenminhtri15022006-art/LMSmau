import React, { useState, useEffect } from "react";
import "../../../styles/student/DetailCourse/CourseCurriculum.css";

const LESSON_TYPE_CONFIG = {
    VIDEO:      { icon: "ondemand_video", label: "Video",        color: "blue"   },
    DOCUMENT:   { icon: "description",   label: "Tài liệu",     color: "green"  },
    QUIZ:       { icon: "quiz",          label: "Quiz",          color: "amber"  },
    CODING:     { icon: "code",          label: "Bài tập code", color: "purple" },
    LIVE_CLASS: { icon: "live_tv",       label: "Trực tiếp",    color: "red"    },
};

function countLessons(nodes) {
    return nodes.reduce((sum, node) => {
        if (node.type === "lesson") return sum + 1;
        return sum + countLessons(node.items ?? []);
    }, 0);
}

// ── Single recursive component — mirrors ChaptersList's CurriculumItem ──
function CurriculumNode({ node, level = 0, forceOpen }) {
    const isFolder = node.type === "folder";

    const [isOpen, setIsOpen] = useState(() =>
        forceOpen !== undefined ? forceOpen : level === 0
    );

    useEffect(() => {
        if (forceOpen !== undefined) setIsOpen(forceOpen);
    }, [forceOpen]);

    if (!isFolder) {
        const typeConfig = LESSON_TYPE_CONFIG[node.lessonType?.toUpperCase()];
        return (
            <div className="course-curriculum__item">
                <div className="course-curriculum__item-left">
                    <span
                        className={`material-symbols-outlined course-curriculum__item-icon course-curriculum__item-icon--${typeConfig?.color ?? "default"}`}
                    >
                        {typeConfig?.icon ?? "play_circle"}
                    </span>
                    <span className="course-curriculum__item-title">{node.title}</span>
                </div>
                <span
                    className={`course-curriculum__item-type course-curriculum__item-type--${typeConfig?.color ?? "default"}`}
                >
                    {typeConfig?.label ?? "Bài học"}
                </span>
            </div>
        );
    }

    const children = node.items ?? [];
    const lectureCount = node.lectures ?? countLessons(children);

    return (
        <div
            className={`course-curriculum__section${isOpen ? " course-curriculum__section--open" : ""}${level > 0 ? " course-curriculum__section--nested" : ""}`}
        >
            <div
                className="course-curriculum__section-header"
                onClick={() => setIsOpen(prev => !prev)}
            >
                <div className="course-curriculum__section-left">
                    <span
                        className={`material-symbols-outlined course-curriculum__section-icon${isOpen ? " course-curriculum__section-icon--open" : ""}`}
                    >
                        expand_more
                    </span>
                    <span className="course-curriculum__section-title">{node.title}</span>
                </div>
                <span className="course-curriculum__section-meta">
                    {lectureCount} bài học
                </span>
            </div>

            {isOpen && children.length > 0 && (
                <div className="course-curriculum__items">
                    {children.map(child => (
                        <CurriculumNode
                            key={child.id}
                            node={child}
                            level={level + 1}
                            forceOpen={forceOpen}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default function CourseCurriculum({ sections = [] }) {
    const [allOpen, setAllOpen] = useState(true);

    if (!sections.length) return null;

    const totalLessons = countLessons(sections);

    return (
        <div className="course-curriculum">
            <h2 className="course-curriculum__title">Nội dung khóa học</h2>
            <div className="course-curriculum__header">
                <span>{totalLessons} bài học</span>
                <button
                    className="course-curriculum__expand-btn"
                    onClick={() => setAllOpen(p => !p)}
                >
                    {allOpen ? "Thu gọn tất cả" : "Xem tất cả"}
                </button>
            </div>

            <div className="course-curriculum__list">
                {sections.map(node => (
                    <CurriculumNode
                        key={node.id}
                        node={node}
                        level={0}
                        forceOpen={allOpen}
                    />
                ))}
            </div>
        </div>
    );
}
