# Báo cáo trình bày

| Tệp | Nội dung |
|---|---|
| `slide-bao-ve.pptx` | 20 slide, khổ 16:9, tiêu đề 44pt, nội dung 32pt |
| `slide-bao-ve.pdf` | Bản PDF của cùng bộ slide, để chiếu khi máy không có PowerPoint |
| `poster.pdf` | Poster **60cm × 90cm hướng đứng**, đúng kích thước vật lý, đem đi in là ra đúng cỡ |
| `poster-xem-truoc.png` | Ảnh xem nhanh của poster, không dùng để in |

Kịch bản nói cho từng slide ở **`docs/kich-ban-bao-ve.md`**, và cũng nằm ngay
trong phần ghi chú của từng slide khi mở bằng PowerPoint.

## Dựng lại

```sh
pip install python-pptx
python3 scripts/tao-slide.py thesis/abs/slide-bao-ve.pptx
python3 scripts/tao-kich-ban.py docs/kich-ban-bao-ve.md

npm install playwright          # poster dựng bằng Chromium
node scripts/tao-poster.mjs thesis/abs/poster.pdf
```

Nội dung slide nằm ở `scripts/noi-dung-slide.py` — sửa ở đó rồi chạy lại **cả
hai** script để slide và kịch bản nói không lệch nhau.

## Việc còn phải làm tay

1. **Điền thông tin sinh viên ở slide bìa và trên poster**: tên GVHD, tên SVTH,
   mã số, lớp. Hiện là dấu chấm cho điền tay, giống các trang bìa của quyển báo
   cáo. Sửa trong `scripts/noi-dung-slide.py` và `scripts/tao-poster.mjs` rồi
   dựng lại, đừng sửa thẳng vào tệp đã xuất.
2. **Tập nói và bấm giờ.** Hội đồng cho 10–20 phút *kể cả hỏi đáp*, nên phần
   trình bày chỉ nên 10–12 phút — trung bình 35 giây một slide.
3. **Đặt in poster** trên giấy A0 hoặc bạt, 60cm × 90cm. Nội dung poster lấy
   từ chính báo cáo nên không phải viết lại; chỉ cần điền tên và đem đi in.

## Bản trình diễn

| Tệp | Nội dung |
|---|---|
| `demo-homestay-tvh.mp4` | Video 2–3 phút đi hết một vòng, có chú thích chạy dưới màn hình |
| `demo-anh/` | 15 ảnh chụp từng bước, cùng lượt với video |
| `demo-tong-hop.png` | Bảng gộp 15 ảnh để xem nhanh |

Thứ tự trong video: trang chủ → **lịch có ngày bị chặn sẵn** → chọn phòng (giá
là tổng cả kỳ) → ba bước đặt phòng → mã QR và đồng hồ đếm ngược → webhook ngân
hàng xác nhận → thư xác nhận trong hộp thư → tra cứu đơn → khu quản trị: buộc
đổi mật khẩu, tổng quan, danh sách đơn, chi tiết đơn, đối soát, ngày khả dụng,
đánh giá.

Toàn bộ chạy trên bản đóng gói `docker compose`, đi qua đúng máy chủ web và
đúng webhook thật — không có bước nào gọi tắt vào tầng trong.

### Quay lại

```sh
docker compose down -v && docker compose up -d
docker compose logs api | grep "Mat khau"
npm install playwright
node scripts/quay-demo.mjs http://localhost <mat-khau-quan-tri>
```

**Phải dựng lại cơ sở dữ liệu sạch trước mỗi lần quay.** Kịch bản đi qua bước
buộc đổi mật khẩu tạm, nên chạy lần thứ hai với mật khẩu cũ sẽ đăng nhập thất
bại. Script có hai chốt kiểm tra chặn việc đó: một chốt bắt buộc phải vào được
khu quản trị, một chốt bắt buộc đơn phải sang trạng thái đã xác nhận sau webhook.

### Dữ liệu mẫu cho buổi trình diễn

| Thứ | Nằm ở đâu |
|---|---|
| **Cuối tuần cháy phòng** — mọi loại phòng đều hết | +12 đến +14 ngày kể từ ngày khởi động. Lịch chặn sẵn hai đêm đó |
| Khoảng đóng phòng **đang áp dụng** | phòng B02, từ hôm qua tới +3 ngày |
| Khoảng đóng phòng **đã qua** (hệ thống tự mở lại) | phòng 101, −20 đến −15 ngày |
| Khoảng đóng phòng **sắp tới** | phòng 101, +40 đến +45 ngày |
| Ghi chú nội bộ | sáu đơn `TVHDEMO…` gần nhất |
| Thư đã gửi và **một thư thất bại** | đơn `TVHFULL01` mang trạng thái `FAILED` |
| Khoản thiếu tiền và khoản thừa tiền | màn hình đối soát, có sẵn khi mở |
