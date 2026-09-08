-- ============================================================================
-- V4 — Thanh toán, nhật ký webhook, hộp thư đi
--
-- Ba bảng này nằm ở V3+1 chứ không đợi đến phase thanh toán, vì kế hoạch chốt
-- nguyên tắc migration bất biến: thêm bảng sau nghĩa là vá bằng V7/V8.
-- ============================================================================

-- ─── payments ──────────────────────────────────────────────────────────────
-- Một đơn có thể có NHIỀU lần thanh toán: khách chuyển hai lần, chuyển thiếu
-- rồi bù, hoặc đặt lại sau khi hết hạn giữ chỗ.
CREATE TABLE payments (
    id               bigserial     PRIMARY KEY,
    booking_id       bigint        NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    attempt_no       integer       NOT NULL DEFAULT 1,
    provider         varchar(20)   NOT NULL,
    amount_expected  numeric(12,2) NOT NULL,
    -- Cộng dồn qua nhiều lần chuyển khoản, không phải ghi đè.
    amount_received  numeric(12,2) NOT NULL DEFAULT 0,
    qr_content       text,
    qr_image_url     varchar(500),
    -- code + 2 chữ số attempt_no, ví dụ TVH8F3K2Q01. Nhờ hậu tố này mà QR của
    -- lần trước không khớp nhầm vào lần sau.
    transfer_content varchar(50)   NOT NULL,
    status           varchar(20)   NOT NULL DEFAULT 'PENDING',
    reconcile_status varchar(20)   NOT NULL DEFAULT 'NONE',
    provider_txn_id  varchar(100),
    paid_at          timestamptz,
    expires_at       timestamptz,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT ck_payments_attempt   CHECK (attempt_no >= 1),
    CONSTRAINT ck_payments_provider  CHECK (provider IN ('SEPAY', 'MANUAL')),
    CONSTRAINT ck_payments_expected  CHECK (amount_expected > 0),
    CONSTRAINT ck_payments_received  CHECK (amount_received >= 0),
    CONSTRAINT ck_payments_status    CHECK (status IN (
        'PENDING', 'PARTIAL', 'SUCCEEDED', 'OVERPAID', 'EXPIRED', 'FAILED')),
    CONSTRAINT ck_payments_reconcile CHECK (reconcile_status IN (
        'NONE', 'NEEDS_REVIEW', 'REFUND_REQUIRED', 'RESOLVED'))
);

CREATE UNIQUE INDEX uq_payments_transfer_content ON payments (transfer_content);
CREATE UNIQUE INDEX uq_payments_booking_attempt  ON payments (booking_id, attempt_no);
CREATE INDEX        idx_payments_booking         ON payments (booking_id);
-- Màn hình đối soát của admin chỉ quan tâm những dòng cần người xử lý.
CREATE INDEX        idx_payments_reconcile       ON payments (reconcile_status)
    WHERE reconcile_status <> 'NONE';

-- ─── payment_webhook_events ────────────────────────────────────────────────
-- Nhật ký mọi webhook nhận được, kể cả cái không khớp đơn nào. Khoá chống
-- xử lý trùng là (provider, external_id).
--
-- provider_txn_id ở bảng payments KHÔNG unique toàn cục: nhà cung cấp có thể
-- gửi cùng một referenceCode cho các sự kiện khác nhau. Đặt UNIQUE ở đó sẽ
-- làm rơi những webhook hợp lệ.
CREATE TABLE payment_webhook_events (
    id                bigserial   PRIMARY KEY,
    provider          varchar(20) NOT NULL,
    external_id       varchar(100) NOT NULL,
    payment_id        bigint      REFERENCES payments (id),
    -- Lưu nguyên payload để đối soát lại được khi tranh chấp.
    payload           jsonb       NOT NULL,
    processing_result varchar(20) NOT NULL,
    error_message     text,
    received_at       timestamptz NOT NULL DEFAULT now(),
    processed_at      timestamptz,

    CONSTRAINT ck_pwe_result CHECK (processing_result IN (
        'MATCHED', 'UNMATCHED', 'LATE', 'DUPLICATE', 'ERROR'))
);

CREATE UNIQUE INDEX uq_webhook_provider_external
    ON payment_webhook_events (provider, external_id);
CREATE INDEX idx_pwe_payment ON payment_webhook_events (payment_id)
    WHERE payment_id IS NOT NULL;

-- ─── outbound_emails — hộp thư đi ──────────────────────────────────────────
-- Email xếp hàng ở đây trong cùng transaction với nghiệp vụ, rồi một bộ quét
-- riêng mới gửi đi. Gửi thẳng trong transaction thì hoặc mất thư khi rollback,
-- hoặc gửi thư cho một đơn rốt cuộc không tồn tại. Bảng này cũng là bằng
-- chứng đã gửi khi khách bảo không nhận được.
CREATE TABLE outbound_emails (
    id         bigserial    PRIMARY KEY,
    booking_id bigint       REFERENCES bookings (id) ON DELETE SET NULL,
    template   varchar(100) NOT NULL,
    to_email   varchar(255) NOT NULL,
    payload    jsonb        NOT NULL,
    status     varchar(20)  NOT NULL DEFAULT 'PENDING',
    attempts   integer      NOT NULL DEFAULT 0,
    last_error text,
    sent_at    timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_outbound_emails_status   CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_outbound_emails_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbound_emails_pending ON outbound_emails (status)
    WHERE status = 'PENDING';
