import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';

export type ToastKind = 'success' | 'error' | 'warning' | 'info';

const TONES: Record<ToastKind, string> = {
  success: 'border-success text-success',
  error: 'border-danger text-danger',
  warning: 'border-warning text-warning',
  info: 'border-focus text-focus',
};

const ICONS: Record<ToastKind, string> = {
  success: '✓',
  error: '✕',
  warning: '!',
  info: 'i',
};

/**
 * Thông báo nổi.
 *
 * `aria-live="assertive"` cho lỗi, `polite` cho phần còn lại: lỗi phải cắt
 * ngang trình đọc màn hình, còn "đã lưu" thì chờ đọc xong câu hiện tại.
 *
 * Đồng hồ tự đóng DỪNG khi hover hoặc focus vào bên trong. Không dừng thì
 * thông báo biến mất ngay lúc người dùng vừa rê chuột tới định bấm nút trong
 * đó — và với người đọc chậm thì nó biến mất trước khi đọc xong.
 */
@Component({
  selector: 'ui-toast',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(mouseenter)': 'paused.set(true)',
    '(mouseleave)': 'paused.set(false)',
    '(focusin)': 'paused.set(true)',
    '(focusout)': 'paused.set(false)',
  },
  template: `
    <div
      [attr.role]="kind() === 'error' ? 'alert' : 'status'"
      [attr.aria-live]="kind() === 'error' ? 'assertive' : 'polite'"
      [class]="classes()">
      <span class="font-bold" aria-hidden="true">{{ icon() }}</span>
      <p class="flex-1 text-sm text-text">{{ message() }}</p>
      <button
        type="button"
        class="flex h-11 w-11 shrink-0 items-center justify-center rounded-md text-text-muted
               transition-colors duration-[var(--dur-fast)] hover:bg-surface-2"
        aria-label="Đóng thông báo"
        (click)="dismissed.emit()">✕</button>
    </div>
  `,
})
export class UiToast implements OnDestroy {
  readonly kind = input<ToastKind>('info');
  readonly message = input.required<string>();
  /** 0 = không tự đóng. */
  readonly durationMs = input(5000);
  readonly dismissed = output<void>();

  protected readonly paused = signal(false);
  private timer: ReturnType<typeof setTimeout> | null = null;

  protected readonly icon = computed(() => ICONS[this.kind()]);
  protected readonly classes = computed(
    () =>
      'flex items-center gap-3 rounded-md border-l-4 bg-surface p-3 shadow-2 ' + TONES[this.kind()],
  );

  constructor() {
    effect(() => {
      const duration = this.durationMs();
      const isPaused = this.paused();
      this.clear();
      if (duration > 0 && !isPaused) {
        this.timer = setTimeout(() => this.dismissed.emit(), duration);
      }
    });
  }

  ngOnDestroy(): void {
    this.clear();
  }

  private clear(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}
