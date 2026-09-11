-- ============================================================================
-- V7 — Ghi chú nội bộ của quản trị viên trên một đơn
--
-- Vì sao cần bảng riêng thay vì dùng booking_status_history.note: mỗi dòng
-- lịch sử trạng thái BẮT BUỘC kèm một lần chuyển trạng thái (from_status →
-- to_status, cả hai đều có CHECK), và máy trạng thái không cho phép chuyển
-- X → X. Nhét ghi chú vào đó thì hoặc phải bịa ra một lần chuyển trạng thái
-- không có thật, hoặc phải ghi thẳng vào bảng ngoài máy trạng thái — cả hai
-- đều làm hỏng thứ mà bảng lịch sử tồn tại để bảo đảm: mỗi dòng ở đó là một
-- lần trạng thái đơn thực sự thay đổi.
--
-- Ghi chú là dữ liệu NỘI BỘ. Nó không bao giờ được trả về cho khách; các API
-- công khai của đơn không đọc bảng này.
-- ============================================================================

CREATE TABLE booking_notes (
    id         bigserial   PRIMARY KEY,
    booking_id bigint      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    -- NULL khi tài khoản viết ghi chú về sau bị xoá. Mất tên người viết vẫn
    -- hơn mất nội dung ghi chú.
    author_id  bigint      REFERENCES users (id) ON DELETE SET NULL,
    content    text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    -- Ghi chú rỗng không phải ghi chú. Chặn ở đây vì một dòng trắng trong
    -- dòng thời gian của đơn chỉ làm nhiễu, không mang thông tin nào.
    CONSTRAINT ck_booking_notes_content CHECK (length(btrim(content)) > 0)
);

CREATE INDEX idx_booking_notes_booking ON booking_notes (booking_id, created_at);
