import React, { useState } from "react";
import "../../styles/us/AboutCapabilities.css";

const DEFAULT_ABOUT_ROLES = {
    student: {
        icon: "school",
        title: "Học viên",
        items: [
            "Xem khóa học public mà không cần đăng nhập.",
            "Đăng nhập để đăng ký khóa học và lưu tiến độ.",
            "Học theo chương mục, bài học và tài liệu.",
            "Làm quiz, coding challenge và xem kết quả.",
            "Theo dõi khóa học đã đăng ký trên dashboard cá nhân.",
            "Tham gia blog để đọc, viết và trao đổi kiến thức."
        ]
    },
    instructor: {
        icon: "co_present",
        title: "Giảng viên",
        items: [
            "Quản lý khóa học do mình phụ trách.",
            "Tạo cấu trúc chương, bài học và nội dung học liệu.",
            "Quản lý học viên trong khóa học.",
            "Tạo quiz, câu hỏi và bài kiểm tra.",
            "Tạo bài lập trình, test case và cấu hình chấm code.",
            "Theo dõi kết quả học tập, submission và hoạt động của học viên."
        ]
    }
};

const techHighlights = [
    "React",
    "Spring Boot",
    "Spring Cloud Gateway",
    "Eureka",
    "JWT/RBAC",
    "MySQL",
    "MongoDB",
    "MinIO",
    "Judge0",
];

const futureItems = [
    "Gợi ý khóa học cá nhân hóa theo hành vi học tập.",
    "Thêm leaderboard, huy hiệu và gamification.",
    "Mở rộng chatbot/AI hỗ trợ giải thích bài học.",
    "Cải thiện dashboard phân tích kết quả cho giảng viên.",
    "Tối ưu trải nghiệm mobile cho học viên.",
];

export default function AboutCapabilities() {
    const [roles] = useState(() => {
        const saved = localStorage.getItem('web_content_about_roles');
        if (saved) {
            try {
                return JSON.parse(saved);
            } catch (e) {
                console.error(e);
                return DEFAULT_ABOUT_ROLES;
            }
        }
        return DEFAULT_ABOUT_ROLES;
    });

    const roleCapabilities = [
        {
            icon: roles.student.icon,
            title: roles.student.title,
            tone: "student",
            items: roles.student.items
        },
        {
            icon: roles.instructor.icon,
            title: roles.instructor.title,
            tone: "instructor",
            items: roles.instructor.items
        }
    ];

    return (
        <>
            <section className="about-capabilities">
                <div className="about-capabilities__inner">
                    <div className="about-capabilities__header" data-reveal>
                        <span>Vai trò trong hệ thống</span>
                        <h2>EduLearn được thiết kế theo từng nhóm người dùng</h2>
                        <p>
                            Học viên có luồng học tập rõ ràng, còn giảng viên có bộ công cụ để
                            xây dựng nội dung, quản lý bài tập và theo dõi kết quả học tập.
                        </p>
                    </div>

                    <div className="about-capabilities__grid">
                        {roleCapabilities.map((role) => (
                            <article
                                className={`about-role-card about-role-card--${role.tone}`}
                                key={role.title}
                                data-reveal
                            >
                                <div className="about-role-card__head">
                                    <span className="material-symbols-outlined">{role.icon}</span>
                                    <h3>{role.title}</h3>
                                </div>
                                <ul>
                                    {role.items.map((item) => (
                                        <li key={item}>
                                            <span className="material-symbols-outlined">done</span>
                                            {item}
                                        </li>
                                    ))}
                                </ul>
                            </article>
                        ))}
                    </div>
                </div>
            </section>

            <section className="about-architecture">
                <div className="about-architecture__inner">
                    <div className="about-architecture__content" data-reveal>
                        <span>Nền tảng kỹ thuật</span>
                        <h2>Kiến trúc microservices cho một LMS có thể mở rộng</h2>
                        <p>
                            EduLearn dùng API Gateway làm điểm vào chính. Các service chính gồm Identity
                            Service cho xác thực và phân quyền, Course Service cho khóa học và học liệu,
                            Assignment Service cho quiz/code judge, Blog Service cho nội dung cộng đồng.
                        </p>
                    </div>
                    <div className="about-architecture__tags" data-reveal style={{ "--reveal-delay": "110ms" }}>
                        {techHighlights.map((item, index) => (
                            <span key={item} style={{ "--tag-index": index }}>{item}</span>
                        ))}
                    </div>
                </div>
            </section>

            <section className="about-future">
                <div className="about-future__inner">
                    <div className="about-future__header" data-reveal>
                        <span>Hướng phát triển</span>
                        <h2>Các hướng mở rộng sau phiên bản hiện tại</h2>
                    </div>
                    <div className="about-future__list">
                        {futureItems.map((item, index) => (
                            <div className="about-future__item" key={item} data-reveal style={{ "--reveal-delay": `${index * 55}ms` }}>
                                <span className="material-symbols-outlined">arrow_forward</span>
                                {item}
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </>
    );
}
