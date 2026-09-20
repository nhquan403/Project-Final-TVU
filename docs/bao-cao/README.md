# Báo cáo đồ án — thứ tự ghép tệp

Nội dung quyển báo cáo, tách theo chương để mỗi tệp không vượt 800 dòng theo quy
ước dự án.

## Cấu trúc theo đúng quy định của Khoa Kỹ thuật và Công nghệ — ĐH Trà Vinh

| # | Tệp | Nội dung | Tính vào 30–50 trang |
|---|---|---|---|
| 1 | `00-phan-dau.md` | Bìa, nhận xét, lời mở đầu, lời cảm ơn, mục lục, danh mục, viết tắt, tóm tắt | Không |
| 2 | `01-mo-dau.md` | MỞ ĐẦU — lí do, mục đích, đối tượng, phạm vi | **Có** |
| 3 | `02-chuong-1.md` | CHƯƠNG 1. TỔNG QUAN | **Có** |
| 4 | `03-chuong-2.md` | CHƯƠNG 2. NGHIÊN CỨU LÝ THUYẾT | **Có** |
| 5 | `04a-chuong-3-thiet-ke.md` | CHƯƠNG 3, mục 3.1–3.6 — phân tích và thiết kế | **Có** |
| 6 | `04b-chuong-3-cai-dat.md` | CHƯƠNG 3, mục 3.7–3.10 — cài đặt và triển khai | **Có** |
| 7 | `05-chuong-4.md` | CHƯƠNG 4. KẾT QUẢ NGHIÊN CỨU | **Có** |
| 8 | `06-chuong-5.md` | CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN | **Có** |
| 9 | `07-tai-lieu-tham-khao.md` | DANH MỤC TÀI LIỆU THAM KHẢO — định dạng IEEE | Không |
| 10 | `08a-phu-luc-dac-ta.md` | PHỤ LỤC A–B — đặc tả use case và bảng dữ liệu | Không |
| 11 | `08b-phu-luc-ky-thuat.md` | PHỤ LỤC C–N — mã nguồn, bảng tra cứu, lệnh | Không |

Lệnh ghép:

```bash
cat 00-phan-dau.md 01-mo-dau.md 02-chuong-1.md 03-chuong-2.md \
    04a-chuong-3-thiet-ke.md 04b-chuong-3-cai-dat.md 05-chuong-4.md \
    06-chuong-5.md 07-tai-lieu-tham-khao.md \
    08a-phu-luc-dac-ta.md 08b-phu-luc-ky-thuat.md > bao-cao-day-du.md
```

## Quy định trình bày phải áp dụng khi xuất Word

| Hạng mục | Quy định |
|---|---|
| Độ dài nội dung | **30–50 trang A4** (không kể bìa, lời cảm ơn, mục lục, tài liệu tham khảo, phụ lục) |
| Font | Times New Roman, **13pt** |
| Giãn dòng | **1.5 lines** |
| Cách đoạn | Before **6pt**, After **6pt** |
| Lề | trên 2cm · dưới 2cm · **trái 3cm** · **phải 2cm** |
| Số trang | **góc phải dưới**, bắt đầu đánh từ Chương 1 |
| Footer | `GVHD: ...` bên trái, `SVTH: ...` bên phải |
| Đánh số mục | Hệ thống số Ả Rập, **không dùng số La Mã**. `Chương 3` → `3.1.` → `3.1.1.` |
| Mục lục | Không quá **04 cấp** tiểu mục |
| Bảng/sơ đồ/hình | Đánh số theo chương. Cuối mỗi cái **phải ghi chú, nêu rõ nguồn trích hoặc sao chụp** |
| Tài liệu tham khảo | **Định dạng IEEE**, xếp theo thứ tự từ điển, tách Tiếng Việt / Tiếng Anh |
| Bìa | Bìa cứng, chữ nhũ vàng. Tên khoa: **KHOA KỸ THUẬT VÀ CÔNG NGHỆ** |

## Chỗ chèn hình

28 chỗ, đánh dấu bằng một dòng dạng `[Hình 3.5]`, mỗi hình đều có câu dẫn trong
văn bản. Phân bố: Chương 1 có 3 hình, Chương 2 có 2, Chương 3 có 21, Chương 4
có 2.
