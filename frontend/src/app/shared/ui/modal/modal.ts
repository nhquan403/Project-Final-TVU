import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  output,
} from '@angular/core';
import { FocusTrapDirective } from '../../a11y/focus-trap.directive';
import { EscCloseDirective } from '../../a11y/esc-close.directive';
import { UiSkeleton } from '../skeleton/skeleton';

let nextId = 0;

/**
 * Hộp thoại. Bốn thứ bắt buộc, thiếu cái nào cũng thành cái bẫy cho người dùng
 * bàn phím: bẫy focus bên trong, `Esc` đóng, trả focus về nơi đã mở, khoá cuộn
 * nền.
 *
 * Khoá cuộn nền không chỉ là chi tiết thẩm mỹ: trên mobile, cuộn trong modal
 * mà nền cuộn theo khiến người dùng mất luôn vị trí trang phía sau.
 */
@Component({
  selector: 'ui-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusTrapDirective, EscCloseDirective, UiSkeleton],
  template: `
    @if (open()) {
      <!-- Lớp phủ chỉ là lối tắt cho chuột. Người dùng bàn phím đã có Esc và nút
           ✕ trong tiêu đề, nên không ai bị khoá chức năng nào ở đây — đó cũng
           chính là điều hai quy tắc dưới đây bảo vệ. -->
      <!-- eslint-disable-next-line @angular-eslint/template/click-events-have-key-events, @angular-eslint/template/interactive-supports-focus -->
      <div
        class="fixed inset-0 z-50 flex items-end justify-center bg-text/50 p-0 sm:items-center sm:p-4"
        (click)="onBackdrop($event)"
        uiEscClose
        (escape)="closed.emit()">
        <div
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="id + '-title'"
          uiFocusTrap
          class="ui-modal-panel flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden
                 rounded-t-lg bg-surface shadow-3 sm:rounded-lg">
          <header class="flex items-start justify-between gap-4 border-b border-border p-4">
            <h2 [id]="id + '-title'" class="text-h2 font-semibold">{{ title() }}</h2>
            <button
              type="button"
              class="flex h-[var(--touch-min)] w-[var(--touch-min)] shrink-0 items-center
                     justify-center rounded-md text-text-muted
                     transition-colors duration-[var(--dur-fast)] hover:bg-surface-2"
              aria-label="Đóng"
              (click)="closed.emit()">✕</button>
          </header>

          <div class="overflow-y-auto p-4">
            @if (loading()) {
              <div class="flex flex-col gap-2">
                <ui-skeleton shape="line" />
                <ui-skeleton shape="line" />
                <ui-skeleton shape="line" />
              </div>
            } @else if (error(); as message) {
              <p class="text-body font-semibold text-danger" role="alert">{{ message }}</p>
            } @else {
              <ng-content />
            }
          </div>

          <footer class="border-t border-border p-4">
            <ng-content select="[modal-footer]" />
          </footer>
        </div>
      </div>
    }
  `,
  styles: `
    .ui-modal-panel {
      animation: ui-modal-in var(--dur-slow) var(--ease-out);
    }
    @keyframes ui-modal-in {
      from { opacity: 0; transform: translateY(12px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `,
})
export class UiModal {
  protected readonly id = `ui-modal-${nextId++}`;

  readonly open = input(false);
  readonly title = input.required<string>();
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  /** Bấm ra nền có đóng không. Tắt khi trong modal có form đang nhập dở. */
  readonly closeOnBackdrop = input(true);
  readonly closed = output<void>();

  constructor() {
    effect((onCleanup) => {
      if (!this.open()) {
        return;
      }
      // Khoá cuộn nền trong lúc modal mở.
      const previous = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      onCleanup(() => {
        document.body.style.overflow = previous;
      });
    });
  }

  protected onBackdrop(event: MouseEvent): void {
    if (this.closeOnBackdrop() && event.target === event.currentTarget) {
      this.closed.emit();
    }
  }
}
