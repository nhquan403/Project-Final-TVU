# CHƯƠNG 1 — TỔNG QUAN VỀ ĐỀ TÀI

## 1.1. Giới thiệu bài toán

### 1.1.1. Mô tả cơ sở

Homestay TVH là một cơ sở lưu trú đặt tại Trà Vinh, quy mô **15 phòng vật lý**
thuộc **4 loại phòng**: Standard, Deluxe, Family và Bungalow. Mỗi loại phòng có
giá cơ bản theo đêm, sức chứa người lớn và trẻ em riêng, bộ tiện nghi riêng và
bộ ảnh riêng.

Nghiệp vụ cần tin học hoá gồm bốn nhóm:

**Nhóm 1 — Giới thiệu.** Trình bày loại phòng, tiện nghi, hình ảnh, tin tức và
đánh giá của khách cũ, sao cho khách chưa từng biết tới homestay vẫn hình dung
được họ sẽ nhận được gì.

**Nhóm 2 — Đặt phòng.** Khách chọn khoảng ngày và số khách, hệ thống trả về các
loại phòng còn chỗ kèm chi phí dự kiến, khách gửi yêu cầu đặt phòng và đặt cọc.

**Nhóm 3 — Quản lý vận hành.** Chủ homestay theo dõi đơn theo trạng thái, xác
nhận hoặc huỷ, đối soát tiền đã nhận, và quản lý danh mục phòng, giá, khuyến
mãi, nội dung trang chủ.

**Nhóm 4 — Báo cáo.** Doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ.

### 1.1.2. Bài toán cốt lõi: không được bán trùng một phòng

Đây là ràng buộc nghiệp vụ nghiêm ngặt nhất và là trục kỹ thuật của toàn đồ án.
Phát biểu hình thức:

> Với mọi phòng vật lý *p*, không tồn tại hai đơn đặt phòng còn hiệu lực cùng
> giữ *p* mà khoảng ngày ở của chúng giao nhau.

Điểm tinh tế nằm ở chữ "giao nhau". Nó phải hiểu theo **khoảng nửa mở**. Khách A
trả phòng ngày 10/03 và khách B nhận phòng ngày 10/03 là hợp lệ — buổi sáng A
trả, buổi chiều B nhận. Nếu cài đặt bằng phép so sánh ngày thông thường, chỉ cần
lệch một dấu bằng là hoặc mất một đêm doanh thu mỗi phòng, hoặc bán trùng.

Hình 1.1 minh hoạ sự khác biệt giữa hai khoảng chạm nhau và hai khoảng chồng lấn
thật sự.

[Hình 1.1]

## 1.2. Khảo sát hiện trạng

### 1.2.1. Quy trình thủ công hiện tại

Hình 1.2 mô tả quy trình đặt phòng thủ công đang được áp dụng phổ biến tại các
cơ sở homestay quy mô nhỏ, cùng ba điểm rủi ro chính.

[Hình 1.2]

Bảng dưới đây đối chiếu từng bước của quy trình với nhược điểm tương ứng.

| Bước | Cách làm hiện tại | Nhược điểm |
|---|---|---|
| Tìm hiểu thông tin | Xem ảnh trên trang mạng xã hội | Ảnh lẫn lộn, không có giá rõ ràng, không biết còn phòng hay không |
| Hỏi phòng trống | Nhắn tin, chờ chủ nhà trả lời | Phụ thuộc giờ giấc chủ nhà; khách nhắn lúc nửa đêm phải chờ tới sáng |
| Kiểm tra lịch | Chủ nhà tra sổ hoặc nhớ lại | **Nguồn gốc của việc đặt trùng phòng** |
| Báo giá | Chủ nhà tự tính nhẩm | Dễ nhầm khi khách ở nhiều đêm hoặc đặt nhiều phòng |
| Đặt cọc | Chuyển khoản, gửi ảnh biên lai | Chủ nhà phải tự đối chiếu sao kê bằng mắt |
| Xác nhận | Nhắn tin xác nhận | Không có bằng chứng nào ngoài đoạn hội thoại |
| Quản lý | Sổ giấy hoặc bảng tính | Không thống kê được; mất sổ là mất toàn bộ dữ liệu |

### 1.2.2. Khảo sát người dùng

Đề tài xác định **ba nhóm người dùng** với nhu cầu khác hẳn nhau.

**Khách vãng lai — chiếm đa số.** Họ muốn xem phòng, biết giá, và đặt được
phòng. Họ không muốn tạo tài khoản. Việc bắt buộc đăng ký trước khi đặt là chỗ
mất khách nhiều nhất của một trang đặt phòng. Đây là một quyết định thiết kế
quan trọng của đồ án: tài khoản là **tuỳ chọn**, không phải điều kiện.

**Khách có tài khoản.** Thực hiện được mọi thao tác của khách vãng lai, cộng
thêm việc xem lại toàn bộ đơn đã đặt ở một nơi.

**Quản trị viên — chủ homestay và nhân viên.** Cần nhìn thấy toàn bộ đơn, xử lý
tiền, và cập nhật thông tin phòng. Họ sử dụng hệ thống hằng ngày nên màn hình
quản trị ưu tiên mật độ thông tin cao hơn là tính thẩm mỹ.

## 1.3. Khảo sát các hệ thống tương tự

Đề tài khảo sát hai nền tảng đặt phòng lớn nhất tại thị trường Việt Nam là
Booking.com và Agoda, cùng mô hình Airbnb cho phân khúc lưu trú cá nhân.

Hình 1.3 thể hiện trang kết quả tìm kiếm của Booking.com với bốn thành phần
được phân tích: thanh tìm kiếm cố định, thẻ phòng, tổng tiền cả kỳ và nhãn
khan hiếm.

[Hình 1.3]

Hình 1.4 thể hiện lịch chọn ngày của Agoda, trong đó giá từng đêm hiển thị trực
tiếp trên ô ngày và các ngày hết phòng bị chặn sẵn.

[Hình 1.4]

### 1.3.1. Bảng so sánh

| Tiêu chí | Booking.com | Agoda | Airbnb | **Homestay TVH** |
|---|---|---|---|---|
| Đối tượng | Khách sạn, mọi phân khúc | Khách sạn, mạnh ở châu Á | Lưu trú cá nhân | **Một homestay duy nhất** |
| Hoa hồng | 15–18% | 15–20% | Thu cả chủ và khách | **0%** |
| Đặt không cần tài khoản | Có | Có | Không | **Có** |
| Phương thức thanh toán | Thẻ hoặc trả tại nơi ở | Thẻ | Thẻ | **Chuyển khoản mã QR** |
| Hiện tổng tiền cả kỳ | Có | Có | Có | **Có, ở mọi bước** |
| Chặn sẵn ngày hết phòng | Có | Có | Có | **Có** |
| Nhãn khan hiếm | Có, rất mạnh | Có | Vừa phải | **Có, nhưng chỉ dùng số thật** |
| Hiển thị số người đang xem | Có | Có | Không | **Cố ý không có** |

### 1.3.2. Mẫu thiết kế áp dụng và mẫu cố ý không áp dụng

Đề tài áp dụng các mẫu thiết kế đã được kiểm chứng qua nghiên cứu hành vi người
dùng trên nền tảng đặt chỗ lưu trú [13], nhưng **từ chối** nhóm mẫu gây áp lực
giả (dark pattern) được phân tích trong khảo sát đối sánh hai nền tảng [14].

| Áp dụng từ hai nền tảng | Cố ý làm khác |
|---|---|
| Khoảng trắng rộng, tương phản cao | Nhãn "còn N phòng" lấy trực tiếp từ cơ sở dữ liệu, có ràng buộc loại trừ bảo chứng là đúng |
| Tổng tiền cả kỳ hiển thị ở mọi bước, không chỉ giá mỗi đêm | Đồng hồ đếm ngược chỉ dùng cho hạn giữ chỗ có thật trong cơ sở dữ liệu |
| Thanh điều hướng cố định ở trang chi tiết | Không hiển thị số người đang xem — hệ thống không đo được nên không hiển thị |
| Chặn sẵn ngày hết phòng trong lịch chọn ngày | Không hiển thị thông báo vừa có người đặt nếu không có người đặt thật |
| Khối đánh giá đặt ngay dưới phần chọn phòng | |

Luận điểm của đề tài: các nhãn khan hiếm trên nền tảng lớn thường không tương
ứng với dữ liệu thật. Đồ án giữ lại **hình thức** của chúng, vì chúng thật sự
giúp khách quyết định nhanh, nhưng buộc mọi con số phải có nguồn gốc kiểm chứng
được. Lựa chọn này khả thi chính vì ràng buộc ở tầng cơ sở dữ liệu bảo đảm con
số phòng còn trống luôn đúng.

## 1.4. Xác định yêu cầu

### 1.4.1. Yêu cầu chức năng

**Nhóm A — Dành cho khách**

| Mã | Yêu cầu |
|---|---|
| CN-01 | Xem danh sách loại phòng kèm tiện nghi, hình ảnh, giá |
| CN-02 | Xem chi tiết một loại phòng, xem thư viện ảnh |
| CN-03 | Tìm phòng trống theo khoảng ngày, số người lớn, trẻ em, số phòng |
| CN-04 | Xem lịch từng đêm: đêm nào còn phòng, đêm nào hết |
| CN-05 | Gửi yêu cầu đặt phòng, nhận mã đơn |
| CN-06 | Xem chi phí dự kiến trước khi xác nhận đơn |
| CN-07 | Áp dụng mã khuyến mãi và thấy ngay số tiền được giảm |
| CN-08 | Thanh toán đặt cọc bằng mã QR |
| CN-09 | Tra cứu đơn bằng mã đơn và số điện thoại |
| CN-10 | Huỷ đơn |
| CN-11 | Nhận thư xác nhận qua thư điện tử |
| CN-12 | Viết đánh giá sau khi trả phòng |
| CN-13 | Đăng ký, đăng nhập, xem lại toàn bộ đơn đã đặt |
| CN-14 | Đọc tin tức, xem thư viện ảnh của homestay |

**Nhóm B — Dành cho quản trị viên**

| Mã | Yêu cầu |
|---|---|
| CN-15 | Đăng nhập khu quản trị; buộc đổi mật khẩu tạm ở lần đầu |
| CN-16 | Xem danh sách đơn, lọc theo trạng thái và khoảng ngày |
| CN-17 | Xem chi tiết đơn: lịch sử trạng thái, thanh toán, thư đã gửi, ghi chú |
| CN-18 | Chuyển trạng thái đơn theo máy trạng thái hợp lệ |
| CN-19 | Ghi chú nội bộ trên đơn |
| CN-20 | Đối soát thanh toán: xác nhận thủ công, xử lý khoản lệch |
| CN-21 | Quản lý loại phòng: thêm, sửa, xoá, gán tiện nghi, tải ảnh |
| CN-22 | Quản lý phòng vật lý: thêm, sửa, đổi trạng thái vận hành |
| CN-23 | **Đóng phòng theo khoảng ngày** — quản lý ngày khả dụng |
| CN-24 | Quản lý mã khuyến mãi |
| CN-25 | Duyệt, từ chối, trả lời đánh giá của khách |
| CN-26 | Quản lý nội dung trang chủ, biểu ngữ, thư viện ảnh, tin tức |
| CN-27 | Xem tổng quan: doanh thu, tỉ lệ lấp đầy, tỉ lệ huỷ |
| CN-28 | Xuất báo cáo đơn ra tệp định dạng CSV |

**Nhóm C — Hệ thống tự động**

| Mã | Yêu cầu |
|---|---|
| CN-29 | Nhận webhook khi có tiền về, tự đối chiếu và xác nhận đơn |
| CN-30 | Tự chuyển đơn quá hạn giữ chỗ sang trạng thái hết hạn và nhả phòng |
| CN-31 | Gửi thư qua hàng đợi, ghi nhận mọi thư đã gửi |
| CN-32 | Xử lý tiền về muộn: mở lại đơn, thử giành lại phòng |

### 1.4.2. Yêu cầu phi chức năng

| Mã | Loại | Yêu cầu | Cách kiểm chứng |
|---|---|---|---|
| PCN-01 | Toàn vẹn | Không bao giờ bán trùng một phòng, kể cả khi có tranh chấp | Kiểm thử đa luồng thật |
| PCN-02 | Toàn vẹn | Số phòng đã gán luôn khớp số phòng đơn yêu cầu | Trigger hoãn ở cơ sở dữ liệu |
| PCN-03 | Toàn vẹn | Không nhánh nào để tiền biến mất im lặng | Kiểm thử cửa sổ tranh chấp webhook |
| PCN-04 | Bảo mật | Mặc định từ chối mọi endpoint chưa khai quyền | Kiểm thử ma trận 30 tiền tố |
| PCN-05 | Bảo mật | Chống dò mật khẩu và spam đặt phòng | Giới hạn tần suất theo 9 khoá |
| PCN-06 | Bảo mật | Không có bí mật nào nằm trong mã nguồn | Ứng dụng dừng khởi động khi thiếu biến |
| PCN-07 | Khả dụng | Giao diện sử dụng được trên điện thoại | Kiểm ở 3 độ rộng màn hình |
| PCN-08 | Khả dụng | Đạt chuẩn WCAG 2.1 mức A và AA [10] | Quét tự động |
| PCN-09 | Khả dụng | Mọi vùng chạm tối thiểu 44×44 điểm ảnh | Đo tự động |
| PCN-10 | Hiệu năng | Lịch cả kỳ lấy trong một truy vấn, không lặp từng đêm | Đọc mã truy vấn |
| PCN-11 | Triển khai | Ba lệnh từ lúc sao chép mã nguồn tới lúc chạy | Thực nghiệm |
| PCN-12 | Bảo trì | Lược đồ do công cụ migration quản lý, migration bất biến | Kiểm thử checksum |

## 1.5. Phạm vi và giới hạn của hệ thống

**Trong phạm vi:** toàn bộ 32 yêu cầu chức năng và 12 yêu cầu phi chức năng nêu
trên, đã cài đặt và kiểm thử.

**Ngoài phạm vi và lý do:**

| Không có | Lý do |
|---|---|
| Cổng thanh toán thật | Đề tài không yêu cầu; hệ thống viết đúng luồng thật, chỉ thiếu tài khoản thật |
| Hoàn tiền tự động | Hoàn tiền là quyết định kinh doanh; hệ thống đưa vào hàng đợi cho người xử lý |
| Ứng dụng di động | Website đã đáp ứng; xây dựng ứng dụng di động là một đề tài riêng |
| Nhiều chi nhánh | Bài toán khác hẳn về mô hình dữ liệu |
| Giao thức HTTPS trong bản đóng gói | Cần tên miền và chứng chỉ thật; đây là giới hạn đã biết, trình bày ở mục 5.7 |
