import React from "react";
import {Link} from "react-router-dom";
import "./styles/FooterStudent.css";
import useAuth from "../../hooks/useAuth";

/**
 * Footer component adaptable for different user roles.
 *
 * @param {{role: string}} props - Role of the current user. Accepted values:
 *   "student" – default student footer.
 *   "instructor" – instructor‑specific footer.
 *   Future roles can be added here.
 */
export default function Footer({role = "student"}) {
    const {isAuthenticated, user} = useAuth();
    const isInstructor = role === "instructor" || user?.role === "INSTRUCTOR";

    const publicExplore = [
        {to: "/", label: "Trang chủ"},
        {to: "/list-course", label: "Khóa học"},
        {to: "/blog", label: "Bài viết"},
        {to: "/about-us", label: "Về chúng tôi"},
    ];

    const studentExplore = [
        {to: "/dashboard", label: "Trang chủ"},
        {to: "/list-course", label: "Khóa học"},
        {to: "/my-courses", label: "Khóa học của tôi"},
        {to: "/exercises", label: "Bài tập"},
        {to: "/blog", label: "Bài viết"},
    ];

    const supportLinks = [
        {to: "/settings", label: "Cài đặt"},
        {to: "/about-us", label: "Về chúng tôi"},
    ];

    // Instructor‑specific overrides – customize as needed.
    const instructorExplore = [
        {to: "/instructor/home", label: "Trang quản trị"},
        {to: "/manage/courses", label: "Quản lý khóa học"},
        {to: "/instructor/exercises", label: "Quản lý bài tập"},
        {to: "/instructor/blog", label: "Bài viết"},
    ];

    const instructorSupport = [
        {to: "/settings", label: "Cài đặt"},
    ];

    const exploreLinks = isInstructor
        ? instructorExplore
        : isAuthenticated
            ? studentExplore
            : publicExplore;

    const activeSupportLinks = isInstructor
        ? instructorSupport
        : isAuthenticated
            ? supportLinks
            : [{to: "/about-us", label: "Về chúng tôi"}];

    const renderLinks = (heading, links) => (
        <div>
            <h4 className="footer__heading">{heading}</h4>
            <ul className="footer__links">
                {links.map((link) => (
                    <li key={link.label}>
                        {link.to.startsWith("#") ? (
                            <a href={link.to} className="footer__link">{link.label}</a>
                        ) : (
                            <Link to={link.to} className="footer__link">{link.label}</Link>
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );

    return (
        <footer className="footer">
            <div className="footer__inner">
                <div className="footer__grid">
                    {/* Logo & description */}
                    <div className="footer__col-1">
                        <Link to="/" className="footer__logo" style={{textDecoration: 'none'}}>
                            <div className="footer__logo-icon">
                                <img src="/LMSicon.png" alt="LMSicon Logo" style={{width: "24px", height: "24px"}}/>
                            </div>
                            <h2 className="footer__title">DevNest</h2>
                        </Link>
                        <p className="footer__desc">
                            Trao quyền cho người học trên toàn thế giới thông qua giáo dục trực tuyến chất lượng cao, dễ
                            tiếp cận.
                        </p>
                        <div className="footer__socials">
                            <a href="#" className="social-btn"><span className="material-symbols-outlined">public</span></a>
                            <a href="#" className="social-btn"><span className="material-symbols-outlined">share</span></a>
                        </div>
                    </div>

                    {/* Explore Section */}
                    {renderLinks("Khám phá", exploreLinks)}

                    {/* Support Section */}
                    {renderLinks("Hỗ trợ", activeSupportLinks)}

                    {/* Subscribe Section – kept the same for both roles */}
                    <div>
                        <h4 className="footer__heading">Đăng ký</h4>
                        <p className="footer__desc" style={{marginBottom: "16px"}}>
                            Luôn cập nhật với các khóa học mới nhất và tin tức giáo dục của chúng tôi.
                        </p>
                        <form className="footer__form" onSubmit={e => e.preventDefault()}>
                            <input type="email" placeholder="Enter your email" className="footer__input"/>
                            <button className="footer__submit">Đăng ký</button>
                        </form>
                    </div>
                </div>

                {/* Bottom legal section */}
                <div className="footer__bottom">
                    <p className="footer__copyright">© {new Date().getFullYear()} HVP LMS Inc. Bảo lưu mọi
                        quyền.</p>
                    <div className="footer__legal">
                        <a href="#">Chính sách bảo mật</a>
                        <a href="#">Điều khoản sử dụng</a>
                        <a href="#">Chính sách cookie</a>
                    </div>
                </div>
            </div>
        </footer>
    );
}
