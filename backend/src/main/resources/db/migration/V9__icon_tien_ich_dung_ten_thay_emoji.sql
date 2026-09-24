-- Cột `amenities.icon` trước đây chứa emoji ('📶', '❄️', '🅿️'…).
--
-- Emoji hỏng ở ba chỗ cùng lúc: mỗi hệ điều hành vẽ một kiểu nên giao diện
-- không nhất quán giữa Windows, Android và iOS; không nhuộm được theo token
-- màu nên nó luôn lạc khỏi bảng màu; và cỡ vẽ không khớp chữ bên cạnh.
--
-- Cột này giờ chứa TÊN icon. Hình do frontend vẽ bằng SVG nội tuyến
-- (shared/ui/icon). Dữ liệu chỉ đặt tên cho ý nghĩa, không mô tả hình dáng.
--
-- Cập nhật theo `code` chứ không theo `icon` cũ: `code` là khoá nghiệp vụ ổn
-- định, còn so khớp chuỗi emoji phụ thuộc cách từng client mã hoá ZWJ và
-- variation selector (U+FE0F) — '❄️' và '❄' là hai chuỗi khác nhau.

UPDATE amenities SET icon = 'wifi'         WHERE code = 'wifi';
UPDATE amenities SET icon = 'dieu-hoa'     WHERE code = 'dieu-hoa';
UPDATE amenities SET icon = 'nuoc-nong'    WHERE code = 'nuoc-nong';
UPDATE amenities SET icon = 'tv'           WHERE code = 'tv';
UPDATE amenities SET icon = 'tu-lanh'      WHERE code = 'tu-lanh';
UPDATE amenities SET icon = 'ban-cong'     WHERE code = 'ban-cong';
UPDATE amenities SET icon = 'ban-lam-viec' WHERE code = 'ban-lam-viec';
UPDATE amenities SET icon = 'bai-do-xe'    WHERE code = 'bai-do-xe';
UPDATE amenities SET icon = 'bua-sang'     WHERE code = 'bua-sang';
UPDATE amenities SET icon = 'san-vuon'     WHERE code = 'san-vuon';
UPDATE amenities SET icon = 'xe-dap'       WHERE code = 'xe-dap';
UPDATE amenities SET icon = 'le-tan'       WHERE code = 'le-tan';

-- Tiện ích do quản trị viên thêm sau này mà chưa có icon tương ứng thì để
-- rỗng, KHÔNG gán một icon bừa: frontend bỏ qua tên lạ và chỉ hiện chữ, như
-- vậy còn đọc được; một cái icon sai nghĩa thì không.
UPDATE amenities SET icon = NULL
WHERE icon IS NOT NULL
  AND code NOT IN ('wifi', 'dieu-hoa', 'nuoc-nong', 'tv', 'tu-lanh', 'ban-cong',
                   'ban-lam-viec', 'bai-do-xe', 'bua-sang', 'san-vuon', 'xe-dap',
                   'le-tan');
