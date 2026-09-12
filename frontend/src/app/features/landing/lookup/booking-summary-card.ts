import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiButton, UiStatusBadge, VndCurrencyPipe } from '../../../shared/ui';
import type { Booking } from '../../../core/services/booking.service';

/** Trạng thái mà khách còn tự huỷ được. Khớp bảng chuyển trạng thái ở backend. */
const CANCELLABLE = new Set(['PENDING_PAYMENT', 'AWAITING_REVIEW', 'CONFIRMED']);

/**
 * Thẻ tóm tắt một đơn, dùng chung ở trang tra cứu và trang "Đơn của tôi".
 *
 * <p>Các nút hành động hiện theo TRẠNG THÁI, không hiện hết rồi báo lỗi khi
 * bấm: "Huỷ đơn" trên một đơn đã trả phòng chỉ dẫn tới một thông báo từ chối.
 * Danh sách trạng thái huỷ được ở đây khớp bảng chuyển trạng thái của backend —
 * backend vẫn là nơi quyết định, phần này chỉ là để không mời khách bấm nhầm.
 */
@Component({
  selector: 'app-booking-summary-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiButton, UiStatusBadge, VndCurrencyPipe],
  template: `
    <article class="rounded-lg border border-border bg-surface p-4">
      <header class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p class="font-semibold text-text">{{ booking().roomTypeName }}</p>
          <p class="text-sm text-text-muted">
            Mã đơn <strong class="font-mono">{{ booking().code }}</strong>
          </p>
        </div>
        <ui-status-badge [status]="booking().status" />
      </header>

      <dl class="mt-3 flex flex-col gap-1 text-sm">
        <div class="flex justify-between">
          <dt class="text-text-muted">Nhận phòng</dt>
          <dd class="text-text">{{ displayDate(booking().checkIn) }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-text-muted">Trả phòng</dt>
          <dd class="text-text">{{ displayDate(booking().checkOut) }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-text-muted">{{ booking().nights }} đêm · {{ booking().roomQuantity }} phòng</dt>
          <dd class="text-text">{{ booking().adults + booking().children }} khách</dd>
        </div>
        @if (booking().rooms.length) {
          <div class="flex justify-between">
            <dt class="text-text-muted">Phòng được xếp</dt>
            <dd class="text-text">{{ roomNumbers() }}</dd>
          </div>
        }
        <div class="flex justify-between border-t border-border pt-2">
          <dt class="font-semibold text-text">Tổng cả kỳ</dt>
          <dd class="font-bold text-price">{{ booking().totalAmount | vndCurrency }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-text-muted">Tiền cọc</dt>
          <dd class="text-text">{{ booking().depositAmount | vndCurrency }}</dd>
        </div>
      </dl>

      <div class="mt-4 flex flex-wrap gap-2">
        @if (booking().status === 'PENDING_PAYMENT') {
          <a
            [routerLink]="['/dat-phong/thanh-toan', booking().code]"
            class="inline-flex min-h-[var(--touch-min)] items-center rounded-md bg-primary px-4
                   text-sm font-semibold text-text-invert hover:bg-primary-hover">
            Tiếp tục thanh toán
          </a>
        }

        @if (booking().status === 'CHECKED_OUT') {
          <a
            [routerLink]="['/danh-gia', booking().code]"
            class="inline-flex min-h-[var(--touch-min)] items-center rounded-md border
                   border-primary px-4 text-sm font-semibold text-primary hover:bg-surface-2">
            Viết đánh giá
          </a>
        }

        @if (cancellable()) {
          <ui-button variant="danger" [loading]="cancelling()" (pressed)="cancelRequested.emit()">
            Huỷ đơn
          </ui-button>
        }
      </div>
    </article>
  `,
})
export class BookingSummaryCard {
  readonly booking = input.required<Booking>();
  readonly cancelling = input(false);
  readonly cancelRequested = output<void>();

  protected readonly cancellable = computed(() => CANCELLABLE.has(this.booking().status));

  protected readonly roomNumbers = computed(() =>
    this.booking()
      .rooms.map((room) => room.roomNumber)
      .join(', '),
  );

  protected displayDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}
