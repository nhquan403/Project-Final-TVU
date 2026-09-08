import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

let nextId = 0;

/**
 * Ô nhập một dòng. Trạng thái lỗi đổi CẢ màu viền LẪN độ dày viền, không chỉ
 * màu — người mù màu không phân biệt được nếu chỉ đổi màu.
 */
@Component({
  selector: 'ui-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="block">
      <span class="block text-xs font-medium uppercase tracking-wide text-text-muted mb-1">
        {{ label() }}
        @if (required()) { <span class="text-danger" aria-hidden="true">*</span> }
      </span>
      <input
        [id]="id"
        [type]="type()"
        [value]="value()"
        [placeholder]="placeholder()"
        [disabled]="disabled()"
        [required]="required()"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="error() ? id + '-err' : null"
        [class]="classes()"
        (input)="value.set($any($event.target).value)" />
      @if (error(); as message) {
        <span [id]="id + '-err'" class="block mt-1 text-xs font-semibold text-danger" aria-live="polite">
          {{ message }}
        </span>
      } @else if (hint(); as h) {
        <span class="block mt-1 text-xs text-text-muted">{{ h }}</span>
      }
    </label>
  `,
})
export class UiInput {
  protected readonly id = `ui-input-${nextId++}`;

  readonly label = input.required<string>();
  readonly type = input<'text' | 'email' | 'tel' | 'password' | 'number'>('text');
  readonly placeholder = input('');
  readonly hint = input<string | null>(null);
  readonly error = input<string | null>(null);
  readonly disabled = input(false);
  readonly required = input(false);
  readonly value = model('');

  protected readonly classes = computed(() => {
    const base =
      'w-full rounded-md bg-surface px-3 min-h-[var(--touch-min)] text-body ' +
      'transition-colors duration-[var(--dur-fast)] ' +
      'disabled:opacity-50 disabled:cursor-not-allowed';
    // Viền dùng border-strong (3.67:1) chứ không phải border (1.31:1):
    // đây là ranh giới của thành phần tương tác, WCAG 1.4.11 yêu cầu ≥3:1.
    return this.error()
      ? `${base} border-2 border-danger`
      : `${base} border border-border-strong hover:border-text-muted`;
  });
}
