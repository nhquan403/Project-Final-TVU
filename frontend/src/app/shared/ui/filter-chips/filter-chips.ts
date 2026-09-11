import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** Một điều kiện lọc đang được áp. */
export interface FilterChip {
  /** Khoá để nơi gọi biết bỏ điều kiện nào. */
  key: string;
  /** Nhãn đọc được, ví dụ `Trạng thái: Đã xác nhận`. */
  label: string;
}

/**
 * Hàng chip hiển thị mọi điều kiện lọc đang áp.
 *
 * Bộ lọc giấu sau một nút "Filter" là cách nhanh nhất khiến người dùng quên
 * mình đang xem một tập con rồi kết luận sai về dữ liệu — "tháng này không có
 * đơn nào" trong khi thật ra bộ lọc trạng thái vẫn đang bật từ hôm qua.
 *
 * Mỗi chip có nút bỏ riêng, cộng một nút xoá tất cả khi có từ hai điều kiện.
 * Vùng chạm của mọi nút đạt `--touch-min`.
 */
@Component({
  selector: 'ui-filter-chips',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (chips().length) {
      <div class="flex flex-wrap items-center gap-2" role="group" [attr.aria-label]="ariaLabel()">
        <span class="text-xs font-semibold uppercase tracking-wide text-text-muted">
          Đang lọc theo
        </span>

        @for (chip of chips(); track chip.key) {
          <span
            class="inline-flex min-h-[var(--touch-min)] items-center gap-1 rounded-sm
                   border border-border-strong bg-surface px-2 text-sm text-text">
            {{ chip.label }}
            <button
              type="button"
              class="inline-flex min-h-[var(--touch-min)] min-w-[var(--touch-min)] items-center
                     justify-center rounded-sm text-text-muted
                     transition-colors duration-[var(--dur-fast)] hover:text-danger"
              [attr.aria-label]="'Bỏ điều kiện ' + chip.label"
              (click)="removed.emit(chip.key)">
              ×
            </button>
          </span>
        }

        @if (chips().length > 1) {
          <button
            type="button"
            class="min-h-[var(--touch-min)] rounded-sm px-2 text-sm font-semibold text-focus underline
                   transition-colors duration-[var(--dur-fast)] hover:text-primary"
            (click)="cleared.emit()">
            Xoá tất cả bộ lọc
          </button>
        }
      </div>
    }
  `,
})
export class UiFilterChips {
  readonly chips = input<readonly FilterChip[]>([]);
  readonly ariaLabel = input('Bộ lọc đang áp dụng');

  readonly removed = output<string>();
  readonly cleared = output<void>();
}
