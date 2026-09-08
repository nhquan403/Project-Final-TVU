import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Phân trang. Ẩn hoàn toàn khi chỉ có một trang — một dãy nút không bấm được
 * chỉ làm rối mắt chứ không mang thêm thông tin nào.
 *
 * Dải số rút gọn quanh trang hiện tại (`1 … 4 5 6 … 20`) để không tràn ngang
 * ở 360px khi bảng có hàng trăm trang.
 */
@Component({
  selector: 'ui-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <nav [attr.aria-label]="label()" class="flex flex-wrap items-center justify-center gap-1">
        <button
          type="button"
          [class]="navClasses"
          [disabled]="disabled() || page() <= 1"
          aria-label="Trang trước"
          (click)="go(page() - 1)">‹</button>

        @for (item of items(); track $index) {
          @if (item === null) {
            <span class="px-2 text-text-muted" aria-hidden="true">…</span>
          } @else {
            <button
              type="button"
              [class]="pageClasses(item)"
              [disabled]="disabled()"
              [attr.aria-current]="item === page() ? 'page' : null"
              [attr.aria-label]="'Trang ' + item"
              (click)="go(item)">{{ item }}</button>
          }
        }

        <button
          type="button"
          [class]="navClasses"
          [disabled]="disabled() || page() >= totalPages()"
          aria-label="Trang sau"
          (click)="go(page() + 1)">›</button>

        @if (loading()) {
          <span class="ml-2 text-sm text-text-muted" aria-live="polite">Đang tải…</span>
        }
      </nav>
    }
  `,
})
export class UiPagination {
  readonly page = input(1);
  readonly totalPages = input(1);
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly label = input('Phân trang');
  readonly pageChange = output<number>();

  protected readonly navClasses =
    'flex h-[var(--touch-min)] w-[var(--touch-min)] items-center justify-center rounded-md ' +
    'border border-border-strong bg-surface text-body text-text ' +
    'transition-colors duration-[var(--dur-fast)] hover:bg-surface-2 active:bg-surface-2 ' +
    'disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-surface';

  /** `null` là dấu lược `…`. */
  protected readonly items = computed<(number | null)[]>(() => {
    const total = this.totalPages();
    const current = this.page();
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }
    const around = [current - 1, current, current + 1].filter((p) => p > 1 && p < total);
    const result: (number | null)[] = [1];
    if (around[0] !== undefined && around[0] > 2) {
      result.push(null);
    }
    result.push(...around);
    if (around[around.length - 1]! < total - 1) {
      result.push(null);
    }
    result.push(total);
    return result;
  });

  protected pageClasses(item: number): string {
    const active =
      item === this.page()
        ? 'bg-primary text-text-invert border-primary font-semibold'
        : 'bg-surface text-text border-border-strong hover:bg-surface-2 active:bg-surface-2';
    return (
      'flex h-[var(--touch-min)] min-w-[var(--touch-min)] items-center justify-center rounded-md ' +
      'border px-2 text-body transition-colors duration-[var(--dur-fast)] ' +
      'disabled:cursor-not-allowed disabled:opacity-50 ' +
      active
    );
  }

  protected go(target: number): void {
    if (target < 1 || target > this.totalPages() || target === this.page()) {
      return;
    }
    this.pageChange.emit(target);
  }
}
