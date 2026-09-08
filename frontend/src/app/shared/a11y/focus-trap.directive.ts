import { Directive, ElementRef, OnDestroy, inject } from '@angular/core';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), ' +
  'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Giữ focus bên trong phần tử (modal, lightbox) và trả focus về nơi đã mở nó
 * khi đóng.
 *
 * Không trả focus lại là lỗi hay gặp nhất ở modal tự viết: người dùng bàn phím
 * đóng hộp thoại rồi rơi về đầu trang, phải Tab lại từ đầu để về chỗ cũ.
 */
@Directive({
  selector: '[uiFocusTrap]',
  host: { '(keydown.Tab)': 'onTab($any($event))' },
})
export class FocusTrapDirective implements OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly previouslyFocused = document.activeElement as HTMLElement | null;

  constructor() {
    // Đưa focus vào trong ngay khi phần tử xuất hiện.
    queueMicrotask(() => this.focusable()[0]?.focus());
  }

  ngOnDestroy(): void {
    this.previouslyFocused?.focus();
  }

  protected onTab(event: KeyboardEvent): void {
    const items = this.focusable();
    if (items.length === 0) {
      return;
    }
    const first = items[0]!;
    const last = items[items.length - 1]!;
    const active = document.activeElement;

    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private focusable(): HTMLElement[] {
    return Array.from(
      (this.host.nativeElement as HTMLElement).querySelectorAll<HTMLElement>(FOCUSABLE),
    ).filter((element) => element.offsetParent !== null);
  }
}
