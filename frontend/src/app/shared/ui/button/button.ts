import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost';

/**
 * Nút, ba cấp. Trạng thái loading giữ nguyên bề rộng nút để bố cục không nhảy
 * khi spinner xuất hiện — nhảy layout ngay lúc người dùng vừa bấm là cách
 * nhanh nhất khiến họ bấm nhầm lần hai.
 */
@Component({
  selector: 'ui-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading() ? 'true' : null"
      [class]="classes()"
      (click)="pressed.emit($event)">
      @if (loading()) {
        <span class="ui-spinner" aria-hidden="true"></span>
        <span class="sr-only">Đang xử lý</span>
      }
      <span [class.invisible]="loading()"><ng-content /></span>
    </button>
  `,
  styles: `
    .ui-spinner {
      position: absolute;
      width: 1em;
      height: 1em;
      border: 2px solid currentColor;
      border-right-color: transparent;
      border-radius: 50%;
      animation: ui-spin 0.7s linear infinite;
    }
    @keyframes ui-spin { to { transform: rotate(360deg); } }
  `,
})
export class UiButton {
  readonly variant = input<ButtonVariant>('primary');
  readonly type = input<'button' | 'submit'>('button');
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly fullWidth = input(false);
  readonly pressed = output<MouseEvent>();

  protected readonly classes = computed(() => {
    const base =
      'relative inline-flex items-center justify-center gap-2 rounded-md px-5 ' +
      'min-h-[var(--touch-min)] text-sm font-semibold ' +
      'transition-colors duration-[var(--dur-fast)] ' +
      'disabled:opacity-50 disabled:cursor-not-allowed';
    const width = this.fullWidth() ? 'w-full' : '';
    const variant = {
      primary:
        'bg-primary text-text-invert hover:bg-primary-hover active:bg-primary-hover ' +
        'disabled:hover:bg-primary',
      secondary:
        'bg-surface text-text border border-border-strong hover:bg-surface-2 ' +
        'active:bg-surface-2 disabled:hover:bg-surface',
      ghost: 'bg-transparent text-primary hover:bg-surface-2 active:bg-surface-2',
    }[this.variant()];
    return [base, width, variant].filter(Boolean).join(' ');
  });
}
