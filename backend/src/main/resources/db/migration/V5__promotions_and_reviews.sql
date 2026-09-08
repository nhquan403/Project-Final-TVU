-- ============================================================================
-- V5 — Khuyến mãi và đánh giá
-- ============================================================================

CREATE TABLE promotions (
    id                  bigserial     PRIMARY KEY,
    code                varchar(50)   NOT NULL,
    name                varchar(150)  NOT NULL,
    description         text,
    discount_type       varchar(20)   NOT NULL,
    discount_value      numeric(12,2) NOT NULL,
    -- NULL = không có trần. Chỉ có ý nghĩa với discount_type = 'PERCENT'.
    max_discount_amount numeric(12,2),
    min_nights          integer       NOT NULL DEFAULT 1,
    min_total_amount    numeric(12,2) NOT NULL DEFAULT 0,
    starts_at           timestamptz   NOT NULL,
    ends_at             timestamptz   NOT NULL,
    -- NULL = không giới hạn lượt dùng.
    usage_limit         integer,
    used_count          integer       NOT NULL DEFAULT 0,
    active              boolean       NOT NULL DEFAULT true,

    CONSTRAINT uq_promotions_code       UNIQUE (code),
    CONSTRAINT ck_promotions_type       CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_promotions_value      CHECK (discount_value > 0),
    CONSTRAINT ck_promotions_max        CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT ck_promotions_nights     CHECK (min_nights >= 1),
    CONSTRAINT ck_promotions_min_total  CHECK (min_total_amount >= 0),
    CONSTRAINT ck_promotions_window     CHECK (ends_at > starts_at),
    CONSTRAINT ck_promotions_used       CHECK (used_count >= 0),
    -- Trần lượt dùng ép ở tầng dữ liệu, không phó mặc cho tầng service: hai
    -- yêu cầu song song cùng đọc used_count = 9 với usage_limit = 10 thì cả
    -- hai đều thấy còn lượt, và mã bị dùng 11 lần.
    CONSTRAINT ck_promotions_usage      CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

-- Khoá ngoại từ bookings sang promotions chỉ đặt được ở đây, vì bookings ra
-- đời ở V3 trước khi bảng này tồn tại.
ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions (id);

CREATE INDEX idx_bookings_promotion ON bookings (promotion_id)
    WHERE promotion_id IS NOT NULL;

-- ─── reviews ───────────────────────────────────────────────────────────────
-- Một đơn một đánh giá. user_id NULL với khách vãng lai, nên tên hiển thị
-- được chụp lại tại chỗ.
CREATE TABLE reviews (
    id                  bigserial    PRIMARY KEY,
    booking_id          bigint       NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    user_id             bigint       REFERENCES users (id),
    guest_name_snapshot varchar(150) NOT NULL,
    rating              smallint     NOT NULL,
    -- Lưu VĂN BẢN THUẦN, không HTML. Nội dung do khách nhập; nơi an toàn nhất
    -- để chặn XSS là không bao giờ nhận HTML vào từ đầu.
    title               varchar(200),
    content             text,
    status              varchar(20)  NOT NULL DEFAULT 'PENDING',
    admin_reply         text,
    replied_at          timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_reviews_booking UNIQUE (booking_id),
    CONSTRAINT ck_reviews_rating  CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_status  CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_reviews_status ON reviews (status, created_at);
