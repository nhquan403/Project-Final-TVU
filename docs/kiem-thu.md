# Kiểm thử

**129 test, 18 lớp**, tất cả xanh. Chạy:

```bash
cd backend && ./mvnw verify
```

`verify` cần **Docker**: mọi test tích hợp dựng một PostgreSQL 16 thật bằng
Testcontainers. Không mock cơ sở dữ liệu, vì phần lớn thứ đáng kiểm ở dự án này
*chính là* hành vi của cơ sở dữ liệu — ràng buộc `EXCLUDE`, kiểu `daterange`,
trigger hoãn, mã `SQLSTATE`. Mock chúng là kiểm thử một thứ không tồn tại.

Dự án **không** cấu hình failsafe. Surefire được nới để nhận cả lớp hậu tố `*IT`,
nên `verify` = biên dịch + chạy 129 test + đóng gói, không có pha
integration-test riêng.

## Từng lớp chứng minh điều gì

### Lược đồ và ràng buộc

| Lớp | Test | Chứng minh |
|---|---:|---|
| `SchemaMigrationTest` | 8 | Migration dựng đúng **21 bảng**; mọi migration `success`; không migration nào bị sửa sau khi phát hành (checksum); cả hai cột `daterange` đều là cột sinh tự động. Đây là lớp canh nguyên tắc "migration bất biến". |
| `SchemaConstraintIT` | 9 | Từng ràng buộc `CHECK` thật sự chặn: email không chữ thường, trạng thái sai chính tả, số tiền âm, `check_out <= check_in`, `rating` ngoài 1–5. Ràng buộc không được kiểm là ràng buộc có thể đã bị viết sai từ đầu mà không ai biết. |
| `SqlStatesIT` | 2 | `SQLSTATE 23P01` (exclusion violation) và `25P02` (giao dịch đã hỏng) được dịch đúng. Lớp này tồn tại vì toàn bộ chiến lược xử lý tranh chấp dựa trên việc đọc đúng hai mã đó. |

### Đặt phòng — phần lõi

| Lớp | Test | Chứng minh |
|---|---:|---|
| **`BookingConcurrencyIT`** | 3 | **Lớp quan trọng nhất của dự án.** Ba kịch bản với luồng thật chạy song song: (1) nhiều luồng cùng đặt phòng cuối — đúng một luồng thắng, không bán trùng; (2) sau khi bắt `23P01`, vòng thử phòng tiếp theo phải ở giao dịch MỚI; (3) đơn nhiều phòng gán được nhờ trigger `DEFERRABLE`. |
| `AvailabilityQueryIT` | 6 | Đếm phòng trống đúng trong những tình huống dễ đếm sai: phòng `MAINTENANCE` đang có đơn **không** bị trừ hai lần; loại phòng hết sạch biến mất khỏi kết quả; khoảng nửa mở `[)` cho khách A trả và khách B nhận cùng ngày; sức chứa so theo **từng phòng**, không so tổng khách; lịch giá trả đủ mọi ngày kể cả ngày đã kín; lịch không kèm `roomTypeId` gộp mọi loại phòng. |
| `BookingLifecycleIT` | 8 | Bảng chuyển trạng thái: bước hợp lệ đi được, bước không hợp lệ bị `INVALID_STATE_TRANSITION`. `CHECKED_OUT` **không** nhả phòng — bài kiểm này sinh ra từ một lỗi thật: nhả phòng lúc trả phòng làm trigger `assert_booking_room_count` bác giao dịch. |
| `BookingExpiryIT` | 4 | Bộ quét chuyển đơn quá hạn sang `EXPIRED`, nhả phòng, hoàn lượt khuyến mãi; đơn `PARTIAL` (đã có tiền) **không** bị quét — quét nhầm là xoá một đơn đã trả tiền thật. |
| **`RoomClosureIT`** | 9 | Khoảng đóng phòng trừ đúng phòng và đúng đêm ở **cả bốn** truy vấn phòng trống. Bài quan trọng nhất là `closedRoomIsNeverAssignedToNewBooking`: sót truy vấn chọn phòng vật lý thì đơn vẫn tạo được và phòng đang sửa chữa vẫn bị gán — lỗi im lặng chỉ lộ ra khi khách tới nhận phòng. Kèm biên nửa mở, lịch không rơi mất ngày, chồng khoảng bị `23P01`, và đóng phòng **không** huỷ đơn. |

### Thanh toán

| Lớp | Test | Chứng minh |
|---|---:|---|
| `SepayWebhookIT` | 10 | Sai khoá API bị từ chối; webhook gửi lại không cộng tiền hai lần (`UNIQUE (provider, external_id)`); đủ tiền → `CONFIRMED`; thiếu tiền → `AWAITING_REVIEW` + `NEEDS_REVIEW`; thừa tiền → `REFUND_REQUIRED`; webhook không khớp đơn nào vẫn được ghi nhật ký. |
| **`PaymentRaceIT`** | 5 | Cửa sổ tranh chấp giữa webhook và bộ quét hết hạn. Tiền về **sau khi** đơn đã `EXPIRED`: đơn mở lại sang `AWAITING_REVIEW`, thử gán phòng lại; không gán được thì vào hàng đợi hoàn tiền. Nhánh giành lại phòng cũng phải tôn trọng **khoảng đóng phòng** — bài kiểm đó quan trọng vì nhánh này chạy trong webhook chứ không theo cú bấm của ai, nên sai ở đây là sai im lặng. Đây là bài kiểm cho tiêu chí "không nhánh nào để tiền biến mất im lặng". |

### Xác thực và phân quyền

| Lớp | Test | Chứng minh |
|---|---:|---|
| `AuthFlowIT` | 7 | Đăng ký, đăng nhập, làm mới token, đăng xuất; `token_version` tăng làm token cũ chết ngay; refresh token lưu dạng hash. |
| **`EndpointAuthorizationIT`** | 3 | Mọi endpoint đã đăng ký đều nằm trong ma trận 30 tiền tố; **cấm** khai lại dòng bao `/api/admin`; endpoint không khai gì bị `denyAll()` chặn. Lớp này biến "nhớ phân quyền" thành "không thể quên". |

### Nội dung và đánh giá

| Lớp | Test | Chứng minh |
|---|---:|---|
| `ContentSanitizerTest` | 17 | Bộ lọc HTML: `<script>` bị gỡ, `javascript:` trong `href` bị chặn, `<iframe>`/`<object>` bị gỡ, ảnh ngoài danh sách nguồn cho phép bị gỡ, thẻ hợp lệ được giữ, liên kết ra ngoài được gắn `rel="nofollow"`, và URL tương đối vẫn dùng được. |
| `PublicContentIT` | 7 | API công khai chỉ trả nội dung **đã đăng**; nội dung mặc định hiện ra khi CMS còn trống; bài viết nháp không lộ qua API. |
| `ReviewIT` | 8 | Gửi đánh giá cần mã truy cập hoặc số điện thoại; tên khách chụp **từ đơn**, không nhận từ client; `UNIQUE (booking_id)` chặn đánh giá thứ hai; chỉ đánh giá `APPROVED` mới ra công khai. |

### Báo cáo và lưu trữ

| Lớp | Test | Chứng minh |
|---|---:|---|
| `DashboardServiceIT` | 6 | Doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ tính đúng trên dữ liệu thật; tỉ lệ lấp đầy dùng mẫu số cố định nên không vượt 100% và không đổi hồi tố. |
| `CsvExportTest` | 10 | Xuất CSV thoát dấu phẩy, dấu nháy và xuống dòng trong dữ liệu; tiêu đề cột khớp. |
| `ImageUploadIT` | 7 | Từ chối kiểu tệp sai; từ chối tệp vượt 5MB; giải mã lại ảnh và đổi tên thành UUID; chỉ `ADMIN` tải lên được. |

## Kiểm thử phía frontend

Frontend không có bộ test tự động. Chất lượng được canh bằng ba cổng chạy trong
`npm run lint` và `npm run build`:

```bash
cd frontend
npm run build    # biên dịch + kiểm kiểu + kiểm template của Angular
npm run lint     # check-hardcoded-colors.mjs, rồi ng lint
```

`scripts/check-hardcoded-colors.mjs` chặn mọi mã màu nằm ngoài `tokens.css`.
Xoá bảng màu mặc định của Tailwind chặn được `text-blue-500`, nhưng không chặn
được ai đó viết thẳng `style="color:#3b82f6"` trong component — hai lớp bổ sung
cho nhau.

Kiểm thử giao diện trong các phase trước làm bằng trình duyệt thật (Playwright)
và được ghi lại trong báo cáo từng phase, không phải một bộ test thường trực.
Đây là một **giới hạn đã biết**: hồi quy giao diện hiện phải phát hiện bằng mắt.

## Kiểm thử thủ công trước khi bảo vệ

Danh sách tối thiểu nên chạy tay trên bản đóng gói:

1. Trang chủ có nội dung thật: 4 loại phòng, thư viện ảnh, đánh giá, tin tức.
2. Đặt một đơn từ đầu đến khi ra mã QR.
3. F5 ở màn hình QR — vẫn thấy QR và đồng hồ đếm ngược.
4. Tra cứu đơn bằng mã + số điện thoại; thử sai số điện thoại → bị từ chối.
5. Huỷ đơn; kiểm tra phòng mở lại (đặt lại đúng ngày đó được).
6. Đăng nhập quản trị → bị buộc đổi mật khẩu → vào dashboard.
7. `/admin/payments` có sẵn khoản cần đối soát.
8. Sửa khối "hero" trong CMS → mở lại trang chủ thấy đổi.
9. Mở `localhost:8025` xem thư xác nhận đã gửi.
10. `/admin/rooms` → khối "Ngày không nhận khách": đóng một phòng vài ngày, tìm
    phòng đúng khoảng đó thấy số phòng giảm 1, đêm mở lại **không** giảm.

Dữ liệu mẫu có sẵn một khoảng đóng ở `CURRENT_DATE + 40 .. + 45` — cố ý đặt xa
khoảng ngày hội đồng thường thử đặt, nên nó không che mất phòng lúc trình diễn.
