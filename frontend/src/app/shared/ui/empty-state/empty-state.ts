import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Trạng thái rỗng. Luôn có một câu giải thích VÌ SAO rỗng và một lối đi tiếp —
 * bảng trắng không chữ khiến người dùng không phân biệt được "không có dữ liệu"
 * với "trang hỏng".
 */
@Component({
  selector: 'ui-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      <p class="text-h3 font-semibold">{{ title() }}</p>
      <p class="max-w-sm text-sm text-text-muted">{{ description() }}</p>
      @if (actionLabel(); as label) {
        <button
          type="button"
          class="mt-2 min-h-[var(--touch-min)] rounded-md bg-primary px-5 text-sm font-semibold
                 text-text-invert transition-colors duration-[var(--dur-fast)] hover:bg-primary-hover"
          (click)="action.emit()">
          {{ label }}
        </button>
      }
    </div>
  `,
})
export class UiEmptyState {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
