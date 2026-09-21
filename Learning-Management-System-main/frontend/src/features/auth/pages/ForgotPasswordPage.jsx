import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/AuthPage.css';
import ForgotPassword from '../components/ForgotPassword';
import HeroPanel from '../components/HeroPanel';

function ForgotPasswordPage() {
    const navigate = useNavigate();

    // Tăng tốc độ cuộn cho form side
    const handleWheel = (e) => {
        const multiplier = 1.3;
        e.currentTarget.scrollTop += e.deltaY * multiplier;
    };

    return (
        <div className="auth-page">
            <main className="auth-layout">
                {/* Cột trái: form */}
                <section className="auth-form-side" onWheel={handleWheel}>
                    <ForgotPassword 
                        onBackToLogin={() => navigate('/login')} 
                    />
                </section>

                {/* Cột phải: hero (ẩn trên mobile) */}
                <aside className="auth-hero-side">
                    <HeroPanel activeTab="forgot" />
                </aside>
            </main>
        </div>
    );
}

export default ForgotPasswordPage;
