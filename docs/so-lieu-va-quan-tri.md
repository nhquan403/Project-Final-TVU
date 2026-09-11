# Số liệu dashboard và khu quản trị

Tài liệu cho người vận hành homestay và người bảo trì mã nguồn. Phần quan trọng
nhất là mục đầu: **ba chỉ số dùng ba trục thời gian khác nhau**, và hiểu nhầm
điều đó là cách nhanh nhất để đọc sai toàn bộ báo cáo.

## Ba trục thời gian — đọc kỹ trước khi so sánh hai con số

| Chỉ số | Định nghĩa | Trục thời gian |
|---|---|---|
| **Giá trị booking** | `SUM(total_amount)` các đơn `CONFIRMED/CHECKED_IN/CHECKED_OUT` | ngày **nhận phòng** (`check_in`) |
| **Tiền đã thu** | `SUM(amount_received)` các lần thanh toán `SUCCEEDED/OVERPAID` | ngày **tiền về** (`paid_at`) |
| **Tỉ lệ lấp đầy** | đêm-phòng đã bán ÷ (tổng số phòng × số đêm của tháng) | khoảng ngày **lưu trú** |
| **Đơn mới** | `COUNT(*)` | ngày **tạo đơn** (`created_at`) |
| **Tỉ lệ huỷ** | `COUNT(CANCELLED + NO_SHOW) ÷ COUNT(*)` | ngày **tạo đơn** |
| **Cần đối soát** | số lần thanh toán có `reconcile_status IN ('NEEDS_REVIEW','REFUND_REQUIRED')` | hiện tại |

**Không cái nào tên là "doanh thu", và đó là chủ ý.** Hệ thống chỉ thu 30% tiền
cọc trước, nên `SUM(total_amount)` là giá trị các đơn đã chốt — phần lớn số tiền
đó chưa về tài khoản. Gọi nó là doanh thu sẽ sai ngay khi có người hỏi "tiền này
đã về chưa?".

Giao diện in câu mô tả trục thời gian ngay dưới mỗi thẻ số liệu; API cũng trả
chúng trong trường `axes`, nên hai nơi không thể lệch nhau.

## Tỉ lệ lấp đầy: mẫu số là TỔNG SỐ PHÒNG VẬT LÝ

Mẫu số là `COUNT(*) FROM rooms`, **không lọc theo `status`**.

Lọc theo `status = 'AVAILABLE'` khiến mẫu số phản ánh trạng thái *hiện tại*
trong khi tử số là dữ liệu *lịch sử*: đưa ba phòng đi bảo trì hôm nay là tỉ lệ
lấp đầy của mọi tháng đã qua nhảy vọt, và có thể vượt 100%. Kết quả vẫn được kẹp
`LEAST(ratio, 1)` như một lớp chặn thứ hai.

Đơn vắt qua ranh giới tháng chỉ tính phần nằm trong tháng đó, bằng
`LEAST(check_out, cuối_tháng) - GREATEST(check_in, đầu_tháng)`.

`DashboardServiceIT` kiểm đúng bốn tình huống này.

## Múi giờ

Hai loại cột được xử lý khác nhau, có chủ ý:

- `check_in` / `check_out` là kiểu `date` — một ngày lịch, không có giờ và không
  có múi giờ. `date_trunc` trên chúng đã đúng sẵn.
- `created_at` / `paid_at` là `timestamptz`. Gom nhóm chúng **bắt buộc** kèm
  `AT TIME ZONE 'Asia/Ho_Chi_Minh'`; thiếu mệnh đề đó, đơn tạo lúc 00:00–07:00
  giờ Việt Nam ngày mùng 1 bị xếp vào tháng trước.

## Đối chiếu số liệu bằng tay

Dashboard và truy vấn thô chỉ khớp khi truyền **cùng tham số kỳ**. `to` là ngày
**không** bao gồm.

```bash
PERIOD_START=2026-01-01; PERIOD_END=2027-01-01
docker exec homestay-db psql -U postgres -d homestay -c "
SELECT date_trunc('month', check_in)::date AS thang, SUM(total_amount)
FROM bookings
WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
  AND check_in >= '$PERIOD_START' AND check_in < '$PERIOD_END'
GROUP BY 1 ORDER BY 1;"

curl -s "localhost:8080/api/admin/dashboard?from=$PERIOD_START&to=$PERIOD_END" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.bookingValueByMonth'
```

## Vòng đời đơn: trả phòng KHÔNG nhả dòng `booking_rooms`

Chỉ `CANCELLED`, `EXPIRED` và `NO_SHOW` trả phòng về kho — đó là những trạng
thái mà chuyến đi **không** diễn ra. `CHECKED_OUT` giữ nguyên các dòng `ACTIVE`
vì chúng là bằng chứng số đêm-phòng đã bán; nhả chúng vừa bị ràng buộc
`assert_booking_room_count` (V3) bác, vừa đưa tỉ lệ lấp đầy của mọi chuyến đã
hoàn tất về 0. Việc này không giam phòng: ràng buộc chống trùng chỉ so khoảng
ngày, mà khoảng ngày của chuyến đã trả phòng nằm trong quá khứ.

## Tải ảnh lên

- Danh sách trắng **tường minh**: JPEG, PNG, WebP. **SVG bị loại** — nó là XML
  thuần, không có magic bytes cố định nên mọi bộ dò nội dung đều gọi là "ảnh hợp
  lệ", và khi phục vụ cùng origin nó chạy JavaScript.
- Mọi ảnh được **giải mã lại và ghi lại** bằng bộ mã hoá của chính hệ thống. Việc
  này vứt bỏ phần đuôi của tệp polyglot và toàn bộ metadata EXIF (gồm cả toạ độ
  GPS của khách). Ảnh WebP hợp lệ được ghi lại thành PNG vì thư viện đang dùng
  chỉ đọc được WebP.
- Tên tệp là UUID do máy chủ sinh; tên do client gửi bị bỏ hoàn toàn.
- Giới hạn 5MB được đặt ở **cả hai** nơi và phải khớp nhau:
  `spring.servlet.multipart.max-file-size` và `storage.max-image-bytes`. Chỉ đặt
  ở tầng ứng dụng là vô nghĩa — mặc định 1MB của Spring Boot chặn trước.
- Chưa cấu hình `CLOUDINARY_URL` thì ảnh lưu vào `uploads/` và được phục vụ qua
  `/uploads/**` kèm `X-Content-Type-Options: nosniff`, `Content-Type` do máy chủ
  quyết định theo danh sách trắng đuôi tệp, và `Content-Disposition: inline`.
  Các header này đặt ở **tầng ứng dụng**, không đợi nginx — bản chạy tay lúc
  trình diễn không có nginx.

## Xuất CSV

`GET /api/admin/reports/bookings.csv` có **BOM UTF-8** (Excel trên Windows đọc
tệp không BOM bằng bảng mã ANSI và làm hỏng dấu tiếng Việt) **và** thoát ký tự
chống công thức: ô bắt đầu bằng `=`, `+`, `-`, `@`, tab hoặc CR được thêm tiền
tố dấu nháy đơn. `guest_name` và `special_request` do khách ẩn danh nhập; xuất
thẳng vào tệp mà quản trị viên mở bằng Excel là một đường thực thi lệnh trên máy
quản trị viên.

## Phân quyền khu quản trị

Việc thi hành nằm ở **một** dòng trong `SecurityConfig`:
`/api/admin/** → hasRole("ADMIN")`. Một quy tắc duy nhất tốt hơn ba mươi quy tắc
để lệch nhau.

Lưới chắn là `EndpointAuthorizationIT`: ma trận trong test liệt kê **từng nhóm**
endpoint quản trị và có thêm một kiểm tra cấm khai lại dòng bao `"/api/admin"`.
Nhờ vậy, thêm một nhóm endpoint mới buộc người viết phải khai nó ra và dừng lại
nghĩ một lần xem nhóm đó có thật sự chỉ dành cho ADMIN hay không. Trước Phase 7,
ma trận chỉ có một dòng `"/api/admin"` — nghĩa là lưới chắn được tuyên bố nhưng
không bắt được endpoint quản trị nào.

## Endpoint mô phỏng thanh toán

Xem `docs/thanh-toan-sepay.md` — ba rào chắn: đường dẫn dưới `/api/admin/**`,
cờ `payments.simulator.enabled` mặc định tắt, và không nạp ở profile `prod`.
