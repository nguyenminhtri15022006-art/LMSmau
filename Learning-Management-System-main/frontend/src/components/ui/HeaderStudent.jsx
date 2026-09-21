import React, {useEffect, useRef, useState} from "react";
import {Link, useLocation} from "react-router-dom";
import "./styles/HeaderStudentFrom.css";
import useAuth from "../../hooks/useAuth";
import useTheme from "../../hooks/useTheme";

const PUBLIC_LINKS = [
    {label: "Trang chủ", href: "/"},
    {label: "Khóa học", href: "/list-course"},
    {label: "Bài viết", href: "/blog"},
    {label: "Về chúng tôi", href: "/about-us"},
];

const STUDENT_LINKS = [
    {label: "Trang chủ", href: "/dashboard"},
    {label: "Khóa học", href: "/list-course"},
    {label: "Khóa học của tôi", href: "/my-courses"},
    {label: "Bài tập", href: "/exercises"},
    {label: "Bài viết", href: "/blog"},
];

const INSTRUCTOR_LINKS = [
    {label: "Trang chủ", href: "/instructor/home"},
    {label: "Quản lý khóa học", href: "/manage/courses"},
    {label: "Quản lý bài tập", href: "/instructor/exercises"},
    {label: "Bài viết", href: "/instructor/blog"},
];

const ADMIN_LINKS = [
    {label: "Trang chủ", href: "/admin/home"},
    {label: "Quản lý khóa học", href: "/admin/all-courses"},
    {label: "Quản lý người dùng", href: "/admin/users"},
    {label: "Bài viết", href: "/instructor/blog"},
    {label: "Bài tập", href: "/instructor/exercises"},
];

export default function Header() {
    const location = useLocation();
    const {user, isAuthenticated, logout} = useAuth();
    const {isDark, toggleTheme} = useTheme();

    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [imgError, setImgError] = useState(false);
    const dropdownRef = useRef(null);

    // Dynamic Home Link based on role
    const getHomeUrl = () => {
        if (!isAuthenticated) return "/";
        if (user?.role === 'INSTRUCTOR') return "/instructor/home";
        if (user?.role === 'ADMIN') return "/admin/home";
        return "/dashboard";
    };

    const homeUrl = getHomeUrl();
    const isActiveLink = (href) => {
        if (href === "/") return location.pathname === "/";
        return location.pathname === href || location.pathname.startsWith(`${href}/`);
    };

    // Close dropdown when clicking outside
    useEffect(() => {
        function handleClickOutside(event) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsDropdownOpen(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <header className="header">
            <div className="header__inner">
                {/* Logo */}
                <div className="header__logo">
                    <div className="header__logo-icon">
                        <Link to={homeUrl}>
                            <img src="/LMSicon.png" alt="LMSicon Logo" style={{width: "60px", height: "50px"}}/>
                        </Link>
                    </div>
                    <Link to={homeUrl}>
                        <h2 className="header__title">DevNest</h2>
                    </Link>
                </div>

                {/* Navigation */}
                <nav className="header__nav">
                    {(() => {
                        let links = isAuthenticated ? STUDENT_LINKS : PUBLIC_LINKS;
                        if (user?.role === 'INSTRUCTOR') links = INSTRUCTOR_LINKS;
                        if (user?.role === 'ADMIN') links = ADMIN_LINKS;

                        return links.map((link) => (
                            <Link
                                key={link.label}
                                to={link.href}
                                className={`header__nav-link ${isActiveLink(link.href) ? "active" : ""}`}
                            >
                                {link.label}
                            </Link>
                        ));
                    })()}
                </nav>

                {/* Auth & Profile Section */}
                <div className="header__actions">
                    <button
                        type="button"
                        className="theme-toggle"
                        onClick={toggleTheme}
                        title={isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối"}
                        aria-label={isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối"}
                    >
                        <span className="material-symbols-outlined">{isDark ? "light_mode" : "dark_mode"}</span>
                    </button>

                    {!isAuthenticated ? (
                        <div className="header_login_register">
                            <div className="Login-btn">
                                <Link to="/login">
                                    <button>Đăng nhập</button>
                                </Link>
                            </div>
                            <div className="Register-btn">
                                <Link to="/register">
                                    <button>Đăng ký</button>
                                </Link>
                            </div>
                        </div>
                    ) : (
                        <div className="header__profile" ref={dropdownRef}>
                            <div className="profile-btn" onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
                                <div className="profile-avatar" style={{
                                    background: 'linear-gradient(135deg,#4299e1,#2563eb)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    color: 'white',
                                    fontWeight: '700',
                                    fontSize: '0.9rem'
                                }}>
                                    {user?.avatarUrl && !imgError ? (
                                        <img src={user.avatarUrl} alt="Profile" referrerPolicy="no-referrer"
                                             crossOrigin="anonymous" onError={() => setImgError(true)} style={{
                                            width: '100%',
                                            height: '100%',
                                            borderRadius: '50%',
                                            objectFit: 'cover'
                                        }}/>
                                    ) : (
                                        <span className="material-symbols-outlined"
                                              style={{fontSize: '1.2rem'}}>person</span>
                                    )}
                                </div>
                                <span className="profile-name">{user?.fullname || user?.username || 'User'}</span>
                                <span className="material-symbols-outlined profile-chevron">expand_more</span>
                            </div>

                            {/* Dropdown Menu */}
                            {isDropdownOpen && (
                                <div className="profile-dropdown">
                                    <div className="profile-dropdown-header">
                                        <div className="profile-avatar" style={{
                                            background: 'linear-gradient(135deg,#4299e1,#2563eb)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            color: 'white',
                                            fontWeight: '700',
                                            fontSize: '1rem',
                                            width: '40px',
                                            height: '40px'
                                        }}>
                                            {user?.avatarUrl && !imgError ? (
                                                <img src={user.avatarUrl} alt="Profile" referrerPolicy="no-referrer"
                                                     crossOrigin="anonymous" onError={() => setImgError(true)} style={{
                                                    width: '100%',
                                                    height: '100%',
                                                    borderRadius: '50%',
                                                    objectFit: 'cover'
                                                }}/>
                                            ) : (
                                                <span className="material-symbols-outlined"
                                                      style={{fontSize: '1.5rem'}}>person</span>
                                            )}
                                        </div>
                                        <div className="profile-dropdown-header-info">
                                            <span className="name">{user?.fullname || user?.username || 'User'}</span>
                                            <span className="email">{user?.email || 'user@example.com'}</span>
                                        </div>
                                    </div>
                                    <div className="profile-dropdown-body">
                                        <Link to="/settings" className="profile-dropdown-item"
                                              onClick={() => setIsDropdownOpen(false)}>Cài đặt</Link>
                                        <button
                                            className="profile-dropdown-item logout"
                                            onClick={() => {
                                                setIsDropdownOpen(false);
                                                logout();
                                            }}
                                        >
                                            Đăng xuất
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}
