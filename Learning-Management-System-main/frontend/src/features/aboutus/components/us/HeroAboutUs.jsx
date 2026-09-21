import React, { useState } from "react";
import { Link } from "react-router-dom";
import "../../styles/us/HeroAboutUs.css";

export default function Hero() {
    const [images] = useState(() => {
        const saved = localStorage.getItem('web_content_inspire');
        return saved ? JSON.parse(saved) : [];
    });

    const aboutImg = [...images].reverse().find(img => img.position === 'About Us Banner' && img.status === 'Hiển thị');
    const aboutSrc = aboutImg ? aboutImg.imageUrl : "https://res.cloudinary.com/drjgjihjh/image/upload/q_auto/f_auto/v1775215503/Aboutus_pvuxks.jpg";

    return (
        <section className="hero about-hero">
            <div className="hero__inner">
                <div className="hero__content" data-reveal>
                    <span className="about-hero__eyebrow">Learning Management System</span>
                    <h1 className="hero__title">
                        EduLearn - nền tảng LMS cho <span>học tập và thực hành lập trình</span>
                    </h1>
                    <p className="hero__desc">
                        Dự án được xây dựng nhằm tạo ra một môi trường học trực tuyến có đủ khóa học,
                        bài giảng, bài tập, chấm code, quiz và cộng đồng trao đổi trong cùng một hệ thống.
                    </p>

                    <div className="hero__button">
                        <Link to="/list-course" className="btn-primary">
                            <span className="material-symbols-outlined">school</span>
                            Khám phá khóa học
                        </Link>
                        <a href="#about-system" className="btn-secondary">Tìm hiểu hệ thống</a>
                    </div>
                </div>

                <div className="hero__image-wrapper" data-reveal style={{ "--reveal-delay": "120ms" }}>
                    <div className="hero__image-box">
                        <div
                            className="hero__image-img"
                            style={{ backgroundImage: `url('${aboutSrc}')` }}
                        />
                        <div className="about-hero__badge about-hero__badge--course">
                            <span className="material-symbols-outlined">menu_book</span>
                            Course Service
                        </div>
                        <div className="about-hero__badge about-hero__badge--judge">
                            <span className="material-symbols-outlined">code</span>
                            Code Judge
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
