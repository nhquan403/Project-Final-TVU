import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Khung xương lúc tải. Ba hình dạng, dùng thay cho spinner giữa màn hình:
 * người dùng cần thấy trước bố cục sẽ ra sao, không phải một vòng xoay.
 */
@Component({
  selector: 'ui-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="classes()" aria-hidden="true"></div>
  `,
  styles: `
    :host { display: block; }
    div {
      background: linear-gradient(90deg,
        var(--color-surface-2) 25%,
        var(--color-border) 37%,
        var(--color-surface-2) 63%);
      background-size: 400% 100%;
      animation: ui-shimmer 1.4s ease-in-out infinite;
    }
    @keyframes ui-shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
  `,
})
export class UiSkeleton {
  readonly shape = input<'line' | 'card' | 'row'>('line');

  protected readonly classes = computed(
    () =>
      ({
        line: 'h-4 w-full rounded-sm',
        card: 'h-40 w-full rounded-lg',
        row: 'h-12 w-full rounded-sm',
      })[this.shape()],
  );
}
