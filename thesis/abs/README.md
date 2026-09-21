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
