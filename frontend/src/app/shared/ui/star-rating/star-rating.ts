import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

/**
 * Sao đánh giá, hai chế độ.
 *
 * - `readonly` (mặc định): chỉ hiển thị, không nhận focus, không nằm trong
 *   luồng Tab. Trang danh sách có hàng chục thẻ đánh giá; để mỗi thẻ chiếm
 *   năm điểm dừng Tab là biến bàn phím thành cực hình.
 * - Chế độ nhập: một radiogroup thật, mũi tên trái/phải đổi sao.
 */
@Component({
  selector: 'ui-star-rating',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (readonly()) {
      <span class="inline-flex items-center gap-1" [attr.aria-label]="value() + ' trên 5 sao'">
        @for (star of stars; track star) {
          <span [class]="star <= value() ? filled : empty" aria-hidden="true">★</span>
        }
        @if (count(); as n) {
          <span class="ml-1 text-sm text-text-muted">({{ n }})</span>
        }
      </span>
    } @else {
      <div
        role="radiogroup"
        [attr.aria-label]="label()"
        [attr.aria-invalid]="error() ? 'true' : null"
        class="inline-flex items-center gap-1">
        @for (star of stars; track star) {
          <button
            type="button"
            role="radio"
            [attr.aria-checked]="star === value()"
            [attr.aria-label]="star + ' sao'"
            [disabled]="disabled()"
            [tabindex]="star === tabbableStar() ? 0 : -1"
            [class]="inputStarClasses(star)"
            (click)="value.set(star)"
            (keydown)="onKeydown($event)">★</button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-1 text-xs font-semibold text-danger" aria-live="polite">{{ message }}</p>
      }
    }
  `,
})
export class UiStarRating {
  protected readonly stars = [1, 2, 3, 4, 5] as const;
  protected readonly filled = 'text-warning';
  protected readonly empty = 'text-border-strong';

  readonly readonly = input(true);
  readonly label = input('Chấm điểm');
  readonly disabled = input(false);
  readonly error = input<string | null>(null);
  /** Số lượt đánh giá, chỉ dùng ở chế độ đọc. */
  readonly count = input<number | null>(null);
  readonly value = model(0);

  /** Khi chưa chọn sao nào, sao đầu tiên giữ điểm dừng Tab của cả nhóm. */
  protected readonly tabbableStar = computed(() => this.value() || 1);

  protected inputStarClasses(star: number): string {
    const base =
      'flex h-[var(--touch-min)] w-[var(--touch-min)] items-center justify-center rounded-sm ' +
      'text-h2 leading-none transition-colors duration-[var(--dur-fast)] ' +
      'hover:text-warning active:scale-95 disabled:cursor-not-allowed disabled:opacity-50';
    return `${base} ${star <= this.value() ? this.filled : this.empty}`;
  }

  protected onKeydown(event: KeyboardEvent): void {
    const delta = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1 }[event.key];
    if (delta === undefined || this.disabled()) {
      return;
    }
    event.preventDefault();
    const next = Math.min(5, Math.max(1, this.value() + delta));
    this.value.set(next);
    // Focus theo sao mới để vòng focus đi cùng lựa chọn.
    const group = (event.target as HTMLElement).parentElement;
    (group?.children[next - 1] as HTMLElement | undefined)?.focus();
  }
}
