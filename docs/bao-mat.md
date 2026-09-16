# Mô hình bảo mật

Tài liệu này mô tả các lớp bảo vệ đang có, **và những giới hạn đã biết**. Phần
giới hạn ở cuối quan trọng không kém phần đầu: một tài liệu bảo mật chỉ liệt kê
điểm mạnh là tài liệu tiếp thị.

## Xác thực

### Hai loại token, hai vòng đời

| | Access token | Refresh token |
|---|---|---|
| Dạng | JWT | chuỗi ngẫu nhiên |
| Lưu ở client | **bộ nhớ JavaScript** | cookie `HttpOnly` |
| Tuổi thọ | ngắn (phút) | dài (ngày) |
| Lưu ở server | không | **hash SHA-256** trong `refresh_tokens` |

Access token nằm trong bộ nhớ, **không** trong `localStorage`. Refresh token nằm
trong cookie `HttpOnly`, nên mã JavaScript — kể cả mã do kẻ tấn công chèn qua
XSS — không đọc được nó.

Cái giá phải chấp nhận: tải lại trang là mất access token, và ứng dụng phải gọi
`/api/auth/refresh` lúc khởi động để lấy lại bằng cookie. Đó là cái giá đúng để
đổi lấy việc một lỗ hổng XSS không mang đi được phiên đăng nhập kéo dài bảy
ngày.

Cookie đặt `Secure` kể cả lúc chạy trên `http://localhost` — trình duyệt vẫn
chấp nhận cookie `Secure` trên localhost vì coi đó là origin tin cậy — và
`SameSite=Strict`, nên endpoint refresh không bị CSRF lợi dụng.

Bảng `refresh_tokens` lưu **hash** của token, không lưu token. Rò bảng này không
cho ai đăng nhập được.

### `token_version` — thu hồi tức thì

JWT mang `token_version` của người dùng lúc phát hành. Mỗi lần đăng xuất, đổi
mật khẩu, khoá tài khoản hoặc đổi quyền, cột `users.token_version` tăng lên.
Token cũ lệch số là bị từ chối **ngay**, không phải chờ hết hạn.

Đây là câu trả lời cho nhược điểm cố hữu của JWT: token đã phát hành thì không
thu hồi được. Có `token_version`, thu hồi được — đổi lại là mỗi request phải đọc
số phiên bản, nên nó được cache trong bộ nhớ và chỉ bị xoá khi có thay đổi.

### Buộc đổi mật khẩu tạm

Tài khoản quản trị dựng sẵn nhận mật khẩu tạm đi qua tay người khác (log
container, tin nhắn). `MustChangePasswordFilter` trả 403
`PASSWORD_CHANGE_REQUIRED` cho **mọi** đường trừ ba đường tối thiểu: xem thông
tin của chính mình, đổi mật khẩu, đăng xuất. Cho dùng tiếp khi chưa đổi là biến
mật khẩu tạm thành mật khẩu vĩnh viễn.

Đổi mật khẩu thành công sẽ thu hồi **mọi** phiên của tài khoản đó, kể cả phiên
đang thao tác — đổi mật khẩu thường là phản ứng khi nghi bị lộ, nên phiên của kẻ
đã chiếm được tài khoản phải chết theo. Giao diện tự đăng nhập lại bằng mật khẩu
vừa nhập để người dùng không phải gõ hai lần.

## Phân quyền

Mặc định **đóng**: `SecurityConfig` kết thúc bằng `anyRequest().denyAll()`. Một
endpoint mới quên khai quyền sẽ bị từ chối, không lọt ra công khai.

`EndpointAuthorizationIT` giữ một ma trận 29 tiền tố và kiểm hai điều:

1. Mọi endpoint đã đăng ký đều nằm trong ma trận.
2. **Cấm** khai lại dòng bao `/api/admin`.

Điều thứ hai tồn tại vì một dòng bao khiến mọi endpoint quản trị mới được che
miễn phí — và "được che miễn phí" nghĩa là không ai phải nghĩ về quyền của nó.
Liệt kê từng nhóm buộc người thêm endpoint phải quyết định.

## Mã đơn ≠ mã truy cập

`bookings.code` (ví dụ `TVH8F3K2Q`) được in trên thư, đọc qua điện thoại, và
**nằm trong nội dung chuyển khoản** — nên nó xuất hiện trên sao kê ngân hàng của
homestay và trong tin nhắn biến động số dư. Thứ đoán được và lộ ra như vậy không
được phép cấp quyền.

`bookings.access_token` là 32 ký tự hex từ `SecureRandom`, chỉ có trong liên kết
của thư xác nhận và trong phản hồi ngay sau khi tạo đơn.

| Thao tác | Cần gì |
|---|---|
| Tra cứu đơn | mã đơn **+** số điện thoại |
| Huỷ đơn | mã truy cập **hoặc** (mã đơn + số điện thoại) |
| Viết đánh giá | như huỷ đơn |
| Theo dõi thanh toán | **bắt buộc** mã truy cập |

Sai mã và sai số điện thoại trả **cùng một** lỗi `BOOKING_NOT_FOUND`. Phân biệt
hai câu là biến ô tra cứu thành công cụ dò xem mã nào có thật.

## Giới hạn tần suất sau nginx

Chín hạn mức, khoá theo IP hoặc theo định danh nghiệp vụ (email, số điện thoại,
mã đơn) — bảng đầy đủ ở [api.md](./api.md#giới-hạn-tần-suất).

Điểm dễ hỏng nhất không nằm ở hạn mức mà ở chỗ **lấy IP**. Sau nginx,
`request.getRemoteAddr()` trả IP container nginx cho mọi khách, và giới hạn 10
request/phút biến thành giới hạn toàn hệ thống: một vòng `curl` khoá tính năng
đăng nhập của tất cả mọi người. Hai thứ cùng phải đúng:

```yaml
# application.yml
server.forward-headers-strategy: framework
```

```nginx
# nginx: GHI ĐÈ, không nối thêm
proxy_set_header X-Forwarded-For $remote_addr;
```

Biến quen thuộc `$proxy_add_x_forwarded_for` **nối** giá trị khách tự gửi vào
đầu chuỗi. Spring lấy phần tử đầu tiên làm IP khách, nên kẻ tấn công chỉ cần gửi
kèm `X-Forwarded-For: 1.2.3.4` và đổi số mỗi lần là mỗi request có một khoá
riêng — giới hạn vô hiệu hoàn toàn. `$remote_addr` là IP của kết nối TCP, không
giả được bằng header.

Điều này chỉ an toàn vì **cổng 8080 không publish ra máy chủ**: nginx là đường
vào duy nhất tới API. Nếu mở cổng 8080, `forward-headers-strategy: framework`
trở thành lỗ hổng — ai gọi thẳng cũng tự khai IP được.

## Webhook thanh toán

Bốn lớp:

1. **Khoá API** trong header `Apikey`, so bằng hằng thời gian.
2. **Chống xử lý trùng** bằng `UNIQUE (provider, external_id)` — webhook gửi lại
   không cộng tiền hai lần.
3. **Đối chiếu nội dung chuyển khoản** với `transfer_content` (mã đơn + số thứ
   tự lần thanh toán), không chỉ so số tiền.
4. **Ghi nhật ký mọi sự kiện**, kể cả cái không khớp đơn nào. Không có nhánh nào
   để tiền biến mất im lặng.

## Nội dung HTML của CMS

HTML do quản trị viên soạn được lọc **ở tầng vào**, trước khi lưu — không lọc
lúc hiển thị, vì mỗi nơi hiển thị mới là một chỗ có thể quên.

Bộ lọc là OWASP `java-html-sanitizer` với danh sách thẻ **cho phép tường minh**:
`p h2 h3 h4 ul ol li strong em a img br blockquote`. Mọi thẻ khác bị gỡ. Liên
kết chỉ nhận `http`, `https`, `mailto` và đường dẫn nội bộ bắt đầu bằng `/`; ảnh
chỉ nhận nguồn khai trong `content.image-src-allowlist`. Liên kết ra ngoài được
gắn `rel="nofollow"`.

Danh sách cho phép, không phải danh sách cấm: danh sách cấm luôn thiếu một thứ
mà người viết chưa nghĩ tới.

**Văn bản do KHÁCH nhập** (tên, tiêu đề và nội dung đánh giá) thì ngược lại:
lưu **nguyên văn**, không escape lúc ghi, và phòng thủ ở tầng hiển thị bằng text
binding `{{ }}` của Angular. Escape ở tầng vào sẽ biến dấu `&` trong tên khách
thành `&amp;` trên màn hình — hỏng dữ liệu để giải quyết một vấn đề mà tầng hiển
thị vốn đã giải quyết đúng.

## Header bảo mật

nginx đặt cho toàn site: `X-Content-Type-Options: nosniff`,
`X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, và
một `Content-Security-Policy` với `script-src 'self'`, `frame-ancestors 'none'`.

Thư mục `/uploads` có thêm `Content-Security-Policy: sandbox; default-src 'none'`
— nếu một tệp lạ lọt được vào đó, trình duyệt vẫn không thực thi nó như HTML
trên chính tên miền của site.

## Ảnh tải lên

Ba lớp: kiểm kiểu MIME và phần mở rộng, **giải mã lại ảnh** rồi ghi ra tệp mới
(phá mọi payload giấu trong metadata), và đổi tên tệp thành UUID (không giữ tên
người dùng đặt). Trần 5MB khai ở **cả hai** chỗ —
`spring.servlet.multipart.max-file-size` và `storage.max-image-bytes` — vì mặc
định 1MB của Spring sẽ chặn ở tầng servlet trước khi lớp kiểm tra kịp chạy.

## Bí mật

Không biến bí mật nào có giá trị mặc định trong mã nguồn.
`RequiredSecretsValidator` dừng khởi động khi thiếu, **ở mọi profile**.
`.env.example` liệt kê tên biến với giá trị trống; `scripts/init-env.sh` là
đường duy nhất tạo ra giá trị thật.

Repo công khai, nên một giá trị mặc định "an toàn cho demo" nằm trong repo đồng
nghĩa với việc bất kỳ ai cũng tự ký được token `role=ADMIN` cho mọi bản triển
khai dùng repo này.

Mật khẩu quản trị demo sinh ngẫu nhiên lúc nạp dữ liệu mẫu và **chỉ** in ra log
container — không vào `.env`, không vào tài liệu, không vào commit.

---

## Giới hạn đã biết

Những điều dưới đây **chưa** được xử lý. Chúng nằm ngoài phạm vi một đồ án tốt
nghiệp nhưng phải xử lý trước khi hệ thống nhận tiền thật.

### 1. Giới hạn tần suất chỉ đúng với một instance

Bucket lưu trong bộ nhớ tiến trình. Chạy hai instance sau bộ cân bằng tải thì
hạn mức thực tế nhân đôi, và khởi động lại là xoá sạch bộ đếm.

*Khuyến nghị:* chuyển sang Redis (bucket4j có sẵn lớp tích hợp) khi triển khai
nhiều instance.

### 2. Webhook xác thực bằng khoá API tĩnh, không phải chữ ký

Khoá API trong header là bí mật dùng lại mãi. Ai đọc được nó — qua log của một
proxy trung gian chẳng hạn — có thể giả webhook và tự xác nhận đơn của mình.

*Khuyến nghị:* chuyển sang **HMAC-SHA256** ký trên thân request kèm dấu thời
gian (chống phát lại), cộng **whitelist IP** của nhà cung cấp. Cả hai đều là
thay đổi cục bộ trong `SepayWebhookController`.

### 3. Không có CSRF token

Hệ thống dựa vào `SameSite=Strict` cho cookie refresh và vào việc access token
đi bằng header `Authorization` (không tự động gửi kèm như cookie). Đủ cho mô
hình hiện tại, nhưng không còn đủ nếu về sau có endpoint nào nhận xác thực bằng
cookie.

### 4. Hoàn tiền là thao tác thủ công

Hệ thống chỉ **đánh dấu** khoản cần hoàn (`REFUND_REQUIRED`); việc chuyển tiền
làm ngoài hệ thống. Không có đối chiếu tự động giữa "đã đánh dấu hoàn" và "đã
thật sự hoàn".

*Khuyến nghị:* ghi lại mã giao dịch hoàn tiền vào `payments` khi đánh dấu
`RESOLVED`.

### 5. Không có nhật ký kiểm toán cho thao tác quản trị

`booking_status_history` ghi ai đổi trạng thái đơn, nhưng sửa loại phòng, sửa
giá, sửa nội dung CMS thì không để lại vết.

*Khuyến nghị:* một bảng `audit_log` chung, ghi từ một `@Aspect` trên các
controller quản trị.

### 6. Mật khẩu không có yêu cầu độ phức tạp ngoài độ dài

Tối thiểu 8 ký tự, không kiểm tra danh sách mật khẩu phổ biến.

*Khuyến nghị:* đối chiếu với danh sách rò rỉ (zxcvbn hoặc API của Have I Been
Pwned dùng k-anonymity).

### 7. Không có HTTPS trong bản đóng gói

`docker-compose.yml` phục vụ HTTP trần trên cổng 80. Cookie đặt `Secure` nên
trên môi trường thật **bắt buộc** phải có TLS, nếu không trình duyệt sẽ bỏ
cookie và không ai đăng nhập được.

*Khuyến nghị:* đặt một reverse proxy có chứng chỉ (Caddy, Traefik, hoặc nginx +
certbot) phía trước, hoặc chấm dứt TLS ở tầng hạ tầng.
