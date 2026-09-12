import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  AvailabilityService,
  type RoomTypeAvailability,
} from '../../../core/services/availability.service';
import { AuthService } from '../../../core/services/auth.service';
import { BookingService } from '../../../core/services/booking.service';
import {
  PromotionService,
  type CheckPromotionResult,
} from '../../../core/services/promotion.service';
import { UiButton, UiInput, UiSkeleton, VndCurrencyPipe } from '../../../shared/ui';
import { errorCodeOf, errorMessageOf } from '../shared/api-error';
import { BookingFlowStore } from './booking-flow.store';

/**
 * Bước 3: thông tin khách và xác nhận.
 *
 * <h2>Hỏi lại phòng trống NGAY tại bước này</h2>
 *
 * Khách có thể dừng ở bước 2 vài phút, hoặc mở lại link cũ hôm sau. Hiện lại
 * con số cũ ở màn hình cuối là hứa một chỗ có thể không còn. Vì thế bước này
 * hỏi lại `/api/availability` khi vào, và bắt lại lỗi `ROOM_NOT_AVAILABLE` lúc
 * bấm đặt — hai lớp cho hai khoảng thời gian khác nhau: lớp đầu bắt "đã hết từ
 * lâu", lớp sau bắt "vừa hết trong lúc đang gõ".
 *
 * <h2>Tiền</h2>
 *
 * Không có phép nhân nào ở đây. Tổng cả kỳ lấy từ `/api/availability`; khi khách
 * nhập mã giảm giá thì lấy từ `/api/promotions/check`, chính là công thức
 * backend sẽ dùng lúc tạo đơn. Nhân giá × số đêm ở frontend là tạo ra một con
 * số thứ hai để về sau đi giải thích vì sao nó khác hoá đơn.
 */
@Component({
  selector: 'app-step-guest',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton, UiSkeleton, VndCurrencyPipe],
  template: `
    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="rounded-lg border border-border bg-surface p-4" aria-labelledby="buoc-3">
        <h2 id="buoc-3" class="text-h2 font-bold text-text">Thông tin người đặt</h2>

        <form class="mt-4 flex flex-col gap-3" (submit)="submit($event)">
          <ui-input
            label="Họ và tên"
            [required]="true"
            placeholder="Nguyễn Văn A"
            [error]="fieldErrors().guestName"
            [value]="guest().guestName"
            (valueChange)="patch({ guestName: $event })" />

          <ui-input
            label="Số điện thoại"
            type="tel"
            [required]="true"
            placeholder="09xxxxxxxx"
            hint="Dùng để tra cứu và huỷ đơn. Homestay gọi số này khi cần."
            [error]="fieldErrors().guestPhone"
            [value]="guest().guestPhone"
            (valueChange)="patch({ guestPhone: $event })" />

          <ui-input
            label="Email"
            type="email"
            [required]="true"
            placeholder="ban@vidu.com"
            hint="Thư xác nhận và mã đơn được gửi tới đây."
            [error]="fieldErrors().guestEmail"
            [value]="guest().guestEmail"
            (valueChange)="patch({ guestEmail: $event })" />

          <ui-input
            label="Yêu cầu thêm"
            placeholder="Ví dụ: nhận phòng muộn sau 21h"
            hint="Homestay cố gắng đáp ứng nhưng không cam kết."
            [value]="guest().specialRequest"
            (valueChange)="patch({ specialRequest: $event })" />

          <div class="rounded-md bg-surface-2 p-3">
            <div class="flex items-end gap-2">
              <div class="flex-1">
                <ui-input
                  label="Mã khuyến mãi"
                  placeholder="Nhập mã nếu có"
                  [error]="promoError()"
                  [value]="guest().promotionCode"
                  (valueChange)="patch({ promotionCode: $event })" />
              </div>
              <ui-button
                variant="secondary"
                [loading]="checkingPromo()"
                [disabled]="guest().promotionCode.trim() === ''"
                (pressed)="checkPromotion()">
                Áp dụng
              </ui-button>
            </div>

            @if (promotion(); as applied) {
              <p class="mt-2 text-sm font-semibold text-success" aria-live="polite">
                Đã áp dụng “{{ applied.name }}” — giảm {{ applied.discountAmount | vndCurrency }}.
              </p>
            }
          </div>

          @if (submitError()) {
            <div
              class="rounded-md bg-danger/10 p-3"
              role="alert"
              aria-live="assertive">
              <p class="text-sm font-semibold text-danger">{{ submitError() }}</p>
              @if (soldOut()) {
                <div class="mt-2">
                  <ui-button variant="secondary" (pressed)="backToRooms()">
                    Chọn loại phòng khác
                  </ui-button>
                </div>
              }
            </div>
          }

          <ui-button
            type="submit"
            [fullWidth]="true"
            [loading]="submitting()"
            [disabled]="quote() === null || soldOut()">
            Đặt phòng và chuyển sang thanh toán
          </ui-button>

          <p class="text-xs text-text-muted">
            Bấm đặt phòng nghĩa là bạn đồng ý giữ chỗ trong 15 phút và chuyển tiền cọc trong
            khoảng thời gian đó. Chưa mất phí gì ở bước này.
          </p>
        </form>
      </section>

      <!-- TÓM TẮT ĐƠN -->
      <aside class="lg:sticky lg:top-24 lg:self-start">
        <div class="rounded-lg border border-border bg-surface p-4" aria-live="polite">
          <h2 class="text-h3 font-semibold text-text">Tóm tắt đơn</h2>

          @if (rechecking()) {
            <div class="mt-3 flex flex-col gap-2">
              <ui-skeleton shape="line" />
              <ui-skeleton shape="line" />
              <ui-skeleton shape="line" />
            </div>
          } @else if (quote(); as found) {
            <p class="mt-3 font-semibold text-text">{{ found.name }}</p>
            <p class="text-sm text-text-muted">
              {{ displayDate(params().checkIn) }} → {{ displayDate(params().checkOut) }}
            </p>
            <p class="text-sm text-text-muted">
              {{ nights() }} đêm · {{ params().roomQuantity }} phòng ·
              {{ params().adults + params().children }} khách
            </p>

            <dl class="mt-3 flex flex-col gap-1 border-t border-border pt-3 text-sm">
              <div class="flex justify-between">
                <dt class="text-text-muted">Tiền phòng</dt>
                <dd class="text-text">{{ subtotal() | vndCurrency }}</dd>
              </div>

              @if (promotion(); as applied) {
                <div class="flex justify-between">
                  <dt class="text-text-muted">Giảm giá ({{ applied.code }})</dt>
                  <dd class="text-success">− {{ applied.discountAmount | vndCurrency }}</dd>
                </div>
              }

              <div class="flex justify-between border-t border-border pt-2">
                <dt class="font-semibold text-text">Tổng cả kỳ</dt>
                <dd class="text-h3 font-bold text-price">{{ total() | vndCurrency }}</dd>
              </div>

              @if (deposit() !== null) {
                <div class="flex justify-between">
                  <dt class="text-text-muted">Cọc trả trước</dt>
                  <dd class="font-semibold text-text">{{ deposit()! | vndCurrency }}</dd>
                </div>
                <p class="text-xs text-text-muted">
                  Phần còn lại thanh toán tại homestay khi nhận phòng.
                </p>
              }
            </dl>
          } @else if (soldOut()) {
            <p class="mt-3 text-sm font-semibold text-danger">
              Loại phòng này vừa hết chỗ cho khoảng ngày bạn chọn.
            </p>
            <div class="mt-3">
              <ui-button variant="secondary" [fullWidth]="true" (pressed)="backToRooms()">
                Chọn loại phòng khác
              </ui-button>
            </div>
          }
        </div>
      </aside>
    </div>
  `,
})
export class StepGuest {
  private readonly store = inject(BookingFlowStore);
  private readonly availability = inject(AvailabilityService);
  private readonly bookings = inject(BookingService);
  private readonly promotions = inject(PromotionService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly params = this.store.params;
  protected readonly guest = this.store.guestDraft;
  protected readonly nights = this.store.nights;

  protected readonly quote = signal<RoomTypeAvailability | null>(null);
  protected readonly rechecking = signal(true);
  protected readonly soldOut = signal(false);
  protected readonly promotion = signal<CheckPromotionResult | null>(null);
  protected readonly promoError = signal<string | null>(null);
  protected readonly checkingPromo = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<'guestName' | 'guestEmail' | 'guestPhone', string | null>>({
    guestName: null,
    guestEmail: null,
    guestPhone: null,
  });

  /** Khuyến mãi đã áp dụng thì MỌI con số lấy từ kết quả kiểm mã, không trộn hai nguồn. */
  protected readonly subtotal = computed(
    () => this.promotion()?.subtotalAmount ?? this.quote()?.totalPrice ?? 0,
  );

  protected readonly total = computed(
    () => this.promotion()?.totalAmount ?? this.quote()?.totalPrice ?? 0,
  );

  protected readonly deposit = computed(() => this.promotion()?.depositAmount ?? null);

  constructor() {
    const user = this.auth.currentUser();
    const draft = this.guest();
    if (user) {
      // Điền sẵn từ tài khoản, nhưng KHÔNG ghi đè thứ khách đã tự gõ.
      this.store.patchGuest({
        guestName: draft.guestName || user.fullName,
        guestEmail: draft.guestEmail || user.email,
        guestPhone: draft.guestPhone || (user.phone ?? ''),
      });
    }
    this.recheck();

    // Mã giảm giá được cất cùng bản nháp, nên sau khi tải lại trang ô mã vẫn
    // có chữ. Không thử lại mã ngay thì tổng tiền hiện ra là giá chưa giảm,
    // khách bấm đặt và mất phần giảm mà vẫn nhìn thấy mã của mình trong ô.
    if (draft.promotionCode.trim()) {
      this.checkPromotion();
    }
  }

  protected patch(value: Partial<{ guestName: string; guestEmail: string; guestPhone: string; specialRequest: string; promotionCode: string }>): void {
    this.store.patchGuest(value);
    if ('promotionCode' in value) {
      // Đổi mã thì kết quả cũ không còn đúng — bỏ nó đi thay vì để tổng tiền
      // hiển thị mức giảm của một mã mà khách vừa xoá.
      this.promotion.set(null);
      this.promoError.set(null);
    }
  }

  protected checkPromotion(): void {
    const code = this.guest().promotionCode.trim();
    const roomTypeId = this.params().roomTypeId;
    if (!code || roomTypeId === null) {
      return;
    }
    this.checkingPromo.set(true);
    this.promoError.set(null);
    this.promotions
      .check({
        code,
        roomTypeId,
        checkIn: this.params().checkIn,
        checkOut: this.params().checkOut,
        roomQuantity: this.params().roomQuantity,
      })
      .subscribe({
        next: (result) => {
          this.promotion.set(result);
          this.checkingPromo.set(false);
        },
        error: (failure) => {
          this.promotion.set(null);
          this.promoError.set(errorMessageOf(failure, 'Không kiểm tra được mã. Thử lại sau.'));
          this.checkingPromo.set(false);
        },
      });
  }

  protected submit(event: Event): void {
    event.preventDefault();
    if (!this.validate()) {
      return;
    }
    const roomTypeId = this.params().roomTypeId;
    if (roomTypeId === null) {
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);
    const draft = this.guest();

    this.bookings
      .create({
        roomTypeId,
        checkIn: this.params().checkIn,
        checkOut: this.params().checkOut,
        roomQuantity: this.params().roomQuantity,
        adults: this.params().adults,
        children: this.params().children,
        guestName: draft.guestName.trim(),
        guestEmail: draft.guestEmail.trim(),
        guestPhone: draft.guestPhone.trim(),
        specialRequest: draft.specialRequest.trim() || null,
        promotionCode: this.promotion()?.code ?? null,
      })
      .subscribe({
        next: (booking) => {
          this.store.setCreatedBooking(booking);
          this.submitting.set(false);
          void this.router.navigate(['/dat-phong/thanh-toan', booking.code]);
        },
        error: (failure) => {
          this.submitting.set(false);
          const code = errorCodeOf(failure);
          // Phòng vừa bị người khác lấy mất trong lúc khách đang gõ. Đây là
          // kết quả ĐÚNG của ràng buộc chống trùng ở cơ sở dữ liệu, không phải
          // sự cố: nói thẳng và đưa khách về chỗ chọn lại, đừng để họ bấm lại
          // vô vọng.
          this.soldOut.set(code === 'ROOM_NOT_AVAILABLE');
          this.submitError.set(
            errorMessageOf(failure, 'Không tạo được đơn. Thử lại sau giây lát.'),
          );
        },
      });
  }

  protected backToRooms(): void {
    this.store.goToStep(2);
  }

  protected displayDate(iso: string): string {
    if (!iso) {
      return '';
    }
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }

  private validate(): boolean {
    const draft = this.guest();
    const errors = {
      guestName: draft.guestName.trim() ? null : 'Nhập họ tên người đặt.',
      guestPhone: /^0\d{9,10}$/.test(draft.guestPhone.trim())
        ? null
        : 'Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng 0.',
      guestEmail: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(draft.guestEmail.trim())
        ? null
        : 'Email không hợp lệ. Thư xác nhận sẽ gửi tới địa chỉ này.',
    };
    this.fieldErrors.set(errors);
    return Object.values(errors).every((message) => message === null);
  }

  /** Hỏi lại phòng trống khi vào bước cuối. Xem javadoc của lớp. */
  private recheck(): void {
    const { checkIn, checkOut, adults, children, roomQuantity, roomTypeId } = this.params();
    if (roomTypeId === null) {
      return;
    }
    const quantity = Math.max(1, roomQuantity);
    this.rechecking.set(true);
    this.availability
      .search({
        checkIn,
        checkOut,
        adults: Math.ceil(adults / quantity),
        children: Math.ceil(children / quantity),
        roomQuantity: quantity,
      })
      .subscribe({
        next: (response) => {
          const match = response.roomTypes.find((item) => item.roomTypeId === roomTypeId) ?? null;
          this.quote.set(match);
          this.soldOut.set(match === null);
          this.rechecking.set(false);
        },
        error: (failure) => {
          this.rechecking.set(false);
          this.submitError.set(
            errorMessageOf(failure, 'Không kiểm tra lại được phòng trống. Thử tải lại trang.'),
          );
        },
      });
  }
}
