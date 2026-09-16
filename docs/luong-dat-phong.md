# Luồng đặt phòng và thanh toán

## Đặt phòng — từ lúc chọn ngày tới lúc có mã QR

```mermaid
sequenceDiagram
    autonumber
    actor K as Khách
    participant W as Web (Angular)
    participant A as API
    participant DB as PostgreSQL

    K->>W: chọn ngày, số khách
    W->>A: GET /api/availability
    A->>DB: đếm phòng vừa khả dụng vừa rảnh
    DB-->>A: danh sách loại phòng + tổng tiền cả kỳ
    A-->>W: kết quả
    W-->>K: thẻ phòng kèm "còn N phòng"

    K->>W: chọn loại phòng, nhập thông tin
    Note over W,A: Hỏi LẠI phòng trống ở bước cuối —<br/>kết quả bước trước có thể đã cũ vài phút
    W->>A: GET /api/availability (lần hai)

    K->>W: bấm Đặt phòng
    W->>A: POST /api/bookings

    rect rgb(240, 238, 232)
    Note over A,DB: MỘT giao dịch
    A->>DB: chọn phòng vật lý còn rảnh
    A->>DB: INSERT booking_rooms
    Note right of DB: EXCLUDE USING gist bác ngay<br/>nếu ai đó vừa lấy mất (23P01)
    A->>DB: INSERT bookings (PENDING_PAYMENT, hạn 15 phút)
    A->>DB: INSERT payments + nội dung chuyển khoản
    A->>DB: INSERT outbound_emails
    end

    A-->>W: mã đơn + mã truy cập + thông tin QR
    W-->>K: màn hình QR, đếm ngược 15 phút
```

Nếu bước ghi `booking_rooms` bị ràng buộc bác, hệ thống thử phòng còn lại **trong
một giao dịch mới** — PostgreSQL huỷ cả giao dịch khi ràng buộc bị vi phạm, nên
thử tiếp trong cùng giao dịch chỉ nhận `SQLSTATE 25P02`. Hết phòng thật thì trả
`ROOM_NOT_AVAILABLE`.

## Thanh toán và webhook

```mermaid
sequenceDiagram
    autonumber
    actor K as Khách
    participant W as Web
    participant NH as Ngân hàng
    participant SP as SePay
    participant A as API
    participant DB as PostgreSQL

    K->>NH: chuyển khoản đúng số tiền và nội dung
    NH-->>SP: biến động số dư
    SP->>A: POST /api/payments/webhook/sepay (Apikey)

    A->>A: xác thực khoá API
    A->>DB: INSERT payment_webhook_events
    Note right of DB: UNIQUE (provider, external_id)<br/>webhook gửi lại không xử lý hai lần

    A->>DB: tìm payment theo transfer_content
    A->>DB: cộng dồn amount_received

    alt Đủ tiền
        A->>DB: booking -> CONFIRMED, payment_status DEPOSIT_PAID
        A->>DB: INSERT outbound_emails (thư xác nhận)
    else Thiếu tiền
        A->>DB: booking -> AWAITING_REVIEW, gia hạn giữ chỗ 24h
        A->>DB: payment.reconcile_status = NEEDS_REVIEW
    else Thừa tiền
        A->>DB: booking -> CONFIRMED
        A->>DB: payment.reconcile_status = REFUND_REQUIRED
    end

    A-->>SP: 200

    loop mỗi 3 giây, tối đa 15 phút
        W->>A: GET /api/bookings/{code}/payment-status?token=...
        A-->>W: trạng thái hiện tại
    end
    W-->>K: tự chuyển sang trang hoàn tất khi CONFIRMED
```

### Nhánh tiền về muộn

Cửa sổ tranh chấp là có thật: ngân hàng → nhà cung cấp → API trễ vài chục giây,
còn đồng hồ đếm ngược lại đẩy khách bấm chuyển khoản vào đúng phút cuối.

```mermaid
sequenceDiagram
    autonumber
    participant S as Bộ quét hết hạn
    participant DB as PostgreSQL
    participant SP as SePay
    participant A as API

    S->>DB: booking quá hạn -> EXPIRED, nhả phòng
    Note over DB: phòng đã mở lại cho khách khác

    SP->>A: webhook (tiền của khách vừa về)
    A->>DB: đơn đang EXPIRED
    A->>DB: EXPIRED -> AWAITING_REVIEW
    A->>DB: thử gán lại phòng

    alt Gán lại được
        A->>DB: AWAITING_REVIEW -> CONFIRMED
    else Không còn phòng
        A->>DB: reconcile_status = REFUND_REQUIRED
        Note over DB: vào hàng đợi đối soát,<br/>KHÔNG im lặng bỏ qua
    end
```

`CANCELLED` và `EXPIRED` có **đúng một** đường ra: sang `AWAITING_REVIEW`, và chỉ
khi tiền của khách về sau khi đơn đã đóng. Không mở thẳng sang `CONFIRMED` vì
phòng đã được nhả cho khách khác — phải qua `AWAITING_REVIEW` rồi mới thử gán
lại.

## Biểu đồ trạng thái đơn — tám trạng thái

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: khách tạo đơn

    PENDING_PAYMENT --> CONFIRMED: đủ tiền cọc
    PENDING_PAYMENT --> AWAITING_REVIEW: thiếu tiền
    PENDING_PAYMENT --> EXPIRED: quá 15 phút
    PENDING_PAYMENT --> CANCELLED: khách huỷ

    AWAITING_REVIEW --> CONFIRMED: đối soát xong / bù đủ
    AWAITING_REVIEW --> CANCELLED: khách huỷ

    CONFIRMED --> CHECKED_IN: khách nhận phòng
    CONFIRMED --> CANCELLED: huỷ trước ngày ở
    CONFIRMED --> NO_SHOW: không đến

    CHECKED_IN --> CHECKED_OUT: trả phòng
    CHECKED_OUT --> [*]

    CANCELLED --> AWAITING_REVIEW: tiền về sau khi đóng
    EXPIRED --> AWAITING_REVIEW: tiền về sau khi đóng

    NO_SHOW --> [*]
```

**Ba trạng thái nhả phòng:** `CANCELLED`, `EXPIRED`, `NO_SHOW` — những chuyến
**không diễn ra**. Các dòng `booking_rooms` chuyển sang `RELEASED` và lượt khuyến
mãi được hoàn.

**`CHECKED_OUT` cố ý KHÔNG nhả phòng.** Khách đã ở thật, nên những dòng
`booking_rooms` của đơn đó là bằng chứng lịch sử về số đêm-phòng đã bán, không
phải chỗ đang bị giữ. Nhả chúng gây hỏng theo hai hướng cùng lúc: ràng buộc
`assert_booking_room_count` bác ngay (danh sách miễn trừ không có `CHECKED_OUT`),
và tỉ lệ lấp đầy của mọi chuyến đã hoàn tất về 0. Không nhả cũng không giam
phòng: ràng buộc chống trùng chỉ so **khoảng ngày**, mà khoảng ngày của một
chuyến đã trả phòng nằm trong quá khứ nên không chặn ai.

`BookingStateMachine` là nơi duy nhất thực hiện những bước chuyển này. Mỗi lần
chuyển kéo theo ba việc phụ mà rải rác ở controller thì chắc chắn có chỗ quên:
ghi nhật ký, nhả phòng khi cần, hoàn lượt khuyến mãi. Quên việc thứ hai là giam
phòng vĩnh viễn; quên việc thứ ba là đốt oan lượt của mã giảm giá.
