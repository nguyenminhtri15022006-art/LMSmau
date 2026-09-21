import React, {useRef, useState} from 'react';
import useAuth from '../../../hooks/useAuth';
import {authApi} from '../../../services/auth.api';
import {useFileUpload} from '../../../hooks/useFileUpload';
import {ENDPOINTS} from '../../../constants/endpoints';

const MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

function formatDateForInput(dateVal) {
    if (!dateVal) return '';
    const d = new Date(dateVal);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
}

function getProfileUpdateErrorMessage(err) {
    const responseData = err?.response?.data;
    if (responseData?.message) return responseData.message;

    const fieldErrors = responseData?.data;
    if (fieldErrors && typeof fieldErrors === 'object') {
        const messages = Object.values(fieldErrors).filter(Boolean);
        if (messages.length > 0) return messages.join(' ');
    }

    return err?.message || 'Cập nhật thất bại.';
}

export default function PersonalInfoSection({user}) {
    const {updateUser} = useAuth();
    const {upload, uploading} = useFileUpload();
    const fileInputRef = useRef(null);

    const [form, setForm] = useState({
        fullname: user?.fullname || '',
        phone: user?.phone || '',
        dob: formatDateForInput(user?.dob),
        bio: user?.bio || '',
    });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);
    const [imgError, setImgError] = useState(false);
    const [avatarPreview, setAvatarPreview] = useState(null);
    const [avatarUrl, setAvatarUrl] = useState(null);
    const [discardedAvatarUrls, setDiscardedAvatarUrls] = useState([]);

    const selectedAvatarUrl = avatarUrl !== null ? avatarUrl : user?.avatarUrl;
    const displayUrl = avatarPreview || selectedAvatarUrl;
    const showAvatar = displayUrl && !imgError;

    const handleChange = (e) => {
        setForm((prev) => ({...prev, [e.target.name]: e.target.value}));
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);
        try {
            const payload = {
                fullname: form.fullname,
                phone: form.phone || null,
                dob: form.dob || null,
                bio: form.bio || null,
                avatarUrl: avatarUrl !== null ? avatarUrl : (user?.avatarUrl || null),
                discardedAvatarUrls,
            };
            const updatedUser = (await authApi.updateProfile(payload)).data.data;
            updateUser(updatedUser);
            setAvatarUrl(null);
            setAvatarPreview(null);
            setDiscardedAvatarUrls([]);
            setImgError(false);
            setMessage({type: 'success', text: 'Cập nhật thành công.'});
        } catch (err) {
            setMessage({type: 'error', text: getProfileUpdateErrorMessage(err)});
        } finally {
            setSaving(false);
        }
    };

    const handleFileSelect = async (e) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) return;

        if (!file.type.startsWith('image/')) {
            setMessage({type: 'error', text: 'Vui lòng chọn tệp hình ảnh.'});
            return;
        }
        if (file.size > MAX_AVATAR_SIZE) {
            setMessage({type: 'error', text: 'Kích thước hình ảnh phải nhỏ hơn 5MB.'});
            return;
        }

        setMessage(null);
        try {
            const previousUploadedAvatarUrl = avatarUrl && avatarUrl !== user?.avatarUrl ? avatarUrl : null;
            setAvatarPreview(URL.createObjectURL(file));
            const publicUrl = await upload(file, {folder: 'avatars', endpoint: ENDPOINTS.UPLOAD.IDENTITY_PRESIGNED});
            if (previousUploadedAvatarUrl) {
                setDiscardedAvatarUrls((prev) => [...prev, previousUploadedAvatarUrl]);
            }
            setAvatarUrl(publicUrl);
            setImgError(false);
            setMessage({type: 'success', text: 'Ảnh đại diện đã được tải lên. Nhấn Lưu thay đổi để áp dụng.'});
        } catch {
            setAvatarPreview(null);
            setMessage({type: 'error', text: 'Tải lên ảnh đại diện thất bại.'});
        }
    };

    const handleRemoveAvatar = () => {
        if (avatarUrl && avatarUrl !== user?.avatarUrl) {
            setDiscardedAvatarUrls((prev) => [...prev, avatarUrl]);
        }
        setAvatarPreview(null);
        setAvatarUrl('');
        setImgError(false);
        if (user?.avatarUrl) {
            setMessage({type: 'success', text: 'Ảnh đại diện sẽ được xóa khi lưu. Nhấn Lưu thay đổi để áp dụng.'});
        }
    };

    const resetForm = () => {
        if (avatarUrl && avatarUrl !== user?.avatarUrl) {
            setDiscardedAvatarUrls((prev) => [...prev, avatarUrl]);
        }
        setForm({
            fullname: user?.fullname || '',
            phone: user?.phone || '',
            dob: formatDateForInput(user?.dob),
            bio: user?.bio || '',
        });
        setAvatarPreview(null);
        setAvatarUrl(null);
        setImgError(false);
    };

    return (
        <div className="settings-section">
            <div className="settings-section-header">
                <h2>Thông tin cá nhân</h2>
                <p>Cập nhật ảnh đại diện và thông tin cá nhân của bạn tại đây.</p>
            </div>

            <div className="profile-upload-area">
                <div className="upload-avatar-preview-container" style={{position: 'relative'}}>
                    {showAvatar ? (
                        <img
                            src={displayUrl}
                            alt="Profile"
                            className="upload-avatar-preview"
                            referrerPolicy="no-referrer"
                            crossOrigin="anonymous"
                            onError={() => setImgError(true)}
                        />
                    ) : (
                        <div className="upload-avatar-preview" style={{
                            background: 'linear-gradient(135deg, #4299e1, #2563eb)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'white'
                        }}>
                            <span className="material-symbols-outlined" style={{fontSize: '48px'}}>person</span>
                        </div>
                    )}
                    {uploading && (
                        <div style={{
                            position: 'absolute',
                            inset: 0,
                            borderRadius: '50%',
                            background: 'rgba(0,0,0,0.4)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <div style={{
                                width: '24px',
                                height: '24px',
                                borderRadius: '50%',
                                border: '3px solid white',
                                borderTopColor: 'transparent',
                                animation: 'spin 0.8s linear infinite'
                            }}/>
                        </div>
                    )}
                    <input ref={fileInputRef} type="file" accept="image/*" style={{display: 'none'}}
                           onChange={handleFileSelect}/>
                    <button
                        className="avatar-edit-btn"
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        style={{
                            position: 'absolute',
                            bottom: 0,
                            right: 0,
                            background: '#2563eb',
                            color: 'white',
                            border: '2px solid white',
                            borderRadius: '50%',
                            width: '32px',
                            height: '32px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer'
                        }}
                    >
                        <span className="material-symbols-outlined" style={{fontSize: '18px'}}>edit</span>
                    </button>
                </div>
                <div className="upload-info">
                    <h4 style={{fontSize: '16px', fontWeight: 700, color: '#1e293b', marginBottom: '4px'}}>Ảnh đại
                        diện</h4>
                    <p style={{fontSize: '13px', color: '#64748b', marginBottom: '16px'}}>JPG, GIF hoặc PNG. Kích thước
                        tối đa 5MB</p>
                    <div className="upload-actions">
                        <button type="button" className="btn-secondary" style={{padding: '8px 16px', fontSize: '13px'}}
                                onClick={() => fileInputRef.current?.click()} disabled={uploading}>
                            {uploading ? 'Đang tải lên...' : 'Tải lên mới'}
                        </button>
                        <button type="button" className="btn-secondary" style={{
                            padding: '8px 16px',
                            fontSize: '13px',
                            color: '#ef4444',
                            borderColor: '#fee2e2'
                        }} onClick={handleRemoveAvatar}>
                            Xóa
                        </button>
                    </div>
                </div>
            </div>

            {message && (
                <div className={`form-message ${message.type}`} style={{
                    padding: '12px 16px',
                    borderRadius: '10px',
                    marginBottom: '20px',
                    fontSize: '14px',
                    fontWeight: 500,
                    background: message.type === 'success' ? '#f0fdf4' : '#fef2f2',
                    color: message.type === 'success' ? '#166534' : '#991b1b',
                    border: `1px solid ${message.type === 'success' ? '#bbf7d0' : '#fecaca'}`
                }}>
                    {message.text}
                </div>
            )}

            <form className="settings-form-grid" onSubmit={handleSave}>
                <div className="form-group">
                    <label>Tên đăng nhập</label>
                    <input type="text" value={user?.username || ''} disabled style={{opacity: 0.7}}/>
                </div>
                <div className="form-group">
                    <label>Email</label>
                    <input type="email" value={user?.email || ''} disabled style={{opacity: 0.7}}/>
                </div>
                <div className="form-group">
                    <label>Họ và tên</label>
                    <input type="text" name="fullname" value={form.fullname} onChange={handleChange}
                           placeholder="Your name"/>
                </div>
                <div className="form-group">
                    <label>Ngày sinh</label>
                    <input type="date" name="dob" value={form.dob} onChange={handleChange} lang="en"/>
                </div>
                <div className="form-group">
                    <label>Số điện thoại</label>
                    <input type="text" name="phone" value={form.phone} onChange={handleChange}
                           placeholder="+84 945234532"/>
                </div>
                <div className="form-group full-width">
                    <label>Tiểu sử</label>
                    <textarea name="bio" value={form.bio} onChange={handleChange}
                              placeholder="Chia sẻ một chút về bản thân..."></textarea>
                </div>

                <div className="form-group full-width" style={{
                    display: 'flex',
                    flexDirection: 'row',
                    justifyContent: 'flex-start',
                    gap: '16px',
                    marginTop: '24px'
                }}>
                    <button type="button" className="btn-secondary" onClick={resetForm}>Hủy</button>
                    <button type="submit" className="btn-primary" disabled={saving || uploading}>
                        {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                    </button>
                </div>
            </form>
        </div>
    );
}
