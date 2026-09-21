import React from "react";
import { Link } from "react-router-dom";
import "../styles/PracticeSection.css";

export default function PracticeSection() {
    return (
        <section className="home-practice">
            <div className="home-practice__inner">
                <div className="home-practice__decor" />
                <div className="home-practice__grid">
                    <div data-reveal>
                        <span className="home-practice__eyebrow">Practice engine</span>
                        <h2 className="home-practice__title">Học đến đâu, thực hành đến đó</h2>
                        <p className="home-practice__desc">
                            Mỗi khóa học có thể đi kèm bài quiz và bài lập trình để người học
                            kiểm tra kiến thức ngay sau khi học. Hệ thống chấm code tự động giúp
                            phản hồi nhanh, giảm phụ thuộc vào chấm thủ công.
                        </p>
                        <div className="home-practice__actions">
                            <Link to="/exercises" className="home-practice__btn home-practice__btn--white">
                                <span className="material-symbols-outlined">play_arrow</span>
                                Luyện tập ngay
                            </Link>
                            <Link to="/list-course" className="home-practice__btn home-practice__btn--outline">
                                Xem khóa học
                            </Link>
                        </div>
                    </div>
                    <div className="home-practice__cards">
                        <div className="home-practice-card" data-reveal style={{ "--reveal-delay": "90ms" }}>
                            <div className="home-practice-card__icon">
                                <span className="material-symbols-outlined">code</span>
                            </div>
                            <h3 className="home-practice-card__title">Coding Challenge</h3>
                            <p className="home-practice-card__desc">
                                Viết code, chạy thử, nộp bài và nhận kết quả dựa trên test case.
                            </p>
                            <div className="home-practice-card__meta">
                                <span>Judge0</span>
                                <span>Test cases</span>
                            </div>
                            <Link to="/exercises" className="home-practice-card__link">
                                Luyện code <span className="material-symbols-outlined">arrow_outward</span>
                            </Link>
                        </div>
                        <div className="home-practice-card home-practice-card--quiz" data-reveal style={{ "--reveal-delay": "160ms" }}>
                            <div className="home-practice-card__icon">
                                <span className="material-symbols-outlined">quiz</span>
                            </div>
                            <h3 className="home-practice-card__title">Quiz kiểm tra kiến thức</h3>
                            <p className="home-practice-card__desc">
                                Làm trắc nghiệm theo chủ đề, xem kết quả và lịch sử làm bài.
                            </p>
                            <div className="home-practice-card__meta">
                                <span>Auto grade</span>
                                <span>History</span>
                            </div>
                            <Link to="/exercises" className="home-practice-card__link">
                                Làm quiz <span className="material-symbols-outlined">arrow_outward</span>
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
