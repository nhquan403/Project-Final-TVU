-- ============================================================================
-- V1 — Extension và tài khoản
--
-- btree_gist phải cài ở đây, trước mọi thứ khác: ràng buộc EXCLUDE ở V3 cần
-- toán tử `=` cho kiểu bigint trong chỉ mục GiST, mà GiST mặc định không có.
-- Thiếu extension này thì V3 không chạy được, và không có ràng buộc EXCLUDE
-- thì toàn bộ cơ chế chống đặt trùng phòng của dự án sụp.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ─── users ─────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                   bigserial    PRIMARY KEY,
    email                varchar(255) NOT NULL,
    password_hash        varchar(72)  NOT NULL,
    full_name            varchar(150) NOT NULL,
    phone                varchar(20),
    role                 varchar(20)  NOT NULL,
    enabled              boolean      NOT NULL DEFAULT true,
    must_change_password boolean      NOT NULL DEFAULT false,
    -- Tăng khi logout, khoá tài khoản hoặc đổi quyền. JWT mang token_version
    -- lúc phát hành; lệch số là token bị từ chối ngay, không cần chờ hết hạn.
    token_version        integer      NOT NULL DEFAULT 0,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email          UNIQUE (email),
    -- Email lưu chữ thường ở tầng dữ liệu, không phó mặc cho tầng ứng dụng:
    -- một chỗ quên lower() là tạo được hai tài khoản cho cùng một hộp thư.
    CONSTRAINT ck_users_email_lower    CHECK (email = lower(email)),
    CONSTRAINT ck_users_role           CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT ck_users_token_version  CHECK (token_version >= 0)
);

-- ─── refresh_tokens ────────────────────────────────────────────────────────
-- Chỉ lưu BĂM của token, không bao giờ lưu token gốc. Rò cơ sở dữ liệu thì
-- kẻ đọc được cũng không đăng nhập lại được bằng những gì đọc thấy.
CREATE TABLE refresh_tokens (
    id          bigserial   PRIMARY KEY,
    user_id     bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  char(64)    NOT NULL,
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    -- Băm của token kế nhiệm sau khi xoay vòng. Nếu một token đã bị thay thế
    -- lại được dùng, đó là dấu hiệu token bị đánh cắp — thu hồi cả chuỗi.
    replaced_by char(64),
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens (user_id)
    WHERE revoked_at IS NULL;
