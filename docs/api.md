# API

**77 thao tác trên 61 đường dẫn** — đếm từ `/v3/api-docs` của hệ thống đang chạy
ở profile `demo`, không chép từ kế hoạch:

```bash
curl -s localhost/v3/api-docs \
  | python3 -c "import json,sys; d=json.load(sys.stdin); \
      print(sum(len([m for m in v if m in ('get','post','put','delete','patch')]) \
                for v in d['paths'].values()))"
```

Bản tương tác: `http://localhost/swagger-ui/index.html` (chỉ có ở profile
`demo`; `prod` tắt cả Swagger UI lẫn `/v3/api-docs`).

## Nguyên tắc phân quyền

Mặc định là **đóng**. `SecurityConfig` kết thúc bằng `anyRequest().denyAll()`,
nên một endpoint mới không khai gì sẽ bị từ chối chứ không lọt ra công khai.

`EndpointAuthorizationIT` giữ một ma trận 29 tiền tố và có hai bài kiểm:
mọi endpoint đã đăng ký phải nằm trong ma trận, và **cấm** khai lại dòng bao
`/api/admin`. Liệt kê từng nhóm quản trị buộc người thêm endpoint mới phải nghĩ
về quyền của nó, thay vì được che miễn phí.

Khu quản trị còn một lớp nữa: `MustChangePasswordFilter` trả 403
`PASSWORD_CHANGE_REQUIRED` cho **mọi** đường trừ `/api/me`,
`/api/auth/change-password`, `/api/auth/logout` khi tài khoản còn cờ
`must_change_password`.

## Giới hạn tần suất

Bộ nhớ trong tiến trình (bucket4j), khoá theo IP thật lấy từ `X-Forwarded-For`
mà nginx ghi đè — xem [bao-mat.md](./bao-mat.md#giới-hạn-tần-suất-sau-nginx).

| Khoá | Hạn mức | Áp cho |
|---|---|---|
| `ip:auth` | 10 / phút | mọi `/api/auth/**` |
| `email:login` | 5 / phút | `/api/auth/login`, theo email trong thân request |
| `ip:availability` | 60 / phút | `/api/availability**` |
| `ip:promo` | 20 / phút | `/api/promotions/check` |
| `ip:booking-create` | 10 / phút | `POST /api/bookings` |
| `phone:booking` | 10 / giờ | `POST /api/bookings`, theo số điện thoại |
| `ip:lookup` | 10 / phút | `/api/bookings/lookup` |
| `code:lookup` | 5 / giờ | `/api/bookings/lookup`, theo mã đơn |
| `ip:cancel` | 10 / phút | `/api/bookings/{code}/cancel` |

Hai khoá cùng áp cho một request thì khoá **chặt hơn** chặn trước. Đăng nhập sai
liên tục cùng một email bị chặn ở lần thứ 6 (`email:login`), còn đổi email mỗi
lần thì bị chặn ở lần thứ 11 (`ip:auth`).

Vượt hạn mức → `429` với `code: TOO_MANY_REQUESTS`.

## Mã lỗi

Phản hồi lỗi theo RFC 7807 kèm trường `code` tự thêm. **Giao diện đọc `code`,
không đọc `detail`** — đổi câu chữ ở backend không được phép làm hỏng màn hình.

| `code` | HTTP | Ý nghĩa |
|---|---|---|
| `ROOM_NOT_AVAILABLE` | 409 | Phòng vừa được đặt hết cho khoảng ngày này |
| `INVALID_PROMOTION` | 400 | Mã không hợp lệ hoặc chưa đủ điều kiện |
| `PROMOTION_EXHAUSTED` | 409 | Mã đã hết lượt |
| `BOOKING_NOT_FOUND` | 404 | Không có đơn khớp mã + số điện thoại |
| `INVALID_ACCESS_TOKEN` | 403 | Mã truy cập sai |
| `INVALID_STATE_TRANSITION` | 409 | Bước chuyển trạng thái không hợp lệ |
| `INVALID_BOOKING_REQUEST` | 400 | Dữ liệu đơn không hợp lệ |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Phải đổi mật khẩu tạm trước |
| `IMAGE_TOO_LARGE` | 413 | Ảnh vượt 5MB |
| `TOO_MANY_REQUESTS` | 429 | Vượt giới hạn tần suất |
| `UNAUTHORIZED` | 401 | Thiếu hoặc sai token |

## Bảng endpoint

`—` = không cần đăng nhập. `ADMIN` = cần vai trò quản trị. `CUSTOMER` = cần
đăng nhập bất kỳ vai trò nào.

### Công khai — tra cứu và nội dung

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/health` | — |
| GET | `/api/room-types` | — |
| GET | `/api/room-types/{slug}` | — |
| GET | `/api/availability` | — |
| GET | `/api/availability/calendar` | — |
| GET | `/api/content/home` | — |
| GET | `/api/content/sections` | — |
| GET | `/api/content/banners` | — |
| GET | `/api/content/gallery` | — |
| GET | `/api/posts` | — |
| GET | `/api/posts/{slug}` | — |
| GET | `/api/reviews` | — |
| POST | `/api/promotions/check` | — |
| GET | `/uploads/**` | — |

### Công khai — đơn đặt phòng

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/api/bookings` | — (gắn `user_id` nếu có token) |
| POST | `/api/bookings/lookup` | — (mã đơn + số điện thoại) |
| POST | `/api/bookings/{code}/cancel` | — (mã truy cập **hoặc** số điện thoại) |
| GET | `/api/bookings/{code}/payment-status` | — (bắt buộc mã truy cập) |
| POST | `/api/bookings/{code}/review` | — (mã truy cập **hoặc** số điện thoại) |
| POST | `/api/payments/webhook/sepay` | — (khoá API trong header `Apikey`) |

`payment-status` bắt buộc mã truy cập vì mã đơn nằm trên sao kê ngân hàng: nếu
chỉ cần mã là xem được, ai thấy sao kê cũng theo dõi được đơn của người khác.

### Xác thực

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/api/auth/register` | — |
| POST | `/api/auth/login` | — |
| POST | `/api/auth/refresh` | — (cookie `HttpOnly`) |
| POST | `/api/auth/logout` | CUSTOMER |
| POST | `/api/auth/change-password` | CUSTOMER |
| GET | `/api/me` | CUSTOMER |
| GET | `/api/me/bookings` | CUSTOMER |
| GET | `/api/me/bookings/{code}` | CUSTOMER |

`/api/me/bookings` lọc theo token của phiên, **không** theo tham số trên URL.

### Quản trị — đơn và thanh toán

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/admin/bookings` | ADMIN |
| GET | `/api/admin/bookings/{id}` | ADMIN |
| POST | `/api/admin/bookings/{id}/transition` | ADMIN |
| POST | `/api/admin/bookings/{id}/note` | ADMIN |
| POST | `/api/admin/bookings/{id}/resend-email` | ADMIN |
| GET | `/api/admin/payments` | ADMIN |
| POST | `/api/admin/payments/{id}/confirm-manually` | ADMIN |
| POST | `/api/admin/payments/{id}/resolve` | ADMIN |

### Quản trị — danh mục

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET POST | `/api/admin/room-types` | ADMIN |
| GET PUT DELETE | `/api/admin/room-types/{id}` | ADMIN |
| PUT | `/api/admin/room-types/{id}/amenities` | ADMIN |
| POST PUT | `/api/admin/room-types/{id}/images` | ADMIN |
| DELETE | `/api/admin/room-types/images/{imageId}` | ADMIN |
| GET | `/api/admin/amenities` | ADMIN |
| GET POST | `/api/admin/rooms` | ADMIN |
| PUT PATCH DELETE | `/api/admin/rooms/{id}` | ADMIN |
| GET POST | `/api/admin/promotions` | ADMIN |
| PUT DELETE | `/api/admin/promotions/{id}` | ADMIN |
| POST | `/api/admin/images` | ADMIN |

### Quản trị — nội dung và đánh giá

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/admin/content/sections` | ADMIN |
| PUT | `/api/admin/content/sections/{key}` | ADMIN |
| GET POST | `/api/admin/content/banners` | ADMIN |
| PUT DELETE | `/api/admin/content/banners/{id}` | ADMIN |
| GET POST | `/api/admin/content/gallery` | ADMIN |
| PUT DELETE | `/api/admin/content/gallery/{id}` | ADMIN |
| GET POST | `/api/admin/content/posts` | ADMIN |
| GET PUT DELETE | `/api/admin/content/posts/{id}` | ADMIN |
| GET | `/api/admin/reviews` | ADMIN |
| POST | `/api/admin/reviews/{id}/approve` | ADMIN |
| POST | `/api/admin/reviews/{id}/reject` | ADMIN |
| POST | `/api/admin/reviews/{id}/reply` | ADMIN |

### Quản trị — báo cáo

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/admin/dashboard` | ADMIN |
| GET | `/api/admin/reports/bookings.csv` | ADMIN |

### Chỉ có ở profile demo

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/api/admin/dev/simulate-payment` | ADMIN **và** `payments.simulator.enabled=true` |
| GET | `/swagger-ui/**`, `/v3/api-docs/**` | — |

Endpoint mô phỏng thanh toán cần **cả hai** điều kiện, và cờ mặc định là `false`
ngay cả ở profile `demo`: bật thủ công khi cần trình diễn. Vì thế nó không xuất
hiện trong `/v3/api-docs` của bản đang chạy.
