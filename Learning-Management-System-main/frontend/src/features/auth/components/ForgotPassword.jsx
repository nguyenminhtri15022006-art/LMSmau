import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import "../styles/AuthForm.css";
import Logo from './Logo';
import { ENDPOINTS } from '../../../constants/endpoints';

function ForgotPassword({ onBackToLogin }) {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        email: '',
        otpCode: '',
        password: '',
        confirmPassword: ''
    });

    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [otpLoading, setOtpLoading] = useState(false);

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [errors, setErrors] = useState({});

    // Xử lý thay đổi input
    const handleChange = (e) => {
        const { id, value } = e.target;
        setFormData(prev => ({ ...prev, [id]: value }));
        if (errors[id]) {
            setErrors(prev => ({ ...prev, [id]: '' }));
        }
    };

    // Gửi OTP
    const handleRequestOTP = async (e) => {
        e.preventDefault();
        const newErrors = {};
        const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

        if (!formData.email || !emailRegex.test(formData.email)) {
            newErrors.email = 'Vui lòng nhập email hợp lệ';
            setErrors(newErrors);
            return;
        }

        setOtpLoading(true);
        setError('');
        setMessage('');
        setErrors({});

        try {
            await axios.post(
                ENDPOINTS.AUTH.FORGOT_PASSWORD,
                { email: formData.email }
            );
            setMessage('OTP sent to your email. Please check and enter the code.');
        } catch (err) {
            const errorMsg = err.response?.data?.message || 'Failed to send OTP. Please try again.';
            setError(errorMsg);
        } finally {
            setOtpLoading(false);
        }
    };

    // Reset Password
    const handleSubmit = async (e) => {
        e.preventDefault();
        const newErrors = {};

        const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
        if (!formData.email || !emailRegex.test(formData.email)) {
            newErrors.email = 'Vui lòng nhập email hợp lệ';
        }
        if (!formData.otpCode || formData.otpCode.length < 4) {
            newErrors.otpCode = 'Vui lòng nhập mã OTP';
        }
        if (formData.password.length < 8) {
            newErrors.password = 'Mật khẩu phải có ít nhất 8 ký tự';
        }
        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Mật khẩu và Xác nhận mật khẩu không khớp';
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(prev => ({ ...prev, ...newErrors }));
            return;
        }

        setLoading(true);
        setError('');
        setMessage('');

        try {
            await axios.post(
                ENDPOINTS.AUTH.RESET_PASSWORD,
                {
                    email: formData.email,
                    otp: formData.otpCode,
                    newPassword: formData.password,
                    confirmPassword: formData.confirmPassword
                }
            );
            setMessage('Password reset successful! Redirecting to Login...');
            setTimeout(() => {
                if (onBackToLogin) onBackToLogin();
                else navigate('/login');
            }, 1000);
        } catch (err) {
            const errorMsg = err.response?.data?.message || 'Password reset failed. Please try again.';
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-form-inner">
            <Logo />

            <div className="form-heading">
                <h1>Quên mật khẩu</h1>
                <p>Nhập thông tin bên dưới để đặt lại mật khẩu của bạn.</p>
            </div>

            <form onSubmit={handleSubmit} className="auth-form">

                {/* Email & Get OTP */}
                <div className="field-group">
                    <label htmlFor="email" className="field-label">
                        Email
                    </label>
                    <div className="field-row">
                        <input
                            type="email"
                            id="email"
                            value={formData.email}
                            onChange={handleChange}
                            className="field-input flex-1"
                            placeholder="Nhập email của bạn"
                            required
                        />
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={handleRequestOTP}
                            disabled={otpLoading}
                        >
                            {otpLoading ? 'Đang gửi...' : 'Gửi mã OTP'}
                        </button>
                    </div>
                    {errors.email && (
                        <span className="field-error">
                            {errors.email}
                        </span>
                    )}
                </div>

                {/* OTP Code */}
                <div className="field-group">
                    <label htmlFor="otpCode" className="field-label">
                        OTP Code
                    </label>
                    <input
                        type="text"
                        id="otpCode"
                        value={formData.otpCode}
                        onChange={handleChange}
                        className="field-input"
                        placeholder="Nhập mã OTP"
                        maxLength={6}
                        required
                    />
                    {errors.otpCode && (
                        <span className="field-error">
                            ⚠️ {errors.otpCode}
                        </span>
                    )}
                </div>

                {/* New Password */}
                <div className="field-group">
                    <label htmlFor="password" className="field-label">
                        Mật khẩu mới (tối thiểu 8 ký tự)
                    </label>
                    <div className="password-wrapper">
                        <input
                            type={showPassword ? 'text' : 'password'}
                            id="password"
                            value={formData.password}
                            onChange={handleChange}
                            className="field-input"
                            placeholder="Nhập mật khẩu mới"
                            required
                        />
                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowPassword((v) => !v)}
                            aria-label={showPassword ? 'Hide password' : 'Show password'}
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

                {/* Confirm Password */}
                <div className="field-group">
                    <label htmlFor="confirmPassword" className="field-label">
                        Xác nhận mật khẩu
                    </label>
                    <div className="password-wrapper">
                        <input
                            type={showConfirmPassword ? 'text' : 'password'}
                            id="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            className="field-input"
                            placeholder="Xác nhận mật khẩu"
                            required
                        />
                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowConfirmPassword((v) => !v)}
                            aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                        >
                            <span className="material-symbols-outlined">
                                {showConfirmPassword ? 'visibility_off' : 'visibility'}
                            </span>
                        </button>
                    </div>
                    {errors.confirmPassword && (
                        <span className="field-error">
                            ⚠️ {errors.confirmPassword}
                        </span>
                    )}
                </div>

                {message && (
                    <div style={{ color: 'var(--color-success, #22c55e)', fontSize: '14px', marginTop: '10px' }}>
                        {message}
                    </div>
                )}
                {error && (
                    <div style={{ color: 'var(--color-error, #ef4444)', fontSize: '14px', marginTop: '10px' }}>
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    className="btn-primary"
                    disabled={loading}
                    style={{ marginTop: '10px' }}
                >
                    {loading ? 'Đang xử lý...' : 'Đặt lại mật khẩu'}
                </button>
            </form>

            <p className="form-help-text">
                Nhớ mật khẩu?{' '}
                <a href="#" onClick={(e) => {
                    e.preventDefault();
                    if (onBackToLogin) onBackToLogin();
                    else navigate('/login');
                }}>
                    Quay lại đăng nhập
                </a>
            </p>
        </div>
    );
}

export default ForgotPassword;
