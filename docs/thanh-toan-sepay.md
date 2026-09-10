# Thanh toán qua SePay — vận hành và đối soát

Tài liệu này mô tả cách hệ thống nhận tiền, đối soát, và cách xử lý những khoản
tiền không tự khớp được. Nó viết cho người vận hành homestay và cho người bảo
trì mã nguồn.

## Cấu hình bắt buộc

| Biến môi trường | Vai trò | Có mặc định trong mã? |
|---|---|---|
| `SEPAY_WEBHOOK_API_KEY` | Xác thực webhook gửi tới | **Không.** Thiếu là ứng dụng không khởi động, ở mọi profile |
| `SEPAY_ACCOUNT_NUMBER` | Tài khoản nhận cọc; dùng cho cả ảnh QR lẫn lớp kiểm tra tài khoản đích | Không |
| `SEPAY_BANK_CODE` | Mã ngân hàng cho ảnh QR | Không |

Chưa cấu hình `SEPAY_ACCOUNT_NUMBER` thì hệ thống **từ chối mọi webhook**
(fail-closed) và màn hình thanh toán không hiện ảnh QR. Đây là chủ ý: mở sẵn
đường cho mọi giao dịch khi thiếu cấu hình là biến một lỗi triển khai thành một
lỗ hổng.

## Năm lớp kiểm tra một webhook phải đi qua

1. **Khoá xác thực.** Header `Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>`, so
   bằng `MessageDigest.isEqual` để thời gian trả lời không phụ thuộc số ký tự
   đầu đúng. Sai khoá → HTTP 401, và **không** ghi bản ghi nào.
2. **Chiều tiền.** Bắt buộc `transferType == "in"`. Một giao dịch chuyển **đi**
   có nội dung chứa mã đơn — chẳng hạn nhân viên hoàn tiền cho khách khác rồi gõ
   mã vào nội dung cho dễ đối soát — sẽ bị từ chối thay vì xác nhận đơn.
3. **Tài khoản đích.** `accountNumber` (hoặc `subAccount`) phải khớp
   `SEPAY_ACCOUNT_NUMBER`.
4. **Chống xử lý trùng, CÓ phân biệt kết quả.** Khoá `(provider, external_id)`.
   Bản ghi cũ mang `MATCHED`/`LATE`/`UNMATCHED`/`DUPLICATE` → trả 200 ngay. Mang
   `ERROR` → **xử lý lại**. Chỉ chèn khoá rồi bỏ qua mọi lần sau sẽ khiến một
   lỗi tạm thời khoá vĩnh viễn khoản tiền đó khỏi mọi lần thử lại.
5. **Khớp mã và số tiền.** Regex neo hai đầu `\bTVH[0-9A-HJKMNP-TV-Z]{6}\d{2}\b`
   trên `content`, và chỉ chấp nhận khi tìm được **đúng một** mã. `amount_received`
   được **cộng dồn**, so với `amount_expected` với dung sai ±1.000đ.

Phản hồi luôn là **HTTP 200 kèm thân `{"success": true}`** trừ trường hợp sai
khoá. SePay chỉ coi là giao thành công khi thấy đúng thân đó; trả 200 với thân
rỗng vẫn bị đưa vào hàng đợi gửi lại tới 7 lần trong 5 giờ.

## Bảng quyết định

| Tình huống | `payments.status` | `bookings.status` | Hành động |
|---|---|---|---|
| Đủ tiền, đơn còn `PENDING_PAYMENT`/`AWAITING_REVIEW` | `SUCCEEDED` | `CONFIRMED` | Xếp hàng thư xác nhận |
| Thiếu tiền (cộng dồn chưa đủ) | `PARTIAL` + `NEEDS_REVIEW` | `AWAITING_REVIEW` | Gia hạn giữ chỗ +24h, xếp hàng thư nhắc |
| Thừa tiền | `OVERPAID` + `REFUND_REQUIRED` | `CONFIRMED` | Hiện ở hàng đợi đối soát |
| Đủ tiền nhưng đơn đã `EXPIRED`/`CANCELLED` | `SUCCEEDED` | thử gán lại phòng | Gán được → `CONFIRMED` + thư; hết phòng → `AWAITING_REVIEW` + `NEEDS_REVIEW` |
| Không khớp mã, hoặc khớp nhiều mã | — | không đổi | `processing_result = UNMATCHED` |
| Sai `transferType`/`accountNumber` | — | không đổi | `processing_result = UNMATCHED` + log cảnh báo |

Không nhánh nào kết thúc bằng "tiền đã vào mà không ai biết": mọi webhook nhận
được đều để lại một dòng `payment_webhook_events` kèm nguyên văn payload, kể cả
khi không khớp đơn nào và kể cả khi thân request không đọc được.

## Bằng chứng đối soát nằm trong cơ sở dữ liệu

```sql
-- Những khoản cần người xử lý
SELECT p.transfer_content, p.amount_expected, p.amount_received,
       p.status, p.reconcile_status, b.code, b.status AS booking_status
  FROM payments p JOIN bookings b ON b.id = p.booking_id
 WHERE p.reconcile_status <> 'NONE';

-- Webhook chưa xử lý xong (sẽ được xử lý lại khi SePay gửi lại)
SELECT external_id, processing_result, error_message, received_at
  FROM payment_webhook_events
 WHERE processing_result = 'ERROR';

-- Thư đã gửi hay chưa
SELECT template, status, attempts, last_error, sent_at
  FROM outbound_emails ORDER BY id DESC LIMIT 20;
```

## Email đi qua hộp thư đi, không gửi trực tiếp

Thư được ghi vào `outbound_emails` **trong cùng transaction** với việc xác nhận
đơn, rồi `EmailDispatchScheduler` mới gửi mỗi 30 giây. Nhờ vậy webhook trả lời
ngay thay vì chờ SMTP, và câu hỏi "thư này đã gửi chưa" trả lời được bằng SQL.
Quá 5 lần thử thì dòng thư chuyển `FAILED` và hiện ra cho người xử lý.

Chưa cấu hình `MAIL_HOST` thì hệ thống ghi **toàn bộ nội dung thư ra log** thay
vì gửi SMTP, và vẫn cập nhật `outbound_emails` như thường. Bản đem đi trình diễn
vì thế vẫn chứng minh được tiêu chí "khách nhận được thư xác nhận".

## Endpoint mô phỏng chuyển khoản

`POST /api/admin/dev/payments/{transferContent}/simulate-transfer`

Ba rào chắn cùng lúc, phải đủ cả ba thì endpoint mới tồn tại:

1. Đường dẫn dưới `/api/admin/**` → cần vai trò `ADMIN`.
2. Cờ `payments.simulator.enabled` — **mặc định `false` ở mọi profile**.
3. Không nạp ở profile `prod`.

Chỉ dùng `@Profile("dev")` là không đủ và sai theo hướng nguy hiểm nhất: dự án
chỉ có hai profile, bản `docker compose` chạy `demo`, nên endpoint sẽ có mặt ở
đúng bản đem đi trình diễn.

## Khuyến nghị nâng cấp bảo mật (chưa bắt buộc)

SePay hỗ trợ hai cơ chế mạnh hơn khoá tĩnh, nhưng cả hai phụ thuộc gói tài
khoản nên **không** được đặt làm yêu cầu chặn của hệ thống:

- **HMAC-SHA256 trên thân request.** Chống được cả trường hợp khoá bị lộ qua
  log của một proxy trung gian, vì chữ ký gắn với nội dung từng request thay vì
  là một chuỗi cố định lặp lại ở mọi lần gọi.
- **Whitelist IP nguồn** ở tầng nginx. Rẻ, không đụng vào mã ứng dụng, và chặn
  ngay ở vòng ngoài trước khi request tới được tầng ứng dụng.

Khi bật, cả hai nên là lớp **thêm vào**, không thay thế lớp khoá hiện có: nhà
cung cấp đổi dải IP mà không báo là chuyện đã xảy ra.
