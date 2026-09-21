import React, { useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { authApi } from '../../../services/auth.api';

// =============================================
// Security Settings — Email Verification + Change Password
// =============================================

export default function SecuritySection() {
    const { user, updateUser, logout } = useAuth();

    // ---- Email Verification state ----
    const [verifySending, setVerifySending] = useState(false);
    const [verifySubmitting, setVerifySubmitting] = useState(false);
    const [verifyOtp, setVerifyOtp] = useState('');
    const [verifyMessage, setVerifyMessage] = useState('');
    const [verifyError, setVerifyError] = useState('');

    // ---- Change Password state ----
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordSubmitting, setPasswordSubmitting] = useState(false);
    const [passwordMessage, setPasswordMessage] = useState('');
    const [passwordError, setPasswordError] = useState('');

    const isVerified = user?.verified === true;

    // ---- Email Verification handlers ----

    const handleSendVerifyEmail = async () => {
        setVerifyMessage('');
        setVerifyError('');
        setVerifySending(true);
        try {
            await authApi.sendVerifyEmail(user.email);
            setVerifyMessage('Mã xác minh đã được gửi đến email của bạn.');
        } catch (err) {
            setVerifyError(err.response?.data?.message || 'Gửi mã xác minh thất bại.');
        } finally {
            setVerifySending(false);
        }
    };

    const handleVerifyEmail = async (e) => {
        e.preventDefault();
        if (!verifyOtp || verifyOtp.length !== 6) {
            setVerifyError('Vui lòng nhập mã xác minh gồm 6 chữ số.');
            return;
        }
        setVerifyMessage('');
        setVerifyError('');
        setVerifySubmitting(true);
        try {
            const response = await authApi.verifyEmail(user.email, verifyOtp);
            setVerifyMessage(response.data?.message || 'Xác minh email thành công.');
            setVerifyOtp('');
            updateUser({ ...user, verified: true });
        } catch (err) {
            setVerifyError(err.response?.data?.message || 'Xác minh thất bại.');
        } finally {
            setVerifySubmitting(false);
        }
    };

    // ---- Change Password handlers ----

    const handleUpdatePassword = async (e) => {
        e.preventDefault();
        setPasswordMessage('');
        setPasswordError('');

        if (!oldPassword || !newPassword || !confirmPassword) {
            setPasswordError('Tất cả các trường đều bắt buộc.');
            return;
        }
        if (newPassword.length < 8) {
            setPasswordError('Mật khẩu mới phải có ít nhất 8 ký tự.');
            return;
        }
        if (newPassword !== confirmPassword) {
            setPasswordError('Mật khẩu mới và mật khẩu xác nhận không khớp.');
            return;
        }

        setPasswordSubmitting(true);
        try {
            await authApi.updatePassword({
                oldPassword,
                newPassword,
                confirmPassword
            });
            setPasswordMessage('Cập nhật mật khẩu thành công. Đang chuyển hướng...');
            setTimeout(async () => {
                await logout();
            }, 2000);
        } catch (err) {
            setPasswordError(err.response?.data?.message || 'Cập nhật mật khẩu thất bại.');
        } finally {
            setPasswordSubmitting(false);
        }
    };

    // ---- Render ----

    return (
        <div className="settings-section">
            <div className="settings-section-header">
                <h2>Bảo mật tài khoản</h2>
                <p>Quản lý mật khẩu và các tùy chọn bảo mật tài khoản của bạn.</p>
            </div>

            {/* ==========================================
          EMAIL VERIFICATION SECTION
          ========================================== */}
            <div className="security-card security-email-card" style={{
                background: 'white',
                border: '1px solid #e2e8f0',
                marginBottom: '24px'
            }}>
                <div className="security-card-header" style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    marginBottom: '24px'
                }}>
                    <span className="material-symbols-outlined" style={{ color: '#2563eb' }}>
                        verified_user
                    </span>
                    <div style={{ flex: 1 }}>
                        <h3 style={{ fontSize: '18px', fontWeight: 700, color: '#1e293b', margin: 0 }}>
                            Xác thực Email
                        </h3>
                        <p style={{ color: '#64748b', fontSize: '13px', margin: '4px 0 0' }}>
                            {isVerified
                                ? 'Email của bạn đã được xác thực.'
                                : 'Xác thực email để bảo mật tài khoản của bạn và truy cập tất cả các tính năng.'}
                        </p>
                    </div>
                    <span className={`verification-status-badge ${isVerified ? 'verified' : 'unverified'}`}>
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                            {isVerified ? 'check_circle' : 'warning'}
                        </span>
                        {isVerified ? 'Đã xác thực' : 'Chưa xác thực'}
                    </span>
                </div>

                {!isVerified && (
                    <>
                        <div className="security-email-otp-panel" style={{ borderTop: '1px solid #f1f5f9', paddingTop: '20px' }}>
                            <div className="form-group" style={{ marginBottom: '16px' }}>
                                <label>Mã xác minh OTP</label>
                                <div className="otp-input-group">
                                    <input
                                        type="text"
                                        placeholder="Nhập mã gồm 6 chữ số"
                                        maxLength={6}
                                        value={verifyOtp}
                                        onChange={(e) => setVerifyOtp(e.target.value.replace(/\D/g, ''))}
                                    />
                                    <button
                                        type="button"
                                        className="btn-send-otp"
                                        onClick={handleSendVerifyEmail}
                                        disabled={verifySending}
                                    >
                                        {verifySending ? 'Đang gửi...' : 'Gửi mã'}
                                    </button>
                                </div>
                            </div>

                            <button
                                className="btn-primary"
                                onClick={handleVerifyEmail}
                                disabled={verifySubmitting || verifyOtp.length !== 6}
                            >
                                <span className="material-symbols-outlined"
                                    style={{ fontSize: '18px', verticalAlign: 'middle', marginRight: '6px' }}>
                                    verified
                                </span>
                                {verifySubmitting ? 'Đang xác thực...' : 'Xác thực Email'}
                            </button>
                        </div>

                        {verifyMessage &&
                            <div className="form-message success" style={{ marginTop: '16px' }}>{verifyMessage}</div>}
                        {verifyError &&
                            <div className="form-message error" style={{ marginTop: '16px' }}>{verifyError}</div>}
                    </>
                )}
            </div>

            {/* ==========================================
          CHANGE PASSWORD SECTION
          ========================================== */}
            <div className="security-card security-password-card" style={{
                background: 'white',
                border: '1px solid #e2e8f0',
                marginBottom: '24px'
            }}>
                <div className="security-card-header" style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    color: '#1e293b',
                    marginBottom: '24px'
                }}>
                    <span className="material-symbols-outlined" style={{ color: '#2563eb' }}>sync_lock</span>
                    <h3 style={{ fontSize: '18px', fontWeight: 700, margin: 0 }}>Đổi mật khẩu</h3>
                </div>

                <form onSubmit={handleUpdatePassword}>
                    <div className="settings-form-grid" style={{ marginBottom: 0 }}>
                        <div className="form-group full-width">
                            <label>Mật khẩu hiện tại</label>
                            <input
                                type="password"
                                placeholder="••••••••"
                                value={oldPassword}
                                onChange={(e) => setOldPassword(e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Mật khẩu mới</label>
                            <input
                                type="password"
                                placeholder="Min. 8 characters"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Xác nhận mật khẩu mới</label>
                            <input
                                type="password"
                                placeholder="Re-type new password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                            />
                        </div>
                    </div>

                    {passwordMessage &&
                        <div className="form-message success" style={{ marginTop: '16px' }}>{passwordMessage}</div>}
                    {passwordError &&
                        <div className="form-message error" style={{ marginTop: '16px' }}>{passwordError}</div>}

                    <div className="form-group full-width" style={{ marginTop: '24px' }}>
                        <button
                            type="submit"
                            className="btn-primary"
                            style={{ width: 'fit-content' }}
                            disabled={passwordSubmitting}
                        >
                            {passwordSubmitting ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
                        </button>
                    </div>
                </form>
            </div>

        </div>
    );
}
