import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Tám trạng thái booking của Phase 3. Danh sách này là hợp đồng, không mở rộng tuỳ tiện. */
export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'CONFIRMED'
  | 'AWAITING_REVIEW'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'NO_SHOW';

const LABELS: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  AWAITING_REVIEW: 'Chờ đối soát',
  CHECKED_IN: 'Đã nhận phòng',
  CHECKED_OUT: 'Đã trả phòng',
  CANCELLED: 'Đã huỷ',
  EXPIRED: 'Hết hạn giữ chỗ',
  NO_SHOW: 'Không đến',
};

/** Mỗi trạng thái một màu CỐ ĐỊNH trên toàn hệ thống — không để mỗi màn hình tự chọn. */
const TONES: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'text-warning border-warning',
  CONFIRMED: 'text-success border-success',
  AWAITING_REVIEW: 'text-warning border-warning',
  CHECKED_IN: 'text-focus border-focus',
  CHECKED_OUT: 'text-text-muted border-border-strong',
  CANCELLED: 'text-danger border-danger',
  EXPIRED: 'text-text-muted border-border-strong',
  NO_SHOW: 'text-danger border-danger',
};

@Component({
  selector: 'ui-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="classes()">{{ label() }}</span>
  `,
})
export class UiStatusBadge {
  readonly status = input.required<BookingStatus>();

  protected readonly label = computed(() => LABELS[this.status()]);
  protected readonly classes = computed(
    () =>
      'inline-block rounded-sm border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ' +
      TONES[this.status()],
  );
}
