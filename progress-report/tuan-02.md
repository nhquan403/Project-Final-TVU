# Báo cáo tiến độ — Tuần 02

| | |
|---|---|
| **Họ và tên** | . . . . . . . . . . . . . . . . . . . . |
| **MSSV** | . . . . . . . . . . |
| **Lớp** | . . . . . . . . . . |
| **GVHD** | . . . . . . . . . . . . . . . . . . . . |
| **Tên đề tài** | Xây dựng website giới thiệu và đặt phòng homestay — Homestay TVH |
| **Kỳ báo cáo** | từ 16/09/2026 đến 20/09/2026 |

## 1. Công việc đã hoàn thành trong tuần

| # | Công việc | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Đóng gói toàn hệ thống bằng Docker Compose | Bốn dịch vụ; chạy được bằng ba lệnh | `4bee7bb` |
| 2 | Dữ liệu mẫu đủ mọi trạng thái đơn | 15 phòng, 40 đơn mẫu, nạp ngoài Flyway để không lẫn vào lược đồ | `f197ad4` |
| 3 | Buộc đổi mật khẩu ở lần đăng nhập đầu của tài khoản dựng sẵn | Bộ lọc chặn mọi đường dẫn trừ ba đường tối thiểu | `e6a572c` |
| 4 | **Quản lý ngày khả dụng của phòng** | Bảng `room_closures` dùng lại ràng buộc loại trừ lần thứ hai; điều kiện lọc thêm vào **cả bốn** truy vấn phòng trống | `d7fc0c7`, `d747ae1` |
| 5 | Tám tài liệu kỹ thuật, đối chiếu với hệ thống đang chạy | Người khác tiếp nhận được dự án mà không cần hỏi | `c480d9f` |
| 6 | **Viết toàn bộ phần chữ của báo cáo** | Mở đầu và năm chương, theo đúng tên chương quy định của khoa | `f4dfbe0` → `8db415c` |
| 7 | Tái cấu trúc báo cáo theo đúng quy định Khoa KT-CN | 13 trang đầu theo biểu mẫu BM5, đánh số chương bằng số Ả Rập | `c6c7a0a`, `aada800` |
| 8 | **Dựng 30 hình minh hoạ** | 14 sơ đồ + 13 ảnh chụp từ hệ thống chạy thật + 3 ảnh kết quả đo | `9cb66f3` |
| 9 | Script xuất bản Word theo đúng trình bày TVU | Times New Roman 13pt, giãn dòng 1.5, lề 3/2/2/2, số trang góc phải dưới | `da1241a` |
| 10 | Sửa hai lỗi giao diện tìm ra khi chụp ảnh màn hình | Mở được chi tiết mọi đơn; ô chọn tệp ảnh có tên cho trình đọc màn hình | `368d1a3` |

## 2. Công việc chưa hoàn thành

| # | Công việc | Lý do | Hướng xử lý |
|---|---|---|---|
| 1 | Cắt báo cáo cho vừa trần 50 trang | Đo thật ra 66 trang, nhiều hơn ước lượng | Làm ở tuần 03 |
| 2 | Slide bảo vệ và poster | Phụ thuộc phần chữ đã chốt | Làm ở tuần 03 |
| 3 | Hình 1.3 — ảnh trang kết quả tìm kiếm của Booking.com | Là trang web bên ngoài, phải tự mở trình duyệt chụp và ghi nguồn | Tự chụp trước khi nộp |

## 3. Khó khăn gặp phải

**Ảnh chụp khu quản trị đều ra trang đăng nhập.** Kích thước tệp trông hợp lý
nên suýt bỏ qua; chỉ phát hiện khi **mở ảnh ra xem**. Nguyên nhân: máy chủ từ
chối yêu cầu vì địa chỉ gốc của trình duyệt không nằm trong danh sách cho phép
— tức là hệ thống **hành xử đúng**. Khắc phục bằng cách khai đúng địa chỉ khi
chạy, không nới lỏng cấu hình.

**Khu quản trị đóng nhầm phòng không hiển thị.** Khối giao diện giữ lại định
danh phòng cũ sau khi bộ lọc của màn hình cha thay đổi. Lỗi này do một vòng rà
soát mã độc lập tìm ra **sau khi** đã tự kiểm thử và cho rằng đã xong.

**Bước dựng ảnh Docker không chạy trọn vẹn được** trong môi trường phát triển vì
proxy chặn kho phụ thuộc từ bên trong container. Đã kiểm chứng được toàn bộ phần
còn lại của bản đóng gói; ghi rõ giới hạn này ở mục 4.6.3 của báo cáo thay vì
giấu đi.

## 4. Kế hoạch tuần tới

| # | Công việc dự kiến | Kết quả mong đợi |
|---|---|---|
| 1 | Cắt báo cáo cho vừa trần 50 trang | Đo thật bằng cách xuất PDF và đếm trang |
| 2 | Slide bảo vệ + kịch bản nói | 15–20 slide, trình bày 10–12 phút |
| 3 | Poster 60cm × 90cm hướng đứng | Cộng tối đa 1 điểm |
| 4 | Hoàn tất yêu cầu GitHub | Đổi tên repo, mời GVHD làm Collaborator |

## 5. Commit trong tuần

**Kỳ báo cáo:** từ 2026-09-16 đến 2026-09-20 — **20 commit**

| Mã commit | Nội dung |
|---|---|
| `4bee7bb` | build(docker): đóng gói toàn hệ thống chạy bằng một lệnh |
| `1349b64` | fix(web): trang production mất sạch CSS, và Swagger lệch một cấp đường dẫn |
| `f197ad4` | feat(demo): dữ liệu mẫu đủ mọi trạng thái, nạp ngoài Flyway |
| `e6a572c` | feat(admin): màn hình đổi mật khẩu bắt buộc cho tài khoản dựng sẵn |
| `c480d9f` | docs: tám tài liệu kỹ thuật đối chiếu với hệ thống đang chạy |
| `0df70be` | docs(plan): phase 10 — lịch khoá phòng theo khoảng ngày |
| `d7fc0c7` | feat(rooms): đóng phòng theo khoảng ngày, trừ khỏi cả bốn truy vấn phòng trống |
| `d747ae1` | fix(rooms): khối khoảng đóng không thao tác nhầm phòng, danh sách bỏ khoảng đã qua |
| `93733d5` | docs: đề cương tổng hợp đồ án tốt nghiệp |
| `12bda3f` | docs: kế hoạch viết báo cáo và vá hai khoảng trống của đề cương |
| `f4dfbe0` | docs(bao-cao): phan dau, mo dau va chuong 1 |
| `b3e0157` | docs(bao-cao): chuong 2 - co so ly thuyet |
| `80318a4` | docs(bao-cao): chuong 3 - phan tich va thiet ke |
| `8db415c` | docs(bao-cao): chuong 4, chuong 5, ket luan va phu luc |
| `9053cd3` | chore(plan): danh dau phase 1-7 hoan thanh |
| `c6c7a0a` | docs(bao-cao): tai cau truc theo dung quy dinh cua Khoa KT-CN DH Tra Vinh |
| `aada800` | docs: viet lai de cuong theo quy dinh TVU va dung cay thu muc bat buoc |
| `9cb66f3` | docs(bao-cao): 30 hinh minh hoa va dong bo lai danh so hinh |
| `da1241a` | feat(bao-cao): script xuat ban Word theo dung trinh bay TVU |
| `368d1a3` | fix(admin): mo duoc chi tiet moi don, va dat ten cho o chon tep anh |

---

*Ngày lập: 20/09/2026*
*Sinh viên thực hiện*
