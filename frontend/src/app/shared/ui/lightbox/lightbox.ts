import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { FocusTrapDirective } from '../../a11y/focus-trap.directive';
import { EscCloseDirective } from '../../a11y/esc-close.directive';
import { UiSkeleton } from '../skeleton/skeleton';

export interface LightboxImage {
  url: string;
  alt: string;
}

/**
 * Xem ảnh phóng to.
 *
 * Không cuộn vòng ở ảnh đầu và ảnh cuối: nút bị `disabled` để người dùng biết
 * mình đang ở đâu trong bộ ảnh. Nhảy từ ảnh cuối về ảnh đầu làm mất cảm giác
 * vị trí, nhất là với bộ ảnh dài.
 *
 * Ảnh hỏng hiện thông báo thay vì icon vỡ của trình duyệt — ảnh homestay tải
 * từ Cloudinary, và link hỏng là chuyện xảy ra thật.
 */
@Component({
  selector: 'ui-lightbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusTrapDirective, EscCloseDirective, UiSkeleton],
  host: { '(document:keydown)': 'onKeydown($event)' },
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-50 flex flex-col bg-text/90"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="'Ảnh ' + (index() + 1) + ' trên ' + images().length"
        uiFocusTrap
        uiEscClose
        (escape)="closed.emit()">
        <div class="flex items-center justify-between p-3">
          <span class="text-sm text-text-invert" aria-live="polite">
            {{ index() + 1 }} / {{ images().length }}
          </span>
          <button
            type="button"
            [class]="controlClasses"
            aria-label="Đóng"
            (click)="closed.emit()">✕</button>
        </div>

        <div class="flex flex-1 items-center justify-center gap-2 px-2 pb-4">
          <button
            type="button"
            [class]="controlClasses"
            [disabled]="index() === 0"
            aria-label="Ảnh trước"
            (click)="move(-1)">‹</button>

          <div class="flex max-h-full flex-1 items-center justify-center">
            @if (loading()) {
              <div class="w-full max-w-2xl"><ui-skeleton shape="card" /></div>
            } @else if (broken()) {
              <p class="text-body font-semibold text-text-invert" role="alert">
                Không tải được ảnh này.
              </p>
            } @else if (current(); as image) {
              <img
                [src]="image.url"
                [alt]="image.alt"
                class="max-h-[75vh] w-auto object-contain"
                (error)="broken.set(true)" />
            }
          </div>

          <button
            type="button"
            [class]="controlClasses"
            [disabled]="index() >= images().length - 1"
            aria-label="Ảnh sau"
            (click)="move(1)">›</button>
        </div>
      </div>
    }
  `,
})
export class UiLightbox {
  readonly open = input(false);
  readonly images = input<readonly LightboxImage[]>([]);
  readonly loading = input(false);
  readonly index = model(0);
  readonly broken = model(false);
  readonly closed = output<void>();

  protected readonly controlClasses =
    'flex h-[var(--touch-min)] w-[var(--touch-min)] shrink-0 items-center justify-center ' +
    'rounded-full bg-surface/15 text-h2 text-text-invert ' +
    'transition-colors duration-[var(--dur-fast)] hover:bg-surface/30 ' +
    'disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-surface/15';

  protected readonly current = computed(() => this.images()[this.index()] ?? null);

  protected onKeydown(event: KeyboardEvent): void {
    if (!this.open()) {
      return;
    }
    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      this.move(-1);
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      this.move(1);
    }
  }

  protected move(delta: number): void {
    const next = this.index() + delta;
    if (next < 0 || next >= this.images().length) {
      return;
    }
    this.broken.set(false);
    this.index.set(next);
  }
}
