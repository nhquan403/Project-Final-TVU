import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

export interface SelectOption<T = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

let nextId = 0;

/**
 * Ô chọn một giá trị. Dùng `<select>` gốc chứ không phải dropdown tự vẽ: bản
 * gốc đã có sẵn điều hướng bàn phím, trình đọc màn hình hiểu, và trên mobile
 * hệ điều hành mở bánh xe chọn quen thuộc. Một dropdown tự vẽ phải làm lại
 * toàn bộ những thứ đó và gần như luôn làm thiếu.
 */
@Component({
  selector: 'ui-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="block">
      <span class="mb-1 block text-xs font-medium uppercase tracking-wide text-text-muted">
        {{ label() }}
        @if (required()) { <span class="text-danger" aria-hidden="true">*</span> }
      </span>

      <div class="relative">
        <select
          [id]="id"
          [disabled]="disabled() || loading() || isEmpty()"
          [attr.aria-invalid]="error() ? 'true' : null"
          [attr.aria-describedby]="error() ? id + '-err' : null"
          [attr.aria-busy]="loading() ? 'true' : null"
          [class]="classes()"
          (change)="value.set($any($event.target).value)">
          @if (loading()) {
            <option value="">Đang tải…</option>
          } @else if (isEmpty()) {
            <!-- Trạng thái rỗng: nói rõ không có gì để chọn, thay vì một ô
                 mở ra không có dòng nào. -->
            <option value="">Không có lựa chọn</option>
          } @else {
            @if (placeholder(); as ph) {
              <option value="" [selected]="value() === ''">{{ ph }}</option>
            }
            @for (option of options(); track option.value) {
              <option
                [value]="option.value"
                [disabled]="option.disabled ?? false"
                [selected]="option.value === value()">
                {{ option.label }}
              </option>
            }
          }
        </select>

        <!-- Mũi tên vẽ tay: appearance-none đã tắt mũi tên gốc để viền và bo
             góc thống nhất với ui-input. -->
        <span
          class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-text-muted"
          aria-hidden="true">▾</span>
      </div>

      @if (error(); as message) {
        <span [id]="id + '-err'" class="mt-1 block text-xs font-semibold text-danger" aria-live="polite">
          {{ message }}
        </span>
      } @else if (hint(); as h) {
        <span class="mt-1 block text-xs text-text-muted">{{ h }}</span>
      }
    </label>
  `,
})
export class UiSelect {
  protected readonly id = `ui-select-${nextId++}`;

  readonly label = input.required<string>();
  readonly options = input<readonly SelectOption[]>([]);
  readonly placeholder = input<string | null>(null);
  readonly hint = input<string | null>(null);
  readonly error = input<string | null>(null);
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly required = input(false);
  readonly value = model('');

  protected readonly isEmpty = computed(() => !this.loading() && this.options().length === 0);

  protected readonly classes = computed(() => {
    const base =
      'w-full appearance-none rounded-md bg-surface px-3 pr-9 min-h-[var(--touch-min)] text-body ' +
      'transition-colors duration-[var(--dur-fast)] ' +
      'disabled:opacity-50 disabled:cursor-not-allowed';
    // border-strong (3.67:1) chứ không phải border (1.31:1) — WCAG 1.4.11.
    return this.error()
      ? `${base} border-2 border-danger`
      : `${base} border border-border-strong hover:border-text-muted`;
  });
}
