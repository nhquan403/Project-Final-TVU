# CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 5.1. Kết luận

Đề tài đã xây dựng hoàn chỉnh một website giới thiệu và đặt phòng homestay gồm
ba khối: trang bán hàng cho khách, khu quản trị cho chủ homestay, và giao diện
lập trình ứng dụng với 80 thao tác trên 63 đường dẫn. Hệ thống chạy được bằng ba
lệnh, kèm dữ liệu minh hoạ đủ để trình diễn mọi trạng thái nghiệp vụ.

**Về mặt kỹ thuật**, kết quả đáng kể nhất không phải số lượng chức năng mà là
cách giải bài toán chống đặt trùng phòng: đặt ràng buộc ở tầng cơ sở dữ liệu
bằng ràng buộc loại trừ trên kiểu khoảng, thay vì kiểm tra ở tầng ứng dụng. Lựa
chọn này có căn cứ lý thuyết từ chính tài liệu PostgreSQL, vốn khuyến cáo không
dựa vào việc kiểm tra ở tầng ứng dụng trước khi ghi ngay cả ở mức cô lập cao
nhất [9]. Lời giải được chứng minh bằng kiểm thử đa luồng thật trên PostgreSQL
thật, không phải bằng lập luận.

Cùng cơ chế đó được sử dụng lần thứ hai cho một bài toán khác — chống chồng lấn
khoảng đóng phòng — cho thấy lựa chọn công nghệ không nhằm phục vụ một trường
hợp duy nhất.

**Về mặt quy trình**, đề tài cho thấy giá trị của việc kiểm chứng bằng bằng
chứng thay vì bằng khẳng định. Chín lỗi thật được ghi lại ở mục 4.3, trong đó có
những lỗi mà việc biên dịch thành công hoàn toàn không phát hiện được.

**Những nội dung đã tiếp thu được qua quá trình thực hiện:**

- Mẫu kiểm tra rồi ghi ở tầng ứng dụng **không giải được** bài toán tranh chấp.
  Đây là hiểu biết mang tính nguyên lý, áp dụng lại được cho mọi bài toán đặt
  chỗ theo khoảng.
- Hành vi huỷ giao dịch của hệ quản trị cơ sở dữ liệu quan hệ ảnh hưởng trực
  tiếp lên thiết kế mã ứng dụng, không phải chi tiết có thể bỏ qua.
- Kiểm thử phải chạy trên hạ tầng thật khi đối tượng cần kiểm thử chính là hành
  vi của hạ tầng đó.
- Việc biên dịch thành công không nói gì về việc giao diện có hiển thị đúng hay
  không.

## 5.2. Hướng phát triển

### 5.2.1. Ngắn hạn — cần thực hiện trước khi đưa vào sử dụng thật

1. Bổ sung giao thức bảo mật HTTPS bằng chứng chỉ miễn phí.
2. Bổ sung nhật ký kiểm toán cho thao tác quản trị.
3. Kết nối tài khoản dịch vụ thanh toán thật.
4. Thay dữ liệu mẫu bằng dữ liệu thật của cơ sở lưu trú.

### 5.2.2. Trung hạn

5. Chuyển giới hạn tần suất sang bộ nhớ đệm dùng chung để chạy được nhiều bản
   ứng dụng song song.
6. Xây dựng bộ kiểm thử giao diện tự động để chống hồi quy.
7. Bổ sung bảng giá theo mùa. Hiện giá cố định theo loại phòng, chưa có giá cuối
   tuần hay giá ngày lễ.
8. Đồng bộ lịch hai chiều với các sàn đặt phòng qua chuẩn lịch mở, để cơ sở vừa
   bán trên sàn vừa bán trên website riêng mà không đặt trùng.

### 5.2.3. Dài hạn

9. Xây dựng ứng dụng di động sử dụng lại nguyên giao diện lập trình ứng dụng
   hiện có.
10. Mở rộng thành nền tảng phục vụ nhiều chi nhánh.
11. Gợi ý phòng dựa trên lịch sử đặt của khách.
