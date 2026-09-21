# CHƯƠNG 3. HIỆN THỰC HÓA NGHIÊN CỨU

## 3.1. Phân tích chức năng — biểu đồ use case

### 3.1.1. Xác định tác nhân

| Tác nhân | Mô tả | Cơ chế xác thực |
|---|---|---|
| **Khách vãng lai** | Không có tài khoản. Đặt, tra cứu và huỷ đơn bằng mã đơn và số điện thoại | Không đăng nhập. Thao tác trên một đơn cụ thể cần mã đơn và số điện thoại, hoặc mã truy cập trong liên kết thư xác nhận |
| **Khách có tài khoản** | Thực hiện được mọi thao tác của khách vãng lai, thêm việc xem lại mọi đơn ở một nơi | Token, vai trò `CUSTOMER` |
| **Quản trị viên** | Chủ homestay và nhân viên | Token, vai trò `ADMIN` |
| *Tác nhân hệ thống* | Nhà cung cấp thanh toán gọi webhook; bộ quét định kỳ | Khoá trong phần đầu yêu cầu, hoặc nội bộ |

Hình 3.1 thể hiện biểu đồ use case tổng quát của hệ thống.

[Hình 3.1]

### 3.1.2. Danh sách use case

| Mã | Tên use case | Tác nhân chính |
|---|---|---|
| UC1 | Tìm phòng trống | Khách vãng lai, Khách có tài khoản |
| UC2 | Đặt phòng | Khách vãng lai, Khách có tài khoản |
| UC3 | Thanh toán cọc | Khách vãng lai, Khách có tài khoản |
| UC4 | Đối soát thanh toán | Quản trị viên |
| UC5 | Tra cứu và huỷ đơn | Khách vãng lai, Khách có tài khoản |
| UC6 | Quản lý đơn đặt phòng | Quản trị viên |
| UC7 | Xem lại toàn bộ đơn đã đặt | Khách có tài khoản |
| UC8 | Xem báo cáo doanh thu | Quản trị viên |
| UC9 | Đặt ngày khả dụng của phòng | Quản trị viên |
| UC10 | Soạn nội dung trang chủ | Quản trị viên |
| UC11 | Viết đánh giá | Khách đã trả phòng |

### 3.1.3. Đặc tả use case

Đặc tả đầy đủ của **cả mười một use case** — tác nhân, tiền điều kiện, luồng
chính, luồng thay thế, hậu điều kiện — trình bày ở Phụ lục A. Hai use case trọng
tâm là UC1 *Tìm phòng trống* và UC2 *Đặt phòng*: UC1 là nơi truy vấn còn phòng
chạy, UC2 là nơi ràng buộc loại trừ của cơ sở dữ liệu ra quyết định cuối cùng
khi hai khách cùng đặt một phòng.

## 3.2. Thiết kế cơ sở dữ liệu

### 3.2.1. Tổng quan

Lược đồ gồm **21 bảng nghiệp vụ**, dựng bởi **tám migration** Flyway từ `V1` tới
`V8`. Bảng thứ 22 trong lược đồ công khai là sổ ghi chép của chính Flyway, không
thuộc mô hình nghiệp vụ.

Hình 3.2 thể hiện sơ đồ quan hệ thực thể tổng thể.

[Hình 3.2]

| Nhóm | Migration | Các bảng | Vai trò |
|---|---|---|---|
| Người dùng và xác thực | V1 | `users`, `refresh_tokens` | Tài khoản, vai trò, phiên đăng nhập |
| Phòng và tiện nghi | V2 | `amenities`, `room_types`, `rooms`, `room_type_images`, `room_type_amenities` | Danh mục sản phẩm |
| Đặt phòng | V3 | `bookings`, `booking_rooms`, `booking_status_history` | **Lõi nghiệp vụ** |
| Thanh toán và thư | V4 | `payments`, `payment_webhook_events`, `outbound_emails` | Tiền và liên lạc |
| Khuyến mãi và đánh giá | V5 | `promotions`, `reviews` | Tiếp thị và uy tín |
| Nội dung | V6 | `site_contents`, `banners`, `gallery_images`, `posts` | Trang chủ, tin tức |
| Ghi chú nội bộ | V7 | `booking_notes` | Trao đổi nội bộ trên đơn |
| Ngày khả dụng | V8 | `room_closures` | Khoảng ngày phòng không nhận khách |

Hình 3.3 thể hiện chi tiết nhóm bảng đặt phòng, nơi đặt ràng buộc quan trọng
nhất của hệ thống.

[Hình 3.3]

### 3.2.2. Mô tả bốn bảng cốt lõi

Bốn bảng dưới đây mang các ràng buộc quyết định của hệ thống; phần này giải
thích **vì sao** mỗi ràng buộc tồn tại. Danh sách cột đầy đủ của cả 21 bảng,
theo mẫu ba cột tên cột – kiểu – mô tả và ràng buộc, trình bày ở Phụ lục B.

**Bảng `users` — tài khoản người dùng**

Ràng buộc `ck_users_email_lower` buộc lưu email chữ thường **ở tầng dữ liệu**
thay vì phó mặc cho tầng ứng dụng. Chỉ cần một chỗ quên chuẩn hoá là tạo được
hai tài khoản cho cùng một hộp thư.

**Bảng `bookings` — đơn đặt phòng**

Bốn cột kết thúc bằng `_snapshot` lưu bản sao giá trị tại thời điểm đặt. Khi
quản trị viên đổi giá hoặc đổi tên loại phòng sau này, đơn cũ vẫn giữ nguyên số
tiền và tên mà khách đã nhìn thấy lúc đặt.

**Bảng `booking_rooms` — gán phòng vật lý cho đơn**

Ràng buộc quyết định của toàn hệ thống nằm trên bảng này:

```sql
EXCLUDE USING gist (room_id WITH =, stay WITH &&) WHERE (status = 'ACTIVE')
```

Bảng này còn mang hai trigger ràng buộc hoãn bảo đảm số dòng `ACTIVE` luôn khớp
`room_quantity` của đơn, đặt ở cả hai phía: khi sửa `booking_rooms` và khi sửa
`room_quantity` trên `bookings`.

**Bảng `room_closures` — khoảng ngày phòng không nhận khách**

Ràng buộc `room_closures_no_overlap` chống hai khoảng đóng chồng nhau trên cùng
một phòng.

### 3.2.3. Hai lần sử dụng ràng buộc loại trừ

Cùng một cơ chế được sử dụng cho hai bài toán khác nhau, cho thấy lựa chọn
PostgreSQL không nhằm phục vụ một trường hợp duy nhất.

| Lần | Bảng | Ràng buộc | Chống điều gì |
|---|---|---|---|
| 1 | `booking_rooms` | `booking_rooms_no_overlap` | Hai đơn cùng giữ một phòng trong những ngày giao nhau |
| 2 | `room_closures` | `room_closures_no_overlap` | Hai khoảng đóng chồng nhau trên cùng một phòng |

Lần thứ hai giải quyết một lỗi rất khó chẩn đoán từ phía người dùng. Nếu cho
phép hai khoảng đóng chồng nhau, màn hình quản trị hiển thị hai dòng mô tả cùng
một điều, và việc xoá một dòng **không mở lại được phòng**: người dùng nhấn xoá,
thấy dòng biến mất, rồi vẫn không bán được phòng mà không có cách nào hiểu vì
sao.

Khác biệt giữa hai ràng buộc: `booking_rooms_no_overlap` có mệnh đề điều kiện
`WHERE (status = 'ACTIVE')` vì dòng gán phòng có trạng thái và cần giữ lịch sử;
`room_closures_no_overlap` không có, vì khoảng đóng không có trạng thái và xoá
là xoá hẳn.

### 3.2.4. Chuẩn hoá dữ liệu

Lược đồ đạt **dạng chuẩn 3**. Hai điểm đáng nêu:

**Tách `room_types` và `rooms` là quyết định mô hình hoá quan trọng nhất.**
Khách đặt một **loại phòng**, nhưng ràng buộc chống trùng lịch phải đặt lên
**phòng vật lý**. Gộp hai khái niệm vào một bảng thì hoặc không chống trùng
được, hoặc buộc khách phải chọn đúng số phòng — điều không nền tảng đặt phòng
nào thực hiện.

**Các chỗ cố ý phi chuẩn hoá.** Cột `guest_name_snapshot` trong bảng `reviews`
và bốn cột kết thúc bằng `_snapshot` trong bảng `bookings` lưu bản sao giá trị
tại thời điểm phát sinh, thay vì tham chiếu. Lý do: những giá trị này phải **đóng
băng** tại thời điểm đó. Khách đổi tên tài khoản hoặc quản trị viên đổi giá
phòng sau này không được phép làm thay đổi nội dung đã phát sinh.

## 3.3. Thiết kế lớp và máy trạng thái

### 3.3.1. Biểu đồ lớp

Hệ thống có **20 thực thể** ánh xạ tới 20 trong số 21 bảng. Hình 3.4 thể hiện
biểu đồ lớp của các thực thể cốt lõi thuộc nhóm đặt phòng và thanh toán.

[Hình 3.4]

Hai lớp cơ sở trừu tượng được dùng lại cho nhiều thực thể:

| Lớp cơ sở | Cột cung cấp | Dùng cho nhóm bảng nào |
|---|---|---|
| `BaseAuditEntity` | `created_at` và `updated_at` | Bảng có vòng đời dài, có sửa đổi |
| `BaseCreatedEntity` | Chỉ `created_at` | Bảng chỉ ghi thêm, không bao giờ sửa |

Việc tách hai lớp cơ sở là bắt buộc chứ không phải lựa chọn phong cách. Chế độ
kiểm tra lược đồ báo lỗi ngay nếu một thực thể khai báo một cột mà migration
không có, nên không thể dùng chung một lớp cơ sở cho cả hai nhóm bảng.

### 3.3.2. Máy trạng thái đơn đặt phòng

Đơn đặt phòng có **tám trạng thái**. Mọi lần chuyển trạng thái đều đi qua một
thành phần duy nhất, vì mỗi lần chuyển kéo theo ba việc phụ dễ bỏ sót: ghi nhật
ký, nhả phòng khi đơn không diễn ra, và hoàn lượt khuyến mãi.

| Trạng thái | Ý nghĩa |
|---|---|
| `PENDING_PAYMENT` | Vừa tạo, đang giữ chỗ chờ tiền cọc |
| `AWAITING_REVIEW` | Có tiền nhưng không khớp, cần người đối soát |
| `CONFIRMED` | Đã nhận đủ cọc, phòng được giữ chắc chắn |
| `CHECKED_IN` | Khách đã nhận phòng |
| `CHECKED_OUT` | Khách đã trả phòng |
| `CANCELLED` | Khách hoặc quản trị viên huỷ |
| `EXPIRED` | Quá hạn giữ chỗ mà không có tiền |
| `NO_SHOW` | Đã xác nhận nhưng khách không đến |

Hình 3.5 thể hiện biểu đồ trạng thái đầy đủ, trong đó hai đường chuyển từ
`CANCELLED` và `EXPIRED` về `AWAITING_REVIEW` là nhánh xử lý tiền về muộn.

[Hình 3.5]

**Một quy tắc phản trực giác cần giải thích.** Trạng thái `CHECKED_OUT` **không**
nhả phòng. Trực giác cho rằng khách trả phòng thì phòng phải được trả về kho.
Nhưng nhả phòng nghĩa là chuyển dòng gán phòng sang `RELEASED`, trong khi trigger
kiểm tra số phòng lại đòi đơn phải luôn giữ đúng số phòng đã đặt — nên nhả phòng
lúc trả phòng làm cơ sở dữ liệu bác cả giao dịch. Quy tắc này phát sinh từ một
lỗi thật gặp phải trong quá trình thực hiện, và có một ca kiểm thử riêng canh nó.

### 3.3.3. Biểu đồ tuần tự

Ba luồng xử lý quan trọng nhất được mô hình hoá bằng biểu đồ tuần tự.

Hình 3.6 thể hiện luồng đặt phòng từ lúc khách chọn ngày tới lúc nhận mã QR, với
bốn đối tượng tham gia: khách, trình duyệt, máy chủ ứng dụng và cơ sở dữ liệu.

[Hình 3.6]

Hình 3.7 thể hiện luồng thanh toán và webhook, bổ sung hai đối tượng bên ngoài
là ngân hàng và nhà cung cấp dịch vụ trung gian.

[Hình 3.7]

Hình 3.8 thể hiện nhánh tiền về muộn — trường hợp tiền tới sau khi đơn đã hết
hạn — cùng hai kết quả có thể xảy ra.

[Hình 3.8]

## 3.4. Thiết kế kiến trúc phần mềm

### 3.4.1. Phân lớp tầng máy chủ

Hình 3.9 thể hiện sơ đồ phân lớp của tầng máy chủ.

[Hình 3.9]

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

Mã nguồn tổ chức theo **tính năng**, không theo loại kỹ thuật: mười sáu gói dưới
`com.tvh.homestay`, mỗi gói là một vùng nghiệp vụ — `auth`, `user`, `room`,
`availability`, `booking`, `payment`, `promotion`, `review`, `cms`, `content`,
`admin`, `report`, `storage`, `mail`, `common` và `demo`. Cây gói đầy đủ kèm chú
thích từng gói trình bày ở Phụ lục P.

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

Hình 3.10 thể hiện giao diện tài liệu tương tác của hệ thống đang chạy.

[Hình 3.10]

### 3.5.3. Giới hạn tần suất

Hệ thống giới hạn tần suất theo **chín khoá** khác nhau, chia ba nhóm: theo địa
chỉ mạng (`ip:auth`, `ip:availability`, `ip:promo`, `ip:booking-create`,
`ip:lookup`, `ip:cancel`), theo danh tính người dùng (`email:login`,
`phone:booking`) và theo mã đơn (`code:lookup`). Hạn mức đầy đủ của từng khoá
trình bày ở Phụ lục Q.

Điểm thiết kế: khi hai khoá cùng áp cho một yêu cầu, **khoá chặt hơn chặn
trước**. Đăng nhập sai liên tục cùng một thư điện tử bị chặn ở lần thứ sáu, còn
đổi thư điện tử mỗi lần thì bị chặn ở lần thứ mười một — hai lớp bổ sung cho
nhau, một lớp chặn dò mật khẩu một tài khoản, lớp kia chặn quét nhiều tài khoản.

### 3.5.4. Thiết kế mã lỗi

Phản hồi lỗi theo chuẩn RFC 7807 [4] kèm trường `code` tự thêm. Tầng giao diện
đọc trường `code`, không đọc câu chữ mô tả, nên việc sửa câu chữ ở tầng máy chủ
không làm hỏng màn hình. Hệ thống định nghĩa 15 mã lỗi nghiệp vụ, trong đó các
mã quan trọng nhất là `ROOM_NOT_AVAILABLE` (409) khi phòng vừa được đặt hết,
`CLOSURE_OVERLAP` (409) khi khoảng đóng phòng chồng lấn, và
`INVALID_STATE_TRANSITION` (409) khi bước chuyển trạng thái không hợp lệ. Bảng
đầy đủ 15 mã lỗi trình bày ở Phụ lục J.

## 3.6. Thiết kế giao diện người dùng

Bảng màu là **định nghĩa thương hiệu** của hệ thống. Mọi mã màu tập trung ở một
tệp token duy nhất, và một script chạy trong quy trình kiểm tra chặn mọi mã màu
viết trực tiếp ở nơi khác. Hai lớp bảo vệ bổ sung cho nhau: xoá bảng màu mặc
định của thư viện định kiểu chặn được các lớp tiện ích màu sẵn có, còn script
kiểm tra bắt trường hợp viết mã màu thẳng vào thuộc tính kiểu nội tuyến.

Sáu quy ước tương tác bắt buộc, bám theo hướng dẫn khả năng tiếp cận mức A và
AA [13]:

| Quy ước | Lý do |
|---|---|
| Vùng chạm tối thiểu 44×44 điểm ảnh trên di động | Ngón tay không chính xác như con trỏ chuột |
| Chữ không nhỏ hơn 16 điểm ảnh trên di động | Dưới mức này một số trình duyệt tự phóng to khi chạm ô nhập |
| Lỗi không bao giờ chỉ báo bằng màu | Người khó phân biệt màu không nhận ra; luôn kèm biểu tượng và chữ |
| Viền chỉ báo tiêu điểm không bao giờ bị xoá | Người dùng bàn phím cần biết đang ở vị trí nào |
| Tôn trọng tuỳ chọn giảm chuyển động của hệ điều hành | Phục vụ người nhạy cảm với chuyển động |
| **Chặn sẵn thay vì báo lỗi sau** | Ngày hết phòng bị chặn ngay trong lịch |

Về điều hướng, hệ thống chia ba khu: khu công khai không cần đăng nhập; khu tài
khoản khách được bộ bảo vệ tuyến canh; và khu quản trị gồm mười một màn hình,
được bộ bảo vệ tuyến canh ở tầng giao diện và kiểm tra vai trò ở tầng máy chủ.
Khu quản trị nạp lười theo tuyến để người truy cập trang đặt phòng không phải
tải mã của những màn hình không sử dụng.
