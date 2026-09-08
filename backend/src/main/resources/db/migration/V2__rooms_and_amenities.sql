-- ============================================================================
-- V2 — Tiện ích, loại phòng, phòng vật lý
--
-- Phân biệt LOẠI PHÒNG với PHÒNG VẬT LÝ là quyết định nền của cả hệ thống:
-- khách đặt một loại phòng, hệ thống gán một phòng vật lý cụ thể, và ràng buộc
-- chống trùng lịch ở V3 đặt trên phòng vật lý chứ không phải loại phòng.
-- ============================================================================

CREATE TABLE amenities (
    id            bigserial    PRIMARY KEY,
    code          varchar(50)  NOT NULL,
    name          varchar(100) NOT NULL,
    icon          varchar(50),
    category      varchar(20)  NOT NULL,
    display_order integer      NOT NULL DEFAULT 0,

    CONSTRAINT uq_amenities_code     UNIQUE (code),
    CONSTRAINT ck_amenities_category CHECK (category IN ('ROOM', 'PROPERTY'))
);

CREATE TABLE room_types (
    id                bigserial     PRIMARY KEY,
    code              varchar(50)   NOT NULL,
    slug              varchar(120)  NOT NULL,
    name              varchar(150)  NOT NULL,
    short_description text,
    description       text,
    base_price        numeric(12,2) NOT NULL,
    capacity_adults   integer       NOT NULL,
    capacity_children integer       NOT NULL DEFAULT 0,
    bed_info          varchar(150),
    area_sqm          numeric(6,2),
    display_order     integer       NOT NULL DEFAULT 0,
    active            boolean       NOT NULL DEFAULT true,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT uq_room_types_code       UNIQUE (code),
    CONSTRAINT uq_room_types_slug       UNIQUE (slug),
    CONSTRAINT ck_room_types_price      CHECK (base_price > 0),
    CONSTRAINT ck_room_types_adults     CHECK (capacity_adults >= 1),
    CONSTRAINT ck_room_types_children   CHECK (capacity_children >= 0),
    CONSTRAINT ck_room_types_area       CHECK (area_sqm IS NULL OR area_sqm > 0)
);

CREATE TABLE rooms (
    id           bigserial   PRIMARY KEY,
    room_type_id bigint      NOT NULL REFERENCES room_types (id),
    room_number  varchar(20) NOT NULL,
    floor        integer,
    status       varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    note         text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_rooms_number UNIQUE (room_number),
    CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'OUT_OF_SERVICE'))
);

-- Truy vấn phòng trống lọc theo (loại phòng, trạng thái) trước khi trừ đi
-- những phòng đã bị chiếm.
CREATE INDEX idx_rooms_type_status ON rooms (room_type_id, status);

CREATE TABLE room_type_images (
    id            bigserial    PRIMARY KEY,
    room_type_id  bigint       NOT NULL REFERENCES room_types (id) ON DELETE CASCADE,
    url           varchar(500) NOT NULL,
    -- id của Cloudinary, NULL khi ảnh lưu cục bộ. Cần để xoá được ảnh trên
    -- cloud khi xoá bản ghi.
    public_id     varchar(255),
    alt_text      varchar(255),
    display_order integer      NOT NULL DEFAULT 0,
    is_cover      boolean      NOT NULL DEFAULT false
);

CREATE INDEX idx_room_type_images_type ON room_type_images (room_type_id);
-- Mỗi loại phòng nhiều nhất một ảnh bìa.
CREATE UNIQUE INDEX uq_room_type_images_cover ON room_type_images (room_type_id)
    WHERE is_cover;

CREATE TABLE room_type_amenities (
    room_type_id bigint NOT NULL REFERENCES room_types (id) ON DELETE CASCADE,
    amenity_id   bigint NOT NULL REFERENCES amenities (id)  ON DELETE CASCADE,

    CONSTRAINT pk_room_type_amenities PRIMARY KEY (room_type_id, amenity_id)
);
