-- ============================================================================
-- V3 — Đặt phòng, gán phòng vật lý, và RÀNG BUỘC CHỐNG TRÙNG LỊCH
--
-- Đây là file quan trọng nhất của cả schema. Ràng buộc EXCLUDE trên
-- booking_rooms là thứ duy nhất bảo đảm không bao giờ bán trùng một phòng —
-- tầng service không bảo đảm được, vì giữa lúc "kiểm tra còn phòng" và lúc
-- "ghi vào bảng" luôn có một khe hở mà một transaction khác chen vào được.
-- ============================================================================

CREATE TABLE bookings (
    id                      bigserial     PRIMARY KEY,
    -- Mã hiển thị cho khách: TVH + 6 ký tự. Ngắn để đọc qua điện thoại được.
    code                    varchar(20)   NOT NULL,
    -- Bí mật thao tác: 32 ký tự hex từ SecureRandom. Khách vãng lai không có
    -- tài khoản vẫn xem và huỷ được đơn của mình qua link chứa token này.
    -- Tách khỏi `code` là chủ ý: `code` in trên email và đọc qua điện thoại
    -- nên phải đoán được; thứ đoán được thì không được phép cấp quyền.
    access_token            char(32)      NOT NULL,
    user_id                 bigint        REFERENCES users (id),
    guest_name              varchar(150)  NOT NULL,
    guest_email             varchar(255)  NOT NULL,
    guest_phone             varchar(20)   NOT NULL,
    check_in                date          NOT NULL,
    check_out               date          NOT NULL,
    adults                  integer       NOT NULL,
    children                integer       NOT NULL DEFAULT 0,
    room_type_id            bigint        NOT NULL REFERENCES room_types (id),
    -- Ảnh chụp tên và giá tại thời điểm đặt. Loại phòng đổi giá về sau không
    -- được phép làm đổi số tiền của một đơn đã chốt.
    room_type_name_snapshot varchar(150)  NOT NULL,
    unit_price_snapshot     numeric(12,2) NOT NULL,
    room_quantity           integer       NOT NULL DEFAULT 1,
    subtotal_amount         numeric(12,2) NOT NULL,
    discount_amount         numeric(12,2) NOT NULL DEFAULT 0,
    total_amount            numeric(12,2) NOT NULL,
    deposit_amount          numeric(12,2) NOT NULL DEFAULT 0,
    promotion_id            bigint,
    status                  varchar(20)   NOT NULL,
    payment_status          varchar(20)   NOT NULL DEFAULT 'UNPAID',
    special_request         text,
    -- Hạn giữ chỗ. Hết hạn mà chưa thanh toán thì đơn chuyển EXPIRED và các
    -- dòng booking_rooms chuyển RELEASED, trả phòng lại cho người khác.
    hold_expires_at         timestamptz,
    client_ip               inet,
    user_agent              varchar(255),
    cancelled_at            timestamptz,
    cancel_reason           text,
    created_at              timestamptz   NOT NULL DEFAULT now(),
    updated_at              timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT uq_bookings_code         UNIQUE (code),
    CONSTRAINT uq_bookings_access_token UNIQUE (access_token),
    -- Khoảng nửa mở [check_in, check_out): trả phòng phải sau nhận phòng.
    -- Bằng nhau nghĩa là 0 đêm, không phải một đơn hợp lệ.
    CONSTRAINT ck_bookings_dates        CHECK (check_out > check_in),
    CONSTRAINT ck_bookings_adults       CHECK (adults >= 1),
    CONSTRAINT ck_bookings_children     CHECK (children >= 0),
    CONSTRAINT ck_bookings_room_qty     CHECK (room_quantity >= 1),
    CONSTRAINT ck_bookings_subtotal     CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_bookings_discount     CHECK (discount_amount >= 0),
    CONSTRAINT ck_bookings_total        CHECK (total_amount >= 0),
    CONSTRAINT ck_bookings_deposit      CHECK (deposit_amount >= 0),
    CONSTRAINT ck_bookings_unit_price   CHECK (unit_price_snapshot > 0),
    -- Đúng tám trạng thái. Frontend (ui-status-badge) gán màu cố định cho
    -- từng giá trị trong tập này; thêm một giá trị ở đây mà quên phía kia là
    -- hiển thị ra ô trống, không lỗi, không ai biết.
    CONSTRAINT ck_bookings_status       CHECK (status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'AWAITING_REVIEW', 'CHECKED_IN',
        'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'NO_SHOW')),
    CONSTRAINT ck_bookings_pay_status   CHECK (payment_status IN (
        'UNPAID', 'PARTIAL', 'DEPOSIT_PAID', 'PAID',
        'OVERPAID', 'REFUND_REQUIRED', 'REFUNDED'))
);

-- ─── booking_rooms — nơi đặt ràng buộc sống còn ────────────────────────────
CREATE TABLE booking_rooms (
    id         bigserial   PRIMARY KEY,
    booking_id bigint      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    room_id    bigint      NOT NULL REFERENCES rooms (id),
    check_in   date        NOT NULL,
    check_out  date        NOT NULL,
    -- Cột sinh tự động, không bao giờ ghi tay. Đây là thứ ràng buộc EXCLUDE
    -- so sánh; để tầng ứng dụng tự tính rồi ghi vào thì sẽ có lúc nó tính sai.
    stay       daterange   GENERATED ALWAYS AS (daterange(check_in, check_out, '[)')) STORED,
    status     varchar(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT ck_booking_rooms_dates  CHECK (check_out > check_in),
    -- CHECK này KHÔNG phải thủ tục. Ràng buộc EXCLUDE bên dưới chỉ soi các
    -- dòng có status = 'ACTIVE'; một lần ghi nhầm 'Active' khiến dòng đó rơi
    -- ra ngoài phạm vi ràng buộc và phòng bị bán trùng ÂM THẦM — không lỗi,
    -- không log, và test đi đường ghi đúng không bao giờ bắt được.
    CONSTRAINT ck_booking_rooms_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

-- Ràng buộc chống trùng lịch. Ba điều nó bảo đảm mà tầng service không:
--   1. Chống race condition thật — hai transaction song song cùng chèn phòng
--      101 cho khoảng ngày chồng nhau thì Postgres bác một cái với 23P01.
--      Không có khe hở giữa "kiểm tra" và "ghi".
--   2. Khoảng nửa mở [) — khách A trả phòng 10/03, khách B nhận phòng 10/03:
--      [08/03,10/03) và [10/03,12/03) KHÔNG chồng nhau, nên hợp lệ. Đúng
--      nghiệp vụ khách sạn.
--   3. WHERE status = 'ACTIVE' — đơn huỷ chuyển dòng sang RELEASED, phòng mở
--      lại ngay, nhưng lịch sử vẫn còn để tra cứu.
ALTER TABLE booking_rooms
    ADD CONSTRAINT booking_rooms_no_overlap
    EXCLUDE USING gist (room_id WITH =, stay WITH &&)
    WHERE (status = 'ACTIVE');

CREATE INDEX idx_booking_rooms_booking ON booking_rooms (booking_id);

-- ─── Bất biến: room_quantity phải khớp số dòng ACTIVE ──────────────────────
-- bookings.room_quantity và số dòng booking_rooms ACTIVE là hai nguồn sự thật
-- song song. Không có gì buộc chúng khớp thì một lần gán phòng thất bại một
-- phần sẽ để lại đơn đã thu cọc 3 phòng nhưng chỉ giữ 2 — và truy vấn phòng
-- trống sẽ bán phòng thứ ba đó cho khách khác.
CREATE FUNCTION assert_booking_room_count(p_booking_id bigint) RETURNS void AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM bookings b
        WHERE b.id = p_booking_id
          AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
          AND b.room_quantity <> (
              SELECT count(*)
              FROM booking_rooms br
              WHERE br.booking_id = b.id
                AND br.status = 'ACTIVE')
    ) THEN
        RAISE EXCEPTION 'booking_rooms khong khop room_quantity';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION check_booking_room_count() RETURNS trigger AS $$
BEGIN
    PERFORM assert_booking_room_count(COALESCE(NEW.booking_id, OLD.booking_id));
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION check_booking_room_count_from_booking() RETURNS trigger AS $$
BEGIN
    PERFORM assert_booking_room_count(NEW.id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- DEFERRABLE INITIALLY DEFERRED là bắt buộc, không phải tuỳ chọn: gán phòng
-- diễn ra lần lượt từng dòng, nên ngay sau dòng đầu tiên bất biến đã tạm sai.
-- Trigger chạy ngay lập tức sẽ khiến không đơn nhiều phòng nào đặt được.
CREATE CONSTRAINT TRIGGER booking_room_count_check
    AFTER INSERT OR UPDATE OR DELETE ON booking_rooms
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_booking_room_count();

-- Bất biến có HAI phía. Trigger trên chỉ canh phía booking_rooms; một lệnh
-- `UPDATE bookings SET room_quantity = 9` không đụng đến bảng kia sẽ đi lọt
-- và để lại đúng cái trạng thái mà bất biến này sinh ra để ngăn.
--
-- Phạm vi hẹp hết mức có thể, và đây là chủ ý:
--   * KHÔNG canh INSERT — đơn vừa tạo mà chưa gán phòng là trạng thái hợp lệ,
--     và luồng đặt phòng ở phase sau có thể tạo đơn trước rồi mới gán. Đơn
--     tạo ra rồi gán THIẾU phòng vẫn bị trigger phía booking_rooms bắt.
--   * Chỉ canh khi room_quantity THỰC SỰ đổi giá trị. Canh mọi UPDATE lên
--     bookings sẽ khiến những lệnh chẳng liên quan gì — đổi payment_status,
--     ghi client_ip — cũng hỏng chỉ vì đơn chưa gán phòng.
CREATE CONSTRAINT TRIGGER bookings_room_count_check
    AFTER UPDATE OF room_quantity ON bookings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    WHEN (OLD.room_quantity IS DISTINCT FROM NEW.room_quantity)
    EXECUTE FUNCTION check_booking_room_count_from_booking();

-- ─── booking_status_history ────────────────────────────────────────────────
CREATE TABLE booking_status_history (
    id          bigserial   PRIMARY KEY,
    booking_id  bigint      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    -- NULL ở dòng đầu tiên: đơn vừa được tạo, chưa có trạng thái trước đó.
    from_status varchar(20),
    to_status   varchar(20) NOT NULL,
    changed_by  bigint      REFERENCES users (id),
    actor       varchar(20) NOT NULL,
    note        text,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ck_bsh_actor       CHECK (actor IN ('GUEST', 'CUSTOMER', 'ADMIN', 'SYSTEM')),
    -- Hai cột này cũng là cột enum trạng thái booking, nên cũng phải có CHECK.
    -- Một dòng lịch sử ghi sai chính tả không gây hỏng ngay, nhưng làm hỏng
    -- mọi báo cáo đọc từ bảng này về sau.
    CONSTRAINT ck_bsh_from_status CHECK (from_status IS NULL OR from_status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'AWAITING_REVIEW', 'CHECKED_IN',
        'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'NO_SHOW')),
    CONSTRAINT ck_bsh_to_status   CHECK (to_status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'AWAITING_REVIEW', 'CHECKED_IN',
        'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'NO_SHOW'))
);

CREATE INDEX idx_bsh_booking ON booking_status_history (booking_id, created_at);

-- ─── Index cho truy vấn nóng ───────────────────────────────────────────────
-- (Ràng buộc EXCLUDE đã tự tạo index GiST cho booking_rooms, không tạo thêm.)
CREATE INDEX idx_bookings_status_checkin ON bookings (status, check_in);
CREATE INDEX idx_bookings_guest_phone    ON bookings (guest_phone);
CREATE INDEX idx_bookings_user           ON bookings (user_id) WHERE user_id IS NOT NULL;
-- Bộ quét đơn hết hạn giữ chỗ chỉ quan tâm các đơn đang chờ thanh toán.
CREATE INDEX idx_bookings_hold_expiry    ON bookings (hold_expires_at)
    WHERE status = 'PENDING_PAYMENT';
