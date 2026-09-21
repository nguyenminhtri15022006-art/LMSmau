import React, {useEffect, useState} from 'react';
import "../styles/AuthForm.css";
import "../styles/RoleSelectionModal.css";
import Logo from './Logo';
import AuthTabs from './AuthTabs';
import SocialLogin from './Social';
import RoleSelector from './RoleSelector';
import {handleSocialLoginClick} from '../../../services/oauthUtils';

const LEGAL_CONTENT = {
    terms: {
        title: 'Điều khoản dịch vụ',
        intro: 'Khi sử dụng EduLearn, bạn đồng ý dùng nền tảng cho mục đích học tập, giảng dạy và quản lý khóa học một cách trung thực.',
        sections: [
            {
                heading: 'Tài khoản và vai trò',
                body: 'Bạn chịu trách nhiệm bảo mật tài khoản của mình, không chia sẻ thông tin đăng nhập và chọn đúng vai trò Student hoặc Instructor khi đăng ký.'
            },
            {
                heading: 'Nội dung học tập',
                body: 'Tài liệu, bài giảng, bài tập và nội dung khóa học chỉ được sử dụng trong phạm vi học tập trên EduLearn; không sao chép hoặc phân phối lại khi chưa được phép.'
            },
            {
                heading: 'Hành vi học tập',
                body: 'Người dùng không gian lận bài tập, quiz, code judge, không đăng nội dung gây hại và không can thiệp trái phép vào hệ thống.'
            },
            {
                heading: 'Xử lý vi phạm',
                body: 'EduLearn có thể giới hạn quyền truy cập, ẩn nội dung hoặc khóa tài khoản khi phát hiện hành vi vi phạm quy định của nền tảng.'
            }
        ]
    },
    privacy: {
        title: 'Chính sách bảo mật',
        intro: 'EduLearn chỉ thu thập dữ liệu cần thiết để vận hành lớp học trực tuyến, xác thực tài khoản và cải thiện trải nghiệm học tập.',
        sections: [
            {
                heading: 'Dữ liệu được thu thập',
                body: 'Hệ thống lưu thông tin đăng ký như họ tên, email, tên đăng nhập, vai trò, trạng thái xác thực và dữ liệu học tập như khóa học, bài nộp, điểm số.'
            },
            {
                heading: 'Mục đích sử dụng',
                body: 'Dữ liệu được dùng để đăng nhập, phân quyền, theo dõi tiến độ học, chấm bài, gửi thông báo khóa học và hỗ trợ người dùng khi cần.'
            },
            {
                heading: 'Bảo vệ dữ liệu',
                body: 'Token đăng nhập được lưu trong trình duyệt theo lựa chọn ghi nhớ của bạn. EduLearn không bán dữ liệu cá nhân cho bên thứ ba.'
            },
            {
                heading: 'Quyền của người dùng',
                body: 'Bạn có thể cập nhật thông tin cá nhân trong phần cài đặt và nên đăng xuất khỏi thiết bị dùng chung để bảo vệ tài khoản.'
            }
        ]
    }
};

function SignUp({onSubmit, onTabChange, isLoading, fieldErrors = {}}) {
    // ---- State ----
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [formData, setFormData] = useState({
        fullname: '',
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        role: '',
        terms: false
    })
    const [errors, setErrors] = useState({})
    const [activeLegalModal, setActiveLegalModal] = useState(null)

    useEffect(() => {
        if (!activeLegalModal) return undefined;

        const handleKeyDown = (event) => {
            if (event.key === 'Escape') {
                setActiveLegalModal(null);
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [activeLegalModal]);

    // ---- Handlers ----
    function handleChange(e) {
        const {name, value, type, checked} = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value,
        }))
        if (errors[name]) {
            setErrors((prev) => ({
                ...prev,
                [name]: ''
            }))
        }
    }

    function validateForm() {
        const newErrors = {};

        if (!formData.role) {
            newErrors.role = 'Vui lòng chọn vai trò (Student hoặc Instructor)';
        }

        if (!formData.fullname || formData.fullname.trim().length < 3) {
            newErrors.fullname = 'Vui lòng nhập tên đầy đủ (ít nhất 3 ký tự)';
        } else if (formData.fullname.trim().length > 50) {
            newErrors.fullname = 'Tên quá dài (tối đa 50 ký tự)';
        } else {
            const nameRegex = /^[A-Za-zÀ-ỹ\s]+$/;
            if (!nameRegex.test(formData.fullname)) {
                newErrors.fullname = 'Tên chỉ được chứa chữ và khoảng trắng';
            }
        }

        if (!formData.username || formData.username.trim().length < 5) {
            newErrors.username = 'Tên đăng nhập phải có ít nhất 5 ký tự';
        } else if (formData.username.trim().length > 30) {
            newErrors.username = 'Tên đăng nhập quá dài (tối đa 30 ký tự)';
        } else {
            const usernameRegex = /^[a-zA-Z0-9_]+$/;
            if (!usernameRegex.test(formData.username)) {
                newErrors.username = 'Tên đăng nhập chỉ được chứa chữ, số và dấu gạch dưới (_)';
            }
        }

        const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
        if (!emailRegex.test(formData.email)) {
            newErrors.email = 'Vui lòng nhập email hợp lệ';
        }

        if (formData.password.length < 8) {
            newErrors.password = 'Mật khẩu phải có ít nhất 8 ký tự';
        } else if (/\s/.test(formData.password)) {
            newErrors.password = 'Mật khẩu không được chứa khoảng trắng';
        }

        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Mật khẩu và Xác nhận mật khẩu không khớp';
        }

        if (!formData.terms) {
            newErrors.terms = 'Vui lòng đồng ý với Điều khoản Dịch vụ và Chính sách Bảo mật';
        }

        return newErrors;
    }

    function handleSubmit(e) {
        e.preventDefault()

        const newErrors = validateForm()

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors)
            return
        }

        onSubmit?.(formData)
    }

    // ---- Render ----
    return (
        < div className="auth-form-inner">
            <Logo/>
            <AuthTabs activeTab="register" onTabChange={onTabChange}/>
            <div className="form-heading">
            </div>
            <form className="auth-form" onSubmit={handleSubmit}>
                {/* Role Selection */}
                <div className="field-group role-selector-field">
                    <label className="field-label">Chọn vai trò đăng ký</label>
                    <RoleSelector
                        selectedRole={formData.role}
                        onSelect={(role) => {
                            setFormData(prev => ({...prev, role}));
                            if (errors.role) setErrors(prev => ({...prev, role: ''}));
                        }}
                    />
                    {errors.role && (
                        <span className="field-error">
                            {errors.role}
                        </span>
                    )}
                </div>
                {/* Full Name */}
                <div className="field-group">
                    <label className="field-label" htmlFor="fullname">
                        Họ và tên
                    </label>
                    <input
                        id="fullname"
                        name="fullname"
                        type="text"
                        className="field-input"
                        placeholder="Nhập đầy đủ họ và tên"
                        value={formData.fullname}
                        onChange={handleChange}
                        required
                    />
                    {errors.fullname && (
                        <span className="field-error">
                            {errors.fullname}
                        </span>
                    )}
                </div>
                {/* User Name */}
                <div className="field-group">
                    <label className="field-label" htmlFor="username">
                        Tên đăng nhập (5-30 ký tự, chữ cái/số/_)
                    </label>
                    <input
                        id="username"
                        name="username"
                        type="text"
                        className="field-input"
                        placeholder="Nhập đầy đủ tên đăng nhập"
                        value={formData.username}
                        onChange={handleChange}
                        required
                    />
                    {errors.username && (
                        <span className="field-error">
                            {errors.username}
                        </span>
                    )}
                    {fieldErrors.username && (
                        <span className="field-error">
                            {fieldErrors.username}
                        </span>
                    )}
                </div>
                {/* Email */}
                <div className="field-group">
                    <label className="field-label" htmlFor="email">
                        Email
                    </label>
                    <input
                        id="email"
                        name="email"
                        type="email"
                        className="field-input"
                        placeholder="Nhập email của bạn"
                        value={formData.email}
                        onChange={handleChange}
                        required
                    />
                    {errors.email && (
                        <span className="field-error">
                            {errors.email}
                        </span>
                    )}
                    {fieldErrors.email && (
                        <span className="field-error">
                            {fieldErrors.email}
                        </span>
                    )}
                </div>
                {/* Password */}
                <div className="field-group">
                    <label className="field-label" htmlFor="password">
                        Mật khẩu (tối thiểu 8 ký tự, không chứa khoảng trắng)
                    </label>
                    <div className="password-wrapper">
                        <input
                            id="password"
                            name="password"
                            type={showPassword ? 'text' : 'password'}
                            className="field-input"
                            placeholder="Nhập mật khẩu"
                            value={formData.password}
                            onChange={handleChange}
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
                            {errors.password}
                        </span>
                    )}
                </div>
                {/* Confirm Password */}
                <div className="field-group">
                    <label className="field-label" htmlFor="confirmPassword">
                        Xác nhận mật khẩu
                    </label>
                    <div className="password-wrapper">
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type={showConfirmPassword ? 'text' : 'password'}
                            className="field-input"
                            placeholder="Nhập lại mật khẩu"
                            value={formData.confirmPassword}
                            onChange={handleChange}
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
                            {errors.confirmPassword}
                        </span>
                    )}
                </div>
                {/* Terms */}
                <div className="checkbox-row">
                    <input
                        id="terms"
                        name="terms"
                        type="checkbox"
                        required
                        checked={formData.terms}
                        onChange={handleChange}
                    />
                    <div className="legal-consent-text">
                        <label htmlFor="terms">Tôi đồng ý với</label>
                        <button
                            type="button"
                            className="legal-inline-button"
                            onClick={() => setActiveLegalModal('terms')}
                        >
                            Điều khoản dịch vụ
                        </button>
                        <span>và</span>
                        <button
                            type="button"
                            className="legal-inline-button"
                            onClick={() => setActiveLegalModal('privacy')}
                        >
                            Chính sách bảo mật
                        </button>
                    </div>
                    {errors.terms && (
                        <span className="field-error">
                            {errors.terms}
                        </span>
                    )}
                </div>
                {/* Submit */}
                <button type="submit" className="btn-primary" disabled={isLoading}>
                    {isLoading ? ' Đang đăng ký...' : 'Đăng Ký'}
                </button>
            </form>
            {/* Help text */}
            <p className="form-help-text">
                Đã có tài khoản?{' '}
                <a href="#" onClick={(e) => {
                    e.preventDefault();
                    onTabChange?.('login');
                }}>
                    Đăng nhập
                </a>
            </p>
            {/* Social login */}
            <SocialLogin onSocialLogin={handleSocialLoginClick}/>

            {activeLegalModal && (
                <div className="legal-modal-overlay" onClick={() => setActiveLegalModal(null)}>
                    <section
                        className="legal-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="legal-modal-title"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="legal-modal-header">
                            <div>
                                <p>EduLearn</p>
                                <h2 id="legal-modal-title">{LEGAL_CONTENT[activeLegalModal].title}</h2>
                            </div>
                            <button
                                type="button"
                                className="legal-modal-close"
                                aria-label="Đóng"
                                onClick={() => setActiveLegalModal(null)}
                            >
                                <span className="material-symbols-outlined">close</span>
                            </button>
                        </div>
                        <p className="legal-modal-intro">{LEGAL_CONTENT[activeLegalModal].intro}</p>
                        <div className="legal-modal-body">
                            {LEGAL_CONTENT[activeLegalModal].sections.map((section) => (
                                <article key={section.heading} className="legal-modal-section">
                                    <h3>{section.heading}</h3>
                                    <p>{section.body}</p>
                                </article>
                            ))}
                        </div>
                        <button
                            type="button"
                            className="legal-modal-confirm"
                            onClick={() => setActiveLegalModal(null)}
                        >
                            Tôi đã hiểu
                        </button>
                    </section>
                </div>
            )}
        </div>
    )
}

export default SignUp;

