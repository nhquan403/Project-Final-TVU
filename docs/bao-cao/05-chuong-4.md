# CHƯƠNG 4. KẾT QUẢ NGHIÊN CỨU

Chương này trình bày các kết quả đạt được sau quá trình thực hiện đồ án: kết quả
kiểm thử, đánh giá hiệu năng và khả năng tiếp cận, đối chiếu với mục đích ban
đầu, và các giới hạn đã biết của hệ thống.

## 4.1. Chiến lược kiểm thử

Hệ thống có **129 ca kiểm thử tự động trong 18 lớp**, tất cả đều thành công.

### 4.1.1. Lý do không sử dụng cơ sở dữ liệu giả lập

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

### 4.1.2. Các cấp độ kiểm thử

| Cấp độ | Ví dụ lớp | Kiểm điều gì |
|---|---|---|
| Kiểm thử đơn vị | Lọc HTML, xuất tệp CSV | Logic thuần, không cần cơ sở dữ liệu |
| Kiểm thử tích hợp | Phần lớn các lớp | Nhiều tầng cùng cơ sở dữ liệu thật |
| Kiểm thử tranh chấp | Đặt phòng đa luồng, đua thanh toán | Nhiều luồng chạy song song thật |
| Kiểm thử lược đồ | Migration, ràng buộc | Chính cấu trúc cơ sở dữ liệu |

Dự án không cấu hình pha kiểm thử tích hợp riêng; công cụ chạy kiểm thử được nới
để nhận cả lớp có hậu tố chỉ kiểm thử tích hợp, nên lệnh kiểm chứng gồm biên
dịch, chạy toàn bộ 129 ca và đóng gói.

## 4.2. Kết quả kiểm thử tự động

### 4.2.1. Tổng hợp theo nhóm

| Nhóm | Số lớp | Số ca | Trọng tâm |
|---|---:|---:|---|
| Lược đồ và ràng buộc | 3 | 19 | Migration dựng đúng 21 bảng; từng ràng buộc kiểm tra thật sự chặn |
| Đặt phòng | 4 | 26 | Tranh chấp đa luồng, đếm phòng trống, máy trạng thái, quét hết hạn |
| Ngày khả dụng | 1 | 9 | Khoảng đóng phòng trừ đúng ở cả bốn truy vấn |
| Thanh toán | 2 | 15 | Webhook, chống cộng tiền hai lần, cửa sổ tranh chấp |
| Xác thực và phân quyền | 2 | 10 | Vòng đời token, ma trận 30 tiền tố |
| Nội dung và đánh giá | 3 | 32 | Lọc HTML, nội dung công khai, quy tắc đánh giá |
| Báo cáo và lưu trữ | 3 | 23 | Số liệu tổng quan, xuất CSV, tải ảnh |
| **Tổng** | **18** | **129** | |

Bảng chi tiết từng lớp kiểm thử trình bày ở Phụ lục I.

### 4.2.2. Ba ca kiểm thử trọng tâm

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

## 4.3. Các lỗi thực tế phát hiện trong quá trình thực hiện

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
thị đúng hay không. Hình 4.3 so sánh trước và sau khi khắc phục lỗi tương phản.

[Hình 4.3]

**Về lỗi số 9:** lỗi này do một vòng rà soát mã độc lập tìm ra **sau khi** tác
giả đã tự kiểm thử và cho rằng chức năng đã hoàn chỉnh. Đây là lý do đề tài giữ
bước rà soát độc lập ở cuối mỗi giai đoạn.

## 4.4. Kiểm thử phi chức năng

### 4.4.1. Khả năng tiếp cận

Quét tự động trên 7 màn hình công khai và 6 màn hình quản trị: **không còn lỗi
vi phạm** hướng dẫn khả năng tiếp cận nội dung web phiên bản 2.0 và 2.1 ở mức A
và AA [13].

Đo vùng chạm ở 3 độ rộng màn hình: **không còn vùng chạm dưới 44×44 điểm ảnh**.

Hình 4.1 thể hiện kết quả quét.

[Hình 4.1]

### 4.4.2. Kiểm thử bảo mật

Chín kịch bản tấn công được thử trên hệ thống đang chạy, **tất cả đều bị chặn**:
giả mạo tiêu đề địa chỉ để vượt giới hạn tần suất; gọi endpoint quản trị không
token hoặc bằng token vai trò khách; dùng token cũ sau khi đổi mật khẩu; gọi
webhook không khoá xác thực; gửi lại cùng một webhook; chèn thẻ kịch bản vào nội
dung quản lý; xem trạng thái thanh toán chỉ bằng mã đơn; và truy cập đơn của
người khác qua tham số đường dẫn. Bảng chi tiết từng kịch bản và kết quả trình
bày ở Phụ lục M.

### 4.4.3. Kiểm thử đáp ứng

Hình 4.2 thể hiện giao diện trên ba kích thước màn hình: điện thoại, máy tính
bảng và máy tính để bàn.

[Hình 4.2]

### 4.4.4. Kiểm thử phía tầng giao diện

Tầng giao diện **không có bộ kiểm thử tự động thường trực**. Chất lượng được
canh bằng ba cổng kiểm tra:

1. Biên dịch, kiểm kiểu và kiểm mẫu hiển thị của khung ứng dụng.
2. Script kiểm tra mã màu viết trực tiếp.
3. Công cụ kiểm tra quy ước mã nguồn.

Kiểm thử giao diện trong các giai đoạn trước thực hiện bằng trình duyệt thật và
được ghi lại trong báo cáo từng giai đoạn, không phải một bộ kiểm thử thường
trực. **Đây là một giới hạn đã biết**, trình bày ở mục 4.7.

## 4.5. Kiểm thử thủ công trước khi bảo vệ

Ngoài bộ kiểm thử tự động, đề tài xác định một danh sách **mười bước kiểm thử
thủ công** cần chạy trên bản đóng gói trước khi trình diễn, phủ toàn bộ luồng
nghiệp vụ chính: từ xem trang chủ, đặt đơn tới khi ra mã QR, tra cứu và huỷ đơn,
đăng nhập quản trị và đổi mật khẩu bắt buộc, đối soát thanh toán, sửa nội dung
trang chủ, xem thư xác nhận, tới đóng phòng theo khoảng ngày và kiểm chứng số
phòng giảm đúng một. Danh sách đầy đủ trình bày ở Phụ lục N.

## 4.6. Đánh giá kết quả đạt được

### 4.6.1. Đối chiếu với mục đích ban đầu

| # | Mục đích | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Khách tìm và đặt phòng thành công | Đạt | Kiểm thử thủ công và 6 ca tự động |
| 2 | Không bao giờ bán trùng phòng | Đạt | 3 ca kiểm thử đa luồng thật |
| 3 | Thanh toán mã QR và webhook tự xác nhận | Đạt | 10 ca kiểm thử webhook |
| 4 | Không để tiền biến mất im lặng | Đạt | 5 ca kiểm thử đua thanh toán |
| 5 | Quản lý phòng, giá, số khách, ngày khả dụng | Đạt | 9 ca kiểm thử khoảng đóng phòng |
| 6 | Giao diện đạt chuẩn thương mại | Đạt | Không còn lỗi vi phạm WCAG mức A và AA |
| 7 | Một lệnh là đủ để chạy toàn hệ thống | Đạt có điều kiện | Xem mục 4.6.3 |
| 8 | Tài liệu kỹ thuật đầy đủ | Đạt | 12 tài liệu |

### 4.6.2. Đối chiếu với yêu cầu của đề tài

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

### 4.6.3. Những điều chưa kiểm chứng được

Đề tài nêu rõ giới hạn của chính quá trình kiểm thử:

- Toàn bộ bản đóng gói đã được dựng lên và kiểm chứng: bốn dịch vụ đều báo
  `healthy` (Hình 3.20), nginx phục vụ trang khách ở cổng 80, lời gọi
  `/api/room-types` đi xuyên qua nginx tới máy chủ ứng dụng trả về đủ bốn loại
  phòng, và thư xác nhận đi trọn đường hàng đợi thư → giao thức SMTP → hộp thư
  giả (Hình 3.21). Phần **chưa** kiểm chứng được chỉ còn là *tầng dựng ảnh*:
  môi trường phát triển có proxy chặn kho phụ thuộc Maven và npm từ bên trong
  container dựng ảnh, nên hai tệp `Dockerfile` được kiểm chứng ở tầng chạy với
  tệp jar và thư mục tĩnh dựng sẵn bên ngoài, chưa phải bằng một lệnh
  `docker compose up -d --build` chạy liền mạch. **Cần chạy lại đủ ba lệnh
  triển khai trên máy có kết nối mạng bình thường trước khi bảo vệ.**
- Tầng giao diện **không có bộ kiểm thử tự động thường trực**, như đã nêu ở mục
  5.4.4. Hồi quy giao diện hiện phải phát hiện bằng mắt.

## 4.7. Các giới hạn đã biết của hệ thống

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
