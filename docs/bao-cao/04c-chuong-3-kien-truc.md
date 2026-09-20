## 3.4. Thiết kế kiến trúc phần mềm

### 3.4.1. Phân lớp tầng máy chủ

Hình 3.10 thể hiện sơ đồ phân lớp của tầng máy chủ.

[Hình 3.10]

| Lớp | Trách nhiệm | Không được làm gì |
|---|---|---|
| **Điều khiển** | Nhận yêu cầu HTTP, kiểm tra định dạng đầu vào, ánh xạ sang đối tượng truyền dữ liệu | Không chứa quy tắc nghiệp vụ |
| **Dịch vụ** | Quy tắc nghiệp vụ, ranh giới giao dịch | Không biết gì về HTTP |
| **Truy cập dữ liệu** | Truy cập cơ sở dữ liệu qua JPA, và SQL thuần khi cần | Không chứa quy tắc nghiệp vụ |
| **Cơ sở dữ liệu** | **Lớp bảo vệ cuối cùng** bằng ràng buộc toàn vẹn | — |

**Hai thành phần được gom về một nơi duy nhất, và lý do giống nhau:**

- Thành phần quản lý máy trạng thái là nơi **duy nhất** đổi trạng thái đơn.
- Thành phần tính giá là nơi **duy nhất** tính tiền. Hai công thức tính tiền song
  song sẽ lệch nhau ngay lần đầu có khuyến mãi hoặc làm tròn, và bên lệch là bên
  khách nhìn thấy.

**Tầng giao diện không tính tiền.** Mọi con số hiển thị đều lấy từ phản hồi của
máy chủ, kể cả số tiền giảm khi khách thử mã khuyến mãi. Endpoint kiểm tra mã
khuyến mãi gọi lại đúng thành phần tính giá mà lúc tạo đơn sẽ sử dụng.

### 3.4.2. Cấu trúc gói tầng máy chủ

Mã nguồn tổ chức theo **tính năng**, không theo loại kỹ thuật:

```
com.tvh.homestay
├── auth/          — đăng nhập, token, phiên làm việc
├── user/          — người dùng
├── room/          — loại phòng, phòng, tiện nghi, khoảng đóng
├── availability/  — truy vấn phòng trống
├── booking/       — đơn, máy trạng thái, gán phòng, tính giá
├── payment/       — webhook, đối soát
├── promotion/     — mã khuyến mãi
├── review/        — đánh giá
├── cms/           — thực thể nội dung trang chủ
├── content/       — điều khiển nội dung công khai và quản trị
├── admin/         — điều khiển và dịch vụ khu quản trị
├── report/        — tổng quan, xuất tệp CSV
├── storage/       — tải ảnh lên
├── mail/          — hàng đợi thư
├── common/        — xử lý ngoại lệ, tiện ích dùng chung
└── demo/          — nạp dữ liệu mẫu, chỉ hoạt động ở cấu hình trình diễn
```

Lý do chọn cách tổ chức này: gom theo loại kỹ thuật khiến một thay đổi nghiệp vụ
nhỏ phải sửa tệp ở ba thư mục xa nhau. Gom theo tính năng thì mọi thành phần liên
quan tới đặt phòng nằm cạnh nhau.

### 3.4.3. Cấu trúc tầng giao diện

```
src/app
├── core/        — dịch vụ gọi máy chủ, bộ chặn yêu cầu, bộ bảo vệ tuyến
├── shared/ui/   — thư viện component tự xây
├── features/
│   ├── landing/ — trang khách: trang chủ, phòng, đặt phòng, tra cứu
│   ├── auth/    — đăng nhập, đăng ký
│   └── admin/   — 11 màn hình quản trị, nạp lười
└── styles/      — token màu, lớp nền
```

## 3.5. Thiết kế giao diện lập trình ứng dụng

Hệ thống phơi bày **80 thao tác trên 63 đường dẫn**. Con số này đếm từ tài liệu
mô tả giao diện của hệ thống **đang chạy**, không lấy từ kế hoạch.

### 3.5.1. Nguyên tắc phân quyền: mặc định là đóng

Cấu hình bảo mật kết thúc bằng lệnh từ chối mọi yêu cầu chưa khớp quy tắc nào
phía trên. Một endpoint mới mà người viết quên khai quyền sẽ **bị từ chối**, chứ
không lọt ra công khai. Đây là lựa chọn an toàn khi thất bại thay vì mở khi thất
bại.

Một lớp kiểm thử giữ một **ma trận 30 tiền tố đường dẫn** và kiểm hai điều: mọi
endpoint đã đăng ký phải nằm trong ma trận, và **cấm** khai một dòng bao trùm
toàn bộ khu quản trị. Việc liệt kê từng nhóm buộc người thêm endpoint mới phải
cân nhắc quyền của nó, thay vì được che miễn phí.

Khu quản trị còn một lớp nữa: bộ lọc trả về lỗi `PASSWORD_CHANGE_REQUIRED` cho
mọi đường dẫn trừ ba đường tối thiểu, khi tài khoản còn cờ buộc đổi mật khẩu.

### 3.5.2. Bảng endpoint theo nhóm

| Nhóm | Quyền | Ví dụ |
|---|---|---|
| Công khai — tra cứu và nội dung | Không cần đăng nhập | `GET /api/room-types`, `GET /api/availability` |
| Công khai — đơn đặt phòng | Mã đơn và số điện thoại, hoặc mã truy cập | `POST /api/bookings`, `POST /api/bookings/lookup` |
| Xác thực | Hỗn hợp | `POST /api/auth/login`, `GET /api/me` |
| Quản trị — đơn và thanh toán | `ADMIN` | `GET /api/admin/bookings`, `GET /api/admin/payments` |
| Quản trị — danh mục | `ADMIN` | `POST /api/admin/rooms/{id}/closures` |
| Quản trị — nội dung và đánh giá | `ADMIN` | `PUT /api/admin/content/sections/{key}` |
| Quản trị — báo cáo | `ADMIN` | `GET /api/admin/dashboard` |
| Chỉ có ở cấu hình trình diễn | — | Tài liệu giao diện tương tác |

Bảng đầy đủ 80 thao tác nằm ở Phụ lục C.

Hình 3.11 thể hiện giao diện tài liệu tương tác của hệ thống đang chạy.

[Hình 3.11]

### 3.5.3. Giới hạn tần suất

Hệ thống giới hạn tần suất theo **chín khoá** khác nhau:

| Khoá | Hạn mức | Áp dụng cho |
|---|---|---|
| `ip:auth` | 10 lần mỗi phút | Mọi endpoint xác thực |
| `email:login` | 5 lần mỗi phút | Đăng nhập, theo thư điện tử trong thân yêu cầu |
| `ip:availability` | 60 lần mỗi phút | Truy vấn phòng trống |
| `ip:promo` | 20 lần mỗi phút | Kiểm tra mã khuyến mãi |
| `ip:booking-create` | 10 lần mỗi phút | Tạo đơn |
| `phone:booking` | 10 lần mỗi giờ | Tạo đơn, theo số điện thoại |
| `ip:lookup` | 10 lần mỗi phút | Tra cứu đơn |
| `code:lookup` | 5 lần mỗi giờ | Tra cứu đơn, theo mã đơn |
| `ip:cancel` | 10 lần mỗi phút | Huỷ đơn |

Khi hai khoá cùng áp cho một yêu cầu, khoá chặt hơn chặn trước. Đăng nhập sai
liên tục cùng một thư điện tử bị chặn ở lần thứ sáu, còn đổi thư điện tử mỗi lần
thì bị chặn ở lần thứ mười một.

### 3.5.4. Thiết kế mã lỗi

Phản hồi lỗi theo chuẩn RFC 7807 [9] kèm trường `code` tự thêm. Tầng giao diện
đọc trường `code`, **không** đọc câu chữ mô tả, nên việc sửa câu chữ ở tầng máy
chủ không làm hỏng màn hình.

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `ROOM_NOT_AVAILABLE` | 409 | Phòng vừa được đặt hết cho khoảng ngày này |
| `INVALID_PROMOTION` | 400 | Mã không hợp lệ hoặc chưa đủ điều kiện |
| `PROMOTION_EXHAUSTED` | 409 | Mã đã hết lượt |
| `BOOKING_NOT_FOUND` | 404 | Không có đơn khớp mã và số điện thoại |
| `INVALID_ACCESS_TOKEN` | 403 | Mã truy cập sai |
| `INVALID_STATE_TRANSITION` | 409 | Bước chuyển trạng thái không hợp lệ |
| `INVALID_BOOKING_REQUEST` | 400 | Dữ liệu đơn không hợp lệ |
| `INVALID_ADMIN_REQUEST` | 400 | Dữ liệu quản trị không hợp lệ |
| `CLOSURE_OVERLAP` | 409 | Khoảng đóng phòng chồng lên khoảng đã có |
| `RESOURCE_IN_USE` | 409 | Còn dữ liệu tham chiếu tới bản ghi cần xoá |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Phải đổi mật khẩu tạm trước |
| `UNSUPPORTED_IMAGE_TYPE` | 415 | Định dạng ảnh không được chấp nhận |
| `IMAGE_TOO_LARGE` | 413 | Ảnh vượt giới hạn dung lượng |
| `TOO_MANY_REQUESTS` | 429 | Vượt giới hạn tần suất |
| `UNAUTHORIZED` | 401 | Thiếu hoặc sai token |

## 3.6. Thiết kế giao diện người dùng

### 3.6.1. Hệ thống thiết kế

Bảng màu là **định nghĩa thương hiệu** của hệ thống, không phải phương án tạm.
Mọi mã màu tập trung ở một tệp token duy nhất; một script chạy trong quy trình
kiểm tra **chặn mọi mã màu viết trực tiếp** ở nơi khác.

Hai lớp bảo vệ bổ sung cho nhau: việc xoá bảng màu mặc định của thư viện định
kiểu chặn được các lớp tiện ích màu sẵn có, nhưng không chặn được ai đó viết mã
màu thẳng vào thuộc tính kiểu nội tuyến; script kiểm tra bắt trường hợp còn lại.

Hình 3.12 thể hiện thư viện component của hệ thống.

[Hình 3.12]

### 3.6.2. Quy ước tương tác bắt buộc

| Quy ước | Lý do |
|---|---|
| Vùng chạm tối thiểu 44×44 điểm ảnh trên thiết bị di động | Ngón tay không chính xác như con trỏ chuột |
| Chữ không bao giờ nhỏ hơn 16 điểm ảnh trên di động | Dưới mức này một số trình duyệt tự phóng to khi chạm vào ô nhập |
| Lỗi không bao giờ chỉ báo bằng màu | Người khó phân biệt màu không nhận ra; luôn kèm biểu tượng và chữ |
| Viền chỉ báo tiêu điểm rõ ràng, không bao giờ bị xoá | Người dùng bàn phím cần biết đang ở vị trí nào |
| Tôn trọng tuỳ chọn giảm chuyển động của hệ điều hành | Phục vụ người nhạy cảm với chuyển động |
| **Chặn sẵn thay vì báo lỗi sau** | Ngày hết phòng bị chặn ngay trong lịch, không để khách chọn rồi mới báo |

Các quy ước trên bám theo hướng dẫn khả năng tiếp cận nội dung web mức A và AA
[10].

### 3.6.3. Sơ đồ điều hướng

Hình 3.13 thể hiện sơ đồ điều hướng của khu vực công khai, và Hình 3.14 thể hiện
sơ đồ điều hướng của khu quản trị.

[Hình 3.13]

[Hình 3.14]

Khu vực công khai gồm các tuyến chính: trang chủ, danh sách phòng, chi tiết
phòng, luồng đặt phòng ba bước, màn hình thanh toán, trang hoàn tất, tra cứu
đơn, tin tức và đăng nhập.

Khu quản trị gồm mười một màn hình, được nạp lười theo tuyến để người truy cập
trang đặt phòng không phải tải mã của những màn hình không sử dụng.

| Khu vực | Đường dẫn tiêu biểu | Cơ chế bảo vệ |
|---|---|---|
| Công khai | `/`, `/phong`, `/dat-phong`, `/tra-cuu`, `/tin-tuc` | Không |
| Tài khoản khách | `/tai-khoan/dat-phong` | Bộ bảo vệ tuyến yêu cầu đăng nhập |
| Quản trị | `/admin/**` | Bộ bảo vệ tuyến và kiểm tra vai trò ở máy chủ |
