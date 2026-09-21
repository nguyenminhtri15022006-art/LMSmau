// components/HeroPanel.jsx
// ==================================
// Cột phải: nền tối galaxy
// Không nhận props - chỉ hiển thị
// ==================================

import React from 'react';
import "../styles/HeroPanel.css";
import imgLogin from '../../../assets/auth/anhLogin.png';
import imgSignUp from '../../../assets/auth/anhSignUp.png';
import imgForgot from '../../../assets/auth/anhForgetPass.png';
import imgReset from '../../../assets/auth/anhResetPass.png';

// Ảnh avatar community (dùng placeholder nếu không có asset thật)
const AVATARS = [
    { src: 'https://i.pravatar.cc/40?img=1', alt: 'User 1' },
    { src: 'https://i.pravatar.cc/40?img=2', alt: 'User 2' },
    { src: 'https://i.pravatar.cc/40?img=3', alt: 'User 3' },
];

// Vị trí các ngôi sao nhỏ trang trí
const STARS = [
    { top: '25%', left: '50%', size: '4px' },
    { top: '33%', left: '25%', size: '4px' },
    { top: '75%', right: '33%', size: '6px' },
    { top: '50%', right: '25%', size: '4px' },
];

function HeroPanel({ activeTab }) {
    let heroImage = imgLogin;
    if (activeTab === 'register') heroImage = imgSignUp;
    if (activeTab === 'forgot') heroImage = imgForgot;
    if (activeTab === 'reset') heroImage = imgReset;
    return (
        <div className="hero-panel">

            {/* Layer 0: gradient nền */}
            <div className="hero-bg-gradient" />

            {/* Layer 1: glow blobs */}
            <div className="hero-glow hero-glow--top" />
            <div className="hero-glow hero-glow--bottom" />

            {/* Layer 2: ngôi sao nhỏ */}
            <div className="hero-stars">
                {STARS.map((s, i) => (
                    <div
                        key={i}
                        className="hero-star"
                        style={{ ...s, width: s.size, height: s.size }}
                    />
                ))}
            </div>

            {/* Layer 3: nội dung chính */}
            <div className="hero-content">

                {/* Globe / Galaxy image */}
                <div className="hero-globe">
                    <div className="hero-globe-ring">
                        <div className="hero-globe-inner-glow" />
                        <img
                            className="hero-galaxy-img"
                            src={heroImage}
                            alt="Image Galaxy"
                        />
                    </div>

                    {/* Decorative planets */}
                    <div className="hero-planet hero-planet--purple" />
                    <div className="hero-planet hero-planet--blue" />
                </div>

                {/* Text */}
                <h2 className="hero-title">Khám phá vũ trụ tri thức</h2>
                <p className="hero-subtitle">
                    Hãy tham gia cộng đồng học tập toàn cầu và bắt đầu hành trình khám phá tri thức ngay hôm nay!
                </p>

                {/* Community */}
                <div className="hero-community">
                    <div className="hero-avatars">
                        {AVATARS.map((av) => (
                            <img
                                key={av.alt}
                                className="hero-avatar"
                                src={av.src}
                                alt={av.alt}
                            />
                        ))}
                    </div>
                    <div className="hero-community-text">
                        <strong>12k+ Thành viên</strong>
                        <span>Tham gia cộng đồng ngay hôm nay</span>
                    </div>
                </div>

            </div>
        </div>
    );
}

export default HeroPanel;
