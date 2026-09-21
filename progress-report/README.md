# Báo cáo tiến độ

Thư mục **bắt buộc** theo mục 4.3 của *"Một số quy định về hình thức trình bày
thực tập đồ án cơ sở ngành và chuyên ngành"* — Khoa Kỹ thuật và Công nghệ,
Trường Đại học Trà Vinh.

## Quy định liên quan

> *"Trong repository của mình, sinh viên tạo một thư mục mang tên
> `progress-report` và upload file báo cáo tiến độ của mình hàng tuần."*

> *"Sinh viên quản lý dự án của mình bằng repository này, commit thường xuyên
> theo nội dung đã thực hiện ít nhất một tuần một lần. **Lịch sử commit sẽ là
> một tiêu chí chấm điểm tiến độ.**"*

> *"Nếu trong lịch sử commit không ghi nhận tiến độ cập nhật dự án (dù có báo
> cáo tiến độ) xem như sinh viên không hoàn thành báo cáo tiến độ."*

## Quy ước đặt tên

```
tuan-01.md, tuan-02.md, ...
```

Dùng `mau-bao-cao-tuan.md` làm khuôn.

## Danh sách báo cáo

| Tuần | Tệp | Kỳ báo cáo | Nội dung chính |
|---|---|---|---|
| 01 | `tuan-01.md` | 07/09 – 12/09/2026 | Kế hoạch, hệ thiết kế, lược đồ cơ sở dữ liệu và ràng buộc chống đặt trùng, xác thực, lõi đặt phòng, thanh toán, khu quản trị, trang bán hàng — **21 commit** |
| 02 | `tuan-02.md` | 16/09 – 20/09/2026 | Đóng gói Docker, dữ liệu mẫu, quản lý ngày khả dụng, tám tài liệu kỹ thuật, toàn bộ phần chữ báo cáo, 30 hình, script xuất Word — **20 commit** |
| 03 | `tuan-03.md` | từ 21/09/2026 | Đưa báo cáo từ 66 xuống 52 trang, slide bảo vệ 20 trang kèm kịch bản nói, poster 60×90cm — **3 commit** |

## Bảng commit lấy từ đâu

Mục 5 của mỗi báo cáo là bảng commit **sinh thẳng từ `git log`**, không chép tay,
vì quy định lấy lịch sử commit làm tiêu chí chấm điểm tiến độ — bảng trong báo
cáo phải khớp từng dòng với lịch sử thật.

```sh
python3 scripts/tao-bao-cao-tuan.py          # liệt kê các tuần có commit
python3 scripts/tao-bao-cao-tuan.py 38       # bảng commit của tuần ISO 38
```

## Việc còn phải làm tay

Điền **họ tên, MSSV, lớp và tên GVHD** vào bảng đầu của cả ba báo cáo — hiện để
dấu chấm, giống các trang bìa của quyển báo cáo.
