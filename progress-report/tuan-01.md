# Báo cáo tiến độ — Tuần 01

| | |
|---|---|
| **Họ và tên** | . . . . . . . . . . . . . . . . . . . . |
| **MSSV** | . . . . . . . . . . |
| **Lớp** | . . . . . . . . . . |
| **GVHD** | . . . . . . . . . . . . . . . . . . . . |
| **Tên đề tài** | Xây dựng website giới thiệu và đặt phòng homestay — Homestay TVH |
| **Kỳ báo cáo** | từ 07/09/2026 đến 12/09/2026 |

## 1. Công việc đã hoàn thành trong tuần

| # | Công việc | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Lập kế hoạch thực hiện, rà soát lại kế hoạch bằng một vòng phản biện | Kế hoạch chia giai đoạn, mỗi giai đoạn có tiêu chí nghiệm thu đo được | `6bb94c4`, `d6a8d8e` |
| 2 | Chốt hệ màu và hệ thiết kế giao diện | Mọi mã màu tập trung ở một tệp token, có script chặn mã màu viết rải rác | `3f3f4bc`, `a355826`, `77b2a7d` |
| 3 | Dựng khung dự án hai tầng | Thư mục `backend/` Java 21 + Spring Boot, `frontend/` Angular 21 | `c82d38d` |
| 4 | **Lược đồ cơ sở dữ liệu và ràng buộc chống đặt trùng phòng** | `EXCLUDE USING gist` trên cột `daterange` sinh tự động — lõi kỹ thuật của đề tài | `d759b80` |
| 5 | Xác thực bằng JWT, thu hồi token tức thì, giới hạn tần suất | Đổi mật khẩu hoặc đăng xuất là mọi token cũ hết hiệu lực ngay | `1f927f9` |
| 6 | Truy vấn phòng trống và lõi đặt phòng | Vòng thử gán phòng, mỗi lượt thử một giao dịch mới | `2facb9d` |
| 7 | Thanh toán: mã QR, webhook, hàng đợi thư | Webhook đi qua năm lớp kiểm tra trước khi ảnh hưởng tới dữ liệu | `20d4535` |
| 8 | Khu quản trị: tổng quan, tải ảnh, xuất CSV | Chín màn hình quản trị đầu tiên | `9a2acfe` |
| 9 | Trang bán hàng và luồng đặt phòng cho khách | Ba bước đặt phòng, bốn màn hình quản lý nội dung | `af71cf9`, `10378b9` |

## 2. Công việc chưa hoàn thành

| # | Công việc | Lý do | Hướng xử lý |
|---|---|---|---|
| 1 | Đóng gói bằng Docker | Ưu tiên làm xong nghiệp vụ trước | Làm ở tuần 02 |
| 2 | Quản lý ngày khả dụng của phòng | Phát sinh sau khi rà lại yêu cầu | Làm ở tuần 02 |

## 3. Khó khăn gặp phải

**Bắt được lỗi ràng buộc rồi thử phòng tiếp theo trong cùng giao dịch thì luôn
thất bại.** PostgreSQL đã huỷ giao dịch khi ràng buộc loại trừ bị vi phạm, nên
mọi lệnh sau đó trả `25P02` chứ không phải lỗi nghiệp vụ. Khắc phục bằng cách
cho **mỗi lượt thử chạy trong một giao dịch mới**.

**Toàn bộ trang web mất sạch định kiểu ở bản dựng thật.** Chính sách bảo mật nội
dung chặn thuộc tính sự kiện mà công cụ tối ưu định kiểu tự sinh ra. Khắc phục
bằng cách tắt tính năng tối ưu đó, **không nới lỏng chính sách bảo mật**.

## 4. Kế hoạch tuần tới

| # | Công việc dự kiến | Kết quả mong đợi |
|---|---|---|
| 1 | Đóng gói toàn hệ thống bằng Docker Compose | Ba lệnh từ lúc sao chép mã tới lúc chạy được |
| 2 | Dữ liệu mẫu đủ mọi trạng thái đơn | Mở lên là có số liệu để xem, không phải tự nhập |
| 3 | Quản lý ngày khả dụng của phòng | Đóng phòng theo khoảng ngày, hệ thống tự mở lại |
| 4 | Tài liệu kỹ thuật | Đủ tài liệu cho người khác tiếp nhận dự án |

## 5. Commit trong tuần

**Kỳ báo cáo:** từ 2026-09-07 đến 2026-09-12 — **21 commit**

| Mã commit | Nội dung |
|---|---|
| `6bb94c4` | docs: add implementation plan for Homestay TVH booking site |
| `d6a8d8e` | docs: apply adversarial review findings to Homestay TVH plan |
| `3f3f4bc` | docs: add design system phase and raise the UI/UX bar |
| `a355826` | docs: confirm the palette as the project's brand definition |
| `c82d38d` | feat: scaffold monorepo skeleton with backend and frontend |
| `77b2a7d` | feat(frontend): add design system and 15-component UI library |
| `de26f7e` | docs: mark phases 1 and 2 as done |
| `d759b80` | feat(backend): add database schema with overlap-proof booking constraint |
| `0210966` | docs: mark phase 3 as done |
| `1f927f9` | feat(auth): add JWT authentication with instant revocation and rate limiting |
| `292c446` | docs: mark phase 4 as done |
| `2facb9d` | feat(booking): add availability query and overlap-proof booking core |
| `554181d` | docs: mark phase 5 as done |
| `20d4535` | feat(payment): add SePay webhook, VietQR payment and email outbox |
| `a0f651c` | docs: mark phase 6 as done and open the late-payment state edge |
| `9a2acfe` | feat(admin): add admin area, dashboard metrics, image upload and CSV export |
| `e0442ea` | docs: mark phase 7 as done and correct two claims it disproved |
| `bbce1ec` | docs: record that the defence machine has Docker and network |
| `af71cf9` | feat(content): thêm API công khai cho landing page và CMS nội dung |
| `193d79e` | fix(ui): chữ nút chính, nhãn sao và màn hình QR trắng trơn |
| `10378b9` | feat(landing): trang bán hàng, luồng đặt phòng và bốn màn hình CMS |

---

*Ngày lập: 12/09/2026*
*Sinh viên thực hiện*
