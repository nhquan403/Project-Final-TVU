# CHƯƠNG 5 — KIỂM THỬ VÀ ĐÁNH GIÁ

## 5.1. Chiến lược kiểm thử

Hệ thống có **129 ca kiểm thử tự động trong 18 lớp**, tất cả đều thành công.

### 5.1.1. Lý do không sử dụng cơ sở dữ liệu giả lập

Mọi ca kiểm thử tích hợp dựng một **PostgreSQL 16 thật** bằng Testcontainers.
Đề tài không giả lập cơ sở dữ liệu, vì phần lớn nội dung đáng kiểm thử ở hệ
thống này **chính là hành vi của cơ sở dữ liệu**: ràng buộc loại trừ, kiểu
khoảng, trigger hoãn, mã trạng thái SQL. Giả lập chúng là kiểm thử một thứ không
tồn tại.

Container được dùng chung cho mọi lớp kiểm thử kế thừa cùng lớp nền, khởi động
một lần cho cả lần chạy. Dựng lại cơ sở dữ liệu cho từng lớp kiểm thử tốn nhiều
thời gian hơn phần việc thật.

Hai biến bí mật bắt buộc được **sinh ngẫu nhiên mỗi lần chạy** chứ không ghi sẵn
vào tệp cấu hình kiểm thử. Cách này vừa tránh đưa giá trị vào kho mã nguồn công
khai, vừa chứng minh luôn rằng ứng dụng thật sự đòi hai biến đó.

### 5.1.2. Các cấp độ kiểm thử

| Cấp độ | Ví dụ lớp | Kiểm điều gì |
|---|---|---|
| Kiểm thử đơn vị | Lọc HTML, xuất tệp CSV | Logic thuần, không cần cơ sở dữ liệu |
| Kiểm thử tích hợp | Phần lớn các lớp | Nhiều tầng cùng cơ sở dữ liệu thật |
| Kiểm thử tranh chấp | Đặt phòng đa luồng, đua thanh toán | Nhiều luồng chạy song song thật |
| Kiểm thử lược đồ | Migration, ràng buộc | Chính cấu trúc cơ sở dữ liệu |

Dự án không cấu hình pha kiểm thử tích hợp riêng; công cụ chạy kiểm thử được nới
để nhận cả lớp có hậu tố chỉ kiểm thử tích hợp, nên lệnh kiểm chứng gồm biên
dịch, chạy toàn bộ 129 ca và đóng gói.

## 5.2. Kết quả kiểm thử tự động

Hình 5.1 thể hiện kết quả chạy toàn bộ bộ kiểm thử.

[Hình 5.1]

### 5.2.1. Bảng tổng hợp theo lớp

| Lớp kiểm thử | Số ca | Chứng minh điều gì |
|---|---:|---|
| `SchemaMigrationTest` | 8 | Migration dựng đúng **21 bảng**; mọi migration thành công; không migration nào bị sửa sau khi phát hành; cả hai cột kiểu khoảng đều là cột sinh tự động |
| `SchemaConstraintIT` | 9 | Từng ràng buộc kiểm tra thật sự chặn: thư điện tử không chữ thường, trạng thái sai chính tả, số tiền âm, ngày trả không sau ngày nhận, điểm đánh giá ngoài khoảng 1 tới 5 |
| `SqlStatesIT` | 2 | Hai mã lỗi `23P01` và `25P02` được nhận diện đúng |
| **`BookingConcurrencyIT`** | 3 | **Lớp quan trọng nhất.** Nhiều luồng thật cùng đặt phòng cuối — đúng một luồng thắng; sau khi bắt lỗi ràng buộc, vòng thử tiếp theo phải ở giao dịch mới; đơn nhiều phòng gán được nhờ trigger hoãn |
| `AvailabilityQueryIT` | 6 | Đếm phòng trống đúng trong các tình huống dễ đếm sai: phòng bảo trì đang có đơn không bị trừ hai lần; loại phòng hết sạch biến mất khỏi kết quả; khoảng nửa mở cho khách trả và khách nhận cùng ngày; sức chứa so theo từng phòng; lịch trả đủ mọi ngày |
| `BookingLifecycleIT` | 8 | Bảng chuyển trạng thái: bước hợp lệ đi được, bước không hợp lệ bị từ chối. `CHECKED_OUT` không nhả phòng |
| `BookingExpiryIT` | 4 | Bộ quét chuyển đơn quá hạn, nhả phòng, hoàn lượt khuyến mãi; đơn đã có tiền không bị quét |
| **`RoomClosureIT`** | 9 | Khoảng đóng phòng trừ đúng phòng, đúng đêm, ở cả bốn truy vấn; biên nửa mở; lịch không rơi mất ngày; khoảng chồng nhau bị từ chối; đóng phòng không huỷ đơn; khoảng đã qua rời khỏi danh sách thao tác |
| `SepayWebhookIT` | 10 | Sai khoá xác thực bị từ chối; webhook gửi lại không cộng tiền hai lần; đủ tiền thì xác nhận; thiếu tiền thì chờ đối soát; thừa tiền thì đánh dấu hoàn; webhook không khớp đơn nào vẫn được ghi nhật ký |
| **`PaymentRaceIT`** | 5 | Cửa sổ tranh chấp giữa webhook và bộ quét hết hạn; nhánh giành lại phòng tôn trọng khoảng đóng phòng |
| `AuthFlowIT` | 7 | Đăng ký, đăng nhập, làm mới token, đăng xuất; số phiên bản token tăng làm token cũ mất hiệu lực ngay; token làm mới lưu dạng băm |
| **`EndpointAuthorizationIT`** | 3 | Mọi endpoint đã đăng ký nằm trong ma trận 30 tiền tố; cấm khai dòng bao trùm; endpoint không khai gì bị chặn |
| `ContentSanitizerTest` | 17 | Bộ lọc HTML gỡ thẻ kịch bản, chặn giao thức nguy hiểm trong liên kết, gỡ khung nhúng, gỡ ảnh ngoài danh sách nguồn cho phép, giữ thẻ hợp lệ |
| `PublicContentIT` | 7 | Giao diện công khai chỉ trả nội dung đã đăng; nội dung mặc định hiện ra khi chưa soạn; bài viết nháp không lộ |
| `ReviewIT` | 8 | Gửi đánh giá cần xác thực; tên khách chụp từ đơn, không nhận từ dữ liệu gửi lên; ràng buộc duy nhất chặn đánh giá thứ hai; chỉ đánh giá đã duyệt mới công khai |
| `DashboardServiceIT` | 6 | Doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ tính đúng trên dữ liệu thật |
| `CsvExportTest` | 10 | Xuất tệp CSV thoát đúng dấu phẩy, dấu nháy và ký tự xuống dòng trong dữ liệu |
| `ImageUploadIT` | 7 | Từ chối kiểu tệp sai; từ chối tệp vượt dung lượng; giải mã lại ảnh và đổi tên; chỉ quản trị viên tải lên được |
| **Tổng** | **129** | |

### 5.2.2. Ba ca kiểm thử trọng tâm

**Thứ nhất, kiểm thử tranh chấp đặt phòng.** Nhiều luồng thật chạy song song
cùng đặt phòng cuối cùng. Kết quả bắt buộc: **đúng một luồng thắng**, không có
hai dòng gán phòng chồng lấn trong cơ sở dữ liệu. Đây là bằng chứng trực tiếp
cho mục đích số 2 của đề tài.

**Thứ hai, kiểm thử phòng đang đóng không bao giờ được gán.** Ba trong bốn truy
vấn phòng trống chỉ ảnh hưởng thứ khách nhìn thấy; bỏ sót truy vấn chọn phòng
vật lý thì **đơn vẫn tạo được và phòng đang sửa chữa vẫn bị gán** — lỗi im lặng,
chỉ lộ ra khi khách tới nhận phòng. Ca kiểm thử này canh đúng chỗ đó.

**Thứ ba, kiểm thử tiền về muộn.** Tiền về **sau khi** đơn đã hết hạn: đơn mở
lại sang trạng thái chờ đối soát, hệ thống thử giành lại phòng; không giành được
thì vào hàng đợi hoàn tiền. Đây là bằng chứng cho tiêu chí không nhánh nào để
tiền biến mất im lặng.

## 5.3. Các lỗi thực tế phát hiện trong quá trình thực hiện

| # | Hiện tượng | Nguyên nhân gốc | Cách khắc phục |
|---|---|---|---|
| 1 | Giao dịch thử phòng tiếp theo luôn thất bại | Bắt lỗi ràng buộc rồi thử tiếp **trong cùng giao dịch**; PostgreSQL đã huỷ giao dịch nên mọi lệnh sau trả `25P02` | Mỗi lượt thử chạy trong một giao dịch mới |
| 2 | Website báo thiếu phòng so với thực tế | Truy vấn lấy tổng trừ đi số đã đặt trừ **hai lần** phòng bảo trì đang có đơn | Đếm trực tiếp phòng vừa khả dụng vừa rảnh |
| 3 | Giao dịch bị bác khi khách trả phòng | Nhả phòng lúc chuyển sang `CHECKED_OUT` làm trigger kiểm số phòng bác cả giao dịch | Trạng thái `CHECKED_OUT` không nhả phòng |
| 4 | **Toàn bộ trang web hiển thị không có định kiểu** | Chính sách bảo mật nội dung chặn thuộc tính sự kiện mà công cụ tối ưu định kiểu tự sinh ra | Tắt tính năng tối ưu đó thay vì nới lỏng chính sách bảo mật |
| 5 | **Mọi nút bấm chính có độ tương phản khoảng 2:1** | Tệp định kiểu nền **không nằm trong tầng CSS** nên thắng mọi lớp tiện ích bất kể độ ưu tiên | Bọc tệp nền trong tầng cơ sở |
| 6 | Ảnh tải lên trả về lỗi 404 | Khối biểu thức chính quy của máy chủ web thắng khối tiền tố thường | Dùng cú pháp tiền tố ưu tiên cho cả bốn khối chuyển tiếp |
| 7 | Màn hình thanh toán trắng trơn | Component gọi hàm đọc tham số đầu vào **trong hàm khởi tạo**, trước khi khung ứng dụng gán tham số | Chuyển sang phương thức chạy sau khi khởi tạo |
| 8 | Bộ nạp dữ liệu mẫu cộng dồn mỗi lần khởi động | Điều kiện chống trùng khoá theo ngày tương đối nên mỗi ngày lại khớp khác | Đổi điều kiện sang khoá toàn bảng |
| 9 | Khu quản trị đóng nhầm phòng không hiển thị | Khối giao diện giữ định danh phòng cũ sau khi bộ lọc của màn hình cha thay đổi | Đối chiếu định danh với danh sách phòng đang hiển thị |

**Về lỗi số 4 và số 5:** cả hai chỉ phát hiện được khi **chụp màn hình bằng
trình duyệt thật**. Việc biên dịch thành công không nói gì về việc trang có hiển
thị đúng hay không. Hình 5.2 so sánh trước và sau khi khắc phục lỗi tương phản.

[Hình 5.2]

**Về lỗi số 9:** lỗi này do một vòng rà soát mã độc lập tìm ra **sau khi** tác
giả đã tự kiểm thử và cho rằng chức năng đã hoàn chỉnh. Đây là lý do đề tài giữ
bước rà soát độc lập ở cuối mỗi giai đoạn.

## 5.4. Kiểm thử phi chức năng

### 5.4.1. Khả năng tiếp cận

Quét tự động trên 7 màn hình công khai và 6 màn hình quản trị: **không còn lỗi
vi phạm** hướng dẫn khả năng tiếp cận nội dung web phiên bản 2.0 và 2.1 ở mức A
và AA [10].

Đo vùng chạm ở 3 độ rộng màn hình: **không còn vùng chạm dưới 44×44 điểm ảnh**.

Hình 5.3 thể hiện kết quả quét.

[Hình 5.3]

### 5.4.2. Kiểm thử bảo mật

| Kịch bản tấn công | Kết quả |
|---|---|
| Giả mạo tiêu đề địa chỉ để vượt giới hạn tần suất | **Thất bại** — vẫn bị chặn ở đúng ngưỡng |
| Gọi endpoint quản trị không có token | Từ chối, mã 401 |
| Gọi endpoint quản trị bằng token vai trò khách | Từ chối, mã 403 |
| Dùng token cũ sau khi đã đổi mật khẩu | Từ chối, mã 401 — token cũ mất hiệu lực ngay |
| Gọi webhook không có khoá xác thực | Từ chối |
| Gửi lại cùng một webhook lần hai | Bỏ qua, không cộng tiền hai lần |
| Chèn thẻ kịch bản vào nội dung quản lý | Bị bộ lọc gỡ bỏ |
| Xem trạng thái thanh toán chỉ bằng mã đơn | Từ chối — bắt buộc mã truy cập |
| Truy cập đơn của người khác qua tham số đường dẫn | Từ chối — lọc theo token của phiên |

### 5.4.3. Kiểm thử đáp ứng

Hình 5.4 thể hiện giao diện trên ba kích thước màn hình: điện thoại, máy tính
bảng và máy tính để bàn.

[Hình 5.4]

### 5.4.4. Kiểm thử phía tầng giao diện

Tầng giao diện **không có bộ kiểm thử tự động thường trực**. Chất lượng được
canh bằng ba cổng kiểm tra:

1. Biên dịch, kiểm kiểu và kiểm mẫu hiển thị của khung ứng dụng.
2. Script kiểm tra mã màu viết trực tiếp.
3. Công cụ kiểm tra quy ước mã nguồn.

Kiểm thử giao diện trong các giai đoạn trước thực hiện bằng trình duyệt thật và
được ghi lại trong báo cáo từng giai đoạn, không phải một bộ kiểm thử thường
trực. **Đây là một giới hạn đã biết**, trình bày ở mục 5.7.

## 5.5. Kiểm thử thủ công trước khi bảo vệ

Danh sách tối thiểu cần chạy trên bản đóng gói:

1. Trang chủ có nội dung thật: 4 loại phòng, thư viện ảnh, đánh giá, tin tức.
2. Đặt một đơn từ đầu đến khi ra mã QR.
3. Tải lại trang ở màn hình thanh toán — vẫn thấy mã QR và đồng hồ đếm ngược.
4. Tra cứu đơn bằng mã và số điện thoại; thử sai số điện thoại thì bị từ chối.
5. Huỷ đơn; kiểm tra phòng mở lại bằng cách đặt lại đúng ngày đó.
6. Đăng nhập quản trị, bị buộc đổi mật khẩu, rồi vào trang tổng quan.
7. Màn hình đối soát có sẵn khoản cần xử lý.
8. Sửa một khối nội dung trang chủ rồi mở lại trang chủ để thấy thay đổi.
9. Mở hộp thư giả lập xem thư xác nhận đã gửi.
10. Đóng một phòng vài ngày; tìm phòng đúng khoảng đó thấy số phòng giảm đúng
    một, và đêm mở bán lại **không** giảm.

## 5.6. Đánh giá kết quả đạt được

### 5.6.1. Đối chiếu với mục đích ban đầu

| # | Mục đích | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Khách tìm và đặt phòng thành công | Đạt | Kiểm thử thủ công và 6 ca tự động |
| 2 | Không bao giờ bán trùng phòng | Đạt | 3 ca kiểm thử đa luồng thật |
| 3 | Thanh toán mã QR và webhook tự xác nhận | Đạt | 10 ca kiểm thử webhook |
| 4 | Không để tiền biến mất im lặng | Đạt | 5 ca kiểm thử đua thanh toán |
| 5 | Quản lý phòng, giá, số khách, ngày khả dụng | Đạt | 9 ca kiểm thử khoảng đóng phòng |
| 6 | Giao diện đạt chuẩn thương mại | Đạt | Không còn lỗi vi phạm WCAG mức A và AA |
| 7 | Một lệnh là đủ để chạy toàn hệ thống | Đạt có điều kiện | Xem mục 5.6.3 |
| 8 | Tài liệu kỹ thuật đầy đủ | Đạt | 12 tài liệu |

### 5.6.2. Đối chiếu với yêu cầu của đề tài

| Yêu cầu | Đáp ứng |
|---|---|
| Hiển thị phòng, tiện nghi và hình ảnh | Đạt |
| Tìm phòng theo ngày | Đạt |
| Gửi yêu cầu đặt phòng | Đạt |
| Quản lý trạng thái đơn | Đạt — tám trạng thái, máy trạng thái có kiểm thử |
| Quản trị thông tin phòng | Đạt |
| Quản trị viên cập nhật phòng, giá, số lượng khách và **ngày khả dụng** | Đạt đủ bốn vế |
| Hệ thống kiểm tra trùng ngày | Đạt — thực hiện ở tầng cơ sở dữ liệu |
| Tính chi phí dự kiến | Đạt — một nơi tính tiền duy nhất |
| Không yêu cầu thanh toán thật | Đạt — có endpoint mô phỏng |
| Có dữ liệu minh hoạ để trình diễn | Đạt — 40 đơn đủ tám trạng thái |

### 5.6.3. Những điều chưa kiểm chứng được

Đề tài nêu rõ giới hạn của chính quá trình kiểm thử:

- Quá trình phát triển chạy trong môi trường có proxy chặn kho phụ thuộc bên
  ngoài từ bên trong container dựng ảnh, nên **bước dựng ảnh trong quy trình
  đóng gói chưa được chạy trọn vẹn một lần** trong môi trường đó. Hệ thống đã
  được kiểm chứng bằng PostgreSQL thật cộng ứng dụng chạy trực tiếp, và mọi số
  liệu trong chương này lấy từ hệ thống chạy thật. **Cần chạy lại đủ ba lệnh
  triển khai trên máy có kết nối mạng bình thường trước khi bảo vệ.**
- Tầng giao diện **không có bộ kiểm thử tự động thường trực**, như đã nêu ở mục
  5.4.4. Hồi quy giao diện hiện phải phát hiện bằng mắt.

## 5.7. Các giới hạn đã biết của hệ thống

| # | Giới hạn | Ảnh hưởng | Hướng khắc phục |
|---|---|---|---|
| 1 | Giới hạn tần suất lưu trong bộ nhớ tiến trình | Chỉ đúng khi chạy **một** bản ứng dụng | Chuyển sang bộ nhớ đệm dùng chung khi mở rộng |
| 2 | Webhook xác thực bằng khoá tĩnh, không phải chữ ký | Khoá lộ thì giả mạo được | Dùng chữ ký băm khi nhà cung cấp hỗ trợ |
| 3 | Không có token chống giả mạo yêu cầu liên trang | Rủi ro thấp vì token nằm trong tiêu đề, không phải cookie | Bổ sung nếu chuyển sang xác thực bằng cookie |
| 4 | Hoàn tiền là thao tác thủ công | Người quản trị phải tự chuyển khoản lại | Tích hợp giao diện hoàn tiền của nhà cung cấp |
| 5 | Không có nhật ký kiểm toán cho thao tác quản trị | Không truy được ai đã sửa gì | Bổ sung bảng nhật ký kiểm toán |
| 6 | Mật khẩu chỉ yêu cầu độ dài, không yêu cầu độ phức tạp | Người dùng đặt được mật khẩu yếu | Bổ sung kiểm tra độ mạnh |
| 7 | **Không có giao thức bảo mật HTTPS trong bản đóng gói** | Mật khẩu và token đi qua mạng dạng rõ | Bổ sung chứng chỉ khi có tên miền |

**Giới hạn số 7 là quan trọng nhất** nếu đưa hệ thống ra sử dụng thật. Bản đóng
gói phục vụ giao thức không mã hoá vì nó chạy trên máy cục bộ khi trình diễn.
