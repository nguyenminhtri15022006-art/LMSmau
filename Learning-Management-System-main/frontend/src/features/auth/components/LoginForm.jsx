// components/LoginForm.jsx
// ==================================
// Form đăng nhập chính
// Props:
//   onSubmit: (formData) => void
// ==================================

import React, { useState } from 'react';
import "../styles/AuthForm.css";
import Logo from './Logo';
import AuthTabs from './AuthTabs';
import SocialLogin from './Social';
import { handleSocialLoginClick } from '../../../services/oauthUtils';
import { useLocation, useNavigate } from 'react-router-dom';

const getPathFromLocation = (value) => {
    if (!value) return null;
    if (typeof value === 'string') return value;
    const pathname = value.pathname || '';
    if (!pathname) return null;
    return `${pathname}${value.search || ''}${value.hash || ''}`;
};

function LoginForm({ onSubmit, onTabChange }) {
    const navigate = useNavigate();
    const location = useLocation();
    // ---- State ----
    const [showPassword, setShowPassword] = useState(false);
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        remember: false,
    });
    const [errors, setErrors] = useState({});

    // ---- Handlers ----
    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value,
        }));
        // Xóa lỗi khi người dùng thay đổi field
        if (errors[name]) {
            setErrors((prev) => ({
                ...prev,
                [name]: ''
            }));
        }
    }

    function validateForm() {
        const newErrors = {};

        if (!formData.username || formData.username.trim() === '') {
            newErrors.username = 'Vui lòng nhập tên đăng nhập hoặc email';
        }

        if (!formData.password || formData.password.trim() === '') {
            newErrors.password = 'Vui lòng nhập mật khẩu';
        }

        return newErrors;
    }

    function handleSubmit(e) {
        e.preventDefault();
        const newErrors = validateForm();

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        onSubmit?.(formData.username, formData.password, formData.remember);
    }



    // ---- Render ----
    return (
        <div className="auth-form-inner">

            {/* Logo */}
            <Logo />

            {/* Tabs */}
            <AuthTabs activeTab="login" onTabChange={onTabChange} />

            {/* Heading */}
            <div className="form-heading">
                <h1>Chào mừng trở lại với EduLearn</h1>
                <p>Vui lòng nhập thông tin để đăng nhập vào tài khoản EduLearn của bạn.</p>
            </div>

            {/* Form */}
            <form className="auth-form" onSubmit={handleSubmit}>

                {/* Email or Username */}
                <div className="field-group">
                    <label className="field-label" htmlFor="identifier">
                        Tên đăng nhập/Email
                    </label>
                    <input
                        id="identifier"
                        name="username"
                        type="text"
                        className="field-input"
                        placeholder="name@example.com or username"
                        value={formData.username}
                        onChange={handleChange}
                        required
                    />
                    {errors.username && (
                        <span className="field-error">
                            ⚠️ {errors.username}
                        </span>
                    )}
                </div>

                {/* Password */}
                <div className="field-group">
                    <label className="field-label" htmlFor="password">
                        Mật khẩu
                    </label>
                    <div className="password-wrapper">
                        <input
                            id="password"
                            name="password"
                            type={showPassword ? 'text' : 'password'}
                            className="field-input"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={handleChange}
                            required
                        />
                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowPassword((v) => !v)}
                            aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        >
                            <span className="material-symbols-outlined">
                                {showPassword ? 'visibility_off' : 'visibility'}
                            </span>
                        </button>
                    </div>
                    {errors.password && (
                        <span className="field-error">
                            ⚠️ {errors.password}
                        </span>
                    )}
                </div>

                {/* Remember me */}
                <div className="checkbox-row">
                    <input
                        id="remember"
                        name="remember"
                        type="checkbox"
                        checked={formData.remember}
                        onChange={handleChange}
                    />
                    <label htmlFor="remember">Duy trì đăng nhập trong 30 ngày</label>
                </div>

                {/* Submit */}
                <button type="submit" className="btn-primary">
                    Đăng nhập
                </button>
            </form>

            {/* Help text */}
            <p className="form-help-text">
                Bạn gặp khó khăn khi đăng nhập?{' '}
                <a href="#" className="forgot-link" onClick={(e) => {
                    e.preventDefault();
                    navigate('/forgot-password');
                }}>
                    Quên mật khẩu
                </a>
            </p>

            {/* Social login */}
            <SocialLogin
                onSocialLogin={(provider) => handleSocialLoginClick(
                    provider,
                    getPathFromLocation(location.state?.from),
                )}
            />

        </div>
    );
}

export default LoginForm;
