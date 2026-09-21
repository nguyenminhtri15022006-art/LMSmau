-- ============================================================
-- IDENTITY SERVICE — V1: Initial schema
-- ============================================================

CREATE TABLE users (
    user_id       CHAR(36)     NOT NULL PRIMARY KEY,
    user_name     VARCHAR(55)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    dob           DATE         NULL,
    avatar_url    VARCHAR(500) NULL,
    bio           TEXT         NULL,
    phone         VARCHAR(20)  NULL,
    role          ENUM('ADMIN','INSTRUCTOR','STUDENT') NOT NULL,
    role_selected TINYINT(1)   NOT NULL DEFAULT 0,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    is_verified   TINYINT(1)   NOT NULL DEFAULT 0,
    auth_provider ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL',
    provider_id   VARCHAR(100) NULL,
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    token_id    CHAR(36)     NOT NULL PRIMARY KEY,
    user_id     CHAR(36)     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    device_info VARCHAR(255) NULL,
    ip_address  VARCHAR(45)  NULL,
    expires_at  DATETIME     NOT NULL,
    is_revoked  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_verifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    CHAR(36)     NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    type       ENUM('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
    expires_at DATETIME     NOT NULL,
    used_at    DATETIME     NULL,
    attempts   TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
