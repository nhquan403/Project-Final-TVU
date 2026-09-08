-- ============================================================================
-- V6 — Nội dung landing page do admin quản lý
-- ============================================================================

CREATE TABLE site_contents (
    id          bigserial    PRIMARY KEY,
    section_key varchar(100) NOT NULL,
    title       varchar(255),
    subtitle    varchar(255),
    -- Đã sanitize TRƯỚC KHI LƯU, không sanitize lúc hiển thị: mỗi nơi hiển
    -- thị mới là một chỗ có thể quên.
    body        text,
    data        jsonb,
    image_url   varchar(500),
    updated_by  bigint       REFERENCES users (id),
    updated_at  timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_site_contents_key UNIQUE (section_key)
);

CREATE TABLE banners (
    id            bigserial    PRIMARY KEY,
    title         varchar(255) NOT NULL,
    image_url     varchar(500) NOT NULL,
    public_id     varchar(255),
    -- Scheme http/https kiểm ở tầng ứng dụng: một link javascript: lọt vào
    -- đây là XSS ngay trên trang chủ.
    link_url      varchar(500),
    display_order integer      NOT NULL DEFAULT 0,
    active        boolean      NOT NULL DEFAULT true,
    starts_at     timestamptz,
    ends_at       timestamptz,

    CONSTRAINT ck_banners_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE TABLE gallery_images (
    id            bigserial    PRIMARY KEY,
    url           varchar(500) NOT NULL,
    public_id     varchar(255),
    caption       varchar(255),
    category      varchar(50),
    display_order integer      NOT NULL DEFAULT 0,
    active        boolean      NOT NULL DEFAULT true
);

CREATE INDEX idx_gallery_images_category ON gallery_images (category, display_order)
    WHERE active;

CREATE TABLE posts (
    id              bigserial    PRIMARY KEY,
    slug            varchar(200) NOT NULL,
    title           varchar(255) NOT NULL,
    excerpt         text,
    content         text,
    cover_image_url varchar(500),
    published       boolean      NOT NULL DEFAULT false,
    published_at    timestamptz,
    author_id       bigint       REFERENCES users (id),

    CONSTRAINT uq_posts_slug UNIQUE (slug)
);

CREATE INDEX idx_posts_published ON posts (published_at DESC) WHERE published;
