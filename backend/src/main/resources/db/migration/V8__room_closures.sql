-- ============================================================================
-- V8 — Khoảng ngày một phòng vật lý không nhận khách
--
-- rooms.status là công tắc VĨNH VIỄN: bật lên thì phòng bán mãi, tắt đi thì
-- ngừng bán mãi. Nó không trả lời được câu "phòng 201 sơn lại từ 20/10 đến
-- 25/10" — chủ homestay phải tự nhớ tắt rồi tự nhớ bật, và ngày quên bật là
-- ngày mất doanh thu mà không ai báo.
--
-- Vì sao lại EXCLUDE USING gist lần thứ hai: hai khoảng đóng chồng nhau trên
-- cùng một phòng làm màn hình quản trị hiện hai dòng nói cùng một điều, và xoá
-- một dòng KHÔNG mở lại được phòng — người dùng bấm xoá rồi tưởng đã xong.
-- Chặn ở tầng cơ sở dữ liệu thì không tầng nào ở trên quên được.
--
-- btree_gist đã bật từ V1 nên không khai lại ở đây.
-- ============================================================================

CREATE TABLE room_closures (
    id         bigserial   PRIMARY KEY,
    room_id    bigint      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    from_date  date        NOT NULL,
    to_date    date        NOT NULL,

    -- Cột sinh tự động, không bao giờ ghi tay — cùng lý do với booking_rooms.stay:
    -- khoảng phải luôn khớp hai cột ngày, kể cả khi ai đó UPDATE thẳng bằng SQL.
    --
    -- Quy ước NỬA MỞ '[)' là bắt buộc chứ không phải sở thích: nó phải khớp với
    -- booking_rooms.stay để toán tử && so sánh đúng. Đóng từ 20/10 đến 25/10
    -- nghĩa là chặn các ĐÊM 20, 21, 22, 23, 24 — đêm 25 vẫn bán được.
    blocked    daterange   GENERATED ALWAYS AS (daterange(from_date, to_date, '[)')) STORED,

    reason     varchar(300),

    -- NULL khi tài khoản tạo khoảng đóng về sau bị xoá. Mất tên người tạo vẫn
    -- hơn mất chính khoảng đóng — cùng lối xử lý với booking_notes.author_id.
    created_by bigint      REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    -- Khoảng rỗng hoặc ngược chiều không phải khoảng đóng. Không có CHECK này
    -- thì daterange(25, 20) ném lỗi thô của PostgreSQL thay vì lỗi có nghĩa.
    CONSTRAINT ck_room_closures_dates CHECK (to_date > from_date)
);

ALTER TABLE room_closures
    ADD CONSTRAINT room_closures_no_overlap
    EXCLUDE USING gist (room_id WITH =, blocked WITH &&);

-- Truy vấn duy nhất không đi qua ràng buộc EXCLUDE: liệt kê khoảng đóng của một
-- phòng cho màn hình quản trị.
CREATE INDEX idx_room_closures_room ON room_closures (room_id, from_date);
