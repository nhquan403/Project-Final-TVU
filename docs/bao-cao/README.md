# Báo cáo đồ án tốt nghiệp — thứ tự ghép tệp

Thư mục này chứa **nội dung** của quyển báo cáo, tách theo chương để mỗi tệp
không vượt quá giới hạn 800 dòng của quy ước dự án.

## Thứ tự ghép

| # | Tệp | Nội dung |
|---|---|---|
| 1 | `00-phan-dau.md` | Bìa, lời cảm ơn, lời cam đoan, mục lục, danh mục |
| 2 | `01-mo-dau.md` | Phần I — Mở đầu, 8 mục |
| 3 | `02-chuong-1.md` | Chương 1 — Tổng quan về đề tài |
| 4 | `03-chuong-2.md` | Chương 2 — Cơ sở lý thuyết và công nghệ |
| 5 | `04a-chuong-3-use-case.md` | Chương 3, mục 3.1 — Phân tích use case |
| 6 | `04b-chuong-3-co-so-du-lieu.md` | Chương 3, mục 3.2–3.3 — Cơ sở dữ liệu, lớp, trạng thái |
| 7 | `04c-chuong-3-kien-truc.md` | Chương 3, mục 3.4–3.6 — Kiến trúc, API, giao diện |
| 8 | `05-chuong-4.md` | Chương 4 — Xây dựng và triển khai |
| 9 | `06-chuong-5.md` | Chương 5 — Kiểm thử và đánh giá |
| 10 | `07-ket-luan.md` | Phần III — Kết luận và tài liệu tham khảo |
| 11 | `08-phu-luc.md` | Phụ lục A–F |

Lệnh ghép toàn bộ thành một tệp:

```bash
cat 00-phan-dau.md 01-mo-dau.md 02-chuong-1.md 03-chuong-2.md \
    04a-chuong-3-use-case.md 04b-chuong-3-co-so-du-lieu.md \
    04c-chuong-3-kien-truc.md 05-chuong-4.md 06-chuong-5.md \
    07-ket-luan.md 08-phu-luc.md > bao-cao-day-du.md
```

## Chỗ chèn hình

Mỗi chỗ cần hình được đánh dấu bằng một dòng dạng `[Hình 3.5]`. Danh sách đầy đủ
40 hình cùng mô tả chi tiết nằm ở Phụ lục D của `../de-cuong-do-an.md`.

## Quy định trình bày tham chiếu

Tài liệu này neo theo quy định của Khoa Kỹ thuật — Công nghệ, ĐH Văn Hiến:

- Tối thiểu 60 trang phần thuyết minh
- Lề: trên 2cm, dưới 2cm, trái 3cm, phải 1.5cm
- Font Times New Roman cỡ 13, giãn dòng 1.3, thụt đầu dòng 1.0cm
- Trang phụ đánh số La Mã, phần chính đánh số Ả Rập
- Đề mục: `Chương` đậm, `1.1` đậm, `1.1.1` đậm nghiêng

Mẫu chính thức của Khoa CNTT — ĐH Trà Vinh chưa có; cần đối chiếu lại trước khi
định dạng.
