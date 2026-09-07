---
title: "Phase 6: Trang admin & dashboard"
status: todo
phase: 6
priority: P1
effort: "13h"
dependencies: [3, 4]
---

# Phase 6: Trang admin & dashboard

## Overview

Toàn bộ khu vực quản trị: đăng nhập, CRUD loại phòng/phòng/ảnh/giá, xử lý booking theo vòng đời, **hàng đợi đối soát thanh toán**, quản lý khuyến mãi, duyệt đánh giá, và dashboard doanh thu + tỉ lệ lấp đầy.

Phase 7 dùng lại `data-table` và `image-uploader` — hai component này đã được chốt trong `shared/` ở cuối Phase 4, nên hai phase không tranh nhau tạo.

## Requirements

- Functional: 7 màn hình quản trị + dashboard; upload ảnh an toàn; đổi trạng thái booking; đối soát thanh toán; xuất CSV.
- Non-functional: mọi endpoint dưới `/api/admin/**` chỉ `ADMIN`; danh sách phân trang phía server; dashboard tổng hợp bằng SQL, không tải hết dữ liệu về app.

## Architecture

### Bố cục admin

```
/admin/login
/admin                      → Dashboard
/admin/bookings             → Danh sách + lọc + chi tiết + dòng thời gian trạng thái
/admin/payments             → Hàng đợi đối soát (tiền lạc, thiếu, thừa, không khớp)
/admin/room-types           → CRUD loại phòng + ảnh + tiện ích
/admin/rooms                → CRUD phòng vật lý + lịch phòng
/admin/promotions           → CRUD mã giảm giá
/admin/reviews              → Duyệt / ẩn / trả lời đánh giá
/admin/content              → (Phase 7) banner, thư viện ảnh, tin tức, nội dung trang
```

`/admin/payments` là màn hình bắt buộc, không phải tuỳ chọn: Phase 5 có ba nhánh kết thúc bằng "chuyển admin xử lý" (thiếu tiền, thừa tiền, tiền về muộn không gán lại được phòng). Không có màn hình này thì những khoản tiền đó nằm im trong bảng và không ai biết. Số dòng cần đối soát hiển thị ngay trên dashboard, không giấu trong menu con.

### Chỉ số dashboard

Tính bằng SQL tổng hợp, không tính ở tầng Java. Tên chỉ số nói rõ đó là tiền gì và tính theo trục thời gian nào:

| Chỉ số | Định nghĩa | Trục thời gian |
|---|---|---|
| Giá trị booking | `SUM(total_amount)` các booking `CONFIRMED/CHECKED_IN/CHECKED_OUT` | `check_in` |
| Tiền đã thu | `SUM(amount_received)` các payment `SUCCEEDED/OVERPAID` | `paid_at` |
| Tỉ lệ lấp đầy | đêm-phòng đã bán ÷ (tổng số phòng × số đêm của kỳ) | `booking_rooms.stay` |
| Booking mới | `COUNT(*)` | `created_at` |
| Tỉ lệ huỷ | `COUNT(CANCELLED + NO_SHOW) ÷ COUNT(*)` | `created_at` |
| Cần đối soát | `COUNT(*)` payment có `reconcile_status <> 'NONE'` | hiện tại |
| Top loại phòng | `COUNT(*)` gom theo `room_type_id` | `check_in` |

Hai chỉ số tiền phải đứng tách nhau và gọi đúng tên. Hệ thống chỉ thu 30% cọc, nên gọi `SUM(total_amount)` là "doanh thu" sẽ sai khi hội đồng hỏi "tiền này đã về tài khoản chưa?".

Ba chỉ số dùng ba trục thời gian khác nhau — điều này phải ghi ngay trên giao diện và trong `docs/api.md`, nếu không hai con số cạnh nhau sẽ bị hiểu là cùng kỳ.

Số đêm-phòng đã bán:

```sql
SELECT SUM(
  LEAST(br.check_out, :periodEnd) - GREATEST(br.check_in, :periodStart)
) AS room_nights
FROM booking_rooms br
JOIN bookings b ON b.id = br.booking_id
WHERE br.status = 'ACTIVE'
  AND b.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
  AND br.stay && daterange(:periodStart, :periodEnd, '[)');
```

Cắt bằng `LEAST`/`GREATEST` để booking vắt qua ranh giới tháng chỉ tính phần nằm trong kỳ.

**Mẫu số là `COUNT(*) FROM rooms`, không lọc theo `status`.** Lọc theo `status = 'AVAILABLE'` sẽ khiến mẫu số phản ánh trạng thái *hiện tại* trong khi tử số là dữ liệu *lịch sử*: admin chuyển 3 phòng sang `MAINTENANCE` là tỉ lệ lấp đầy của mọi tháng đã qua nhảy vọt, có thể vượt 100%. Tổng số phòng vật lý ổn định hơn nhiều. Kết quả vẫn kẹp `LEAST(ratio, 1.0)` và có test riêng cho tình huống này.

Mọi truy vấn gom nhóm theo thời điểm phải viết `date_trunc('month', created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')` — không có mệnh đề này thì booking tạo lúc 00:00–07:00 giờ Việt Nam ngày mùng 1 bị xếp vào tháng trước.

## Related Code Files

**Backend**
- Create: `admin/AdminBookingController.java` — danh sách lọc, chi tiết, đổi trạng thái, ghi chú, gửi lại email
- Create: `admin/AdminPaymentController.java` — hàng đợi đối soát, đánh dấu đã xử lý, xác nhận thủ công
- Create: `admin/AdminRoomTypeController.java`, `AdminRoomController.java`
- Create: `admin/AdminPromotionController.java`, `AdminReviewController.java`
- Create: `report/DashboardController.java`, `DashboardService.java`, `dto/DashboardSummary.java`
- Create: `report/RevenueReportRepository.java` (native query tổng hợp)
- Create: `report/CsvExportService.java` — thoát ký tự chống công thức
- Create: `storage/ImageStorageService.java` (interface), `CloudinaryImageStorage.java`, `LocalImageStorage.java`
- Create: `storage/ImageValidator.java` — danh sách trắng MIME + giải mã lại ảnh
- Create: `storage/ImageUploadController.java` — `POST /api/admin/images` (multipart)
- Create: `config/StorageConfig.java`
- Modify: `pom.xml` — thêm `cloudinary-http44`
- Modify: `phase-03` ma trận phân quyền — thêm mọi endpoint mới của phase này

**Frontend**
- Create: `features/admin/admin.routes.ts` (lazy, bọc `adminGuard`)
- Create: `layouts/admin-layout/` — sidebar, topbar, breadcrumb, badge "cần đối soát"
- Create: `features/admin/login/`, `dashboard/`, `bookings/`, `payments/`, `room-types/`, `rooms/`, `promotions/`, `reviews/`
- Create: `features/admin/bookings/booking-detail.component.ts` — dòng thời gian trạng thái + trạng thái email
- Modify: `frontend/package.json` — thêm thư viện vẽ biểu đồ
- Dùng lại từ `shared/` (đã chốt ở Phase 4): `data-table`, `image-uploader`, `modal`, `badge`, `pagination`

**Test**
- Create: `backend/src/test/java/com/tvh/homestay/report/DashboardServiceIT.java`
- Create: `backend/src/test/java/com/tvh/homestay/report/CsvExportTest.java`
- Create: `backend/src/test/java/com/tvh/homestay/storage/ImageUploadIT.java`

## Implementation Steps

1. `AdminBookingController`: `GET /api/admin/bookings` phân trang + lọc (`status`, `from`, `to`, `q` theo mã/tên/SĐT); `GET /{id}`; `POST /{id}/transition` body `{toStatus, note}` chạy qua `BookingStateMachine`; `POST /{id}/note`; `POST /{id}/resend-email`.
2. Chi tiết booking hiển thị: thông tin khách, ngày, **số phòng vật lý được gán**, `special_request`, lịch sử trạng thái kèm `actor`, lịch sử thanh toán, trạng thái các email trong outbox, mã giảm giá đã dùng.
3. `AdminPaymentController`: `GET /api/admin/payments?reconcileStatus=` liệt kê payment cần xử lý kèm payload webhook gốc; `POST /{id}/resolve` đánh dấu đã xử lý; `POST /{id}/confirm-manually` xác nhận booking bằng tay (qua `BookingStateMachine`, ghi `actor = ADMIN`).
4. `AdminRoomTypeController`: CRUD + gắn tiện ích + sắp thứ tự ảnh + đặt ảnh bìa; hiển thị `area_sqm`, `capacity_children`, `bed_info`. Xoá loại phòng đang có booking → 409, gợi ý `active = false`.
5. `AdminRoomController`: CRUD phòng (gồm `floor`, `note`); đổi `status` sang `MAINTENANCE`/`OUT_OF_SERVICE` cho phòng đang có booking `ACTIVE` tương lai → cảnh báo và liệt kê booking bị ảnh hưởng, không tự huỷ.
6. `ImageValidator`: **danh sách trắng tường minh** `image/jpeg`, `image/png`, `image/webp`. SVG bị loại — SVG là XML thuần, không có magic bytes cố định nên mọi bộ dò nội dung đều trả `image/svg+xml` là "ảnh hợp lệ", và khi phục vụ cùng origin nó thực thi JavaScript. Sau khi kiểm, **giải mã lại ảnh và ghi lại** để loại metadata và tệp polyglot. Đổi tên file thành UUID, bỏ hoàn toàn tên do client gửi (chống path traversal). Giới hạn 5MB.
7. `ImageStorageService`: có `CLOUDINARY_URL` → Cloudinary (lưu `public_id` để xoá được); không có → `LocalImageStorage` ghi `uploads/`, phục vụ qua `/uploads/**` với `X-Content-Type-Options: nosniff`, `Content-Type` do server quyết định, và `Content-Security-Policy: sandbox` (Phase 8 cấu hình nginx).
8. `AdminPromotionController`: CRUD; không cho sửa `code` khi đã có booking dùng; hiển thị `used_count / usage_limit` (hiện "không giới hạn" khi `usage_limit IS NULL`).
9. `AdminReviewController`: danh sách theo trạng thái, duyệt/từ chối, trả lời. Nội dung review hiển thị bằng **text binding**, không `innerHTML` — nội dung này do người ẩn danh gửi và màn hình duyệt là nơi admin bắt buộc phải mở nó.
10. `DashboardService`: gom 7 chỉ số trên, nhận `periodStart`/`periodEnd` (mặc định 12 tháng gần nhất), trả một DTO duy nhất.
11. `CsvExportService`: `GET /api/admin/reports/bookings.csv` có BOM UTF-8, **và** thoát ký tự chống công thức — ô bắt đầu bằng `=`, `+`, `-`, `@`, tab hoặc CR được bọc và tiền tố dấu nháy đơn. `guest_name` và `special_request` do khách ẩn danh nhập; xuất thẳng vào file mà admin mở bằng Excel là đường thực thi lệnh trên máy admin.
12. Frontend `admin-layout` + `data-table` dùng chung cho 6 màn hình danh sách (một component bảng, cấu hình cột theo input).
13. Dashboard: 6 thẻ chỉ số + biểu đồ cột "Giá trị booking" + biểu đồ đường tỉ lệ lấp đầy + bảng top loại phòng, vẽ bằng **thư viện biểu đồ** (quyết định của người dùng — ràng buộc "tự viết component" áp cho UI component library, không áp cho thư viện vẽ biểu đồ). Mỗi điểm dữ liệu vẫn cần nhãn đọc được cho trình đọc màn hình.
14. Màn hình booking: bộ lọc gắn vào query param để admin bookmark/chia sẻ đường dẫn đã lọc.
15. Cập nhật ma trận phân quyền ở `phase-03-auth.md` với mọi endpoint mới; `EndpointAuthorizationIT` sẽ fail nếu quên.

## Verify

```bash
cd backend && ./mvnw -q test -Dtest='Dashboard*IT,CsvExportTest,ImageUploadIT,EndpointAuthorizationIT'

# Đối chiếu dashboard với dữ liệu thô — CÙNG tham số kỳ, cùng trục thời gian
PERIOD_START=2026-01-01; PERIOD_END=2027-01-01
docker exec -it homestay-db psql -U postgres -d homestay -c "
SELECT date_trunc('month', check_in) AS thang, SUM(total_amount)
FROM bookings
WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
  AND check_in >= '$PERIOD_START' AND check_in < '$PERIOD_END'
GROUP BY 1 ORDER BY 1;"
curl -s "localhost:8080/api/admin/dashboard?from=$PERIOD_START&to=$PERIOD_END" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.bookingValueByMonth'
# Hai con số phải khớp

# Tỉ lệ lấp đầy không đổi khi đưa phòng có booking lịch sử sang bảo trì
curl -s -X PATCH localhost:8080/api/admin/rooms/1 -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"status":"MAINTENANCE"}'
curl -s "localhost:8080/api/admin/dashboard?from=$PERIOD_START&to=$PERIOD_END" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.occupancyByMonth'   # KHÔNG đổi, không vượt 1.0

# Phân quyền
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/admin/bookings                      # 401
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/admin/bookings \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"                                                     # 403

# Upload: SVG và polyglot phải bị từ chối
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/admin/images \
  -H "Authorization: Bearer $ADMIN_TOKEN" -F 'file=@xss.svg'                                     # 415

# CSV injection
# Đặt booking với guestName bắt đầu bằng "=" rồi xuất CSV; ô tương ứng KHÔNG được là công thức
curl -s "localhost:8080/api/admin/reports/bookings.csv" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -c "^=" || echo "OK: khong o nao bat dau bang dau bang"

cd ../frontend && npm run build && npx ng lint
```

Kiểm tra thủ công: đặt booking ở landing → `/admin/bookings` thấy đúng → `CONFIRMED` → `CHECKED_IN` → `CHECKED_OUT` → dashboard cộng đúng; gửi webhook thiếu tiền → booking hiện ở `/admin/payments` và badge trên dashboard tăng.

## Todo

- [ ] `AdminBookingController` (lọc, chi tiết, chuyển trạng thái, ghi chú, gửi lại email)
- [ ] `AdminPaymentController` — hàng đợi đối soát + xác nhận thủ công
- [ ] `AdminRoomTypeController` + tiện ích + thứ tự ảnh + ảnh bìa
- [ ] `AdminRoomController` + cảnh báo booking bị ảnh hưởng
- [ ] `ImageValidator`: danh sách trắng MIME (loại SVG), giải mã lại, đổi tên UUID
- [ ] `ImageStorageService` + Cloudinary + fallback local
- [ ] `AdminPromotionController` (hiển thị đúng khi `usage_limit IS NULL`)
- [ ] `AdminReviewController` — hiển thị review bằng text binding
- [ ] `DashboardService` 7 chỉ số, mẫu số lấp đầy là tổng số phòng
- [ ] Mọi `date_trunc` kèm `AT TIME ZONE 'Asia/Ho_Chi_Minh'`
- [ ] `CsvExportService` BOM UTF-8 + thoát ký tự công thức
- [ ] `admin-layout` + badge "cần đối soát" + `data-table` dùng chung
- [ ] Dashboard dùng thư viện biểu đồ + nhãn cho trình đọc màn hình
- [ ] Bộ lọc booking gắn query param
- [ ] Cập nhật ma trận phân quyền Phase 3 với endpoint mới
- [ ] `DashboardServiceIT`, `CsvExportTest`, `ImageUploadIT`

## Success Criteria

- [ ] Admin đăng nhập thấy booking vừa tạo ở landing, đổi được trạng thái theo đúng vòng đời
- [ ] Chuyển trạng thái sai luồng bị chặn với thông báo tiếng Việt rõ ràng
- [ ] Dashboard khớp với truy vấn SQL thô **khi truyền cùng tham số kỳ**
- [ ] Đưa phòng có booking lịch sử sang `MAINTENANCE` không làm đổi tỉ lệ lấp đầy các tháng đã qua
- [ ] Tỉ lệ lấp đầy không bao giờ vượt 100%, kể cả với booking vắt qua hai tháng
- [ ] Booking tạo lúc 01:00 giờ Việt Nam ngày mùng 1 được xếp đúng tháng
- [ ] Payment thiếu/thừa/không khớp hiện ở `/admin/payments` và badge dashboard đếm đúng
- [ ] Mọi `/api/admin/**` trả 403 với token `CUSTOMER`; `EndpointAuthorizationIT` xanh
- [ ] Upload chạy được cả khi có và không có Cloudinary
- [ ] Upload `.svg` chứa script bị từ chối; `.jpg` giả (thực chất `.exe`) bị từ chối
- [ ] Booking có `guestName` bắt đầu bằng `=` → ô CSV không được Excel hiểu là công thức
- [ ] CSV mở trong Excel không lỗi font tiếng Việt
- [ ] `npm run build` sạch, không cảnh báo budget

## Risk Assessment

| Rủi ro | Dấu hiệu | Phản ứng đã định |
|---|---|---|
| Tỉ lệ lấp đầy sai vì mẫu số theo trạng thái hiện tại | Số liệu tháng cũ đổi sau khi admin sửa phòng | Mẫu số là tổng số phòng vật lý; kẹp `LEAST(ratio, 1.0)`; test riêng đưa phòng có booking sang bảo trì |
| "Khớp tuyệt đối" nhưng câu đối chiếu không cùng tham số | Test xanh giả vì dữ liệu mẫu ngắn hơn 12 tháng | Câu đối chiếu nhận cùng `periodStart`/`periodEnd` như API; ghi rõ trong bước Verify |
| Dashboard chậm khi dữ liệu lớn | Trang tải trên 2 giây | Tổng hợp bằng SQL; index `bookings(status, check_in)` từ Phase 2; nếu vẫn chậm → bảng tổng hợp theo ngày |
| Cloudinary hết quota giữa buổi bảo vệ | Upload lỗi 4xx | Fallback local nằm trong phase; ảnh mẫu nhúng sẵn trong repo, không phụ thuộc mạng |
| Xoá loại phòng làm mồ côi booking cũ | Lỗi khoá ngoại hoặc mất lịch sử | Chặn xoá cứng khi còn booking; dùng `active = false`; booking đã snapshot tên và giá |
| Hàng đợi đối soát bị bỏ quên | Tiền lạc nằm im | Badge đếm hiển thị trên dashboard và trên sidebar, không giấu trong menu con |
| Ảnh tải lên phục vụ cùng origin | XSS lưu trữ, mất phiên admin | Danh sách trắng MIME + giải mã lại + `nosniff` + refresh token trong cookie `HttpOnly` (Phase 3) — ba lớp độc lập |

**Rollback:** revert commit của phase; landing (Phase 7) không phụ thuộc admin nên vẫn chạy.
