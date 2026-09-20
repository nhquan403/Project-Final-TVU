# PHỤ LỤC

## Phụ lục A — Đặc tả mười một use case

Đặc tả đầy đủ của cả mười một use case, theo cùng một khuôn mẫu và sắp xếp theo
số hiệu: tác nhân chính, mô tả, tiền điều kiện, hậu điều kiện, luồng sự kiện
chính và các luồng thay thế kèm mã lỗi tương ứng.

#### UC1 — Tìm phòng trống

| Mục | Nội dung |
|---|---|
| **Mã** | UC1 |
| **Tác nhân chính** | Khách vãng lai, Khách có tài khoản |
| **Mô tả** | Khách tìm các loại phòng còn chỗ trong một khoảng ngày |
| **Tiền điều kiện** | Có ít nhất một loại phòng đang bật bán |
| **Hậu điều kiện** | Không có dữ liệu nào được ghi; đây là truy vấn chỉ đọc |

**Luồng sự kiện chính**

1. Khách mở trang chủ; thanh tìm phòng nạp sẵn lịch của toàn homestay.
2. Khách chọn ngày nhận và ngày trả. Ngày quá khứ và ngày đã kín mọi loại phòng
   bị chặn ngay trong lịch, không cho chọn.
3. Khách chọn số người lớn, trẻ em, số phòng.
4. Hệ thống truy vấn và trả về các loại phòng còn chỗ, kèm tổng tiền cả kỳ và số
   phòng còn lại của từng loại.

**Luồng thay thế**

- *2a. Lịch không tải được:* khách vẫn chọn được ngày; mất phần chặn trước, tầng
  máy chủ vẫn từ chối ngày kín ở bước sau.
- *4a. Không loại phòng nào đủ chỗ:* hiện trạng thái rỗng kèm gợi ý đổi ngày
  hoặc giảm số khách mỗi phòng.

---

#### UC2 — Đặt phòng

| Mục | Nội dung |
|---|---|
| **Mã** | UC2 |
| **Tác nhân chính** | Khách vãng lai, Khách có tài khoản |
| **Mô tả** | Khách gửi yêu cầu đặt phòng cho một khoảng ngày và nhận mã đơn |
| **Tiền điều kiện** | Đã hoàn tất UC1 và chọn được một loại phòng còn chỗ |
| **Hậu điều kiện** | Đơn ở trạng thái `PENDING_PAYMENT`; các phòng vật lý đã được gán; hạn giữ chỗ 15 phút bắt đầu chạy |

**Luồng sự kiện chính**

1. Khách nhập họ tên, số điện thoại, thư điện tử và ghi chú nếu có.
2. Khách nhập mã khuyến mãi tuỳ chọn; hệ thống kiểm tra và hiển thị ngay số tiền
   được giảm.
3. Khách xác nhận đơn.
4. Hệ thống mở một giao dịch, tính lại chi phí ở phía máy chủ, không tin số tiền
   do trình duyệt gửi lên.
5. Hệ thống chọn các phòng vật lý còn rảnh và ghi vào bảng gán phòng.
6. Cơ sở dữ liệu kiểm tra ràng buộc loại trừ. Nếu không vi phạm, giao dịch được
   commit.
7. Hệ thống sinh mã đơn, mã truy cập, thông tin thanh toán và trả về cho khách.
8. Hệ thống đưa thư xác nhận vào hàng đợi gửi.

**Luồng thay thế và ngoại lệ**

- *4a. Dữ liệu không hợp lệ* (ngày trả không sau ngày nhận, số khách vượt sức
  chứa mỗi phòng) → trả lỗi `INVALID_BOOKING_REQUEST` mã 400, không ghi gì.
- *2a. Mã khuyến mãi không hợp lệ* → `INVALID_PROMOTION` mã 400.
- *2b. Mã khuyến mãi hết lượt* → `PROMOTION_EXHAUSTED` mã 409.
- **6a. Ràng buộc loại trừ bị vi phạm** — một khách khác vừa đặt xong phòng đó
  trong tích tắc. Cơ sở dữ liệu trả `SQLSTATE 23P01`, giao dịch bị huỷ. Hệ thống
  mở **giao dịch mới** và thử phòng vật lý tiếp theo. Hết phòng để thử thì trả
  `ROOM_NOT_AVAILABLE` mã 409.
- *5a. Vượt giới hạn tần suất* → `TOO_MANY_REQUESTS` mã 429.

---

#### UC3 — Thanh toán cọc

| Mục | Nội dung |
|---|---|
| **Mã** | UC3 |
| **Tác nhân chính** | Khách vãng lai, Khách có tài khoản |
| **Mô tả** | Khách chuyển khoản tiền cọc theo mã QR và hệ thống tự xác nhận |
| **Tiền điều kiện** | Đơn đang ở `PENDING_PAYMENT` và còn trong hạn giữ chỗ |
| **Hậu điều kiện** | Đơn chuyển sang `CONFIRMED` nếu đủ tiền |

**Luồng sự kiện chính**

1. Hệ thống hiển thị mã QR kèm thông tin chuyển khoản dạng chữ và đồng hồ đếm
   ngược tương ứng hạn giữ chỗ thật.
2. Khách quét mã và chuyển khoản.
3. Nhà cung cấp thanh toán phát hiện biến động số dư và gọi webhook.
4. Hệ thống xác thực khoá, đối chiếu nội dung chuyển khoản với mã đơn và số tiền.
5. Đủ tiền cọc, hệ thống chuyển đơn sang `CONFIRMED` và gửi thư xác nhận.
6. Trình duyệt hỏi trạng thái định kỳ và tự chuyển sang trang hoàn tất.

**Luồng thay thế**

- *4a. Thiếu tiền* → đơn sang `AWAITING_REVIEW`, đánh dấu cần đối soát.
- *4b. Thừa tiền* → đánh dấu cần hoàn lại phần thừa.
- *4c. Webhook gửi lại lần hai* → bỏ qua, không cộng tiền hai lần.
- *4d. Không khớp đơn nào* → vẫn ghi vào nhật ký webhook cho người đối soát.
- *4e. Sai khoá xác thực* → từ chối.
- **3a. Tiền về sau khi đơn đã hết hạn** → mở lại đơn sang `AWAITING_REVIEW`,
  thử giành lại phòng; không giành được thì vào hàng đợi hoàn tiền.
- *2a. Hết hạn giữ chỗ mà không có tiền* → bộ quét chuyển đơn sang `EXPIRED` và
  nhả phòng.

---

#### UC9 — Đặt ngày khả dụng của phòng

| Mục | Nội dung |
|---|---|
| **Mã** | UC9 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Đóng một phòng vật lý trong một khoảng ngày; hệ thống tự mở lại khi hết khoảng |
| **Tiền điều kiện** | Đã đăng nhập vai trò `ADMIN`, đã đổi mật khẩu tạm |
| **Hậu điều kiện** | Phòng biến mất khỏi kết quả tìm phòng trong đúng khoảng ngày đó; **không đơn nào bị huỷ** |

**Luồng sự kiện chính**

1. Quản trị viên mở màn hình quản lý phòng và chọn một phòng vật lý.
2. Hệ thống hiển thị các khoảng đóng còn hiệu lực của phòng đó.
3. Quản trị viên chọn ngày bắt đầu đóng và ngày mở bán lại, nhập lý do.
4. Hệ thống ghi khoảng đóng và cơ sở dữ liệu kiểm tra ràng buộc chống chồng lấn.
5. Hệ thống trả về danh sách các đơn đang giao với khoảng vừa đóng, chỉ để cảnh
   báo.

**Luồng thay thế**

- *3a. Ngày mở bán lại không sau ngày bắt đầu* → `INVALID_ADMIN_REQUEST` mã 400.
- *4a. Khoảng mới chồng lên một khoảng đã có* → `CLOSURE_OVERLAP` mã 409.
- *5a. Không có đơn nào trong khoảng* → không hiện cảnh báo.

**Điểm thiết kế đáng lưu ý.** Hệ thống **cố ý không chặn** việc đóng một phòng
đang có đơn. Nếu chặn, chủ homestay không ghi nhận được sự thật rằng phòng hỏng
từ ngày mai chỉ vì hệ thống còn một đơn cũ — trong khi sự thật đó vẫn xảy ra dù
hệ thống có cho ghi hay không. Vai trò của hệ thống là **liệt kê** đúng các đơn
bị ảnh hưởng để người quyết định nhìn thấy, không phải quyết định thay.

Hình A.1 thể hiện màn hình quản lý ngày khả dụng.

[Hình A.1]

---

#### UC4 — Đối soát thanh toán

| Mục | Nội dung |
|---|---|
| **Mã** | UC4 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Xử lý các khoản tiền không khớp tự động được |
| **Tiền điều kiện** | Đã đăng nhập vai trò `ADMIN`, đã đổi mật khẩu tạm |
| **Hậu điều kiện** | Khoản tiền chuyển sang trạng thái đã xử lý |

**Luồng sự kiện chính**

1. Quản trị viên mở màn hình đối soát, thấy danh sách các khoản cần xử lý.
2. Chọn một khoản, xem chi tiết đơn và lịch sử thanh toán.
3. Quyết định: xác nhận thủ công, hoặc đánh dấu đã xử lý.
4. Hệ thống ghi lại quyết định và cập nhật trạng thái đối soát.

**Luồng thay thế**

- *3a. Khoản thừa tiền* → đánh dấu cần hoàn tiền; **hệ thống không tự hoàn
  tiền**, việc chuyển khoản lại do người thực hiện bên ngoài.
- *3b. Khoản không khớp đơn nào* → giữ trong nhật ký để tra cứu.

---

#### UC5 — Tra cứu và huỷ đơn

| Mục | Nội dung |
|---|---|
| **Mã** | UC5 |
| **Tác nhân chính** | Khách vãng lai, Khách có tài khoản |
| **Mô tả** | Khách tra cứu đơn và huỷ đơn mà không cần tài khoản |
| **Tiền điều kiện** | Khách có mã đơn và số điện thoại đã dùng khi đặt, hoặc có mã truy cập |
| **Hậu điều kiện** | Với thao tác huỷ: đơn sang `CANCELLED`, các phòng được nhả |

**Luồng sự kiện chính**

1. Khách nhập mã đơn và số điện thoại.
2. Hệ thống xác thực và trả về thông tin đơn.
3. Khách chọn huỷ đơn.
4. Hệ thống chuyển trạng thái, nhả phòng và hoàn lượt khuyến mãi nếu có.

**Luồng thay thế**

- *2a. Mã đơn sai hoặc số điện thoại không khớp* → trả về **cùng một thông báo
  lỗi** `BOOKING_NOT_FOUND` cho cả hai trường hợp, để không ai dò được mã đơn
  nào có thật.
- *3a. Trạng thái không cho phép huỷ* → `INVALID_STATE_TRANSITION` mã 409.
- *1a. Vượt giới hạn tần suất tra cứu* → `TOO_MANY_REQUESTS` mã 429.

---

#### UC6 — Quản lý đơn đặt phòng

| Mục | Nội dung |
|---|---|
| **Mã** | UC6 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Theo dõi và chuyển trạng thái đơn theo máy trạng thái |
| **Tiền điều kiện** | Đã đăng nhập vai trò `ADMIN` |
| **Hậu điều kiện** | Trạng thái đơn thay đổi và một dòng lịch sử được ghi |

**Luồng sự kiện chính**

1. Quản trị viên mở danh sách đơn, lọc theo trạng thái và khoảng ngày.
2. Mở chi tiết một đơn: thấy lịch sử trạng thái, các lần thanh toán, thư đã gửi
   và ghi chú nội bộ.
3. Chọn trạng thái mới trong số các trạng thái hợp lệ.
4. Hệ thống kiểm tra bước chuyển, ghi nhật ký, và thực hiện các việc kèm theo
   như nhả phòng hoặc hoàn lượt khuyến mãi.

**Luồng thay thế**

- *3a. Bước chuyển không hợp lệ* → `INVALID_STATE_TRANSITION` mã 409.
- *4a. Chuyển sang `CHECKED_OUT`* → **không nhả phòng**, xem giải thích ở mục
  3.3.2.

---

#### UC7 — Xem lại toàn bộ đơn đã đặt

| Mục | Nội dung |
|---|---|
| **Mã** | UC7 |
| **Tác nhân chính** | Khách có tài khoản |
| **Mô tả** | Khách đã đăng nhập xem lại toàn bộ đơn đã đặt ở một nơi |
| **Tiền điều kiện** | Đã đăng nhập với vai trò `CUSTOMER` |
| **Hậu điều kiện** | Không ghi dữ liệu; truy vấn chỉ đọc |

**Luồng sự kiện chính**

1. Khách đăng nhập và mở trang danh sách đơn đã đặt.
2. Hệ thống trả về danh sách đơn có phân trang, mặc định 10 đơn mỗi trang.
3. Khách mở chi tiết một đơn bằng mã đơn.

**Luồng thay thế**

- *2a. Chưa có đơn nào* → hiện trạng thái rỗng kèm liên kết tới trang đặt phòng.
- *3a. Mã đơn không thuộc về tài khoản đang đăng nhập* → từ chối.

**Điểm thiết kế đáng lưu ý.** Danh sách đơn được lọc theo định danh lấy từ
**token của phiên**, không theo tham số trên đường dẫn. Nếu lọc theo tham số,
người dùng chỉ cần đổi số trên thanh địa chỉ là xem được đơn của người khác. Số
phần tử mỗi trang cũng bị giới hạn trần ở phía máy chủ để một yêu cầu không kéo
về toàn bộ bảng.

---

#### UC8 — Xem báo cáo doanh thu

| Mục | Nội dung |
|---|---|
| **Mã** | UC8 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Xem tổng quan doanh thu, tỉ lệ lấp đầy và tỉ lệ huỷ theo tháng |
| **Tiền điều kiện** | Đã đăng nhập vai trò `ADMIN` |
| **Hậu điều kiện** | Không ghi dữ liệu |

**Luồng sự kiện chính**

1. Quản trị viên mở trang tổng quan.
2. Hệ thống tính khoảng thời gian mặc định là một số tháng gần nhất tính tới đầu
   tháng sau.
3. Hệ thống truy vấn và trả về: giá trị đơn theo tháng, số tiền thực nhận theo
   tháng, tỉ lệ lấp đầy theo tháng, số đơn mới theo tháng, các loại phòng bán
   chạy nhất, tổng số đơn, số đơn huỷ, tỉ lệ huỷ, và số khoản chờ đối soát.
4. Quản trị viên xuất danh sách đơn ra tệp CSV nếu cần.

**Luồng thay thế**

- *2a. Quản trị viên chọn khoảng thời gian khác* → hệ thống chuẩn hoá về đầu
  tháng rồi tính lại.
- *3a. Không có đơn nào trong kỳ* → tỉ lệ huỷ trả về 0 thay vì gây lỗi chia cho
  không.

---

#### UC10 — Soạn nội dung trang chủ

| Mục | Nội dung |
|---|---|
| **Mã** | UC10 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Cập nhật các khối nội dung trang chủ, biểu ngữ, thư viện ảnh và tin tức |
| **Tiền điều kiện** | Đã đăng nhập vai trò `ADMIN` |
| **Hậu điều kiện** | Nội dung đã lọc được lưu; trang công khai hiển thị nội dung mới |

**Luồng sự kiện chính**

1. Quản trị viên mở một trong bốn màn hình quản lý nội dung.
2. Sửa nội dung, có thể nhập đoạn HTML cho phần thân.
3. Tải ảnh lên nếu cần.
4. Lưu. Hệ thống **lọc HTML ở tầng vào** theo danh sách thẻ cho phép tường minh
   trước khi ghi vào cơ sở dữ liệu.
5. Nội dung đã đăng xuất hiện trên trang công khai.

**Luồng thay thế**

- *4a. Nội dung chứa thẻ nguy hiểm* → các thẻ đó bị gỡ bỏ, phần còn lại vẫn lưu.
- *3a. Tệp tải lên sai định dạng hoặc vượt dung lượng* → từ chối với mã lỗi
  tương ứng.
- *5a. Bài viết chưa đánh dấu đã đăng* → không xuất hiện trên trang công khai.

---

#### UC11 — Viết đánh giá

| Mục | Nội dung |
|---|---|
| **Mã** | UC11 |
| **Tác nhân chính** | Khách đã trả phòng |
| **Mô tả** | Khách gửi đánh giá về kỳ lưu trú vừa qua |
| **Tiền điều kiện** | Đơn ở trạng thái `CHECKED_OUT`; khách có mã truy cập hoặc số điện thoại |
| **Hậu điều kiện** | Đánh giá được lưu ở trạng thái `PENDING`, chờ duyệt |

**Luồng sự kiện chính**

1. Khách mở liên kết đánh giá và nhập điểm từ 1 tới 5, tiêu đề và nội dung.
2. Hệ thống xác thực khách qua mã truy cập hoặc số điện thoại.
3. Hệ thống kiểm tra đơn đã ở trạng thái `CHECKED_OUT`.
4. Hệ thống lưu đánh giá, **chụp tên khách từ đơn** chứ không nhận từ dữ liệu
   gửi lên, và đặt trạng thái chờ duyệt.
5. Quản trị viên duyệt, từ chối hoặc trả lời đánh giá.
6. Chỉ đánh giá đã duyệt mới hiển thị công khai.

**Luồng thay thế**

- *2a. Xác thực thất bại* → `BOOKING_NOT_FOUND`.
- *3a. Đơn chưa trả phòng* → từ chối kèm thông báo nêu rõ trạng thái hiện tại.
- *4a. Đơn đã có đánh giá* → từ chối. Cột khoá đơn có ràng buộc duy nhất nên cơ
  sở dữ liệu vẫn là chốt chặn cuối cùng; việc kiểm ở tầng dịch vụ chỉ để trả về
  một thông báo dễ hiểu thay vì lỗi ràng buộc thô.

**Điểm thiết kế đáng lưu ý.** Nội dung đánh giá do khách nhập được lưu **nguyên
văn** và hiển thị dưới dạng văn bản thuần, không lọc HTML. Lý do: khách không có
nhu cầu định dạng, nên cách an toàn nhất là không diễn giải nội dung đó như mã
đánh dấu.

---

#### Ảnh màn hình bổ trợ cho phụ lục A

Hai màn hình dưới đây không nằm trong phần chữ của Chương 3 nhưng cần cho việc
đối chiếu khi chấm: bước buộc đổi mật khẩu tạm ở lần đăng nhập quản trị đầu
tiên, và trang chi tiết loại phòng mà khách thấy trước khi vào luồng đặt phòng.

Hình A.2 thể hiện bước buộc đổi mật khẩu tạm.

[Hình A.2]

Hình A.3 thể hiện trang chi tiết một loại phòng.

[Hình A.3]

## Phụ lục B — Mô tả mười bảy bảng còn lại

Bốn bảng cốt lõi (`users`, `bookings`, `booking_rooms`, `room_closures`) đã mô
tả ở mục 3.2.2. Phụ lục này mô tả mười bảy bảng còn lại theo cùng mẫu ba cột,
sắp xếp theo thứ tự migration.

**Bảng `refresh_tokens` — phiên đăng nhập dài hạn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `user_id` | bigint | Khoá ngoại tới `users`, xoá theo tầng |
| `token_hash` | char(64) | `uq_refresh_tokens_hash` duy nhất. **Lưu băm, không lưu token gốc** |
| `expires_at` | timestamptz | Thời điểm hết hạn |
| `revoked_at` | timestamptz | Thời điểm thu hồi, rỗng khi còn hiệu lực |
| `replaced_by` | char(64) | Băm của token kế nhiệm sau khi xoay vòng |
| `created_at` | timestamptz | Thời điểm tạo |

Cột `replaced_by` phục vụ phát hiện token bị đánh cắp: nếu một token đã bị thay
thế lại được sử dụng, đó là dấu hiệu bất thường và cả chuỗi token bị thu hồi.

### Nhóm V2 — Phòng và tiện nghi

**Bảng `amenities` — tiện nghi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_amenities_code` duy nhất |
| `name` | varchar(100) | Tên hiển thị |
| `icon` | varchar(50) | Mã biểu tượng |
| `category` | varchar(20) | `ck_amenities_category` giới hạn `ROOM` và `PROPERTY` |
| `display_order` | integer | Thứ tự hiển thị |

**Bảng `room_types` — loại phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_room_types_code` duy nhất |
| `slug` | varchar(120) | `uq_room_types_slug` duy nhất, dùng trong đường dẫn |
| `name` | varchar(150) | Tên loại phòng |
| `short_description`, `description` | text | Mô tả ngắn và mô tả đầy đủ |
| `base_price` | numeric(12,2) | `ck_room_types_price` phải lớn hơn 0 |
| `capacity_adults` | integer | `ck_room_types_adults` tối thiểu 1 |
| `capacity_children` | integer | `ck_room_types_children` không âm |
| `bed_info` | varchar(150) | Mô tả giường |
| `area_sqm` | numeric(6,2) | `ck_room_types_area` rỗng hoặc lớn hơn 0 |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Tắt để ngừng bán loại phòng |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

**Bảng `rooms` — phòng vật lý**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `room_type_id` | bigint | Khoá ngoại tới `room_types` |
| `room_number` | varchar(20) | `uq_rooms_number` duy nhất |
| `floor` | integer | Tầng, có thể rỗng |
| `status` | varchar(20) | `ck_rooms_status` giới hạn `AVAILABLE`, `MAINTENANCE`, `OUT_OF_SERVICE` |
| `note` | text | Ghi chú nội bộ |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Đây là bảng mà ràng buộc chống trùng lịch đặt lên. Chỉ mục
`idx_rooms_type_status` phục vụ truy vấn đếm phòng trống theo loại.

**Bảng `room_type_images` — ảnh của loại phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `room_type_id` | bigint | Khoá ngoại, xoá theo tầng |
| `url` | varchar(500) | Đường dẫn ảnh |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh, rỗng khi lưu cục bộ |
| `alt_text` | varchar(255) | Văn bản thay thế phục vụ khả năng tiếp cận |
| `display_order` | integer | Thứ tự hiển thị |
| `is_cover` | boolean | Ảnh bìa. Chỉ mục duy nhất từng phần `uq_room_type_images_cover` bảo đảm mỗi loại phòng có tối đa một ảnh bìa |

**Bảng `room_type_amenities` — bảng nối loại phòng và tiện nghi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `room_type_id` | bigint | Khoá ngoại, xoá theo tầng |
| `amenity_id` | bigint | Khoá ngoại, xoá theo tầng |
| | | `pk_room_type_amenities` khoá chính tổ hợp hai cột |

Đây là bảng duy nhất trong lược đồ **không có thực thể riêng** trong mã nguồn.
Quan hệ nhiều–nhiều giữa loại phòng và tiện nghi được ánh xạ trực tiếp, nên tổng
số thực thể là **20** trong khi tổng số bảng là **21**.

### Nhóm V3 — Đặt phòng

**Bảng `booking_status_history` — nhật ký chuyển trạng thái**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `from_status` | varchar(20) | `ck_bsh_from_status` rỗng hoặc thuộc tám trạng thái |
| `to_status` | varchar(20) | `ck_bsh_to_status` thuộc tám trạng thái |
| `changed_by` | bigint | Khoá ngoại tới `users`, rỗng khi hệ thống tự chuyển |
| `actor` | varchar(20) | `ck_bsh_actor` giới hạn `GUEST`, `CUSTOMER`, `ADMIN`, `SYSTEM` |
| `note` | text | Ghi chú kèm theo lần chuyển |
| `created_at` | timestamptz | Thời điểm chuyển |

### Nhóm V4 — Thanh toán và thư

**Bảng `payments` — lần thanh toán**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `attempt_no` | integer | `ck_payments_attempt` tối thiểu 1. Cùng `booking_id` tạo thành chỉ mục duy nhất |
| `provider` | varchar(20) | `ck_payments_provider` giới hạn `SEPAY` và `MANUAL` |
| `amount_expected` | numeric(12,2) | `ck_payments_expected` lớn hơn 0 |
| `amount_received` | numeric(12,2) | `ck_payments_received` không âm |
| `qr_content`, `qr_image_url` | text, varchar(500) | Nội dung và ảnh mã QR |
| `transfer_content` | varchar(50) | `uq_payments_transfer_content` duy nhất. Mã đơn ghép số thứ tự lần thử |
| `status` | varchar(20) | `ck_payments_status` liệt kê sáu trạng thái |
| `reconcile_status` | varchar(20) | `ck_payments_reconcile` giới hạn `NONE`, `NEEDS_REVIEW`, `REFUND_REQUIRED`, `RESOLVED` |
| `provider_txn_id` | varchar(100) | Mã giao dịch của nhà cung cấp |
| `paid_at`, `expires_at` | timestamptz | Thời điểm thanh toán và hết hạn |
| `created_at`, `updated_at` | timestamptz | Thời điểm tạo và cập nhật |

Chỉ mục từng phần `idx_payments_reconcile` chỉ đánh chỉ mục các dòng cần đối
soát, giúp màn hình đối soát truy vấn nhanh mà không phải quét toàn bảng.

**Bảng `payment_webhook_events` — nhật ký webhook**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `provider` | varchar(20) | Nhà cung cấp gửi webhook |
| `external_id` | varchar(100) | Mã sự kiện bên nhà cung cấp |
| `payment_id` | bigint | Khoá ngoại tới `payments`, rỗng khi không khớp đơn nào |
| `payload` | jsonb | Toàn bộ nội dung nhận được |
| `processing_result` | varchar(20) | `ck_pwe_result` giới hạn `MATCHED`, `UNMATCHED`, `LATE`, `DUPLICATE`, `ERROR` |
| `error_message` | text | Thông báo lỗi nếu xử lý thất bại |
| `received_at`, `processed_at` | timestamptz | Thời điểm nhận và xử lý |

Chỉ mục duy nhất `uq_webhook_provider_external` trên cặp nhà cung cấp và mã sự
kiện là cơ chế **chống cộng tiền hai lần** khi nhà cung cấp gửi lại webhook.

**Bảng `outbound_emails` — hàng đợi thư đi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, đặt rỗng khi đơn bị xoá |
| `template` | varchar(100) | Tên mẫu thư |
| `to_email` | varchar(255) | Địa chỉ nhận |
| `payload` | jsonb | Dữ liệu điền vào mẫu |
| `status` | varchar(20) | `ck_outbound_emails_status` giới hạn `PENDING`, `SENT`, `FAILED` |
| `attempts` | integer | `ck_outbound_emails_attempts` không âm |
| `last_error` | text | Lỗi lần gửi gần nhất |
| `sent_at`, `created_at` | timestamptz | Thời điểm gửi và tạo |

### Nhóm V5 — Khuyến mãi và đánh giá

**Bảng `promotions` — mã khuyến mãi**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar(50) | `uq_promotions_code` duy nhất |
| `name`, `description` | varchar(150), text | Tên và mô tả |
| `discount_type` | varchar(20) | `ck_promotions_type` giới hạn `PERCENT` và `FIXED` |
| `discount_value` | numeric(12,2) | `ck_promotions_value` lớn hơn 0 |
| `max_discount_amount` | numeric(12,2) | `ck_promotions_max` rỗng hoặc lớn hơn 0 |
| `min_nights` | integer | `ck_promotions_nights` tối thiểu 1 |
| `min_total_amount` | numeric(12,2) | `ck_promotions_min_total` không âm |
| `starts_at`, `ends_at` | timestamptz | `ck_promotions_window` buộc kết thúc sau bắt đầu |
| `usage_limit` | integer | Giới hạn lượt dùng, rỗng là không giới hạn |
| `used_count` | integer | `ck_promotions_used` không âm; `ck_promotions_usage` buộc không vượt giới hạn |
| `active` | boolean | Tắt để ngừng áp dụng |

Ràng buộc `ck_promotions_usage` bảo đảm số lượt đã dùng **không bao giờ vượt quá
giới hạn** ngay ở tầng dữ liệu, kể cả khi nhiều đơn cùng áp một mã.

**Bảng `reviews` — đánh giá của khách**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng. `uq_reviews_booking` **duy nhất — mỗi đơn một đánh giá** |
| `user_id` | bigint | Khoá ngoại tới `users`, rỗng với khách vãng lai |
| `guest_name_snapshot` | varchar(150) | Tên khách chụp từ đơn tại thời điểm đánh giá |
| `rating` | smallint | `ck_reviews_rating` trong khoảng 1 tới 5 |
| `title`, `content` | varchar(200), text | Tiêu đề và nội dung |
| `status` | varchar(20) | `ck_reviews_status` giới hạn `PENDING`, `APPROVED`, `REJECTED` |
| `admin_reply`, `replied_at` | text, timestamptz | Phản hồi của quản trị viên |
| `created_at` | timestamptz | Thời điểm gửi |

### Nhóm V6 — Nội dung

**Bảng `site_contents` — khối nội dung trang chủ**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `section_key` | varchar(100) | `uq_site_contents_key` duy nhất. Khoá định danh khối |
| `title`, `subtitle` | varchar(255) | Tiêu đề và tiêu đề phụ |
| `body` | text | Nội dung HTML **đã lọc ở tầng vào** |
| `data` | jsonb | Dữ liệu có cấu trúc tuỳ khối |
| `image_url` | varchar(500) | Ảnh minh hoạ |
| `updated_by` | bigint | Khoá ngoại tới `users` |
| `updated_at` | timestamptz | Thời điểm cập nhật |

**Bảng `banners` — biểu ngữ**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `title` | varchar(255) | Tiêu đề, bắt buộc |
| `image_url` | varchar(500) | Ảnh, bắt buộc |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh |
| `link_url` | varchar(500) | Đường dẫn khi nhấp vào |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Bật tắt hiển thị |
| `starts_at`, `ends_at` | timestamptz | `ck_banners_window` buộc kết thúc sau bắt đầu khi cả hai cùng có giá trị |

**Bảng `gallery_images` — thư viện ảnh**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `url` | varchar(500) | Đường dẫn ảnh, bắt buộc |
| `public_id` | varchar(255) | Định danh trên dịch vụ lưu ảnh |
| `caption` | varchar(255) | Chú thích |
| `category` | varchar(50) | Phân nhóm ảnh |
| `display_order` | integer | Thứ tự hiển thị |
| `active` | boolean | Bật tắt hiển thị |

**Bảng `posts` — tin tức**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `slug` | varchar(200) | `uq_posts_slug` duy nhất, dùng trong đường dẫn |
| `title` | varchar(255) | Tiêu đề, bắt buộc |
| `excerpt`, `content` | text | Trích dẫn ngắn và nội dung đầy đủ |
| `cover_image_url` | varchar(500) | Ảnh bìa |
| `published` | boolean | Cờ đã đăng |
| `published_at` | timestamptz | Thời điểm đăng |
| `author_id` | bigint | Khoá ngoại tới `users` |

Chỉ mục từng phần `idx_posts_published` chỉ đánh chỉ mục các bài đã đăng, phục
vụ trang tin tức công khai.

### Nhóm V7 — Ghi chú nội bộ

**Bảng `booking_notes` — ghi chú của quản trị viên trên đơn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, xoá theo tầng |
| `author_id` | bigint | Khoá ngoại tới `users`, đặt rỗng khi tài khoản bị xoá |
| `content` | text | `ck_booking_notes_content` buộc không rỗng sau khi cắt khoảng trắng |
| `created_at` | timestamptz | Thời điểm ghi |

Bảng này tách khỏi `booking_status_history` vì mỗi dòng lịch sử bắt buộc kèm một
lần chuyển trạng thái thật, mà máy trạng thái không cho phép chuyển từ một trạng
thái sang chính nó. Ghi chú là dữ liệu **nội bộ** và không endpoint công khai
nào đọc bảng này.

### Nhóm V8 — Ngày khả dụng

---
