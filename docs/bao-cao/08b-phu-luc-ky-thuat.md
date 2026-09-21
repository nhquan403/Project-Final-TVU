# PHỤ LỤC (tiếp theo)

## Phụ lục C — Mã nguồn migration tạo ràng buộc chống trùng lịch

Trích từ tệp `V3__bookings_and_exclusion.sql`:

```sql
CREATE TABLE booking_rooms (
    id         bigserial   PRIMARY KEY,
    booking_id bigint      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    room_id    bigint      NOT NULL REFERENCES rooms (id),
    check_in   date        NOT NULL,
    check_out  date        NOT NULL,
    stay       daterange   GENERATED ALWAYS AS
                           (daterange(check_in, check_out, '[)')) STORED,
    status     varchar(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_booking_rooms_dates  CHECK (check_out > check_in),
    CONSTRAINT ck_booking_rooms_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

ALTER TABLE booking_rooms
    ADD CONSTRAINT booking_rooms_no_overlap
    EXCLUDE USING gist (room_id WITH =, stay WITH &&)
    WHERE (status = 'ACTIVE');
```

## Phụ lục D — Trigger ràng buộc hoãn

Trích từ cùng tệp migration:

```sql
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

CREATE CONSTRAINT TRIGGER booking_room_count_check
    AFTER INSERT OR UPDATE OR DELETE ON booking_rooms
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_booking_room_count();
```

Trigger được đặt ở **cả hai phía**: khi sửa bảng gán phòng, và khi sửa số lượng
phòng trên đơn. Đặt một phía là để lọt trường hợp còn lại.

## Phụ lục E — Bảng endpoint đầy đủ

Bảng đầy đủ 80 thao tác trên 63 đường dẫn được liệt kê trong tài liệu kỹ thuật
`docs/api.md` của dự án, và xem được trực tiếp qua giao diện tài liệu tương tác
của hệ thống đang chạy ở cấu hình trình diễn.

Cách đếm lại số thao tác từ hệ thống đang chạy:

```bash
curl -s localhost/v3/api-docs | python3 -c "
import json,sys
d = json.load(sys.stdin)
methods = ('get','post','put','delete','patch')
print(sum(len([m for m in v if m in methods]) for v in d['paths'].values()))"
```

## Phụ lục F — Migration bảng khoảng đóng phòng

Trích từ tệp `V8__room_closures.sql`:

```sql
CREATE TABLE room_closures (
    id         bigserial   PRIMARY KEY,
    room_id    bigint      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    from_date  date        NOT NULL,
    to_date    date        NOT NULL,
    blocked    daterange   GENERATED ALWAYS AS
                           (daterange(from_date, to_date, '[)')) STORED,
    reason     varchar(300),
    created_by bigint      REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_room_closures_dates CHECK (to_date > from_date)
);

ALTER TABLE room_closures
    ADD CONSTRAINT room_closures_no_overlap
    EXCLUDE USING gist (room_id WITH =, blocked WITH &&);

CREATE INDEX idx_room_closures_room ON room_closures (room_id, from_date);
```

## Phụ lục G — Cấu hình đóng gói

Tệp cấu hình đóng gói `docker-compose.yml` và cấu hình máy chủ web
`frontend/nginx.conf` được lưu trong kho mã nguồn của dự án.

Điểm đáng lưu ý trong cấu hình máy chủ web là cú pháp tiền tố ưu tiên ở cả bốn
khối chuyển tiếp:

```nginx
location ^~ /api/         { ... }
location ^~ /swagger-ui/  { ... }
location ^~ /v3/api-docs/ { ... }
location ^~ /uploads/     { ... }
```

## Phụ lục H — Lệnh kiểm chứng

| Mục đích | Lệnh |
|---|---|
| Chạy toàn bộ kiểm thử | `cd backend && ./mvnw verify` |
| Biên dịch tầng giao diện | `cd frontend && npm run build` |
| Kiểm tra quy ước mã nguồn | `cd frontend && npm run lint` |
| Dựng và chạy toàn hệ thống | `docker compose up -d --build` |
| Kiểm tra trạng thái các dịch vụ | `docker compose ps` |
| Đếm số bảng trong lược đồ | `SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history';` |

---

## Phụ lục I — Chi tiết 18 lớp kiểm thử

### 4.2.1. Bảng tổng hợp theo lớp

| Lớp kiểm thử | Số ca | Chứng minh điều gì |
|---|---:|---|
| `SchemaMigrationTest` | 8 | Migration dựng đúng **21 bảng**; mọi migration thành công; không migration nào bị sửa sau khi phát hành; cả hai cột kiểu khoảng đều là cột sinh tự động |
| `SchemaConstraintIT` | 9 | Từng ràng buộc kiểm tra thật sự chặn: thư điện tử không chữ thường, trạng thái sai chính tả, số tiền âm, ngày trả không sau ngày nhận, điểm đánh giá ngoài khoảng 1 tới 5 |
| `SqlStatesIT` | 2 | Hai mã lỗi `23P01` và `25P02` được nhận diện đúng |
| **`BookingConcurrencyIT`** | 3 | **Lớp quan trọng nhất.** Nhiều luồng thật cùng đặt phòng cuối — đúng một luồng thắng; sau khi bắt lỗi ràng buộc, vòng thử tiếp theo phải ở giao dịch mới; đơn nhiều phòng gán được nhờ trigger hoãn |
| `AvailabilityQueryIT` | 6 | Đếm phòng trống đúng trong các tình huống dễ đếm sai: phòng bảo trì đang có đơn không bị trừ hai lần; loại phòng hết sạch biến mất khỏi kết quả; khoảng nửa mở cho khách trả và khách nhận cùng ngày; sức chứa so theo từng phòng; lịch trả đủ mọi ngày |
| `BookingLifecycleIT` | 8 | Bảng chuyển trạng thái: bước hợp lệ đi được, bước không hợp lệ bị từ chối. `CHECKED_OUT` không nhả phòng |
| `BookingExpiryIT` | 4 | Bộ quét chuyển đơn quá hạn, nhả phòng, hoàn lượt khuyến mãi; đơn đã có tiền không bị quét |
| **`RoomClosureIT`** | 9 | Khoảng đóng phòng trừ đúng phòng, đúng đêm, ở cả bốn truy vấn; biên nửa mở; lịch không rơi mất ngày; khoảng chồng nhau bị từ chối; đóng phòng không huỷ đơn; khoảng đã qua rời khỏi danh sách thao tác |
| `SepayWebhookIT` | 10 | Sai khoá xác thực bị từ chối; webhook gửi lại không cộng tiền hai lần; đủ tiền thì xác nhận; thiếu tiền thì chờ đối soát; thừa tiền thì đánh dấu hoàn; webhook không khớp đơn nào vẫn được ghi nhật ký |
| **`PaymentRaceIT`** | 5 | Cửa sổ tranh chấp giữa webhook và bộ quét hết hạn; nhánh giành lại phòng tôn trọng khoảng đóng phòng |
| `AuthFlowIT` | 7 | Đăng ký, đăng nhập, làm mới token, đăng xuất; số phiên bản token tăng làm token cũ mất hiệu lực ngay; token làm mới lưu dạng băm |
| **`EndpointAuthorizationIT`** | 3 | Mọi endpoint đã đăng ký nằm trong ma trận 30 tiền tố; cấm khai dòng bao trùm; endpoint không khai gì bị chặn |
| `ContentSanitizerTest` | 17 | Bộ lọc HTML gỡ thẻ kịch bản, chặn giao thức nguy hiểm trong liên kết, gỡ khung nhúng, gỡ ảnh ngoài danh sách nguồn cho phép, giữ thẻ hợp lệ |
| `PublicContentIT` | 7 | Giao diện công khai chỉ trả nội dung đã đăng; nội dung mặc định hiện ra khi chưa soạn; bài viết nháp không lộ |
| `ReviewIT` | 8 | Gửi đánh giá cần xác thực; tên khách chụp từ đơn, không nhận từ dữ liệu gửi lên; ràng buộc duy nhất chặn đánh giá thứ hai; chỉ đánh giá đã duyệt mới công khai |
| `DashboardServiceIT` | 6 | Doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ tính đúng trên dữ liệu thật |
| `CsvExportTest` | 10 | Xuất tệp CSV thoát đúng dấu phẩy, dấu nháy và ký tự xuống dòng trong dữ liệu |
| `ImageUploadIT` | 7 | Từ chối kiểu tệp sai; từ chối tệp vượt dung lượng; giải mã lại ảnh và đổi tên; chỉ quản trị viên tải lên được |
| **Tổng** | **129** | |

---

## Phụ lục J — Bảng đầy đủ mã lỗi

### 3.5.4. Thiết kế mã lỗi

Phản hồi lỗi theo chuẩn RFC 7807 [4] kèm trường `code` tự thêm. Tầng giao diện
đọc trường `code`, **không** đọc câu chữ mô tả, nên việc sửa câu chữ ở tầng máy
chủ không làm hỏng màn hình.

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `ROOM_NOT_AVAILABLE` | 409 | Phòng vừa được đặt hết cho khoảng ngày này |
| `INVALID_PROMOTION` | 400 | Mã không hợp lệ hoặc chưa đủ điều kiện |
| `PROMOTION_EXHAUSTED` | 409 | Mã đã hết lượt |
| `BOOKING_NOT_FOUND` | 404 | Không có đơn khớp mã và số điện thoại |
| `INVALID_ACCESS_TOKEN` | 403 | Mã truy cập sai |
| `INVALID_STATE_TRANSITION` | 409 | Bước chuyển trạng thái không hợp lệ |
| `INVALID_BOOKING_REQUEST` | 400 | Dữ liệu đơn không hợp lệ |
| `INVALID_ADMIN_REQUEST` | 400 | Dữ liệu quản trị không hợp lệ |
| `CLOSURE_OVERLAP` | 409 | Khoảng đóng phòng chồng lên khoảng đã có |
| `RESOURCE_IN_USE` | 409 | Còn dữ liệu tham chiếu tới bản ghi cần xoá |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Phải đổi mật khẩu tạm trước |
| `UNSUPPORTED_IMAGE_TYPE` | 415 | Định dạng ảnh không được chấp nhận |
| `IMAGE_TOO_LARGE` | 413 | Ảnh vượt giới hạn dung lượng |
| `TOO_MANY_REQUESTS` | 429 | Vượt giới hạn tần suất |
| `UNAUTHORIZED` | 401 | Thiếu hoặc sai token |

---

## Phụ lục K — Danh sách 32 yêu cầu chức năng

### 1.4.1. Yêu cầu chức năng

**Nhóm A — Dành cho khách**

| Mã | Yêu cầu |
|---|---|
| CN-01 | Xem danh sách loại phòng kèm tiện nghi, hình ảnh, giá |
| CN-02 | Xem chi tiết một loại phòng, xem thư viện ảnh |
| CN-03 | Tìm phòng trống theo khoảng ngày, số người lớn, trẻ em, số phòng |
| CN-04 | Xem lịch từng đêm: đêm nào còn phòng, đêm nào hết |
| CN-05 | Gửi yêu cầu đặt phòng, nhận mã đơn |
| CN-06 | Xem chi phí dự kiến trước khi xác nhận đơn |
| CN-07 | Áp dụng mã khuyến mãi và thấy ngay số tiền được giảm |
| CN-08 | Thanh toán đặt cọc bằng mã QR |
| CN-09 | Tra cứu đơn bằng mã đơn và số điện thoại |
| CN-10 | Huỷ đơn |
| CN-11 | Nhận thư xác nhận qua thư điện tử |
| CN-12 | Viết đánh giá sau khi trả phòng |
| CN-13 | Đăng ký, đăng nhập, xem lại toàn bộ đơn đã đặt |
| CN-14 | Đọc tin tức, xem thư viện ảnh của homestay |

**Nhóm B — Dành cho quản trị viên**

| Mã | Yêu cầu |
|---|---|
| CN-15 | Đăng nhập khu quản trị; buộc đổi mật khẩu tạm ở lần đầu |
| CN-16 | Xem danh sách đơn, lọc theo trạng thái và khoảng ngày |
| CN-17 | Xem chi tiết đơn: lịch sử trạng thái, thanh toán, thư đã gửi, ghi chú |
| CN-18 | Chuyển trạng thái đơn theo máy trạng thái hợp lệ |
| CN-19 | Ghi chú nội bộ trên đơn |
| CN-20 | Đối soát thanh toán: xác nhận thủ công, xử lý khoản lệch |
| CN-21 | Quản lý loại phòng: thêm, sửa, xoá, gán tiện nghi, tải ảnh |
| CN-22 | Quản lý phòng vật lý: thêm, sửa, đổi trạng thái vận hành |
| CN-23 | **Đóng phòng theo khoảng ngày** — quản lý ngày khả dụng |
| CN-24 | Quản lý mã khuyến mãi |
| CN-25 | Duyệt, từ chối, trả lời đánh giá của khách |
| CN-26 | Quản lý nội dung trang chủ, biểu ngữ, thư viện ảnh, tin tức |
| CN-27 | Xem tổng quan: doanh thu, tỉ lệ lấp đầy, tỉ lệ huỷ |
| CN-28 | Xuất báo cáo đơn ra tệp định dạng CSV |

**Nhóm C — Hệ thống tự động**

| Mã | Yêu cầu |
|---|---|
| CN-29 | Nhận webhook khi có tiền về, tự đối chiếu và xác nhận đơn |
| CN-30 | Tự chuyển đơn quá hạn giữ chỗ sang trạng thái hết hạn và nhả phòng |
| CN-31 | Gửi thư qua hàng đợi, ghi nhận mọi thư đã gửi |
| CN-32 | Xử lý tiền về muộn: mở lại đơn, thử giành lại phòng |

---

## Phụ lục L — Bảng tổng hợp công nghệ

| Tầng | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ tầng máy chủ | Java | 21 (LTS) |
| Nền tảng tầng máy chủ | Spring Boot | 3.5.6 |
| Truy cập dữ liệu | Spring Data JPA / Hibernate, Spring JDBC | theo nền tảng |
| Bảo mật | Spring Security và JWT | theo nền tảng |
| Cơ sở dữ liệu | PostgreSQL | 16 |
| Quản lý lược đồ | Flyway | theo nền tảng |
| Giới hạn tần suất | bucket4j | — |
| Lọc HTML | OWASP java-html-sanitizer | 20240325.1 |
| Kiểm thử | JUnit 5, AssertJ, Testcontainers | — |
| Ngôn ngữ tầng giao diện | TypeScript | theo Angular |
| Nền tảng tầng giao diện | Angular | 21 |
| Định kiểu | Tailwind CSS | 4 |
| Máy chủ web | nginx | alpine |
| Đóng gói | Docker, Docker Compose | Engine 24+, Compose v2 |
| Thư trong môi trường trình diễn | Mailpit | — |
| Thanh toán | SePay và VietQR | — |

---

## Phụ lục M — Chi tiết kiểm thử bảo mật

| Kịch bản tấn công | Kết quả |
|---|---|
| Giả mạo tiêu đề địa chỉ để vượt giới hạn tần suất | **Thất bại** — vẫn bị chặn ở đúng ngưỡng |
| Gọi endpoint quản trị không có token | Từ chối, mã 401 |
| Gọi endpoint quản trị bằng token vai trò khách | Từ chối, mã 403 |
| Dùng token cũ sau khi đã đổi mật khẩu | Từ chối, mã 401 — token cũ mất hiệu lực ngay |
| Gọi webhook không có khoá xác thực | Từ chối |
| Gửi lại cùng một webhook lần hai | Bỏ qua, không cộng tiền hai lần |
| Chèn thẻ kịch bản vào nội dung quản lý | Bị bộ lọc gỡ bỏ |
| Xem trạng thái thanh toán chỉ bằng mã đơn | Từ chối — bắt buộc mã truy cập |
| Truy cập đơn của người khác qua tham số đường dẫn | Từ chối — lọc theo token của phiên |

---

## Phụ lục N — Danh sách kiểm thử thủ công

Danh sách tối thiểu cần chạy trên bản đóng gói:

1. Trang chủ có nội dung thật: 4 loại phòng, thư viện ảnh, đánh giá, tin tức.
2. Đặt một đơn từ đầu đến khi ra mã QR.
3. Tải lại trang ở màn hình thanh toán — vẫn thấy mã QR và đồng hồ đếm ngược.
4. Tra cứu đơn bằng mã và số điện thoại; thử sai số điện thoại thì bị từ chối.
5. Huỷ đơn; kiểm tra phòng mở lại bằng cách đặt lại đúng ngày đó.
6. Đăng nhập quản trị, bị buộc đổi mật khẩu, rồi vào trang tổng quan.
7. Màn hình đối soát có sẵn khoản cần xử lý.
8. Sửa một khối nội dung trang chủ rồi mở lại trang chủ để thấy thay đổi.
9. Mở hộp thư giả lập xem thư xác nhận đã gửi.
10. Đóng một phòng vài ngày; tìm phòng đúng khoảng đó thấy số phòng giảm đúng
    một, và đêm mở bán lại **không** giảm.

---

## Phụ lục O — Mười hai yêu cầu phi chức năng

Bảng đầy đủ của mục 1.4.2. Cột cuối là cách kiểm chứng, không phải lời hứa.

| Mã | Loại | Yêu cầu | Cách kiểm chứng |
|---|---|---|---|
| PCN-01 | Toàn vẹn | Không bao giờ bán trùng một phòng, kể cả khi có tranh chấp | Kiểm thử đa luồng thật |
| PCN-02 | Toàn vẹn | Số phòng đã gán luôn khớp số phòng đơn yêu cầu | Trigger hoãn ở cơ sở dữ liệu |
| PCN-03 | Toàn vẹn | Không nhánh nào để tiền biến mất im lặng | Kiểm thử cửa sổ tranh chấp webhook |
| PCN-04 | Bảo mật | Mặc định từ chối mọi endpoint chưa khai quyền | Kiểm thử ma trận 30 tiền tố |
| PCN-05 | Bảo mật | Chống dò mật khẩu và spam đặt phòng | Giới hạn tần suất theo 9 khoá |
| PCN-06 | Bảo mật | Không có bí mật nào nằm trong mã nguồn | Ứng dụng dừng khởi động khi thiếu biến |
| PCN-07 | Khả dụng | Giao diện sử dụng được trên điện thoại | Kiểm ở 3 độ rộng màn hình |
| PCN-08 | Khả dụng | Đạt chuẩn WCAG 2.1 mức A và AA [13] | Quét tự động |
| PCN-09 | Khả dụng | Mọi vùng chạm tối thiểu 44×44 điểm ảnh | Đo tự động |
| PCN-10 | Hiệu năng | Lịch cả kỳ lấy trong một truy vấn, không lặp từng đêm | Đọc mã truy vấn |
| PCN-11 | Triển khai | Ba lệnh từ lúc sao chép mã nguồn tới lúc chạy | Thực nghiệm |
| PCN-12 | Bảo trì | Lược đồ do công cụ migration quản lý, migration bất biến | Kiểm thử checksum |

---

## Phụ lục P — Cây gói tầng máy chủ

Cây gói đầy đủ của mục 3.4.2.

```
com.tvh.homestay
├── auth/          — đăng nhập, token, phiên làm việc
├── user/          — người dùng
├── room/          — loại phòng, phòng, tiện nghi, khoảng đóng
├── availability/  — truy vấn phòng trống
├── booking/       — đơn, máy trạng thái, gán phòng, tính giá
├── payment/       — webhook, đối soát
├── promotion/     — mã khuyến mãi
├── review/        — đánh giá
├── cms/           — thực thể nội dung trang chủ
├── content/       — điều khiển nội dung công khai và quản trị
├── admin/         — điều khiển và dịch vụ khu quản trị
├── report/        — tổng quan, xuất tệp CSV
├── storage/       — tải ảnh lên
├── mail/          — hàng đợi thư
├── common/        — xử lý ngoại lệ, tiện ích dùng chung
└── demo/          — nạp dữ liệu mẫu, chỉ hoạt động ở cấu hình trình diễn
```

---

## Phụ lục Q — Chín khoá giới hạn tần suất

Hạn mức đầy đủ của mục 3.5.3.

| Khoá | Hạn mức | Áp dụng cho |
|---|---|---|
| `ip:auth` | 10 lần mỗi phút | Mọi endpoint xác thực |
| `email:login` | 5 lần mỗi phút | Đăng nhập, theo thư điện tử trong thân yêu cầu |
| `ip:availability` | 60 lần mỗi phút | Truy vấn phòng trống |
| `ip:promo` | 20 lần mỗi phút | Kiểm tra mã khuyến mãi |
| `ip:booking-create` | 10 lần mỗi phút | Tạo đơn |
| `phone:booking` | 10 lần mỗi giờ | Tạo đơn, theo số điện thoại |
| `ip:lookup` | 10 lần mỗi phút | Tra cứu đơn |
| `code:lookup` | 5 lần mỗi giờ | Tra cứu đơn, theo mã đơn |
| `ip:cancel` | 10 lần mỗi phút | Huỷ đơn |
