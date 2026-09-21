-- ============================================================
-- IDENTITY SERVICE — V2: Performance indexes
-- ============================================================

-- users: lọc user đang active / chưa verify
CREATE INDEX idx_users_active_verified ON users (is_active, is_verified);

-- users: tìm user theo số điện thoại
CREATE INDEX idx_users_phone ON users (phone);

-- refresh_tokens: lấy tất cả token chưa revoked của 1 user (logout all devices)
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, is_revoked);

-- refresh_tokens: cleanup token hết hạn (scheduled job)
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- email_verifications: kiểm tra user đã có token chưa hết hạn (chống spam gửi mail)
CREATE INDEX idx_email_verif_user_type_expires ON email_verifications (user_id, type, expires_at);

-- email_verifications: cleanup token hết hạn (scheduled job)
CREATE INDEX idx_email_verif_expires_at ON email_verifications (expires_at);

CREATE INDEX idx_users_role_created_at ON users (role, created_at);
