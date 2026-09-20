## 3.2. Thiết kế cơ sở dữ liệu

### 3.2.1. Tổng quan

Lược đồ gồm **21 bảng nghiệp vụ**, dựng bởi **tám migration** Flyway từ `V1` tới
`V8`. Bảng thứ 22 trong lược đồ công khai là sổ ghi chép của chính Flyway, không
thuộc mô hình nghiệp vụ.

Hình 3.3 thể hiện sơ đồ quan hệ thực thể tổng thể.

[Hình 3.3]

| Nhóm | Migration | Các bảng | Vai trò |
|---|---|---|---|
| Người dùng và xác thực | V1 | `users`, `refresh_tokens` | Tài khoản, vai trò, phiên đăng nhập |
| Phòng và tiện nghi | V2 | `amenities`, `room_types`, `rooms`, `room_type_images`, `room_type_amenities` | Danh mục sản phẩm |
| Đặt phòng | V3 | `bookings`, `booking_rooms`, `booking_status_history` | **Lõi nghiệp vụ** |
| Thanh toán và thư | V4 | `payments`, `payment_webhook_events`, `outbound_emails` | Tiền và liên lạc |
| Khuyến mãi và đánh giá | V5 | `promotions`, `reviews` | Tiếp thị và uy tín |
| Nội dung | V6 | `site_contents`, `banners`, `gallery_images`, `posts` | Trang chủ, tin tức |
| Ghi chú nội bộ | V7 | `booking_notes` | Trao đổi nội bộ trên đơn |
| Ngày khả dụng | V8 | `room_closures` | Khoảng ngày phòng không nhận khách |

Hình 3.4 thể hiện chi tiết nhóm bảng đặt phòng, nơi đặt ràng buộc quan trọng
nhất của hệ thống.

[Hình 3.4]

### 3.2.2. Mô tả chi tiết các bảng

### Nhóm V1 — Người dùng và xác thực

**Bảng `users` — tài khoản người dùng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `email` | varchar(255) | `uq_users_email` duy nhất; `ck_users_email_lower` buộc lưu chữ thường |
| `password_hash` | varchar(72) | Băm mật khẩu theo thuật toán bcrypt |
| `full_name` | varchar(150) | Họ tên, bắt buộc |
| `phone` | varchar(20) | Số điện thoại, có thể rỗng |
| `role` | varchar(20) | `ck_users_role` giới hạn hai giá trị `CUSTOMER` và `ADMIN` |
| `enabled` | boolean | Mặc định bật; tắt để khoá tài khoản |
| `must_change_password` | boolean | Cờ buộc đổi mật khẩu tạm ở lần đăng nhập đầu |
| `token_version` | integer | `ck_users_token_version` không âm. Tăng khi đăng xuất hoặc đổi mật khẩu để vô hiệu hoá token cũ ngay |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Ràng buộc `ck_users_email_lower` buộc lưu email chữ thường **ở tầng dữ liệu**
thay vì phó mặc cho tầng ứng dụng. Chỉ cần một chỗ quên chuẩn hoá là tạo được
hai tài khoản cho cùng một hộp thư.

**Bảng `refresh_tokens` — phiên đăng nhập dài hạn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `user_id` | bigint | Khoá ngoại tới `users`, xoá theo tầng |
| `token_hash` | char(64) | `uq_refresh_tokens_hash` duy nhất. **Lưu băm, không lưu token gốc** |
| `expires_at` | timestamptz | Thời điểm hết hạn |
| `revoked_at` | timestamptz | Thời điểm thu hồi, rỗng khi còn hiệu lực |
| `replaced_by` | char(64) | Băm của token kế nhiệm sau khi xoay vòng |
| `created_at` | timestamptz | Thời điểm tạo |

Cột `replaced_by` phục vụ phát hiện token bị đánh cắp: nếu một token đã bị thay
thế lại được sử dụng, đó là dấu hiệu bất thường và cả chuỗi token bị thu hồi.

### Nhóm V2 — Phòng và tiện nghi

**Bảng `amenities` — tiện nghi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_amenities_code` duy nhất |
| `name` | varchar(100) | Tên hiển thị |
| `icon` | varchar(50) | Mã biểu tượng |
| `category` | varchar(20) | `ck_amenities_category` giới hạn `ROOM` và `PROPERTY` |
| `display_order` | integer | Thứ tự hiển thị |

**Bảng `room_types` — loại phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_room_types_code` duy nhất |
| `slug` | varchar(120) | `uq_room_types_slug` duy nhất, dùng trong đường dẫn |
| `name` | varchar(150) | Tên loại phòng |
| `short_description`, `description` | text | Mô tả ngắn và mô tả đầy đủ |
| `base_price` | numeric(12,2) | `ck_room_types_price` phải lớn hơn 0 |
| `capacity_adults` | integer | `ck_room_types_adults` tối thiểu 1 |
| `capacity_children` | integer | `ck_room_types_children` không âm |
| `bed_info` | varchar(150) | Mô tả giường |
| `area_sqm` | numeric(6,2) | `ck_room_types_area` rỗng hoặc lớn hơn 0 |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Tắt để ngừng bán loại phòng |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

**Bảng `rooms` — phòng vật lý**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `room_type_id` | bigint | Khoá ngoại tới `room_types` |
| `room_number` | varchar(20) | `uq_rooms_number` duy nhất |
| `floor` | integer | Tầng, có thể rỗng |
| `status` | varchar(20) | `ck_rooms_status` giới hạn `AVAILABLE`, `MAINTENANCE`, `OUT_OF_SERVICE` |
| `note` | text | Ghi chú nội bộ |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Đây là bảng mà ràng buộc chống trùng lịch đặt lên. Chỉ mục
`idx_rooms_type_status` phục vụ truy vấn đếm phòng trống theo loại.

**Bảng `room_type_images` — ảnh của loại phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `room_type_id` | bigint | Khoá ngoại, xoá theo tầng |
| `url` | varchar(500) | Đường dẫn ảnh |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh, rỗng khi lưu cục bộ |
| `alt_text` | varchar(255) | Văn bản thay thế phục vụ khả năng tiếp cận |
| `display_order` | integer | Thứ tự hiển thị |
| `is_cover` | boolean | Ảnh bìa. Chỉ mục duy nhất từng phần `uq_room_type_images_cover` bảo đảm mỗi loại phòng có tối đa một ảnh bìa |

**Bảng `room_type_amenities` — bảng nối loại phòng và tiện nghi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `room_type_id` | bigint | Khoá ngoại, xoá theo tầng |
| `amenity_id` | bigint | Khoá ngoại, xoá theo tầng |
| | | `pk_room_type_amenities` khoá chính tổ hợp hai cột |

Đây là bảng duy nhất trong lược đồ **không có thực thể riêng** trong mã nguồn.
Quan hệ nhiều–nhiều giữa loại phòng và tiện nghi được ánh xạ trực tiếp, nên tổng
số thực thể là **20** trong khi tổng số bảng là **21**.

### Nhóm V3 — Đặt phòng

**Bảng `bookings` — đơn đặt phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(20) | `uq_bookings_code` duy nhất. Mã đơn khách dùng để tra cứu |
| `access_token` | char(32) | `uq_bookings_access_token` duy nhất. **Mã truy cập — bí mật thao tác, khác mã đơn** |
| `user_id` | bigint | Khoá ngoại tới `users`, **rỗng với khách vãng lai** |
| `guest_name` | varchar(150) | Họ tên khách |
| `guest_email` | varchar(255) | Thư điện tử khách |
| `guest_phone` | varchar(20) | Số điện thoại khách |
| `check_in`, `check_out` | date | `ck_bookings_dates` buộc ngày trả sau ngày nhận |
| `adults` | integer | `ck_bookings_adults` tối thiểu 1 |
| `children` | integer | `ck_bookings_children` không âm |
| `room_type_id` | bigint | Khoá ngoại tới `room_types` |
| `room_type_name_snapshot` | varchar(150) | Tên loại phòng chụp tại thời điểm đặt |
| `unit_price_snapshot` | numeric(12,2) | `ck_bookings_unit_price` lớn hơn 0. Giá chụp tại thời điểm đặt |
| `room_quantity` | integer | `ck_bookings_room_qty` tối thiểu 1 |
| `subtotal_amount` | numeric(12,2) | `ck_bookings_subtotal` không âm |
| `discount_amount` | numeric(12,2) | `ck_bookings_discount` không âm |
| `total_amount` | numeric(12,2) | `ck_bookings_total` không âm |
| `deposit_amount` | numeric(12,2) | `ck_bookings_deposit` không âm |
| `promotion_id` | bigint | Khoá ngoại `fk_bookings_promotion` thêm ở V5 |
| `status` | varchar(20) | `ck_bookings_status` liệt kê **tám trạng thái** |
| `payment_status` | varchar(20) | `ck_bookings_pay_status` liệt kê bảy trạng thái thanh toán |
| `special_request` | text | Yêu cầu đặc biệt của khách |
| `hold_expires_at` | timestamptz | Hạn giữ chỗ |
| `client_ip` | inet | Địa chỉ mạng của người đặt |
| `user_agent` | varchar(255) | Thông tin trình duyệt |
| `cancelled_at`, `cancel_reason` | timestamptz, text | Thời điểm và lý do huỷ |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Bốn cột kết thúc bằng `_snapshot` lưu bản sao giá trị tại thời điểm đặt. Khi
quản trị viên đổi giá hoặc đổi tên loại phòng sau này, đơn cũ vẫn giữ nguyên số
tiền và tên mà khách đã nhìn thấy lúc đặt.

**Bảng `booking_rooms` — gán phòng vật lý cho đơn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại tới `bookings`, xoá theo tầng |
| `room_id` | bigint | Khoá ngoại tới `rooms` |
| `check_in`, `check_out` | date | `ck_booking_rooms_dates` buộc ngày trả sau ngày nhận |
| `stay` | daterange | **Cột sinh tự động** `GENERATED ALWAYS AS (daterange(check_in, check_out, '[)')) STORED` |
| `status` | varchar(20) | `ck_booking_rooms_status` giới hạn `ACTIVE` và `RELEASED` |

Ràng buộc quyết định của toàn hệ thống nằm trên bảng này:

```sql
EXCLUDE USING gist (room_id WITH =, stay WITH &&) WHERE (status = 'ACTIVE')
```

Bảng này còn mang hai trigger ràng buộc hoãn bảo đảm số dòng `ACTIVE` luôn khớp
`room_quantity` của đơn, đặt ở cả hai phía: khi sửa `booking_rooms` và khi sửa
`room_quantity` trên `bookings`.

**Bảng `booking_status_history` — nhật ký chuyển trạng thái**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `from_status` | varchar(20) | `ck_bsh_from_status` rỗng hoặc thuộc tám trạng thái |
| `to_status` | varchar(20) | `ck_bsh_to_status` thuộc tám trạng thái |
| `changed_by` | bigint | Khoá ngoại tới `users`, rỗng khi hệ thống tự chuyển |
| `actor` | varchar(20) | `ck_bsh_actor` giới hạn `GUEST`, `CUSTOMER`, `ADMIN`, `SYSTEM` |
| `note` | text | Ghi chú kèm theo lần chuyển |
| `created_at` | timestamptz | Thời điểm chuyển |

### Nhóm V4 — Thanh toán và thư

**Bảng `payments` — lần thanh toán**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `attempt_no` | integer | `ck_payments_attempt` tối thiểu 1. Cùng `booking_id` tạo thành chỉ mục duy nhất |
| `provider` | varchar(20) | `ck_payments_provider` giới hạn `SEPAY` và `MANUAL` |
| `amount_expected` | numeric(12,2) | `ck_payments_expected` lớn hơn 0 |
| `amount_received` | numeric(12,2) | `ck_payments_received` không âm |
| `qr_content`, `qr_image_url` | text, varchar(500) | Nội dung và ảnh mã QR |
| `transfer_content` | varchar(50) | `uq_payments_transfer_content` duy nhất. Mã đơn ghép số thứ tự lần thử |
| `status` | varchar(20) | `ck_payments_status` liệt kê sáu trạng thái |
| `reconcile_status` | varchar(20) | `ck_payments_reconcile` giới hạn `NONE`, `NEEDS_REVIEW`, `REFUND_REQUIRED`, `RESOLVED` |
| `provider_txn_id` | varchar(100) | Mã giao dịch của nhà cung cấp |
| `paid_at`, `expires_at` | timestamptz | Thời điểm thanh toán và hết hạn |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Chỉ mục từng phần `idx_payments_reconcile` chỉ đánh chỉ mục các dòng cần đối
soát, giúp màn hình đối soát truy vấn nhanh mà không phải quét toàn bảng.

**Bảng `payment_webhook_events` — nhật ký webhook**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `provider` | varchar(20) | Nhà cung cấp gửi webhook |
| `external_id` | varchar(100) | Mã sự kiện bên nhà cung cấp |
| `payment_id` | bigint | Khoá ngoại tới `payments`, rỗng khi không khớp đơn nào |
| `payload` | jsonb | Toàn bộ nội dung nhận được |
| `processing_result` | varchar(20) | `ck_pwe_result` giới hạn `MATCHED`, `UNMATCHED`, `LATE`, `DUPLICATE`, `ERROR` |
| `error_message` | text | Thông báo lỗi nếu xử lý thất bại |
| `received_at`, `processed_at` | timestamptz | Thời điểm nhận và xử lý |

Chỉ mục duy nhất `uq_webhook_provider_external` trên cặp nhà cung cấp và mã sự
kiện là cơ chế **chống cộng tiền hai lần** khi nhà cung cấp gửi lại webhook.

**Bảng `outbound_emails` — hàng đợi thư đi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, đặt rỗng khi đơn bị xoá |
| `template` | varchar(100) | Tên mẫu thư |
| `to_email` | varchar(255) | Địa chỉ nhận |
| `payload` | jsonb | Dữ liệu điền vào mẫu |
| `status` | varchar(20) | `ck_outbound_emails_status` giới hạn `PENDING`, `SENT`, `FAILED` |
| `attempts` | integer | `ck_outbound_emails_attempts` không âm |
| `last_error` | text | Lỗi lần gửi gần nhất |
| `sent_at`, `created_at` | timestamptz | Thời điểm gửi và tạo |

### Nhóm V5 — Khuyến mãi và đánh giá

**Bảng `promotions` — mã khuyến mãi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_promotions_code` duy nhất |
| `name`, `description` | varchar(150), text | Tên và mô tả |
| `discount_type` | varchar(20) | `ck_promotions_type` giới hạn `PERCENT` và `FIXED` |
| `discount_value` | numeric(12,2) | `ck_promotions_value` lớn hơn 0 |
| `max_discount_amount` | numeric(12,2) | `ck_promotions_max` rỗng hoặc lớn hơn 0 |
| `min_nights` | integer | `ck_promotions_nights` tối thiểu 1 |
| `min_total_amount` | numeric(12,2) | `ck_promotions_min_total` không âm |
| `starts_at`, `ends_at` | timestamptz | `ck_promotions_window` buộc kết thúc sau bắt đầu |
| `usage_limit` | integer | Giới hạn lượt dùng, rỗng là không giới hạn |
| `used_count` | integer | `ck_promotions_used` không âm; `ck_promotions_usage` buộc không vượt giới hạn |
| `active` | boolean | Tắt để ngừng áp dụng |

Ràng buộc `ck_promotions_usage` bảo đảm số lượt đã dùng **không bao giờ vượt quá
giới hạn** ngay ở tầng dữ liệu, kể cả khi nhiều đơn cùng áp một mã.

**Bảng `reviews` — đánh giá của khách**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng. `uq_reviews_booking` **duy nhất — mỗi đơn một đánh giá** |
| `user_id` | bigint | Khoá ngoại tới `users`, rỗng với khách vãng lai |
| `guest_name_snapshot` | varchar(150) | Tên khách chụp từ đơn tại thời điểm đánh giá |
| `rating` | smallint | `ck_reviews_rating` trong khoảng 1 tới 5 |
| `title`, `content` | varchar(200), text | Tiêu đề và nội dung |
| `status` | varchar(20) | `ck_reviews_status` giới hạn `PENDING`, `APPROVED`, `REJECTED` |
| `admin_reply`, `replied_at` | text, timestamptz | Phản hồi của quản trị viên |
| `created_at` | timestamptz | Thời điểm gửi |

### Nhóm V6 — Nội dung

**Bảng `site_contents` — khối nội dung trang chủ**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `section_key` | varchar(100) | `uq_site_contents_key` duy nhất. Khoá định danh khối |
| `title`, `subtitle` | varchar(255) | Tiêu đề và tiêu đề phụ |
| `body` | text | Nội dung HTML **đã lọc ở tầng vào** |
| `data` | jsonb | Dữ liệu có cấu trúc tuỳ khối |
| `image_url` | varchar(500) | Ảnh minh hoạ |
| `updated_by` | bigint | Khoá ngoại tới `users` |
| `updated_at` | timestamptz | Thời điểm cập nhật |

**Bảng `banners` — biểu ngữ**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `title` | varchar(255) | Tiêu đề, bắt buộc |
| `image_url` | varchar(500) | Ảnh, bắt buộc |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh |
| `link_url` | varchar(500) | Đường dẫn khi nhấp vào |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Bật tắt hiển thị |
| `starts_at`, `ends_at` | timestamptz | `ck_banners_window` buộc kết thúc sau bắt đầu khi cả hai cùng có giá trị |

**Bảng `gallery_images` — thư viện ảnh**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `url` | varchar(500) | Đường dẫn ảnh, bắt buộc |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh |
| `caption` | varchar(255) | Chú thích |
| `category` | varchar(50) | Phân nhóm ảnh |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Bật tắt hiển thị |

**Bảng `posts` — tin tức**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `slug` | varchar(200) | `uq_posts_slug` duy nhất, dùng trong đường dẫn |
| `title` | varchar(255) | Tiêu đề, bắt buộc |
| `excerpt`, `content` | text | Trích dẫn ngắn và nội dung đầy đủ |
| `cover_image_url` | varchar(500) | Ảnh bìa |
| `published` | boolean | Cờ đã đăng |
| `published_at` | timestamptz | Thời điểm đăng |
| `author_id` | bigint | Khoá ngoại tới `users` |

Chỉ mục từng phần `idx_posts_published` chỉ đánh chỉ mục các bài đã đăng, phục
vụ trang tin tức công khai.

### Nhóm V7 — Ghi chú nội bộ

**Bảng `booking_notes` — ghi chú của quản trị viên trên đơn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `author_id` | bigint | Khoá ngoại tới `users`, đặt rỗng khi tài khoản bị xoá |
| `content` | text | `ck_booking_notes_content` buộc không rỗng sau khi cắt khoảng trắng |
| `created_at` | timestamptz | Thời điểm ghi |

Bảng này tách khỏi `booking_status_history` vì mỗi dòng lịch sử bắt buộc kèm một
lần chuyển trạng thái thật, mà máy trạng thái không cho phép chuyển từ một trạng
thái sang chính nó. Ghi chú là dữ liệu **nội bộ** và không endpoint công khai
nào đọc bảng này.

### Nhóm V8 — Ngày khả dụng

**Bảng `room_closures` — khoảng ngày phòng không nhận khách**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `room_id` | bigint | Khoá ngoại tới `rooms`, xoá theo tầng |
| `from_date` | date | Đêm đầu tiên bị chặn |
| `to_date` | date | Ngày mở bán lại. `ck_room_closures_dates` buộc lớn hơn `from_date` |
| `blocked` | daterange | **Cột sinh tự động** theo quy ước nửa mở, cùng quy ước với `booking_rooms.stay` |
| `reason` | varchar(300) | Lý do đóng phòng |
| `created_by` | bigint | Khoá ngoại tới `users`, đặt rỗng khi tài khoản bị xoá |
| `created_at` | timestamptz | Thời điểm tạo |

Ràng buộc `room_closures_no_overlap` chống hai khoảng đóng chồng nhau trên cùng
một phòng.

### 3.2.3. Hai lần sử dụng ràng buộc loại trừ

Cùng một cơ chế được sử dụng cho hai bài toán khác nhau, cho thấy lựa chọn
PostgreSQL không nhằm phục vụ một trường hợp duy nhất.

| Lần | Bảng | Ràng buộc | Chống điều gì |
|---|---|---|---|
| 1 | `booking_rooms` | `booking_rooms_no_overlap` | Hai đơn cùng giữ một phòng trong những ngày giao nhau |
| 2 | `room_closures` | `room_closures_no_overlap` | Hai khoảng đóng chồng nhau trên cùng một phòng |

Lần thứ hai giải quyết một lỗi rất khó chẩn đoán từ phía người dùng. Nếu cho
phép hai khoảng đóng chồng nhau, màn hình quản trị hiển thị hai dòng mô tả cùng
một điều, và việc xoá một dòng **không mở lại được phòng**: người dùng nhấn xoá,
thấy dòng biến mất, rồi vẫn không bán được phòng mà không có cách nào hiểu vì
sao.

Khác biệt giữa hai ràng buộc: `booking_rooms_no_overlap` có mệnh đề điều kiện
`WHERE (status = 'ACTIVE')` vì dòng gán phòng có trạng thái và cần giữ lịch sử;
`room_closures_no_overlap` không có, vì khoảng đóng không có trạng thái và xoá
là xoá hẳn.

### 3.2.4. Chuẩn hoá dữ liệu

Lược đồ đạt **dạng chuẩn 3**. Hai điểm đáng nêu:

**Tách `room_types` và `rooms` là quyết định mô hình hoá quan trọng nhất.**
Khách đặt một **loại phòng**, nhưng ràng buộc chống trùng lịch phải đặt lên
**phòng vật lý**. Gộp hai khái niệm vào một bảng thì hoặc không chống trùng
được, hoặc buộc khách phải chọn đúng số phòng — điều không nền tảng đặt phòng
nào thực hiện.

**Các chỗ cố ý phi chuẩn hoá.** Cột `guest_name_snapshot` trong bảng `reviews`
và bốn cột kết thúc bằng `_snapshot` trong bảng `bookings` lưu bản sao giá trị
tại thời điểm phát sinh, thay vì tham chiếu. Lý do: những giá trị này phải **đóng
băng** tại thời điểm đó. Khách đổi tên tài khoản hoặc quản trị viên đổi giá
phòng sau này không được phép làm thay đổi nội dung đã phát sinh.

## 3.3. Thiết kế lớp và máy trạng thái

### 3.3.1. Biểu đồ lớp

Hệ thống có **20 thực thể** ánh xạ tới 20 trong số 21 bảng. Hình 3.5 thể hiện
biểu đồ lớp của các thực thể cốt lõi thuộc nhóm đặt phòng và thanh toán.

[Hình 3.5]

Hai lớp cơ sở trừu tượng được dùng lại cho nhiều thực thể:

| Lớp cơ sở | Cột cung cấp | Dùng cho nhóm bảng nào |
|---|---|---|
| `BaseAuditEntity` | `created_at` và `updated_at` | Bảng có vòng đời dài, có sửa đổi |
| `BaseCreatedEntity` | Chỉ `created_at` | Bảng chỉ ghi thêm, không bao giờ sửa |

Việc tách hai lớp cơ sở là bắt buộc chứ không phải lựa chọn phong cách. Chế độ
kiểm tra lược đồ báo lỗi ngay nếu một thực thể khai báo một cột mà migration
không có, nên không thể dùng chung một lớp cơ sở cho cả hai nhóm bảng.

### 3.3.2. Máy trạng thái đơn đặt phòng

Đơn đặt phòng có **tám trạng thái**. Mọi lần chuyển trạng thái đều đi qua một
thành phần duy nhất, vì mỗi lần chuyển kéo theo ba việc phụ dễ bỏ sót: ghi nhật
ký, nhả phòng khi đơn không diễn ra, và hoàn lượt khuyến mãi.

| Trạng thái | Ý nghĩa |
|---|---|
| `PENDING_PAYMENT` | Vừa tạo, đang giữ chỗ chờ tiền cọc |
| `AWAITING_REVIEW` | Có tiền nhưng không khớp, cần người đối soát |
| `CONFIRMED` | Đã nhận đủ cọc, phòng được giữ chắc chắn |
| `CHECKED_IN` | Khách đã nhận phòng |
| `CHECKED_OUT` | Khách đã trả phòng |
| `CANCELLED` | Khách hoặc quản trị viên huỷ |
| `EXPIRED` | Quá hạn giữ chỗ mà không có tiền |
| `NO_SHOW` | Đã xác nhận nhưng khách không đến |

Hình 3.6 thể hiện biểu đồ trạng thái đầy đủ, trong đó hai đường chuyển từ
`CANCELLED` và `EXPIRED` về `AWAITING_REVIEW` là nhánh xử lý tiền về muộn.

[Hình 3.6]

**Một quy tắc phản trực giác cần giải thích.** Trạng thái `CHECKED_OUT` **không**
nhả phòng. Trực giác cho rằng khách trả phòng thì phòng phải được trả về kho.
Nhưng nhả phòng nghĩa là chuyển dòng gán phòng sang `RELEASED`, trong khi trigger
kiểm tra số phòng lại đòi đơn phải luôn giữ đúng số phòng đã đặt — nên nhả phòng
lúc trả phòng làm cơ sở dữ liệu bác cả giao dịch. Quy tắc này phát sinh từ một
lỗi thật gặp phải trong quá trình thực hiện, và có một ca kiểm thử riêng canh nó.

### 3.3.3. Biểu đồ tuần tự

Ba luồng xử lý quan trọng nhất được mô hình hoá bằng biểu đồ tuần tự.

Hình 3.7 thể hiện luồng đặt phòng từ lúc khách chọn ngày tới lúc nhận mã QR, với
bốn đối tượng tham gia: khách, trình duyệt, máy chủ ứng dụng và cơ sở dữ liệu.

[Hình 3.7]

Hình 3.8 thể hiện luồng thanh toán và webhook, bổ sung hai đối tượng bên ngoài
là ngân hàng và nhà cung cấp dịch vụ trung gian.

[Hình 3.8]

Hình 3.9 thể hiện nhánh tiền về muộn — trường hợp tiền tới sau khi đơn đã hết
hạn — cùng hai kết quả có thể xảy ra.

[Hình 3.9]
