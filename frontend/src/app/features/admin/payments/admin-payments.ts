import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminBookingService } from '../../../core/services/admin-booking.service';
import type { ReconcileRow } from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiFilterChips,
  UiInput,
  UiSelect,
  UiSkeleton,
  type FilterChip,
  type SelectOption,
} from '../../../shared/ui';

const MONEY = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

const RECONCILE_LABELS: Record<string, string> = {
  NEEDS_REVIEW: 'Cần xem xét',
  REFUND_REQUIRED: 'Cần hoàn tiền',
  RESOLVED: 'Đã xử lý',
};

/**
 * Hàng đợi đối soát.
 *
 * Đây là nơi ba nhánh "chuyển người xử lý" của Phase 6 đổ về: chuyển thiếu
 * tiền, chuyển thừa tiền, và tiền về muộn khi không còn phòng để gán lại.
 * Không có màn hình này thì những khoản đó nằm im trong bảng `payments` và
 * không ai biết — đúng cái kết cục mà cả thiết kế thanh toán sinh ra để ngăn.
 *
 * Nguyên văn payload webhook hiện ngay tại dòng: khi khách và nhà cung cấp nói
 * khác nhau, thứ phân xử được là bản ghi gốc, không phải trí nhớ.
 */
@Component({
  selector: 'app-admin-payments',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiSelect, UiInput, UiEmptyState, UiSkeleton, UiFilterChips, UiConfirmDialog],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Đối soát thanh toán</h1>

    <div class="mb-3 max-w-xs">
      <ui-select
        label="Trạng thái đối soát"
        [options]="statusOptions"
        placeholder="Tất cả khoản cần xử lý"
        [(value)]="status" />
    </div>
    <div class="mb-3 flex gap-2">
      <ui-button (pressed)="load()">Áp dụng</ui-button>
    </div>
    <div class="mb-4">
      <ui-filter-chips [chips]="chips()" (removed)="clearFilter()" (cleared)="clearFilter()" />
    </div>

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else if (rows().length === 0) {
      <ui-empty-state
        [title]="status() ? 'Không có khoản nào ở trạng thái này' : 'Không còn khoản nào cần đối soát'"
        [description]="
          status()
            ? 'Bỏ bộ lọc để xem toàn bộ hàng đợi đối soát.'
            : 'Mọi khoản tiền đã khớp tự động. Khoản nào không khớp sẽ xuất hiện ở đây.'
        "
        [actionLabel]="status() ? 'Xoá bộ lọc' : null"
        (action)="clearFilter()" />
    } @else {
      <ul class="space-y-3">
        @for (row of rows(); track row.paymentId) {
          <li class="rounded-md border border-border bg-surface p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-text">
                  {{ row.bookingCode }} · {{ row.guestName }}
                </p>
                <p class="text-sm text-text-muted">
                  Nội dung chuyển khoản: <span class="font-mono">{{ row.transferContent }}</span>
                </p>
                <p class="text-sm text-text-muted">
                  Đã nhận
                  <strong class="tabular-nums text-text">
                    {{ money(row.amountReceived) }}
                  </strong>
                  / cần
                  <strong class="tabular-nums text-text">
                    {{ money(row.amountExpected) }}
                  </strong>
                  · đơn đang {{ row.bookingStatus }}
                </p>
              </div>
              <span
                class="rounded-sm border px-2 py-0.5 text-xs font-semibold uppercase"
                [class.text-warning]="row.reconcileStatus !== 'RESOLVED'"
                [class.border-warning]="row.reconcileStatus !== 'RESOLVED'"
                [class.text-success]="row.reconcileStatus === 'RESOLVED'"
                [class.border-success]="row.reconcileStatus === 'RESOLVED'">
                {{ label(row.reconcileStatus) }}
              </span>
            </div>

            @if (row.webhookPayloads.length) {
              <details class="mt-3">
                <summary class="cursor-pointer text-sm text-focus underline">
                  Xem {{ row.webhookPayloads.length }} payload webhook gốc
                </summary>
                @for (payload of row.webhookPayloads; track payload) {
                  <pre
                    class="mt-2 overflow-x-auto rounded-sm bg-surface-2 p-2 text-xs text-text">{{ payload }}</pre>
                }
              </details>
            }

            <div class="mt-3 grid gap-2 sm:grid-cols-[1fr_auto_auto]">
              <ui-input label="Ghi chú đối soát" [(value)]="note" />
              <div class="flex items-end">
                <ui-button variant="secondary" (pressed)="resolve(row)">Đánh dấu đã xử lý</ui-button>
              </div>
              <div class="flex items-end">
                <ui-button variant="primary" (pressed)="askConfirm(row)">
                  Xác nhận đơn thủ công
                </ui-button>
              </div>
            </div>
          </li>
        }
      </ul>
    }

    @if (error(); as message) {
      <p class="mt-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    @if (pending(); as row) {
      <ui-confirm-dialog
        [open]="true"
        title="Xác nhận đơn {{ row.bookingCode }} bằng tay?"
        question="Chỉ làm việc này SAU khi đã đối chiếu sao kê ngân hàng."
        [consequences]="confirmConsequences(row)"
        confirmLabel="Xác nhận đơn"
        [loading]="working()"
        (confirmed)="confirmManually(row)"
        (cancelled)="pending.set(null)" />
    }
  `,
})
export class AdminPayments {
  private readonly bookings = inject(AdminBookingService);

  protected readonly rows = signal<ReconcileRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly working = signal(false);
  protected readonly status = signal('');
  protected readonly note = signal('');
  protected readonly pending = signal<ReconcileRow | null>(null);

  protected readonly statusOptions: SelectOption[] = Object.entries(RECONCILE_LABELS).map(
    ([value, label]) => ({ value, label }),
  );

  protected readonly chips = computed<FilterChip[]>(() =>
    this.status()
      ? [{ key: 'status', label: 'Trạng thái: ' + this.label(this.status()) }]
      : [],
  );

  constructor() {
    this.load();
  }

  protected label(status: string): string {
    return RECONCILE_LABELS[status] ?? status;
  }

  protected money(amount: number): string {
    return MONEY.format(amount) + ' đ';
  }

  protected confirmConsequences(row: ReconcileRow): string[] {
    return [
      `Đơn ${row.bookingCode} chuyển sang ĐÃ XÁC NHẬN và khách nhận thư xác nhận.`,
      `Lần thanh toán ${row.transferContent} được đánh dấu đã thu đủ, dù hệ thống mới ghi nhận ${this.money(row.amountReceived)}.`,
      'Nhật ký ghi lại tài khoản của bạn là người xác nhận.',
    ];
  }

  protected clearFilter(): void {
    this.status.set('');
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.bookings.reconcileQueue(this.status() || null).subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được hàng đợi đối soát.');
        this.loading.set(false);
      },
    });
  }

  protected resolve(row: ReconcileRow): void {
    this.bookings.resolvePayment(row.paymentId, this.note() || undefined).subscribe({
      next: () => {
        this.note.set('');
        this.load();
      },
      error: () => this.error.set('Không đánh dấu được khoản này.'),
    });
  }

  protected askConfirm(row: ReconcileRow): void {
    this.pending.set(row);
  }

  protected confirmManually(row: ReconcileRow): void {
    this.working.set(true);
    this.bookings.confirmPaymentManually(row.paymentId, this.note() || undefined).subscribe({
      next: () => {
        this.working.set(false);
        this.pending.set(null);
        this.note.set('');
        this.load();
      },
      error: (response) => {
        this.working.set(false);
        this.pending.set(null);
        this.error.set(response?.error?.detail ?? 'Không xác nhận được đơn này.');
      },
    });
  }
}
