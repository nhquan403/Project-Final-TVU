# Thiết kế giao diện

Tài liệu này ghi lại các quyết định thị giác và tương tác của dự án, kèm lý do.
Mục đích: khi bị hỏi "vì sao làm thế này", có câu trả lời truy vết được, không
phải "vì thấy trang khác làm vậy".

## 1. Bảng màu là định nghĩa thương hiệu

Homestay TVH chưa có bộ nhận diện thương hiệu và chưa có logo. Bảng màu trong
[`frontend/src/styles/tokens.css`](../frontend/src/styles/tokens.css) **là**
định nghĩa thương hiệu chính thức của dự án, không phải màu tạm chờ thay.

Hướng: xanh rừng cho hành động, đất nung cho giá tiền, nền giấy ấm. Trang
`/ui-kit` vì thế đóng luôn vai trò tài liệu tham chiếu thương hiệu.

Nếu về sau có logo với màu chủ đạo khác: sửa đúng một file `tokens.css`, nhưng
phải **tính lại toàn bộ tỉ lệ tương phản**, không chỉ thay mã hex. Trang
`/ui-kit` đo tương phản lúc chạy nên sẽ báo ngay ô nào tụt dưới ngưỡng.

### Bảng token màu

Tỉ lệ tính bằng công thức luminance của WCAG 2.x.

| Token | Giá trị | Dùng cho | Tương phản |
|---|---|---|---|
| `--color-bg` | `#FBF9F6` | Nền trang | — |
| `--color-surface` | `#FFFFFF` | Thẻ, modal, bảng | — |
| `--color-surface-2` | `#F2EEE8` | Nền phụ, hàng bảng xen kẽ | — |
| `--color-text` | `#1C1917` | Chữ chính | 16.64:1 trên `bg` · 17.49:1 trên `surface` |
| `--color-text-muted` | `#6B625A` | Chữ phụ, nhãn | 5.68:1 trên `bg` · 5.97:1 trên `surface` |
| `--color-text-invert` | `#FFFFFF` | Chữ trên nền đậm | xem hai dòng `primary` |
| `--color-primary` | `#0F5C4C` | Hành động chính, header | 7.53:1 trên `bg`; chữ trắng trên nó 7.91:1 |
| `--color-primary-hover` | `#0B4B3E` | Trạng thái hover | chữ trắng trên nó 10.05:1 |
| `--color-price` | `#B03A0B` | Giá tiền | 5.78:1 trên `bg` · 6.08:1 trên `surface` |
| `--color-success` | `#15803D` | Đã xác nhận, đã thanh toán | 4.77:1 trên `bg` |
| `--color-warning` | `#9A5B08` | Chờ thanh toán, cần đối soát | 5.16:1 trên `bg` |
| `--color-danger` | `#B91C1C` | Huỷ, lỗi, hành động phá huỷ | 6.16:1 trên `bg`; chữ trắng trên nó 6.47:1 |
| `--color-focus` | `#1D4ED8` | Vòng focus | 6.38:1 trên `bg` |
| `--color-border` | `#E2DBD1` | Vạch phân cách trang trí | 1.31:1 |
| `--color-border-strong` | `#8C8071` | Viền ô nhập, viền nút | 3.67:1 |

Con số trong bảng này là bản chép tay để đọc offline. **Nguồn sự thật là trang
`/ui-kit`**, nơi tỉ lệ được đo lại từ token thật mỗi lần tải trang.

### Vì sao có hai token viền

WCAG 1.4.11 yêu cầu ranh giới của thành phần tương tác đạt tối thiểu 3:1. Một
viền hairline nhạt trông tinh tế nhưng làm ô nhập **biến mất** với người thị
lực kém. Vạch phân cách trang trí thì không chịu ràng buộc đó.

Nên: `--color-border` (1.31:1) **chỉ** cho vạch phân cách; `--color-border-strong`
(3.67:1) cho viền ô nhập và viền nút. Gộp hai token thành một là hỏng một trong
hai mục đích.

### Vì sao xoá bảng màu mặc định của Tailwind

`tokens.css` mở đầu bằng `--color-*: initial;`. Nếu chỉ *thêm* màu mới mà giữ
bảng mặc định, `text-blue-500` vẫn dùng được và lint chặn mã màu mất tác dụng —
màu lạ vẫn lọt vào qua đường class tiện ích.

Lint `frontend/scripts/check-hardcoded-colors.mjs` chặn đường còn lại: mã hex,
`rgb()`, `hsl()` viết thẳng trong `src/**/*.{ts,html,css}`. Chỉ `tokens.css`
được miễn. Hai lớp này bổ sung cho nhau, thiếu lớp nào cũng thủng.

## 2. Chuẩn tham chiếu — lấy gì, không lấy gì

Chuẩn tham chiếu là Booking.com và Agoda. Nhưng hai trang đó không phải cái gì
cũng đáng chép.

**Lấy từ Agoda:** khoảng trắng rộng và tương phản cao cho thông tin quan trọng;
làm mờ nền khi ô tìm kiếm được focus để dồn sự chú ý; thanh điều hướng dính khi
cuộn ở trang chi tiết; gallery một ảnh lớn cộng hai ảnh nhỏ.

**Lấy từ Booking:** hiển thị **tổng tiền cả kỳ nghỉ** ngay trên thẻ loại phòng
và ở mọi bước, không chỉ giá mỗi đêm. Đây đúng là điểm Agoda bị chê: chỉ hiện
giá/đêm rồi để tổng tiền lộ ra ở bước cuối, khiến khách thấy bị hớ đúng lúc sắp
trả tiền. Khối đánh giá đặt ngay dưới phần chọn phòng, nơi khách đang cân nhắc.

### Không lấy: nhóm gây áp lực giả

Thứ dễ nhận ra nhất ở hai trang này lại là nhóm tạo áp lực không kiểm chứng
được: "X người đang xem chỗ này", đồng hồ đếm ngược khuyến mãi không có thật,
"deal sắp hết hạn". Booking.com đã bị cơ quan quản lý châu Âu xử lý vì nhóm này.

Với một đồ án tốt nghiệp, chép chúng là tự đặt bẫy: hội đồng chỉ cần hỏi "con số
này lấy từ đâu?" là gãy.

Nhưng hệ thống này có một lợi thế mà việc chép không mang lại: **nó biết sự
thật**. Nên dùng đúng những pattern đó, ở phiên bản trung thực:

| Pattern của họ | Phiên bản của chúng ta | Nguồn dữ liệu |
|---|---|---|
| "Chỉ còn 1 phòng!" | "Còn 2 phòng cho khoảng ngày này" | `availableCount` từ truy vấn có ràng buộc `EXCLUDE` bảo chứng |
| Đồng hồ đếm ngược khuyến mãi bịa | Đồng hồ giữ chỗ 15 phút trên màn hình QR | `bookings.hold_expires_at` thật trong cơ sở dữ liệu |
| "X người đang xem" | **Không có.** Hệ thống không đo được, nên không nói | — |

**Nguyên tắc:** không hiển thị con số nào mà hệ thống không truy vết được về một
hàng trong cơ sở dữ liệu.

Hệ quả cụ thể trong mã: `ui-room-card` chỉ hiện "Còn N phòng" khi `N ≤ 3`, và N
luôn đến từ truy vấn tồn phòng. Không có biến đếm giả nào trong toàn bộ frontend.

## 3. Quy ước tương tác

### Focus

Một quy ước duy nhất, khai báo một lần trong `base.css`:

```css
:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
```

**Không có `outline: none` ở bất kỳ đâu trong dự án.** Bỏ vòng focus là cắt
đường đi của người dùng bàn phím và người dùng trình đọc màn hình.

Kiểm nhanh: mở `/ui-kit`, nhấn Tab một lượt hết trang. Mọi thành phần tương tác
phải có vòng focus nhìn thấy được.

### Vùng chạm

`--touch-min: 44px`. Mọi nút, ô nhập, ô chọn ngày đều đạt, kể cả khi trông nhỏ
hơn — nới bằng padding chứ không thu vùng chạm.

### Chữ không bao giờ dưới 16px trên mobile

`--text-body: 1rem`. Dưới ngưỡng này iOS tự phóng to trang khi người dùng chạm
vào ô nhập, và bố cục nhảy ngay giữa lúc đang điền form.

### Giảm chuyển động

Toàn bộ hiệu ứng bị tắt (không phải làm chậm) khi hệ điều hành bật "giảm chuyển
động". Người bật thiết lập này thường vì chuyển động gây chóng mặt hoặc buồn
nôn.

### Lỗi không bao giờ chỉ báo bằng màu

`ui-input` và `ui-select` ở trạng thái lỗi đổi **cả màu viền lẫn độ dày viền**,
kèm một dòng chữ dưới ô có `aria-describedby`. Chỉ đổi màu thì người mù màu
không phân biệt được.

### Chặn sẵn thay vì báo lỗi sau

`ui-date-range-picker` vô hiệu hoá ngày quá khứ và ngày hết phòng ngay trên
lịch. Cho chọn rồi mới báo "ngày này không đặt được" là một trong những trải
nghiệm khó chịu nhất trên các trang đặt phòng: người dùng đã đầu tư công sức vào
lựa chọn trước khi bị từ chối.

Khoảng ngày dùng quy ước **nửa mở `[nhận, trả)`**, khớp với ràng buộc `EXCLUDE`
trên `daterange` ở tầng cơ sở dữ liệu. Hệ quả nhìn thấy được: ngày trả phòng
không cần còn phòng, nên một ngày hết phòng vẫn chọn được làm ngày trả — chỉ
không chọn được làm đêm ở.

## 4. Thư viện component

15 component trong `frontend/src/app/shared/ui/`, mỗi cái là một standalone
component nhận dữ liệu qua signal input và **không tự gọi API**.

`ui-button` · `ui-input` · `ui-select` · `ui-date-range-picker` ·
`ui-guest-stepper` · `ui-room-card` · `ui-status-badge` · `ui-modal` ·
`ui-toast` · `ui-skeleton` · `ui-pagination` · `ui-data-table` ·
`ui-star-rating` · `ui-lightbox` · `ui-empty-state`

Phạm vi khoá cứng ở 15 component này. Màn hình nào cần thêm biến thể thì thêm
vào `shared/ui/` chứ không tự chế trong feature — một biến thể xuất hiện lần thứ
hai là dấu hiệu nó thuộc về thư viện dùng chung.

### Thứ tự thông tin trên thẻ loại phòng

Cố định, không để mỗi màn hình tự sắp:

ảnh → tên loại phòng → sức chứa và giường → 3 tiện ích nổi bật → giá mỗi đêm →
**tổng cả kỳ** → số phòng còn lại → nút đặt.

Ảnh có `width`/`height` và `loading="lazy"`. Thiếu kích thước thì lúc ảnh tải
xong bố cục nhảy, và cú bấm đang nhắm vào nút đặt rơi sang thẻ khác.

## 5. Trang `/ui-kit`

Render toàn bộ 15 component ở mọi trạng thái, kèm bảng tương phản đo lúc chạy.

Chỉ có ở bản `demo`. Ở bản production, `angular.json` thay
`ui-kit.routes.ts` bằng `ui-kit.routes.prod.ts` (route rỗng), nên lệnh
`import()` biến mất khỏi mã nguồn và trình đóng gói **không sinh ra chunk** của
trang này. Chỉ tắt bằng cờ `environment` là chưa đủ: lệnh import động vẫn nằm
đó, chunk vẫn được sinh, và ai đoán đúng tên file vẫn tải về được.

Kiểm chứng bằng đầu ra của `ng build`: bản `demo` có chunk `ui-kit`, bản
`production` không có.
