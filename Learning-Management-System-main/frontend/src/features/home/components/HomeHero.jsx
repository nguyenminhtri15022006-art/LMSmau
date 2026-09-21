import React, { useState } from "react";
import { Link } from "react-router-dom";
import anhnhom from "../../../assets/images/anhnhom.png";
import "../styles/HomeHero.css";

export default function HomeHero() {
    const [slogan] = useState(() => {
        const saved = localStorage.getItem('web_content_slogan');
        return saved ? JSON.parse(saved) : 'Nền tảng Học tập & Đào tạo lập trình thế hệ mới';
    });

    const [images] = useState(() => {
        const saved = localStorage.getItem('web_content_inspire');
        return saved ? JSON.parse(saved) : [];
    });

    const heroImg = [...images].reverse().find(img => img.position === 'Hero Banner' && img.status === 'Hiển thị');
    const heroSrc = heroImg ? heroImg.imageUrl : anhnhom;

    return (
        <section className="home-hero">
            <div className="home-hero__inner">
                <div className="home-hero__content" data-reveal>
                    <span className="home-hero__eyebrow">{slogan}</span>
                    <h1 className="home-hero__title">
                        Học lập trình có lộ trình, <span>thực hành rõ ràng</span>
                    </h1>
                    <p className="home-hero__desc">
                        EduLearn giúp bạn khám phá khóa học, học theo chương mục, làm quiz,
                        luyện coding challenge và theo dõi tiến độ trong một hệ thống thống nhất.
                    </p>

                    <div>
                        <div className="home-hero__actions">
                            <Link to="/list-course" className="home-hero__btn home-hero__btn--primary">
                                <span className="material-symbols-outlined">school</span>
                                Khám phá khóa học
                            </Link>
                            <Link to="/exercises" className="home-hero__btn home-hero__btn--secondary">
                                <span className="material-symbols-outlined">code</span>
                                Bắt đầu luyện tập
                            </Link>
                        </div>
                        <div className="home-hero__search">
                            <div className="home-hero__search-icon">
                                <span className="material-symbols-outlined">search</span>
                            </div>
                            <input
                                className="home-hero__search-input"
                                placeholder="Bạn muốn học gì hôm nay?"
                                type="text"
                            />
                            <Link to="/list-course" className="home-hero__search-btn">
                                Tìm kiếm
                            </Link>
                        </div>
                        <div className="home-hero__tags">
                            <span>Phổ biến:</span>
                            <Link to="/list-course">Java</Link>
                            <Link to="/list-course">Python</Link>
                            <Link to="/list-course">React</Link>
                            <Link to="/list-course">Cấu trúc dữ liệu</Link>
                            <Link to="/list-course">SQL</Link>
                        </div>
                    </div>
                </div>

                <div className="home-hero__image-wrapper" data-reveal style={{ "--reveal-delay": "120ms" }}>
                    <div className="home-hero__image-box">
                        <div
                            className="home-hero__image"
                            style={{ backgroundImage: `url('${heroSrc}')` }}
                        />
                        <div className="home-hero__floating home-hero__floating--code">
                            <span className="material-symbols-outlined">terminal</span>
                            Test case feedback
                        </div>
                        <div className="home-hero__floating home-hero__floating--quiz">
                            <span className="material-symbols-outlined">quiz</span>
                            Quiz tự động
                        </div>
                        <div className="home-hero__stats">
                            <div className="home-hero__stats-icon">
                                <span className="material-symbols-outlined">verified</span>
                            </div>
                            <div className="home-hero__stats-text">
                                <p>Quiz + Code Judge</p>
                                <p>Học, luyện tập và lưu tiến độ</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
