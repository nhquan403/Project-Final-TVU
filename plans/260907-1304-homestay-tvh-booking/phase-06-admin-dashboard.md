---
title: "Phase 6: Trang admin & dashboard"
status: todo
phase: 6
priority: P1
effort: "12h"
dependencies: [3, 4]
---

# Phase 6: Trang admin & dashboard

## Overview

Toàn bộ khu vực quản trị: đăng nhập, CRUD loại phòng/phòng/ảnh/giá, xử lý booking theo vòng đời, quản lý khuyến mãi, duyệt đánh giá, và dashboard doanh thu + tỉ lệ lấp đầy.

**Chạy song song được với Phase 7** sau khi `shared/` đã chốt ở cuối Phase 4.

## Requirements

- Functional: 6 màn hình quản trị + dashboard; upload ảnh; đổi trạng thái booking; xuất CSV.
- Non-functional: mọi endpoint dưới `/api/admin/**` chỉ `ADMIN` truy cập; danh sách phân trang phía server; dashboard truy vấn tổng hợp bằng SQL, không tải hết dữ liệu về app.

## Architecture

### Bố cục admin

```
/admin/login
/admin                      → Dashboard
/admin/bookings             → Danh sách + lọc trạng thái/ngày/mã + chi tiết
/admin/room-types           → CRUD loại phòng + ảnh + tiện ích
/admin/rooms                → CRUD phòng vật lý + lịch phòng
/admin/promotions           → CRUD mã giảm giá
/admin/reviews              → Duyệt / ẩn / trả lời đánh giá
/admin/content              → (Phase 7) banner, thư viện ảnh, tin tức, nội dung trang
```

### Chỉ số dashboard (tính bằng SQL, không tính ở tầng Java)

| Chỉ số | Công thức |
|---|---|
| Doanh thu theo tháng | `SUM(total_amount)` các booking `CONFIRMED/CHECKED_IN/CHECKED_OUT`, gom theo `date_trunc('month', check_in)` |
| Đã thu cọc | `SUM(amount)` các payment `SUCCEEDED` trong kỳ |
| Tỉ lệ lấp đầy | (số đêm-phòng đã bán trong kỳ) ÷ (số phòng khả dụng × số đêm của kỳ) |
| Booking mới | `COUNT(*)` theo `created_at` trong kỳ |
| Tỉ lệ huỷ | `COUNT(CANCELLED + NO_SHOW) ÷ COUNT(*)` |
| Top loại phòng | `COUNT(*)` booking gom theo `room_type_id`, sắp giảm dần |

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

Cắt theo `LEAST`/`GREATEST` để booking vắt qua ranh giới tháng chỉ tính phần nằm trong kỳ — nếu bỏ qua bước này, tỉ lệ lấp đầy sẽ vượt 100%.

## Related Code Files

**Backend**
- Create: `admin/AdminBookingController.java` — danh sách lọc, chi tiết, đổi trạng thái, ghi chú
- Create: `admin/AdminRoomTypeController.java`, `AdminRoomController.java`
- Create: `admin/AdminPromotionController.java`, `AdminReviewController.java`
- Create: `report/DashboardController.java`, `DashboardService.java`, `dto/DashboardSummary.java`
- Create: `report/RevenueReportRepository.java` (native query tổng hợp)
- Create: `storage/ImageStorageService.java` (interface), `CloudinaryImageStorage.java`, `LocalImageStorage.java`
- Create: `storage/ImageUploadController.java` — `POST /api/admin/images` (multipart)
- Create: `config/StorageConfig.java` — chọn bean theo `CLOUDINARY_URL` có mặt hay không
- Modify: `pom.xml` — thêm `cloudinary-http44`

**Frontend**
- Create: `features/admin/admin.routes.ts` (lazy, bọc `adminGuard`)
- Create: `layouts/admin-layout/` — sidebar, topbar, breadcrumb
- Create: `features/admin/login/`, `dashboard/`, `bookings/`, `room-types/`, `rooms/`, `promotions/`, `reviews/`
- Create: `shared/ui/data-table/` — bảng phân trang + sắp xếp + lọc, tự viết
- Create: `shared/ui/image-uploader/` — kéo thả, xem trước, sắp thứ tự
- Create: `shared/ui/chart/` — biểu đồ cột + đường bằng SVG thuần (không thêm thư viện)
- Create: `features/admin/bookings/booking-detail.component.ts` — dòng thời gian trạng thái

**Test**
- Create: `backend/src/test/java/com/tvh/homestay/report/DashboardServiceIT.java`
- Create: `backend/src/test/java/com/tvh/homestay/admin/AdminAuthorizationIT.java`

## Implementation Steps

1. `AdminBookingController`: `GET /api/admin/bookings` phân trang + lọc (`status`, `from`, `to`, `q` theo mã/tên/SĐT); `GET /{id}`; `POST /{id}/transition` body `{toStatus, note}` chạy qua `BookingStateMachine`; `POST /{id}/note`.
2. Chi tiết booking hiển thị: thông tin khách, ngày, phòng được gán (số phòng cụ thể), lịch sử trạng thái, lịch sử thanh toán, mã giảm giá đã dùng.
3. `AdminRoomTypeController`: CRUD + gắn tiện ích + sắp thứ tự ảnh + đặt ảnh bìa. Xoá loại phòng đang có booking → 409, gợi ý dùng `active = false` thay vì xoá.
4. `AdminRoomController`: CRUD phòng; đổi `status` sang `MAINTENANCE` cho phòng đang có booking `ACTIVE` trong tương lai → cảnh báo và liệt kê booking bị ảnh hưởng, không tự huỷ.
5. `ImageStorageService`: có `CLOUDINARY_URL` → upload Cloudinary (lưu `public_id` để xoá được); không có → `LocalImageStorage` ghi vào `uploads/` và serve qua `/uploads/**`. Chặn theo MIME thật (đọc magic bytes, không tin phần mở rộng), giới hạn 5MB.
6. `AdminPromotionController`: CRUD; không cho sửa `code` khi đã có booking dùng; hiển thị `used_count / usage_limit`.
7. `AdminReviewController`: danh sách theo trạng thái, duyệt/từ chối, trả lời. Chỉ review `APPROVED` hiện ra landing.
8. `DashboardService`: gom các truy vấn tổng hợp trên, nhận tham số kỳ (mặc định 12 tháng gần nhất). Trả một DTO duy nhất để trang dashboard chỉ gọi một request.
9. `GET /api/admin/reports/bookings.csv` — xuất CSV có BOM UTF-8 để Excel tiếng Việt không lỗi font.
10. Frontend `admin-layout` + `data-table` dùng chung cho 5 màn hình danh sách (DRY: một component bảng, cấu hình cột theo input).
11. Trang dashboard: 4 thẻ chỉ số + biểu đồ cột doanh thu 12 tháng + biểu đồ đường tỉ lệ lấp đầy + bảng top loại phòng — vẽ bằng SVG thuần, có `aria-label` cho từng cột.
12. Màn hình booking: bộ lọc trên thanh URL (query param) để admin bookmark/chia sẻ được đường dẫn đã lọc.
13. `AdminAuthorizationIT`: quét toàn bộ endpoint `/api/admin/**`, khẳng định 401 khi không token và 403 với token `CUSTOMER`.

## Verify

```bash
cd backend && ./mvnw -q test -Dtest='Dashboard*IT,AdminAuthorizationIT'

# Đối chiếu dashboard với dữ liệu thô
docker exec -it homestay-db psql -U postgres -d homestay -c "
SELECT date_trunc('month', check_in) AS thang, SUM(total_amount)
FROM bookings WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
GROUP BY 1 ORDER BY 1;"
curl -s localhost:8080/api/admin/dashboard -H "Authorization: Bearer $ADMIN_TOKEN" | jq
# Hai con số phải khớp tuyệt đối

# Phân quyền
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/admin/bookings                     # 401
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/admin/bookings \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"                                                    # 403

cd ../frontend && npm run build && npx ng lint
```

Kiểm tra thủ công: đặt một booking ở landing → mở `/admin/bookings` → thấy đúng booking đó → `CONFIRMED` → `CHECKED_IN` → `CHECKED_OUT` → dashboard cộng thêm đúng số tiền.

## Todo

- [ ] `AdminBookingController` (lọc, chi tiết, chuyển trạng thái, ghi chú)
- [ ] `AdminRoomTypeController` + tiện ích + thứ tự ảnh + ảnh bìa
- [ ] `AdminRoomController` + cảnh báo booking bị ảnh hưởng khi bảo trì
- [ ] `ImageStorageService` + Cloudinary + fallback local + kiểm MIME thật
- [ ] `AdminPromotionController`
- [ ] `AdminReviewController`
- [ ] `DashboardService` (6 chỉ số bằng SQL tổng hợp)
- [ ] Xuất CSV có BOM UTF-8
- [ ] `admin-layout` + `data-table` + `image-uploader` dùng chung
- [ ] Dashboard với biểu đồ SVG tự vẽ
- [ ] Bộ lọc booking gắn vào query param
- [ ] `AdminAuthorizationIT` quét toàn bộ endpoint admin

## Success Criteria

- [ ] Admin đăng nhập thấy booking vừa tạo ở landing, đổi được trạng thái theo đúng vòng đời
- [ ] Chuyển trạng thái sai luồng bị chặn với thông báo tiếng Việt rõ ràng
- [ ] Dashboard khớp tuyệt đối với truy vấn SQL thô đối chiếu
- [ ] Tỉ lệ lấp đầy không bao giờ vượt 100%, kể cả với booking vắt qua hai tháng
- [ ] Mọi `/api/admin/**` trả 403 với token `CUSTOMER`
- [ ] Upload ảnh chạy được cả khi có và không có Cloudinary
- [ ] Upload file `.jpg` giả (thực chất là `.exe`) bị từ chối
- [ ] CSV mở trong Excel không lỗi font tiếng Việt
- [ ] `npm run build` sạch, không cảnh báo budget

## Risk Assessment

| Rủi ro | Dấu hiệu | Phản ứng đã định |
|---|---|---|
| Tỉ lệ lấp đầy vượt 100% | Booking dài ngày vắt qua ranh giới tháng bị tính trọn | Cắt bằng `LEAST`/`GREATEST` như công thức trên; test riêng với booking 27/09→03/10 |
| Dashboard chậm khi dữ liệu lớn | Trang tải trên 2 giây | Tổng hợp bằng SQL (không tải hết về app), index `bookings(status, check_in)` đã có từ Phase 2; nếu vẫn chậm → bảng tổng hợp theo ngày |
| Cloudinary hết quota giữa buổi bảo vệ | Upload lỗi 4xx | Fallback local đã nằm trong phase; ảnh seed nhúng sẵn trong repo, không phụ thuộc mạng |
| Xoá loại phòng làm mồ côi booking cũ | Lỗi khoá ngoại hoặc mất dữ liệu lịch sử | Chặn xoá cứng khi còn booking; dùng `active = false`. Booking đã snapshot tên và giá nên lịch sử vẫn đọc được |
| Va chạm với Phase 7 ở `shared/` | Conflict git khi merge | `shared/` chốt cuối Phase 4; phát sinh mới thì thêm file mới, không sửa file `shared/` đang có |

**Rollback:** revert commit của phase; landing (Phase 7) không phụ thuộc admin nên vẫn chạy.
