import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

let nextId = 0;

/**
 * Bộ tăng/giảm số khách. Nút chạm biên bị `disabled` chứ không im lặng bỏ qua
 * cú bấm — người dùng cần biết vì sao không tăng được nữa.
 *
 * Trạng thái lỗi ở đây là "vượt sức chứa": số khách vẫn nhập được nhưng phòng
 * không chứa nổi. Đó là lỗi của lựa chọn phòng, không phải của ô nhập, nên
 * không chặn mà báo.
 */
@Component({
  selector: 'ui-guest-stepper',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-center justify-between gap-4">
      <span [id]="id + '-label'" class="flex flex-col">
        <span class="text-body font-medium">{{ label() }}</span>
        @if (hint(); as h) {
          <span class="text-xs text-text-muted">{{ h }}</span>
        }
      </span>

      <div class="flex items-center gap-1">
        <button
          type="button"
          [class]="buttonClasses"
          [disabled]="disabled() || value() <= min()"
          [attr.aria-label]="'Giảm ' + label()"
          (click)="step(-1)">−</button>

        <output
          [attr.aria-labelledby]="id + '-label'"
          aria-live="polite"
          class="min-w-10 text-center text-body font-semibold tabular-nums">
          {{ value() }}
        </output>

        <button
          type="button"
          [class]="buttonClasses"
          [disabled]="disabled() || value() >= max()"
          [attr.aria-label]="'Tăng ' + label()"
          (click)="step(1)">+</button>
      </div>
    </div>

    @if (overCapacityMessage(); as message) {
      <p class="mt-1 text-xs font-semibold text-danger" aria-live="polite">{{ message }}</p>
    }
  `,
})
export class UiGuestStepper {
  protected readonly id = `ui-stepper-${nextId++}`;

  readonly label = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly min = input(0);
  readonly max = input(20);
  readonly disabled = input(false);
  /** Sức chứa thực của phòng. Vượt quá thì báo, không chặn. */
  readonly capacity = input<number | null>(null);
  readonly value = model(0);

  /** Vùng chạm 44px là bắt buộc dù nút trông tròn nhỏ. */
  protected readonly buttonClasses =
    'flex h-[var(--touch-min)] w-[var(--touch-min)] items-center justify-center rounded-full ' +
    'border border-border-strong bg-surface text-h3 leading-none text-text ' +
    'transition-colors duration-[var(--dur-fast)] hover:bg-surface-2 active:bg-surface-2 ' +
    'disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-surface';

  protected readonly overCapacityMessage = computed(() => {
    const capacity = this.capacity();
    if (capacity === null || this.value() <= capacity) {
      return null;
    }
    return `Vượt sức chứa: phòng này tối đa ${capacity} khách.`;
  });

  protected step(delta: number): void {
    const next = this.value() + delta;
    if (next < this.min() || next > this.max()) {
      return;
    }
    this.value.set(next);
  }
}
