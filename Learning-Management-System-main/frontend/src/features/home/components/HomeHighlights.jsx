import React, { useState } from "react";
import { Link } from "react-router-dom";
import "../styles/HomeHighlights.css";

const DEFAULT_CARDS = [
  { id: 'card-1', title: 'Xem khóa học công khai', desc: 'Khách có thể xem danh sách và chi tiết khóa học trước khi đăng ký.', icon: 'travel_explore', tone: 'blue' },
  { id: 'card-2', title: 'Đăng ký đúng luồng', desc: 'Khi bấm đăng ký, hệ thống kiểm tra đăng nhập rồi mới ghi danh.', icon: 'login', tone: 'green' },
  { id: 'card-3', title: 'Học theo chương bài', desc: 'Khóa học được tổ chức theo chương, bài học và tài liệu rõ ràng.', icon: 'account_tree', tone: 'amber' },
  { id: 'card-4', title: 'Quiz kiểm tra kiến thức', desc: 'Người học làm trắc nghiệm theo khóa học và xem lại kết quả.', icon: 'quiz', tone: 'rose' },
  { id: 'card-5', title: 'Coding challenge', desc: 'Luyện code với test case và chấm tự động qua Judge0.', icon: 'code', tone: 'blue' },
  { id: 'card-6', title: 'Theo dõi tiến độ', desc: 'Xem khóa đã đăng ký, bài học hoàn thành và kết quả học tập.', icon: 'monitoring', tone: 'green' },
  { id: 'card-7', title: 'Blog cộng đồng', desc: 'Đọc, viết, bình luận và chia sẻ kinh nghiệm học tập.', icon: 'forum', tone: 'amber' },
  { id: 'card-8', title: 'Phân quyền theo vai trò', desc: 'Student, Instructor và Admin có quyền truy cập riêng.', icon: 'admin_panel_settings', tone: 'rose' }
];

const DEFAULT_ROLES = {
    student: {
        icon: "school",
        eyebrow: "Student",
        title: "Dành cho học viên",
        items: [
            "Tìm kiếm và lọc khóa học theo danh mục, cấp độ, từ khóa.",
            "Xem mô tả, giảng viên, số bài học, bài tập và cấu trúc khóa học.",
            "Quản lý khóa học đã đăng ký và tiếp tục học trên dashboard.",
            "Làm quiz, coding challenge và xem kết quả luyện tập.",
            "Viết blog, bình luận và trao đổi kiến thức với cộng đồng."
        ]
    },
    instructor: {
        icon: "co_present",
        eyebrow: "Instructor",
        title: "Dành cho giảng viên",
        items: [
            "Tạo và cập nhật khóa học, thumbnail, level, danh mục.",
            "Xây dựng chương, bài học, học liệu và cấu trúc nội dung.",
            "Quản lý học viên, số lượng đăng ký và tình trạng tham gia.",
            "Tạo quiz, câu hỏi, bài coding, starter code và test case.",
            "Theo dõi submission, kết quả quiz/code và hoạt động học viên."
        ]
    }
};

const learningFlow = [
    "Tìm khóa học",
    "Xem chi tiết",
    "Đăng nhập/đăng ký",
    "Học bài",
    "Làm quiz/code",
    "Theo dõi tiến độ",
    "Trao đổi qua blog",
];

function RolePanel({ eyebrow, title, icon, items, tone }) {
    return (
        <article className={`home-role-panel home-role-panel--${tone}`} data-reveal>
            <div className="home-role-panel__head">
                <span className="material-symbols-outlined">{icon}</span>
                <div>
                    <p>{eyebrow}</p>
                    <h3>{title}</h3>
                </div>
            </div>
            <ul>
                {items.map((item) => (
                    <li key={item}>
                        <span className="material-symbols-outlined">check_circle</span>
                        {item}
                    </li>
                ))}
            </ul>
        </article>
    );
}

export default function HomeHighlights() {
    const [customCards] = useState(() => {
        const saved = localStorage.getItem('web_content_cards');
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                if (parsed.length < 8) {
                    localStorage.setItem('web_content_cards', JSON.stringify(DEFAULT_CARDS));
                    return DEFAULT_CARDS;
                }
                return parsed;
            } catch (e) {
                console.error(e);
                return DEFAULT_CARDS;
            }
        }
        return DEFAULT_CARDS;
    });

    const [quotes] = useState(() => {
        const saved = localStorage.getItem('web_content_quotes');
        if (saved) {
            try {
                return JSON.parse(saved);
            } catch (e) {
                console.error(e);
                return [];
            }
        }
        // Fallback quotes
        return [
            { id: 'quote-1', text: 'Học lập trình không phải là học cú pháp, mà là học cách giải quyết vấn đề.', author: 'Steve Jobs' },
            { id: 'quote-2', text: 'Trong thế giới công nghệ thay đổi liên tục, kỹ năng quan trọng nhất bạn cần sở hữu là kỹ năng tự học.', author: 'Bill Gates' },
            { id: 'quote-3', text: 'Mã nguồn chạy được chưa đủ, nó còn phải sạch và dễ hiểu đối với người khác.', author: 'Martin Fowler' },
        ];
    });

    const [roles] = useState(() => {
        const saved = localStorage.getItem('web_content_roles');
        if (saved) {
            try {
                return JSON.parse(saved);
            } catch (e) {
                console.error(e);
                return DEFAULT_ROLES;
            }
        }
        return DEFAULT_ROLES;
    });

    return (
        <>
            <section className="home-highlights">
                <div className="home-highlights__inner">
                    <div className="home-section-heading" data-reveal>
                        <span>Chức năng nổi bật</span>
                        <h2>EduLearn giúp bạn học, luyện tập và theo dõi tiến độ trong một nơi</h2>
                        <p>
                            Hệ thống kết nối khóa học, quiz, bài lập trình, dashboard và blog
                            để tạo thành một luồng học tập liền mạch.
                        </p>
                    </div>

                    <div className="home-highlights__grid">
                        {customCards.map((card, index) => {
                            const tones = ['blue', 'green', 'amber', 'rose'];
                            const tone = card.tone || tones[index % tones.length];
                            return (
                                <article
                                    className={`home-feature-card home-feature-card--${tone}`}
                                    key={card.id || index}
                                    data-reveal
                                    style={{ "--reveal-delay": `${index * 45}ms` }}
                                >
                                    <div className="home-feature-card__icon" style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
                                        {card.icon && card.icon.startsWith('ti-') ? (
                                            <i className={`ti ${card.icon}`} style={{ fontSize: '1.8rem' }}></i>
                                        ) : (
                                            <span className="material-symbols-outlined">{card.icon || 'star'}</span>
                                        )}
                                    </div>
                                    <h3>{card.title}</h3>
                                    <p>{card.desc}</p>
                                </article>
                            );
                        })}
                    </div>
                </div>
            </section>

            <section className="home-roles">
                <div className="home-roles__inner">
                    <div className="home-section-heading home-section-heading--left" data-reveal>
                        <span>Theo từng vai trò</span>
                        <h2>Học viên và giảng viên có bộ công cụ riêng</h2>
                    </div>
                    <div className="home-roles__grid">
                        <RolePanel 
                            eyebrow={roles.student.eyebrow} 
                            title={roles.student.title} 
                            icon={roles.student.icon} 
                            items={roles.student.items} 
                            tone="student" 
                        />
                        <RolePanel 
                            eyebrow={roles.instructor.eyebrow} 
                            title={roles.instructor.title} 
                            icon={roles.instructor.icon} 
                            items={roles.instructor.items} 
                            tone="instructor" 
                        />
                    </div>
                </div>
            </section>

            <section className="home-learning-flow">
                <div className="home-learning-flow__inner">
                    <div className="home-section-heading" data-reveal>
                        <span>Luồng học tập</span>
                        <h2>Một hành trình khép kín từ khám phá đến thực hành</h2>
                    </div>
                    <div className="home-learning-flow__steps" data-reveal>
                        {learningFlow.map((step, index) => (
                            <div className="home-learning-flow__step" key={step} style={{ "--flow-index": index }}>
                                <span>{String(index + 1).padStart(2, "0")}</span>
                                <strong>{step}</strong>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {quotes.length > 0 && (
                <section className="home-quotes">
                    <div className="home-quotes__inner">
                        <div className="home-section-heading home-quotes__heading">
                            <span>Ý kiến phản hồi</span>
                            <h2>Trích dẫn truyền cảm hứng</h2>
                        </div>
                        <div className="home-quotes__grid">
                            {quotes.map((q) => (
                                <div key={q.id} className="home-quote-card">
                                    <p className="home-quote-card__text">"{q.text}"</p>
                                    <span className="home-quote-card__author">— Tác giả: {q.author}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </section>
            )}

            <section className="home-blog-cta">
                <div className="home-blog-cta__inner" data-reveal>
                    <div>
                        <span className="home-blog-cta__eyebrow">Cộng đồng học tập</span>
                        <h2>Không chỉ học, còn có cộng đồng để trao đổi</h2>
                        <p>
                            Người học có thể đọc bài viết, bình luận và chia sẻ kinh nghiệm qua Blog.
                            Đây là nơi kết nối kiến thức khóa học với kinh nghiệm thực tế.
                        </p>
                    </div>
                    <Link to="/blog" className="home-blog-cta__link">
                        Đọc blog
                        <span className="material-symbols-outlined">arrow_forward</span>
                    </Link>
                </div>
            </section>
        </>
    );
}
