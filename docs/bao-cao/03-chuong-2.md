# CHƯƠNG 2 — CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ SỬ DỤNG

## 2.1. Kiến trúc ứng dụng web tách rời

### 2.1.1. Mô hình khách — chủ và giao diện lập trình ứng dụng REST

Hệ thống theo kiến trúc **tách rời**: tầng giao diện và tầng máy chủ là hai ứng
dụng độc lập, giao tiếp qua giao diện lập trình ứng dụng trả về dữ liệu định
dạng JSON. Tầng giao diện không truy cập cơ sở dữ liệu; tầng máy chủ không sinh
HTML.

So sánh với kiến trúc nguyên khối kết xuất phía máy chủ:

| Tiêu chí | Kết xuất phía máy chủ | **Tách rời — lựa chọn của đề tài** |
|---|---|---|
| Độ phức tạp ban đầu | Thấp hơn | Cao hơn |
| Trải nghiệm người dùng | Tải lại toàn bộ trang | Chuyển trang không tải lại |
| Tái sử dụng cho ứng dụng di động | Phải viết lại | **Dùng lại nguyên giao diện lập trình ứng dụng** |
| Phân chia công việc | Khó tách | Tách rõ hai tầng |
| Tối ưu công cụ tìm kiếm | Dễ hơn | Cần thêm công sức |

Lý do lựa chọn kiến trúc tách rời: hệ thống có **hai giao diện rất khác nhau** —
trang bán hàng cho khách và khu quản trị mật độ cao. Dùng chung một giao diện
lập trình ứng dụng cho cả hai, cộng với khả năng mở rộng sang ứng dụng di động
sau này, bù lại chi phí phức tạp ban đầu.

Hình 2.1 thể hiện kiến trúc tổng thể của hệ thống khi triển khai.

[Hình 2.1]

### 2.1.2. Nguyên tắc REST áp dụng trong đề tài

- **Tài nguyên là danh từ, thao tác là động từ HTTP.** Ví dụ `GET /api/room-types`,
  `POST /api/bookings`, `PATCH /api/admin/rooms/{id}`.
- **Tài nguyên con lồng vào tài nguyên cha.** Ví dụ
  `GET /api/admin/rooms/{id}/closures` — khoảng đóng phòng không tồn tại độc lập
  với phòng.
- **Mã trạng thái HTTP mang ngữ nghĩa.** Mã 201 khi tạo mới, 204 khi xoá thành
  công, 409 khi xung đột trạng thái, 429 khi vượt giới hạn tần suất.
- **Lỗi theo chuẩn RFC 7807 [9].** Mọi phản hồi lỗi có cấu trúc thống nhất kèm
  trường `code` do hệ thống tự thêm. Tầng giao diện đọc trường `code`, không đọc
  câu chữ mô tả, nên việc sửa câu chữ ở tầng máy chủ không làm hỏng màn hình.

## 2.2. Ngôn ngữ và nền tảng tầng máy chủ

### 2.2.1. Java 21

Java 21 là phiên bản hỗ trợ dài hạn. Các tính năng ngôn ngữ được đề tài sử dụng:

- **Kiểu `record`** — dùng cho toàn bộ đối tượng truyền dữ liệu. Một `record` là
  bất biến, tự sinh các phương thức so sánh và hiển thị, và không thể vô tình bị
  sửa giữa các tầng.
- **Lớp niêm phong `sealed`** — dùng cho cây ngoại lệ nghiệp vụ. Trình biên dịch
  biết đủ danh sách lớp con, nên việc thêm một loại lỗi mới mà quên xử lý sẽ bị
  báo ngay lúc biên dịch.
- **Khối văn bản** — dùng để viết các câu truy vấn SQL nhiều dòng còn đọc được.

### 2.2.2. Spring Boot 3.5.6

Spring Boot là nền tảng xây dựng ứng dụng Java theo mô hình cấu hình sẵn, giảm
lượng cấu hình thủ công mà nhà phát triển phải viết [5]. Các mô-đun được đề tài
sử dụng:

| Mô-đun | Vai trò trong đề tài |
|---|---|
| Spring Web MVC | Tầng điều khiển, ánh xạ REST |
| Spring Data JPA | Truy cập dữ liệu qua Hibernate cho phần thao tác cơ bản |
| Spring JDBC | **Các truy vấn phòng trống viết SQL thuần** (lý do ở mục 2.4.3) |
| Spring Security | Xác thực bằng token, phân quyền theo vai trò |
| Spring Validation | Kiểm tra dữ liệu đầu vào bằng chú thích |
| Spring Mail | Gửi thư xác nhận |
| Spring Scheduling | Bộ quét đơn quá hạn, bộ gửi thư |

### 2.2.3. Giao dịch và hành vi huỷ giao dịch

Một giao dịch cơ sở dữ liệu có bốn tính chất ACID: tính nguyên tử, tính nhất
quán, tính cô lập và tính bền vững.

Điểm mà đề tài buộc phải hiểu sâu là **hành vi huỷ giao dịch**. Khi PostgreSQL
bác một câu lệnh vì vi phạm ràng buộc, **toàn bộ giao dịch bị đánh dấu hỏng**.
Mọi câu lệnh tiếp theo trong cùng giao dịch đó trả về mã lỗi `25P02` chứ không
thực thi. Spring Framework cũng đánh dấu giao dịch chỉ được huỷ khi gặp ngoại lệ
thời gian chạy [4].

Hệ quả trực tiếp lên thiết kế: vòng thử gán phòng tiếp theo **bắt buộc** phải
chạy trong một giao dịch mới. Chi tiết cài đặt ở mục 4.3.2.

### 2.2.4. Hibernate và chế độ kiểm tra lược đồ

Đề tài đặt `spring.jpa.hibernate.ddl-auto = validate` và không bao giờ dùng
`update`.

Chế độ `update` để Hibernate tự sửa lược đồ. Nó âm thầm thêm cột, không bao giờ
xoá cột thừa, và không ghi lại những thay đổi đã thực hiện. Sau một thời gian, lược đồ
máy phát triển và trên máy chạy thật khác nhau mà không có cách nào biết khác ở
chỗ nào.

Chế độ `validate` chỉ **kiểm tra**: mọi cột mà thực thể khai báo phải tồn tại
đúng kiểu trong cơ sở dữ liệu. Ứng dụng khởi động được nghĩa là **20 thực thể**
khớp hoàn toàn với lược đồ do Flyway dựng ra.

## 2.3. Ngôn ngữ và nền tảng tầng giao diện

### 2.3.1. Angular 21

Đề tài sử dụng Angular 21 với ba đặc điểm hiện đại:

- **Component độc lập.** Không còn khái niệm mô-đun; mỗi component tự khai báo
  những thành phần nó sử dụng [6].
- **Signals.** Cơ chế phản ứng của Angular. Một signal là một giá trị có thể
  theo dõi; giá trị dẫn xuất tự cập nhật khi nguồn thay đổi. Đề tài **không sử
  dụng thư viện quản lý trạng thái bên ngoài**: signals đủ cho quy mô này, và
  thêm một thư viện là thêm một thành phần phải giải thích.
- **Nạp lười theo tuyến.** Khu quản trị gồm mười một màn hình được tải riêng.
  Người truy cập trang đặt phòng không tải mã của những màn hình không sử dụng.

### 2.3.2. Tailwind CSS 4 và hệ thống thiết kế tự xây

Đề tài **không sử dụng** thư viện giao diện dựng sẵn, mà tự xây thư viện
component trên nền Tailwind CSS 4.

Lý do: thư viện dựng sẵn mang theo ngôn ngữ thị giác của nó. Sử dụng một thư
viện phổ biến làm trang web mang dáng dấp của ứng dụng gốc chứ không giống một
trang đặt phòng. Tự xây cho phép kiểm soát hoàn toàn bảng màu, khoảng cách và
trạng thái, và biến việc thiết kế giao diện thành một phần có thể trình bày được
của đồ án thay vì một tuỳ chọn cấu hình.

**Khái niệm tầng CSS.** Tailwind 4 đặt các lớp tiện ích trong tầng `utilities`.
Một quy tắc CSS **không nằm trong tầng nào** luôn thắng mọi quy tắc nằm trong
tầng, bất kể độ ưu tiên. Đây là nguồn gốc của một lỗi thật gặp phải trong quá
trình thực hiện, trình bày ở mục 5.3.

Hình 2.2 thể hiện bảng token màu của hệ thống.

[Hình 2.2]

## 2.4. Hệ quản trị cơ sở dữ liệu PostgreSQL 16

Đây là mục quan trọng nhất của chương. Toàn bộ lời giải cho bài toán cốt lõi nằm
ở đây.

### 2.4.1. Mức cô lập giao dịch và giới hạn của nó

PostgreSQL mặc định chạy ở mức cô lập **Read Committed** [2]. Ở mức này, một câu
`SELECT` chỉ thấy ảnh chụp dữ liệu tại **thời điểm câu lệnh đó bắt đầu chạy** —
nó chỉ thấy dữ liệu đã được commit trước đó, không thấy thay đổi chưa commit của
giao dịch song song.

Điều quan trọng là: **Read Committed không ngăn được hai giao dịch song song
cùng đọc rồi cùng ghi.** Tài liệu PostgreSQL nêu rõ ví dụ một giao dịch cập nhật
giá trị trong khi giao dịch khác đang tìm theo giá trị cũ, dẫn tới kết quả không
như mong đợi [2].

Nâng lên mức **Repeatable Read** thì mọi câu lệnh trong cùng giao dịch dùng chung
một ảnh chụp tại thời điểm bắt đầu giao dịch, ngăn được hiện tượng đọc ảo. Mức
**Serializable** ngăn thêm được bất thường tuần tự hoá, nhưng đánh đổi bằng việc
giao dịch có thể bị bác với mã lỗi `40001` và ứng dụng phải thử lại toàn bộ từ
đầu [2].

Điểm mấu chốt cho đề tài: **ngay cả ở mức Serializable, tài liệu PostgreSQL vẫn
khuyến cáo không dựa vào việc kiểm tra ở tầng ứng dụng trước khi ghi.** Tài liệu
nêu rõ vẫn có thể gặp vi phạm ràng buộc duy nhất do xung đột giữa các giao dịch
chồng lấn, kể cả sau khi đã kiểm tra tường minh rằng khoá chưa tồn tại [2].
Khuyến nghị là **sử dụng ràng buộc toàn vẹn** thay vì kiểm tra ở tầng ứng dụng.

Đây chính là căn cứ lý thuyết cho toàn bộ thiết kế của đồ án.

### 2.4.2. Kiểu dữ liệu khoảng

PostgreSQL có kiểu `daterange` biểu diễn một khoảng ngày như một giá trị duy
nhất, kèm toán tử `&&` kiểm tra hai khoảng có giao nhau hay không [1]:

```sql
SELECT daterange('2026-03-08','2026-03-10','[)')
    && daterange('2026-03-10','2026-03-12','[)');   -- false, không giao nhau
```

Ký hiệu `'[)'` là **khoảng nửa mở**: bao gồm cận dưới, không bao gồm cận trên.
Ngữ nghĩa này trùng khớp với nghiệp vụ lưu trú, và đây là lý do quy ước nửa mở
được lựa chọn.

### 2.4.3. Ràng buộc loại trừ

```sql
ALTER TABLE booking_rooms
    ADD CONSTRAINT booking_rooms_no_overlap
    EXCLUDE USING gist (room_id WITH =, stay WITH &&)
    WHERE (status = 'ACTIVE');
```

Ràng buộc trên đọc là: không được phép tồn tại hai dòng có cùng `room_id` **và**
có `stay` giao nhau, trong số các dòng đang ở trạng thái `ACTIVE`.

Ba lý do ràng buộc này là lời giải đúng:

**Thứ nhất, nó đóng khe hở tranh chấp.** Cách làm quen thuộc là kiểm tra rồi
ghi:

```
SELECT ... WHERE NOT EXISTS (đơn nào giao ngày không?)   -- kiểm tra
INSERT INTO booking_rooms ...                            -- rồi ghi
```

Giữa hai câu lệnh đó có một khoảng thời gian. Hai giao dịch song song cùng đọc
kết quả còn phòng rồi cùng ghi, và phòng bị bán hai lần. Mẫu này sai về nguyên
lý như đã phân tích ở mục 2.4.1, không phải sai vì viết ẩu.

Phương án khoá bi quan bịt được khe hở nhưng biến mọi lượt đặt cùng một loại
phòng thành hàng đợi một luồng, và không còn đúng khi ứng dụng chạy nhiều bản
song song.

Với ràng buộc loại trừ, PostgreSQL bác một trong hai giao dịch ngay trong động
cơ lưu trữ, bằng mã lỗi `SQLSTATE 23P01`. Không còn khe hở nào để chen vào.

Hình 2.3 so sánh trực quan hai cách xử lý tranh chấp.

[Hình 2.3]

**Thứ hai, nó đúng ngữ nghĩa nửa mở** mà không cần một dòng mã ứng dụng nào.

**Thứ ba, nó cho phép huỷ đơn nhả phòng ngay mà vẫn giữ lịch sử.** Mệnh đề
`WHERE (status = 'ACTIVE')` làm ràng buộc chỉ xét những dòng đang giữ chỗ. Huỷ
đơn chỉ đổi trạng thái sang `RELEASED`: phòng mở lại lập tức, dòng dữ liệu vẫn
còn để tra cứu.

### 2.4.4. Chỉ mục GiST và phần mở rộng btree_gist

Ràng buộc loại trừ cần một chỉ mục hỗ trợ toán tử `&&`. Chỉ mục B-tree thông
thường được thiết kế cho dữ liệu vô hướng và **chỉ hỗ trợ so sánh tuyến tính**
— bằng, lớn hơn, nhỏ hơn. Nó không trả lời được câu hỏi hai khoảng có giao nhau
hay không.

**GiST** là viết tắt của Generalized Search Tree — cây tìm kiếm tổng quát. Tài
liệu PostgreSQL định nghĩa nó là một phương thức truy cập có cấu trúc cây cân
bằng, đóng vai trò khuôn mẫu cơ sở để cài đặt các lược đồ đánh chỉ mục tuỳ ý [3].

Điểm khác biệt cốt lõi so với B-tree: B-tree có cấu trúc cố định dựa trên thứ tự
tuyến tính, còn GiST cho phép định nghĩa **lớp toán tử tuỳ chỉnh** cho từng kiểu
dữ liệu. Nhờ đó GiST xử lý được các phép toán như giao nhau, bao hàm, khoảng
cách — những phép toán không quy về được thứ tự tuyến tính [3].

Ràng buộc của đề tài trộn hai toán tử: `room_id WITH =` thuộc về B-tree và
`stay WITH &&` thuộc về GiST. Phần mở rộng **`btree_gist`** cho phép đưa cả hai
vào một chỉ mục GiST duy nhất. Đây là lý do migration đầu tiên của hệ thống bắt
đầu bằng câu lệnh tạo phần mở rộng này.

### 2.4.5. Cột sinh tự động

```sql
stay daterange GENERATED ALWAYS AS (daterange(check_in, check_out, '[)')) STORED
```

Cột `stay` không bao giờ được ghi trực tiếp; PostgreSQL tự tính từ hai cột ngày.
Nếu để tầng ứng dụng tự tính rồi ghi vào, sẽ có lúc nó tính sai — và lúc đó ràng
buộc đang canh một giá trị sai, tức là không canh gì cả.

### 2.4.6. Trigger ràng buộc hoãn

Hệ thống có một bất biến thứ hai: **số phòng đã gán phải khớp số phòng đơn yêu
cầu**. Bất biến này không kiểm được bằng ràng buộc `CHECK` vì nó liên quan tới
hai bảng.

Nếu kiểm tra ngay sau mỗi câu lệnh chèn, một đơn đặt hai phòng sẽ bị bác ngay
sau dòng đầu tiên, vì lúc đó mới có một phòng trong khi đơn yêu cầu hai. Giải
pháp là `CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED`: PostgreSQL hoãn
việc kiểm tra tới **thời điểm commit**, khi mọi dòng đã ghi xong.

## 2.5. Xác thực và phân quyền bằng JSON Web Token

JSON Web Token là một chuỗi ký tự gồm ba phần — phần đầu, phần tải và chữ ký —
nối bằng dấu chấm, đã được ký bằng khoá bí mật của máy chủ. Máy chủ không cần
lưu phiên làm việc; nó chỉ cần xác minh chữ ký.

Đề tài sử dụng **hai loại token với hai vòng đời khác nhau**:

| | Token truy cập | Token làm mới |
|---|---|---|
| Vòng đời | Ngắn, tính bằng phút | Dài, tính bằng ngày |
| Nơi lưu | Bộ nhớ của trang | Cookie chỉ đọc bởi máy chủ |
| Mục đích | Gửi kèm mỗi yêu cầu | Xin token truy cập mới |
| Mã kịch bản đọc được | Có | **Không** |

Lý do tách đôi: token truy cập nằm trong bộ nhớ nên mã độc chèn vào trang có thể
lấy được, nhưng nó hết hạn sau vài phút. Token làm mới sống lâu nhưng nằm trong
cookie mà mã kịch bản không đọc được.

**Thu hồi tức thì.** Nhược điểm cố hữu của JWT là không thu hồi được trước hạn.
Đề tài khắc phục bằng một số phiên bản lưu trong bảng người dùng và nhúng vào
token. Khi đổi mật khẩu hoặc đăng xuất, số đó tăng lên và mọi token cũ mất hiệu
lực ngay.

## 2.6. Quản lý phiên bản lược đồ bằng Flyway

Flyway thực thi các tệp SQL đánh số theo thứ tự, và ghi lại tệp nào đã chạy kèm
**giá trị băm** của nó.

**Nguyên tắc bất biến:** migration đã phát hành không bao giờ được sửa. Sửa làm
giá trị băm thay đổi và Flyway chặn khởi động. Muốn đổi lược đồ thì **thêm** một
migration mới. Hệ thống hiện có **tám migration** từ `V1` tới `V8`.

**Dữ liệu mẫu cố ý không đi qua Flyway.** Nếu dữ liệu minh hoạ là một migration,
thì việc chuyển sang môi trường thật trên cùng khối dữ liệu sẽ mang theo cả 40
đơn giả. Dữ liệu mẫu được nạp bởi một thành phần riêng chỉ kích hoạt ở cấu hình
trình diễn.

## 2.7. Đóng gói bằng Docker và Docker Compose

- **Ảnh Docker** là bản đóng gói gồm ứng dụng và mọi thành phần nó cần để chạy.
- **Tệp Dockerfile nhiều tầng.** Tầng đầu chứa công cụ biên dịch, tầng sau chỉ
  chứa kết quả biên dịch. Ảnh cuối không mang theo trình biên dịch nên nhỏ hơn
  nhiều và ít bề mặt tấn công hơn.
- **Docker Compose** mô tả nhiều dịch vụ trong một tệp cấu hình.

Hệ thống gồm bốn dịch vụ: máy chủ web, ứng dụng, cơ sở dữ liệu và máy chủ thư
giả lập. **Chỉ máy chủ web và giao diện xem thư mở cổng ra máy chủ**; ứng dụng
và cơ sở dữ liệu không mở cổng nào. Đây là điều kiện để tin được thông tin địa
chỉ mà máy chủ web chuyển tiếp, giải thích ở mục 4.4.3.

## 2.8. Thanh toán qua mã QR và webhook

**VietQR** là chuẩn mã QR thanh toán của hệ thống ngân hàng Việt Nam. Quét mã
điền sẵn số tài khoản, số tiền và nội dung chuyển khoản.

**SePay** là dịch vụ trung gian đọc biến động số dư của tài khoản ngân hàng và
gọi webhook tới hệ thống khi có tiền về [8].

**Webhook** là cơ chế ngược với việc hỏi liên tục: thay vì hệ thống hỏi có tiền
chưa mỗi vài giây, nhà cung cấp chủ động gọi vào một endpoint do hệ thống cung
cấp.

Ba vấn đề kỹ thuật phải giải quyết:

| Vấn đề | Cách giải quyết |
|---|---|
| Bất kỳ ai cũng gọi được endpoint webhook | Xác thực bằng khoá trong phần đầu yêu cầu |
| Nhà cung cấp có thể gửi lại cùng một sự kiện | Ràng buộc duy nhất trên cặp nhà cung cấp và mã sự kiện — gửi lại không cộng tiền hai lần |
| Tiền về sau khi đơn đã hết hạn | Mở lại đơn sang trạng thái chờ đối soát, thử giành lại phòng; không được thì vào hàng đợi hoàn tiền |

## 2.9. Kiểm thử với Testcontainers

Testcontainers là thư viện khởi động một container Docker thật trong lúc chạy
kiểm thử.

Lý do đề tài bắt buộc sử dụng nó: phần lớn nội dung đáng kiểm thử ở hệ thống này
**chính là hành vi của cơ sở dữ liệu** — ràng buộc loại trừ, kiểu khoảng, trigger
hoãn, mã trạng thái SQL. Cơ sở dữ liệu trong bộ nhớ không có những tính năng đó.
Kiểm thử chạy trên nền giả lập sẽ thành công mà không chứng minh được điều duy
nhất đáng chứng minh.

## 2.10. Bảng tổng hợp công nghệ

| Tầng | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ tầng máy chủ | Java | 21 (LTS) |
| Nền tảng tầng máy chủ | Spring Boot | 3.5.6 |
| Truy cập dữ liệu | Spring Data JPA / Hibernate, Spring JDBC | theo nền tảng |
| Bảo mật | Spring Security và JWT | theo nền tảng |
| Cơ sở dữ liệu | PostgreSQL | 16 |
| Quản lý lược đồ | Flyway | theo nền tảng |
| Giới hạn tần suất | bucket4j | — |
| Lọc HTML | OWASP java-html-sanitizer | 20240325.1 |
| Kiểm thử | JUnit 5, AssertJ, Testcontainers | — |
| Ngôn ngữ tầng giao diện | TypeScript | theo Angular |
| Nền tảng tầng giao diện | Angular | 21 |
| Định kiểu | Tailwind CSS | 4 |
| Máy chủ web | nginx | alpine |
| Đóng gói | Docker, Docker Compose | Engine 24+, Compose v2 |
| Thư trong môi trường trình diễn | Mailpit | — |
| Thanh toán | SePay và VietQR | — |
