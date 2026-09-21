import React from 'react';

const overlayStyle = {
  position: 'fixed', inset: 0,
  background: 'rgba(0,0,0,0.55)',
  zIndex: 1000,
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  padding: '16px',
};

const boxStyle = {
  background: 'var(--admin-card, #1e2330)',
  borderRadius: '14px',
  padding: '28px',
  width: '100%',
  maxWidth: '520px',
  boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
  border: '1px solid var(--admin-border, #2d3448)',
  maxHeight: '90vh',
  overflowY: 'auto',
};

const labelStyle = {
  fontSize: '0.75rem',
  color: 'var(--admin-muted, #8892a4)',
  marginBottom: '2px',
};

const valueStyle = {
  fontSize: '0.88rem',
  color: 'var(--admin-text, #e2e8f0)',
  wordBreak: 'break-word',
};

const fieldStyle = { marginBottom: '12px' };

const Row = ({ label, value }) => (
  <div style={fieldStyle}>
    <div style={labelStyle}>{label}</div>
    <div style={valueStyle}>{value || '—'}</div>
  </div>
);

const formatDate = (val) => {
  if (!val) return '—';
  try {
    return new Date(val).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  } catch { return '—'; }
};

const getInitials = (name) => {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(-2).map((p) => p[0]?.toUpperCase() || '').join('');
};

/**
 * UserInfoModal — dùng chung cho Admin và Instructor.
 *
 * Props:
 *   user        — object chứa thông tin user (bắt buộc)
 *   onClose     — hàm đóng modal (bắt buộc)
 *   loading     — bool, hiện spinner khi đang fetch
 *   extraFields — mảng { label, value } cho thông tin bổ sung (VD: enrollment date)
 */
export default function UserInfoModal({ user, onClose, loading = false, extraFields = [] }) {
  if (!user && !loading) return null;

  const roleLabelMap = { STUDENT: 'Học viên', INSTRUCTOR: 'Giảng viên', ADMIN: 'Quản trị viên' };
  const roleLabel = roleLabelMap[user?.role] || user?.role || '—';

  const rolePillColor = {
    STUDENT: 'rgba(59,130,246,0.15)',
    INSTRUCTOR: 'rgba(188,140,255,0.15)',
    ADMIN: 'rgba(234,179,8,0.15)',
  }[user?.role] || 'rgba(100,116,139,0.15)';

  const roleTextColor = {
    STUDENT: '#60a5fa',
    INSTRUCTOR: '#bc8cff',
    ADMIN: '#eab308',
  }[user?.role] || '#8892a4';

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div style={boxStyle} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: 0 }}>
            {user?.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt=""
                style={{ width: '52px', height: '52px', borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
              />
            ) : (
              <div style={{
                width: '52px', height: '52px', borderRadius: '50%', flexShrink: 0,
                background: 'var(--admin-accent, #6366f1)', display: 'flex',
                alignItems: 'center', justifyContent: 'center',
                fontSize: '1.1rem', fontWeight: 700, color: '#fff',
              }}>
                {getInitials(user?.fullname || user?.username)}
              </div>
            )}
            <div style={{ minWidth: 0 }}>
              <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--admin-text, #e2e8f0)', marginBottom: '4px' }}>
                {user?.fullname || user?.username || 'N/A'}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                {user?.username && (
                  <span style={{ fontSize: '0.78rem', color: 'var(--admin-muted, #8892a4)' }}>@{user.username}</span>
                )}
                {user?.role && (
                  <span style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: '20px', background: rolePillColor, color: roleTextColor, fontWeight: 600 }}>
                    {roleLabel}
                  </span>
                )}
                {user?.active === false && (
                  <span style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: '20px', background: 'rgba(239,68,68,0.12)', color: '#f87171', fontWeight: 600 }}>
                    Đã khóa
                  </span>
                )}
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--admin-muted, #8892a4)', fontSize: '1.2rem', padding: '4px', flexShrink: 0 }}
          >
            <i className="ti ti-x"></i>
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--admin-muted, #8892a4)', fontSize: '0.85rem' }}>
            <i className="ti ti-loader" style={{ fontSize: '1.4rem' }}></i>
            <div style={{ marginTop: '8px' }}>Đang tải thông tin...</div>
          </div>
        ) : (
          <>
            <div style={{ borderTop: '1px solid var(--admin-border, #2d3448)', paddingTop: '16px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 24px' }}>
                <Row label="Email" value={user?.email} />
                <Row label="Số điện thoại" value={user?.phone} />
                <Row label="Ngày sinh" value={formatDate(user?.dob)} />
                <Row label="Phương thức đăng ký" value={user?.authProvider || 'LOCAL'} />
                <Row label="Ngày tham gia" value={formatDate(user?.createdAt || user?.enrolledAt)} />
                {user?.lastLoginAt && (
                  <Row label="Đăng nhập gần nhất" value={formatDate(user.lastLoginAt)} />
                )}
                {extraFields.map(({ label, value }) => (
                  <Row key={label} label={label} value={value} />
                ))}
              </div>
              {user?.bio && (
                <div style={{ ...fieldStyle, marginTop: '4px' }}>
                  <div style={labelStyle}>Giới thiệu</div>
                  <div style={{ ...valueStyle, fontStyle: 'italic', lineHeight: 1.6 }}>{user.bio}</div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px', borderTop: '1px solid var(--admin-border, #2d3448)', paddingTop: '16px' }}>
              <button className="admin-btn" onClick={onClose}>Đóng</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}