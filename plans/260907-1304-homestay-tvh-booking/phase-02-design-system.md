---
title: "Phase 2: Design system & UI kit"
status: done
phase: 2
priority: P1
effort: "12h"
dependencies: [1]
---

# Phase 2: Design system & UI kit

## Overview

Xây bộ ngôn ngữ thị giác và thư viện component trước khi có màn hình nào. Mọi phase frontend sau này lắp ráp từ đây, không tự chế biến thể riêng.

**Chạy song song được với Phase 3 và Phase 4** — đây là công việc frontend thuần, không cần cơ sở dữ liệu, không cần API. Đó cũng là lý do nó nằm ở vị trí thứ hai: nó là phần duy nhất trong dự án có thể tiến hành trong khi backend còn đang dựng schema.

## Requirements

- Functional: bộ design token, 15 component với đầy đủ trạng thái, trang `/ui-kit` render tất cả.
- Non-functional: không màu nào trong toàn bộ frontend nằm ngoài bảng token; mọi cặp màu chữ/nền đạt WCAG AA; mọi thành phần tương tác có `focus-visible` nhìn thấy được; vùng chạm trên mobile ≥ 44px.

## Architecture

### Nguyên tắc thiết kế — lấy gì, không lấy gì

Chuẩn tham chiếu là Booking.com và Agoda. Nhưng hai trang đó không phải cái gì cũng đáng chép.

**Lấy từ Agoda:** khoảng trắng rộng và tương phản cao cho thông tin quan trọng — bố cục của họ dễ quét mắt hơn hẳn Booking, vốn bị chê là dày đặc và thiếu khoảng thở. Ô tìm kiếm khi focus thì làm mờ nền để dồn sự chú ý. Thanh điều hướng dính khi cuộn ở trang chi tiết. Gallery hiện 1 ảnh lớn + 2 ảnh nhỏ thay vì một ảnh đơn.

**Lấy từ Booking:** hiển thị **tổng tiền cả kỳ nghỉ** ngay trên thẻ loại phòng và ở mọi bước, không chỉ giá mỗi đêm. Đây chính là điểm Agoda bị chê: họ chỉ hiện giá/đêm rồi để tổng tiền lộ ra ở bước cuối, khiến khách thấy bị hớ. Khối đánh giá đặt ngay dưới phần chọn phòng, nơi khách đang cân nhắc.

**Không lấy — và thay bằng gì.** Thứ dễ nhận ra nhất ở hai trang này lại là nhóm gây áp lực giả: "X người đang xem chỗ này", đồng hồ đếm ngược khuyến mãi không có thật, "deal sắp hết hạn". Booking.com đã bị cơ quan quản lý châu Âu xử lý vì nhóm này. Với một đồ án tốt nghiệp, chép chúng là tự đặt bẫy: hội đồng chỉ cần hỏi "con số này lấy từ đâu?" là gãy.

Nhưng hệ thống này có một lợi thế mà việc chép không mang lại: **nó biết sự thật**. Nên dùng đúng những pattern đó, ở phiên bản trung thực:

| Pattern của họ | Phiên bản của chúng ta | Nguồn dữ liệu |
|---|---|---|
| "Chỉ còn 1 phòng!" (thường không kiểm chứng được) | "Còn 2 phòng cho khoảng ngày này" | `availableCount` từ truy vấn có ràng buộc `EXCLUDE` bảo chứng (Phase 5) |
| Đồng hồ đếm ngược khuyến mãi bịa | Đồng hồ giữ chỗ 15 phút trên màn hình QR | `bookings.hold_expires_at` thật trong DB (Phase 5, Phase 6) |
| "X người đang xem" | **Không có.** Hệ thống không đo được, nên không nói | — |

Nguyên tắc: **không hiển thị con số nào mà hệ thống không truy vết được về một hàng trong cơ sở dữ liệu.** Ghi vào `docs/thiet-ke-giao-dien.md` kèm lý do, để trả lời được khi bị hỏi.

### Design token

Khai báo một lần trong `frontend/src/styles/tokens.css` dưới dạng CSS custom property, rồi ánh xạ vào cấu hình Tailwind. Component chỉ đọc token, **không** viết mã màu trực tiếp.

Homestay TVH chưa có bộ nhận diện thương hiệu, và người dùng đã chốt dùng bảng màu dưới đây. Nên `tokens.css` **là định nghĩa thương hiệu của dự án**, không phải màu tạm chờ thay: xanh rừng cho hành động, đất nung cho giá tiền, nền giấy ấm. Trang `/ui-kit` vì thế đóng luôn vai trò tài liệu tham chiếu thương hiệu.

**Màu — kèm tỉ lệ tương phản đã tính, không phải ước lượng**

| Token | Giá trị | Dùng cho | Tương phản |
|---|---|---|---|
| `--c-bg` | `#FBF9F6` | Nền trang | — |
| `--c-surface` | `#FFFFFF` | Thẻ, modal, bảng | — |
| `--c-surface-2` | `#F2EEE8` | Nền phụ, hàng bảng xen kẽ | — |
| `--c-text` | `#1C1917` | Chữ chính | 16.64:1 trên `bg` · 17.49:1 trên `surface` |
| `--c-text-muted` | `#6B625A` | Chữ phụ, nhãn | 5.68:1 trên `bg` · 5.97:1 trên `surface` |
| `--c-primary` | `#0F5C4C` | Hành động chính, header | 7.53:1 trên `bg`; chữ trắng trên nó 7.91:1 |
| `--c-primary-hover` | `#0B4B3E` | Trạng thái hover | chữ trắng trên nó 10.05:1 |
| `--c-price` | `#B03A0B` | Giá tiền | 5.78:1 trên `bg` · 6.08:1 trên `surface` |
| `--c-success` | `#15803D` | Đã xác nhận, đã thanh toán | 4.77:1 trên `bg` |
| `--c-warning` | `#9A5B08` | Chờ thanh toán, cần đối soát | 5.16:1 trên `bg` |
| `--c-danger` | `#B91C1C` | Huỷ, lỗi, hành động phá huỷ | 6.16:1 trên `bg`; chữ trắng trên nó 6.47:1 |
| `--c-focus` | `#1D4ED8` | Vòng focus | 6.38:1 trên `bg` |
| `--c-border` | `#E2DBD1` | Đường kẻ trang trí | 1.31:1 — **chỉ dùng cho vạch phân cách, không dùng cho viền ô nhập** |
| `--c-border-strong` | `#8C8071` | Viền ô nhập, viền nút | 3.67:1 — đạt WCAG 1.4.11 cho thành phần tương tác |

Hai token viền là chủ ý. WCAG 1.4.11 yêu cầu ranh giới của thành phần tương tác đạt tối thiểu 3:1; một viền hairline nhạt trông tinh tế nhưng làm ô nhập biến mất với người thị lực kém. Vạch phân cách trang trí thì không chịu ràng buộc đó.

**Thang chữ** (`clamp()` để co giãn theo màn hình)

| Token | Desktop | Mobile | Dùng cho |
|---|---|---|---|
| `--fs-display` | 44px | 32px | Tiêu đề hero |
| `--fs-h1` | 32px | 26px | Tiêu đề trang |
| `--fs-h2` | 24px | 21px | Tiêu đề khối |
| `--fs-h3` | 19px | 17px | Tiêu đề thẻ |
| `--fs-body` | 16px | 16px | Nội dung — **không bao giờ dưới 16px trên mobile** (dưới ngưỡng này iOS tự phóng to khi focus ô nhập) |
| `--fs-sm` | 14px | 14px | Chữ phụ |
| `--fs-xs` | 12px | 12px | Nhãn, badge |

**Khoảng cách** — thang 4px: `--sp-1` 4px, `--sp-2` 8px, `--sp-3` 12px, `--sp-4` 16px, `--sp-5` 20px, `--sp-6` 24px, `--sp-8` 32px, `--sp-10` 40px, `--sp-12` 48px, `--sp-16` 64px, `--sp-20` 80px.

**Bo góc:** `--r-sm` 4px, `--r-md` 8px, `--r-lg` 12px, `--r-full` 9999px.

**Đổ bóng:** `--sh-1` (thẻ nghỉ), `--sh-2` (thẻ hover), `--sh-3` (modal, thanh dính). Ba mức, không hơn.

**Vùng chạm:** `--touch-min: 44px` — mọi nút, ô nhập, ô chọn ngày trên mobile đều phải đạt, kể cả khi nhìn nhỏ hơn (dùng padding hoặc pseudo-element mở rộng vùng chạm).

**Breakpoint:** `sm` 640px, `md` 768px, `lg` 1024px, `xl` 1280px. Thiết kế từ 360px lên.

**Chuyển động:** `--dur-fast` 120ms (hover, focus), `--dur-base` 200ms (mở/đóng, trượt), `--dur-slow` 320ms (modal, lightbox). Easing `--ease-out: cubic-bezier(.2,.8,.3,1)`. Toàn bộ bọc trong:

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: .01ms !important;
    transition-duration: .01ms !important;
  }
}
```

### Component inventory — 15 component, mỗi cái đủ trạng thái

Không component nào coi là xong khi thiếu một ô trong bảng này. Ô `—` nghĩa là trạng thái đó không tồn tại cho component đó.

| Component | default | hover | focus-visible | active | disabled | loading | error | empty |
|---|---|---|---|---|---|---|---|---|
| `ui-button` (primary/secondary/ghost) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ spinner trong nút, giữ nguyên bề rộng | — | — |
| `ui-input` | ✓ | ✓ | ✓ | — | ✓ | — | ✓ viền `danger` + thông báo dưới ô | — |
| `ui-select` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ "Không có lựa chọn" |
| `ui-date-range-picker` | ✓ | ✓ ngày | ✓ | ✓ | ✓ ngày quá khứ và ngày hết phòng | ✓ khi đang tải lịch giá | ✓ | — |
| `ui-guest-stepper` | ✓ | ✓ | ✓ | ✓ | ✓ ở biên min/max | — | ✓ vượt sức chứa | — |
| `ui-room-card` | ✓ | ✓ nâng bóng | ✓ | — | ✓ hết phòng: mờ 60%, không bấm được | ✓ skeleton | — | — |
| `ui-status-badge` | ✓ 8 trạng thái booking | — | — | — | — | — | — | — |
| `ui-modal` | ✓ | — | ✓ bẫy focus bên trong | — | — | ✓ | ✓ | — |
| `ui-toast` | ✓ 4 loại | ✓ tạm dừng tự đóng | ✓ | — | — | — | — | — |
| `ui-skeleton` | ✓ 3 hình dạng: dòng, thẻ, bảng | — | — | — | — | — | — | — |
| `ui-pagination` | ✓ | ✓ | ✓ | ✓ trang hiện tại | ✓ ở trang đầu/cuối | ✓ | — | ✓ ẩn khi chỉ 1 trang |
| `ui-data-table` | ✓ | ✓ hàng | ✓ ô | ✓ cột đang sắp xếp | — | ✓ skeleton hàng | ✓ | ✓ có hướng dẫn hành động |
| `ui-star-rating` | ✓ đọc | ✓ nhập | ✓ | ✓ | ✓ | — | ✓ chưa chọn sao | — |
| `ui-lightbox` | ✓ | ✓ | ✓ điều hướng bàn phím | ✓ | ✓ ở ảnh đầu/cuối | ✓ | ✓ ảnh hỏng | — |
| `ui-empty-state` | ✓ icon + câu giải thích + nút hành động | — | — | — | — | — | — | — |

Trạng thái `focus-visible` dùng `outline: 2px solid var(--c-focus); outline-offset: 2px` thống nhất — không dùng `outline: none` ở bất kỳ đâu.

### Trang `/ui-kit`

Route chỉ nạp ở profile `demo`, render toàn bộ bảng trên: mỗi component, mỗi trạng thái, cạnh nhau. Ba công dụng:

1. Phát triển: sửa token thấy ngay ảnh hưởng lên tất cả.
2. Kiểm chứng: đi Tab một lượt qua trang là kiểm được toàn bộ `focus-visible`.
3. Bảo vệ đồ án: mở một trang là trình bày được cả hệ thống thiết kế.

Kèm một khối hiển thị bảng màu với tỉ lệ tương phản tính **tại thời điểm chạy** (không hard-code lại con số), để nếu ai đổi token mà tương phản tụt xuống dưới AA thì thấy ngay.

## Related Code Files

- Create: `frontend/src/styles/tokens.css` — nguồn duy nhất của mọi token
- Create: `frontend/src/styles/base.css` — reset, typography, `prefers-reduced-motion`
- Modify: `frontend/tailwind.config.js` — ánh xạ token vào theme Tailwind
- Create: `frontend/src/app/shared/ui/button/`, `input/`, `select/`, `date-range-picker/`, `guest-stepper/`, `room-card/`, `status-badge/`, `modal/`, `toast/`, `skeleton/`, `pagination/`, `data-table/`, `star-rating/`, `lightbox/`, `empty-state/`
- Create: `frontend/src/app/shared/ui/index.ts` — barrel export
- Create: `frontend/src/app/shared/a11y/focus-trap.directive.ts`, `esc-close.directive.ts`
- Create: `frontend/src/app/shared/pipes/vnd-currency.pipe.ts`, `night-count.pipe.ts`
- Create: `frontend/src/app/features/ui-kit/` — trang `/ui-kit` (lazy, chỉ nạp ở `demo`)
- Create: `frontend/src/app/shared/ui/contrast.util.ts` — tính tỉ lệ tương phản cho trang ui-kit
- Create: `frontend/scripts/check-hardcoded-colors.mjs` — lint chặn mã màu ngoài token
- Create: `docs/thiet-ke-giao-dien.md` — nguyên tắc, bảng token, quyết định không dùng dark pattern

## Implementation Steps

1. Viết `tokens.css` với đúng bảng token ở trên. Mỗi màu kèm comment ghi tỉ lệ tương phản đã tính và cặp nền tương ứng.
2. Ánh xạ token vào `tailwind.config.js` để dùng được cả `var(--c-primary)` lẫn class Tailwind. Xoá bảng màu mặc định của Tailwind khỏi theme — còn nó thì `text-blue-500` vẫn dùng được và token mất tác dụng.
3. `base.css`: reset, `font-size: 16px` cho ô nhập trên mobile, `focus-visible` toàn cục, khối `prefers-reduced-motion`.
4. Viết 15 component theo bảng inventory. Mỗi component là một standalone component, nhận input qua signal, không tự gọi API.
5. `ui-date-range-picker` là component khó nhất, làm kỹ:
   - Nhận vào bản đồ `{ngày: {giá, còn phòng}}` để **chặn sẵn** ngày hết phòng thay vì cho chọn rồi mới báo lỗi. Chọn một ngày rồi phát hiện không đặt được là một trong những trải nghiệm khó chịu nhất trên các trang đặt phòng.
   - Hiện giá từng đêm ngay dưới số ngày, như Booking và Airbnb làm.
   - Highlight hôm nay để người dùng có mốc tham chiếu.
   - Khi đã chọn xong khoảng: hiện tổng số đêm.
   - Bàn phím: mũi tên di chuyển ngày, `Enter` chọn, `Esc` đóng, `PageUp/PageDown` đổi tháng.
   - Mobile: một tháng mỗi lần, ô ngày ≥ 44px; desktop: hai tháng cạnh nhau.
6. `ui-room-card` theo đúng thứ tự thông tin đã chốt: ảnh → tên loại phòng → sức chứa và giường → 3 tiện ích nổi bật → giá/đêm → **tổng cả kỳ** → số phòng còn lại → nút đặt. Ảnh `loading="lazy"` và có `width`/`height` để không nhảy layout.
7. `ui-status-badge` phủ đủ 8 trạng thái booking của Phase 3, mỗi trạng thái một màu cố định lấy từ token — không để mỗi màn hình tự chọn màu riêng.
8. `ui-modal`: bẫy focus bên trong, `Esc` đóng, trả focus về phần tử đã mở nó, khoá cuộn nền, `aria-modal` + `aria-labelledby`.
9. `ui-toast`: `aria-live="polite"` cho thông báo thường, `assertive` cho lỗi; dừng đếm giờ tự đóng khi hover hoặc focus.
10. Viết `check-hardcoded-colors.mjs`: quét `src/**/*.{ts,html,css}` tìm `#rrggbb`, `rgb(`, `hsl(` ngoài `tokens.css`; có mã màu lạ thì thoát khác 0. Gắn vào `npm run lint`.
11. Dựng trang `/ui-kit` render toàn bộ inventory, kèm bảng màu tính tương phản lúc chạy.
12. Viết `docs/thiet-ke-giao-dien.md`: nguyên tắc, bảng token kèm số đo tương phản, bảng "lấy gì / không lấy gì" ở trên, lý do từ chối dark pattern, và ghi rõ đây là bộ màu thương hiệu chính thức của dự án (chưa có logo nên bảng token giữ vai trò đó).

## Verify

```bash
cd frontend
npm run build && npx ng lint
node scripts/check-hardcoded-colors.mjs        # thoát 0 = không có mã màu ngoài token

npm start
# Mở http://localhost:4200/ui-kit và kiểm tay:
#  1. Đi Tab một lượt toàn trang — MỌI thành phần tương tác phải có vòng focus nhìn thấy
#  2. Bảng màu hiển thị tỉ lệ tương phản, không ô nào dưới 4.5:1 cho chữ thường
#  3. Thu cửa sổ về 360px — không cuộn ngang
#  4. Bật "Giảm chuyển động" trong hệ điều hành, tải lại — không còn hiệu ứng nào chạy
#  5. Date picker: ngày quá khứ và ngày hết phòng không bấm được; mũi tên bàn phím di chuyển được
#  6. Zoom trình duyệt 200% — bố cục không vỡ, không mất nội dung
```

Đo vùng chạm bằng DevTools: chọn nút bất kỳ ở khung 360px, hộp bao phải ≥ 44×44px.

## Todo

- [x] `tokens.css` đầy đủ 5 nhóm token, mỗi màu ghi kèm tỉ lệ tương phản
- [x] Ánh xạ token vào Tailwind và **xoá bảng màu mặc định** khỏi theme
- [x] `base.css` + `focus-visible` toàn cục + `prefers-reduced-motion`
- [x] `ui-button` 3 cấp, đủ 6 trạng thái
- [x] `ui-input`, `ui-select` đủ trạng thái lỗi và disabled
- [x] `ui-date-range-picker`: chặn ngày hết phòng, giá từng đêm, điều hướng bàn phím, 1 tháng trên mobile
- [x] `ui-guest-stepper` có biên min/max
- [x] `ui-room-card` đúng thứ tự thông tin, có trạng thái hết phòng
- [x] `ui-status-badge` phủ đủ 8 trạng thái booking
- [x] `ui-modal` bẫy focus, `Esc`, trả focus
- [x] `ui-toast` với `aria-live`
- [x] `ui-skeleton` 3 hình dạng
- [x] `ui-pagination`, `ui-data-table` (có trạng thái rỗng kèm hành động)
- [x] `ui-star-rating`, `ui-lightbox` (điều hướng bàn phím), `ui-empty-state`
- [x] `vnd-currency` và `night-count` pipe
- [x] `check-hardcoded-colors.mjs` gắn vào `npm run lint`
- [x] Trang `/ui-kit` chỉ nạp ở profile `demo`, kèm bảng tương phản tính lúc chạy
- [x] `docs/thiet-ke-giao-dien.md`

## Success Criteria

- [x] `check-hardcoded-colors.mjs` thoát 0 — không mã màu nào nằm ngoài `tokens.css`
- [x] Trang `/ui-kit` render **đủ 15 component ở mọi trạng thái** đã khai báo trong bảng inventory
- [x] Đi Tab hết trang `/ui-kit`: mọi thành phần tương tác có vòng focus nhìn thấy được
- [x] Bảng tương phản trên `/ui-kit` không ô nào dưới 4.5:1 với chữ thường, dưới 3:1 với viền tương tác
- [x] Ở 360px không có cuộn ngang; mọi nút và ô nhập có hộp bao ≥ 44×44px
- [x] Bật "giảm chuyển động" của hệ điều hành → không hiệu ứng nào chạy
- [x] Date picker không cho chọn ngày quá khứ và ngày đã hết phòng; di chuyển được bằng mũi tên
- [x] Zoom 200% không vỡ bố cục
- [x] Không có `outline: none` nào trong toàn bộ mã nguồn
- [x] `docs/thiet-ke-giao-dien.md` nêu rõ quyết định không dùng dark pattern kèm lý do

## Risk Assessment

| Rủi ro | Dấu hiệu | Phản ứng đã định |
|---|---|---|
| Làm design system quá đà, thành mục tiêu tự thân | Hết 12h mà chưa có component nào dùng được vào màn hình thật | Phạm vi khoá cứng ở 15 component trong bảng. Cần thêm component thì phase sau tự thêm vào `shared/ui/`, không mở rộng phase này |
| Component viết ra rồi mỗi màn hình vẫn tự chế biến thể | Xuất hiện `<button class="...">` thô trong feature | Lint chặn mã màu là lớp một; code review là lớp hai. Nếu một biến thể xuất hiện lần thứ hai thì đưa vào `shared/ui/` chứ không nhân bản |
| `ui-date-range-picker` ngốn hết ngân sách phase | Hết 6h mà picker vẫn chưa xong | Làm bản tối thiểu trước (chọn khoảng, chặn ngày quá khứ), giá từng đêm và điều hướng bàn phím thêm sau — nhưng **chặn ngày hết phòng là bắt buộc**, không lược |
| Token khai báo rồi nhưng Tailwind vẫn cho dùng màu mặc định | `text-blue-500` vẫn chạy | Xoá `colors` mặc định khỏi theme ở bước 2, không chỉ thêm màu mới |
| Bảng màu đẹp trên màn hình đang làm, xấu trên máy chiếu buổi bảo vệ | Màu nhạt biến mất khi chiếu | Tương phản tối thiểu 4.5:1 đã bao hàm phần lớn rủi ro này; kiểm thêm một lần trên máy chiếu nếu mượn được |
| Sau này có logo với màu chủ đạo khác hẳn | Bộ nhận diện mới không hợp với giao diện đã dựng | Mọi màu nằm trong `tokens.css` và có lint chặn mã màu rải rác, nên đổi thương hiệu = sửa một file. Nhưng phải **tính lại tương phản** cho bảng màu mới, không chỉ thay mã hex |

**Rollback:** revert commit; các phase backend (3, 4, 5, 6) không phụ thuộc phase này nên vẫn chạy tiếp được. Chỉ Phase 7 và Phase 8 phải chờ.
