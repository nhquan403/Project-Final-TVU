# Ảnh minh hoạ cho dữ liệu mẫu

26 tệp SVG phẳng do dự án tự vẽ, dùng đúng bảng màu trong
`src/styles/tokens.css`. `DemoDataSeeder` trỏ dữ liệu mẫu vào những đường dẫn
này.

**Vì sao là SVG tự vẽ, không phải ảnh chụp tải về:** bản đem đi bảo vệ không
được phụ thuộc vào việc phòng bảo vệ có mạng hay không, cũng không được phụ
thuộc vào việc một máy chủ ảnh của người khác còn sống hay không. 26 tệp này
cộng lại khoảng 100KB và nằm ngay trong repo.

**Vì sao nằm ở `public/` chứ không phải `src/`:** chúng chứa mã màu ở dạng hex,
và `scripts/check-hardcoded-colors.mjs` chỉ quét `src/`. Đó là đúng — lint đó
sinh ra để chặn mã màu lọt vào *mã giao diện*, nơi mọi màu phải đọc từ token.
Tài nguyên tĩnh là chuyện khác: một tệp SVG không có cách nào đọc biến CSS của
trang khi nó được nạp qua thẻ `<img>`.

**Không vẽ chữ vào những tệp này.** Tên loại phòng, chú thích thư viện ảnh và
tiêu đề khuyến mãi đều đã hiện thành chữ THẬT ngay cạnh ảnh trong giao diện.
Vẽ thêm vào ảnh là in cùng một câu hai lần, và chữ trong SVG không phóng to
theo thiết lập cỡ chữ của người dùng.

**Nền sáng, không nền kín màu đậm.** Bốn ảnh loại phòng nằm cạnh nhau trên cùng
một lưới thẻ; một tấm nền đậm giữa ba tấm nền sáng trông như thẻ bị lỗi chứ
không phải một loại phòng khác.

**Thay bằng ảnh thật:** chép ảnh chụp vào đây với đúng tên tệp cũ, hoặc đổi
đường dẫn trong `backend/src/main/resources/db/seed/demo-data.sql`. Không cần
sửa một dòng mã nào.

Ảnh dự phòng khi một loại phòng chưa có ảnh nào là
`../room-placeholder.svg` — tệp khác, mục đích khác.
