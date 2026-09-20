## 3.7. Môi trường và quy trình phát triển

Hệ thống phát triển bằng IntelliJ IDEA và Visual Studio Code, quản lý phụ thuộc
bằng Maven và npm, quản lý mã nguồn bằng Git trên GitHub, cơ sở dữ liệu cục bộ
chạy trong container.

Yêu cầu tối thiểu để chạy hệ thống: Docker có Compose phiên bản 2 cho đường chạy
khuyến nghị; hoặc JDK 21, Node.js 20.19 trở lên và **PostgreSQL 16** nếu chạy
thủ công. PostgreSQL 16 là điều kiện cứng vì hệ thống cần phần mở rộng
`btree_gist`.

Đề tài thực hiện theo **mười giai đoạn** tuần tự, tổng khối lượng khoảng 97 giờ
công: khởi tạo dự án (5h), hệ thống thiết kế (12h), lược đồ cơ sở dữ liệu (8h),
xác thực và phân quyền (7h), lõi đặt phòng và chống trùng lịch (11h), thanh toán
và webhook (10h), khu quản trị (14h), trang khách và quản lý nội dung (16h),
đóng gói và tài liệu (7h), lịch khoá phòng theo khoảng ngày (7h). Mỗi giai đoạn
có tiêu chí nghiệm thu riêng và chỉ được đóng khi mọi tiêu chí kiểm chứng được
bằng lệnh.

Hình 3.11 thể hiện sơ đồ phụ thuộc giữa các giai đoạn, trong đó hai cặp giai
đoạn thực hiện song song được vì không dùng chung tệp nào.

[Hình 3.11]

Quy mô mã nguồn tại thời điểm hoàn thành: 174 tệp Java với 15 644 dòng, 100 tệp
TypeScript với 12 737 dòng, 8 migration cơ sở dữ liệu, 18 lớp kiểm thử với 129
ca, 31 commit trải từ 07/09/2026 tới 20/09/2026.

## 3.9. Cài đặt các chức năng chính

### 3.9.1. Chức năng tìm phòng trống

Đây là chức năng có yêu cầu kỹ thuật cao nhất ở tầng truy vấn. Hệ thống có **bốn
truy vấn phòng trống** phục vụ bốn màn hình khác nhau, tất cả đặt trong một lớp
truy cập dữ liệu duy nhất:

| Truy vấn | Phục vụ | Hậu quả nếu sai |
|---|---|---|
| Đếm phòng trống theo loại | Trang danh sách phòng, bước hai của luồng đặt phòng | Khách thấy sai số phòng còn lại |
| **Chọn phòng vật lý để gán** | **Lúc tạo đơn** | **Gán nhầm phòng — lỗi im lặng, chỉ lộ ra khi khách tới nhận phòng** |
| Lịch từng đêm toàn homestay | Thanh tìm phòng ở trang chủ | Lịch chặn sai ngày |
| Lịch từng đêm một loại phòng | Trang chi tiết phòng | Lịch chặn sai ngày |

**Điểm kỹ thuật thứ nhất: đếm trực tiếp thay vì lấy tổng trừ đi.** Truy vấn đếm
phòng trống đếm trực tiếp số phòng vừa khả dụng vừa rảnh.

Cách lấy tổng trừ đi số đã đặt sai ở chỗ nó trừ mọi đơn đang giữ chỗ của loại
phòng đó, kể cả đơn nằm trên phòng đã chuyển sang bảo trì. Một phòng bảo trì
đang có đơn cũ bị trừ **hai lần**: một lần vì nó không còn khả dụng, một lần nữa
vì nó vẫn có đơn. Website báo thiếu phòng và mất doanh thu mà không có dấu hiệu
nào.

**Điểm kỹ thuật thứ hai: gộp cả khoảng ngày vào một câu lệnh.** Truy vấn lịch
toàn homestay dùng hàm sinh chuỗi ngày của PostgreSQL để trả về mọi đêm trong
một lần gọi, thay vì lặp từng đêm. Khoảng tối đa là 120 ngày, và 120 lượt đi về
cơ sở dữ liệu cho một lần mở lịch đủ để người dùng cảm nhận được độ trễ.

**Điểm kỹ thuật thứ ba: vị trí của điều kiện lọc trong phép nối trái.** Truy vấn
lịch toàn homestay dùng phép nối trái để bảo đảm mọi đêm đều xuất hiện trong kết
quả, kể cả đêm không còn phòng nào. Điều kiện lọc khoảng đóng phòng phải nằm
trong mệnh đề điều kiện nối, không phải mệnh đề lọc. Đặt sai chỗ sẽ biến phép
nối trái thành phép nối trong, và những đêm hết phòng biến mất khỏi kết quả thay
vì trả về số không — trong khi tầng giao diện hiểu ngày vắng mặt là ngày không
bị chặn.

Hình 3.12 thể hiện trang chủ với thanh tìm phòng và lịch chọn ngày đang mở, trong
đó các ngày đã kín phòng bị chặn sẵn. Hình 3.13 thể hiện kết quả tìm phòng với
nhãn số phòng còn lại và tổng tiền cả kỳ.

[Hình 3.12]

[Hình 3.13]

### 3.9.2. Chức năng đặt phòng và vòng thử gán phòng

Thuật toán gán phòng là nơi lý thuyết trình bày ở Chương 2 được hiện thực hoá:

```
Với mỗi phòng ứng viên còn rảnh:
    Mở một giao dịch MỚI
    Thử ghi dòng gán phòng
    Nếu cơ sở dữ liệu trả về SQLSTATE 23P01:
        → một khách khác vừa chiếm mất phòng này
        → huỷ giao dịch, thử phòng tiếp theo
    Nếu thành công:
        → xác nhận giao dịch và kết thúc
Hết phòng để thử → trả về lỗi ROOM_NOT_AVAILABLE mã 409
```

**Dòng "mở một giao dịch mới" không phải tuỳ chọn.** Khi PostgreSQL bác một câu
lệnh vì vi phạm ràng buộc, toàn bộ giao dịch bị đánh dấu hỏng; mọi câu lệnh tiếp
theo trong cùng giao dịch đó trả về mã `25P02` chứ không thực thi. Việc thử phòng
tiếp theo trong cùng giao dịch là **không thể thành công**.

Hệ thống cũng chỉ bắt **đúng** mã `23P01`. Bắt chung mọi lỗi toàn vẹn dữ liệu sẽ
khiến một lỗi khoá ngoại — tức là lỗi lập trình thật — bị báo cho khách thành hết
phòng, và lỗi đó không bao giờ được phát hiện.

Hình 3.14 thể hiện ba bước của luồng đặt phòng.

[Hình 3.14]

### 3.9.3. Chức năng thanh toán

Hệ thống sinh mã QR theo chuẩn VietQR, kèm nội dung chuyển khoản gồm **mã đơn
ghép hai chữ số thứ tự lần thử**. Hai chữ số này cho phép phân biệt các lần
chuyển khoản khác nhau của cùng một đơn.

Màn hình thanh toán có đồng hồ đếm ngược 15 phút tương ứng với hạn giữ chỗ **có
thật** trong cơ sở dữ liệu, không phải đồng hồ trang trí.

Hình 3.15 thể hiện màn hình thanh toán.

[Hình 3.15]

**Xử lý webhook** theo bảng quyết định sau:

| Tình huống | Xử lý |
|---|---|
| Đủ tiền cọc | Chuyển sang `CONFIRMED`, gửi thư xác nhận |
| Thiếu tiền | Chuyển sang `AWAITING_REVIEW`, đánh dấu cần đối soát |
| Thừa tiền | Đánh dấu cần hoàn lại phần thừa |
| Webhook gửi lại lần hai | Bỏ qua, không cộng tiền hai lần |
| Không khớp đơn nào | Vẫn ghi vào nhật ký webhook để người đối soát xử lý |
| Tiền về sau khi đơn đã đóng | Mở lại sang `AWAITING_REVIEW`, thử giành lại phòng |

**Nguyên tắc thiết kế xuyên suốt:** không nhánh nào để tiền của khách biến mất
im lặng. Mọi khoản tiền không xử lý tự động được đều vào hàng đợi đối soát thủ
công, chứ không bị bỏ qua.

### 3.9.4. Chức năng quản lý ngày khả dụng

Trước giai đoạn mười, hệ thống chỉ có công tắc trạng thái vận hành cho phòng.
Công tắc đó **không gắn với ngày**. Chủ homestay muốn ghi nhận rằng một phòng sơn
lại trong năm ngày thì phải tự nhớ tắt rồi tự nhớ bật, và ngày quên bật là ngày
mất doanh thu mà không có dấu hiệu nào.

Giải pháp gồm ba phần: bảng `room_closures` với cột sinh tự động dùng cùng quy
ước nửa mở, ràng buộc loại trừ chống chồng lấn, và một điều kiện lọc thêm vào
**cả bốn** truy vấn phòng trống.

**Phần thưởng kiến trúc.** Tầng giao diện phía khách **không phải sửa một dòng
mã nào**. Trang chủ, danh sách phòng, trang chi tiết và luồng đặt phòng đều đọc
phòng trống qua bốn truy vấn đó, nên việc sửa ở tầng dữ liệu làm cả bốn màn hình
tự đúng theo. Đây là kết quả của việc gom mọi truy vấn phòng trống vào một lớp
duy nhất từ giai đoạn năm.

### 3.9.5. Khu quản trị

Khu quản trị gồm **mười một màn hình**, nạp lười theo tuyến.

Hình 3.16 thể hiện trang tổng quan với biểu đồ doanh thu theo tháng, tỉ lệ lấp
đầy và tỉ lệ huỷ. Hình 3.17 thể hiện màn hình đối soát thanh toán với các khoản
thiếu tiền và thừa tiền. Hình 3.18 thể hiện danh sách đơn đặt phòng kèm bộ lọc
theo trạng thái. Hình 3.19 thể hiện
màn hình quản lý nội dung trang chủ.

[Hình 3.16]

[Hình 3.17]

[Hình 3.18]

[Hình 3.19]

### 3.9.6. Quản lý nội dung và lọc HTML

Khu quản trị cho phép soạn nội dung HTML cho trang chủ và tin tức. Đây là một bề
mặt tấn công chèn mã kịch bản cổ điển [6].

Hệ thống lọc HTML **ở tầng vào**, trước khi lưu, bằng thư viện lọc của OWASP với
**danh sách thẻ cho phép tường minh** — không phải danh sách thẻ cấm. Lý do:
danh sách cấm luôn thiếu; mỗi khi xuất hiện một thẻ hoặc thuộc tính nguy hiểm
mới thì danh sách cấm lại lỗi thời, còn danh sách cho phép vẫn đúng.

Với nội dung do **khách** nhập, ví dụ đánh giá, hệ thống không lọc HTML mà hiển
thị dưới dạng văn bản thuần, vì khách không có nhu cầu định dạng.

## 3.10. Cài đặt mô hình bảo mật

### 3.10.1. Xác thực và buộc đổi mật khẩu tạm

Tài khoản quản trị của bản trình diễn được sinh với **mật khẩu ngẫu nhiên, chỉ
in một lần vào nhật ký container**, không ghi vào bất kỳ tệp nào trong mã nguồn.

Tài khoản đó mang cờ buộc đổi mật khẩu. Một bộ lọc chặn **mọi** đường dẫn trừ ba
đường tối thiểu — xem thông tin bản thân, đổi mật khẩu, đăng xuất — cho tới khi
mật khẩu được đổi. Lý do: mật khẩu tạm đã đi qua nhật ký nên **không còn là bí
mật**.

### 3.10.2. Phân biệt mã đơn và mã truy cập

Một quyết định bảo mật tinh tế: hệ thống dùng **hai mã khác nhau** cho một đơn.

**Mã đơn** xuất hiện trên sao kê ngân hàng vì nó nằm trong nội dung chuyển
khoản. Nghĩa là bất kỳ ai nhìn thấy sao kê đều biết mã đơn của người khác.

**Mã truy cập** là bí mật thao tác, chỉ có trong liên kết ở thư xác nhận.

Vì thế endpoint xem trạng thái thanh toán **bắt buộc** mã truy cập: nếu chỉ cần
mã đơn là xem được, ai thấy sao kê cũng theo dõi được đơn của người khác.

### 3.10.3. Giới hạn tần suất sau máy chủ web

Ứng dụng nằm sau máy chủ web, nên mọi yêu cầu đến đều mang địa chỉ mạng của
container máy chủ web. Giới hạn theo địa chỉ đó là giới hạn **toàn bộ người dùng
chung một hạn mức**.

Giải pháp là để máy chủ web **ghi đè** trường tiêu đề chứa địa chỉ thật của
người gọi. Nhưng trường này do phía khách gửi lên nên **giả mạo được**, trừ khi
hai điều kiện cùng thoả mãn:

1. Máy chủ web **ghi đè** trường đó chứ không nối thêm vào giá trị có sẵn [3].
2. Cổng của ứng dụng **không mở ra ngoài**, nên không ai gọi thẳng vào được.

Hai điều kiện này phải đi cùng nhau; thiếu một là lớp giới hạn tần suất trở
thành hình thức.

### 3.10.4. Quản lý bí mật

Hai biến bí mật của hệ thống **không có giá trị mặc định ở bất kỳ đâu**: không
trong tệp cấu hình, không trong tệp biến môi trường mẫu, không trong tệp đóng
gói. Ứng dụng **dừng khởi động** khi thiếu, ở mọi cấu hình.

Lý do: kho mã nguồn này công khai. Một giá trị mặc định được cho là an toàn cho
bản trình diễn nằm trong kho đồng nghĩa với việc bất kỳ ai sao chép về cũng **tự
ký được token vai trò quản trị** cho mọi bản triển khai dùng kho này.

Một script riêng là đường **duy nhất** tạo ra các giá trị thật, sinh ngẫu nhiên
vào tệp biến môi trường với quyền truy cập hạn chế.

### 3.10.5. Các lớp bảo vệ khác

| Lớp | Biện pháp |
|---|---|
| Tiêu đề bảo mật | Chính sách bảo mật nội dung, chống nhúng khung, chống đoán kiểu tệp |
| Ảnh tải lên | Kiểm định dạng, giới hạn dung lượng, giải mã lại và đổi tên thành định danh ngẫu nhiên |
| Xuất tệp CSV | Thoát ký tự đầu dòng để chống chèn công thức bảng tính [5] |
| Mật khẩu | Băm bằng thuật toán bcrypt |

## 3.11. Đóng gói và triển khai

### 3.11.1. Kiến trúc đóng gói

Bốn dịch vụ:

| Dịch vụ | Ảnh nền | Cổng mở ra máy chủ |
|---|---|---|
| Máy chủ web | `nginx:alpine` | **80** |
| Ứng dụng | `eclipse-temurin:21-jre-alpine` | Không |
| Cơ sở dữ liệu | `postgres:16` | Không |
| Máy chủ thư giả lập | `mailpit` | **8025** |

**Việc ứng dụng và cơ sở dữ liệu không mở cổng là một quyết định bảo mật**, không
phải tối giản cấu hình — đó là điều kiện để tin được thông tin địa chỉ mà máy chủ
web chuyển tiếp, như đã trình bày ở mục 3.10.3.

Ảnh ứng dụng chạy bằng **người dùng không có quyền quản trị**, và được dựng bằng
tệp Dockerfile nhiều tầng nên ảnh cuối không chứa công cụ biên dịch.

### 3.11.2. Cấu hình máy chủ web

Máy chủ web phải chuyển tiếp **bốn nhóm đường dẫn** chứ không chỉ nhóm giao diện
lập trình ứng dụng:

| Đường dẫn | Hậu quả nếu quên chuyển tiếp |
|---|---|
| `/api/**` | Toàn bộ hệ thống không hoạt động |
| `/swagger-ui/**` | Trả về trang ứng dụng kèm mã 200 — **trông giống như thành công** |
| `/v3/api-docs/**` | Tương tự |
| `/uploads/**` | Mọi ảnh tải lên trả về lỗi 404 |

Cả bốn khối phải dùng cú pháp tiền tố ưu tiên. Nguyên nhân: khối cuối tệp xử lý
tệp tĩnh là một khối biểu thức chính quy, và máy chủ web cho khối biểu thức chính
quy thắng mọi khối tiền tố thường [3]. Không có tiền tố ưu tiên thì ảnh tải lên
rơi vào khối tĩnh và trả về lỗi.

### 3.11.3. Hai cấu hình chạy

Hệ thống chỉ có **hai** cấu hình, cố ý không nhiều hơn:

| Cấu hình | Sử dụng khi | Bao gồm |
|---|---|---|
| Trình diễn | Phát triển và bản đem bảo vệ | Dữ liệu mẫu, tài liệu tương tác, trang thư viện component |
| Thật | Triển khai thật | Không dữ liệu mẫu, không tài liệu tương tác, không trang thư viện |

Việc đổi cấu hình trên cùng một khối dữ liệu là **an toàn** — đây chính là lý do
dữ liệu mẫu không nằm trong lịch sử migration.

### 3.11.4. Dữ liệu mẫu

Cấu hình trình diễn nạp sẵn: 4 loại phòng, 15 phòng vật lý, 12 tiện nghi, **40
đơn đặt phòng trải đủ tám trạng thái**, 3 khoản cần đối soát, 3 mã khuyến mãi
với ba tình trạng khác nhau, 8 đánh giá, 1 khoảng đóng phòng, và nội dung trang
chủ đầy đủ.

**Mọi ngày trong dữ liệu mẫu là tương đối** so với ngày hiện tại, nên bộ dữ liệu
không cũ đi theo thời gian: ngày bảo vệ vẫn có đơn trong tương lai để trình diễn.

### 3.11.5. Xử lý múi giờ

Toàn hệ thống chạy theo múi giờ Việt Nam, đặt ở **bốn tầng độc lập**: biến môi
trường của container, tham số máy ảo Java, cấu hình chuyển đổi dữ liệu sang JSON,
và cấu hình kết nối cơ sở dữ liệu.

Chỉ đặt cấu hình chuyển đổi JSON là **không đủ** — thuộc tính đó chỉ chi phối
cách dữ liệu được biểu diễn khi trả về, không đổi múi giờ mặc định của máy ảo.
Hệ quả nếu làm sai: đơn tạo trong khung nửa đêm tới rạng sáng giờ Việt Nam bị
gom nhóm vào **tháng trước** ở trang tổng quan, trong khi đồng hồ đếm ngược trên
màn hình thanh toán vẫn đúng — sai một phần nên rất khó nghi ngờ.

Endpoint kiểm tra tình trạng hệ thống phơi bày cả múi giờ ứng dụng lẫn múi giờ
mặc định của máy ảo để kiểm tra được điều này trực tiếp.

### 3.11.6. Quy trình triển khai ba lệnh

```bash
git clone <địa-chỉ-kho> homestay-tvh && cd homestay-tvh
./scripts/init-env.sh          # sinh bí mật ngẫu nhiên
docker compose up -d --build   # dựng và chạy 4 dịch vụ
```

Hình 3.20 thể hiện trạng thái bốn dịch vụ sau khi khởi động, và Hình 3.21 thể
hiện hộp thư giả lập với thư xác nhận đã gửi.

[Hình 3.20]

[Hình 3.21]
