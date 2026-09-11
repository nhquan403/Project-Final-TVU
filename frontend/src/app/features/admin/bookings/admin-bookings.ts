import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminBookingService, type BookingFilter } from '../../../core/services/admin-booking.service';
import type { AdminBookingRow, Page } from '../../../core/services/admin.types';
import {
  UiButton,
  UiDataTable,
  UiFilterChips,
  UiInput,
  UiPagination,
  UiSelect,
  type FilterChip,
  type SelectOption,
  type TableColumn,
} from '../../../shared/ui';

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  AWAITING_REVIEW: 'Chờ đối soát',
  CHECKED_IN: 'Đã nhận phòng',
  CHECKED_OUT: 'Đã trả phòng',
  CANCELLED: 'Đã huỷ',
  EXPIRED: 'Hết hạn giữ chỗ',
  NO_SHOW: 'Không đến',
};

const MONEY = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

/**
 * Bảng đơn đặt phòng.
 *
 * Bộ lọc được đồng bộ lên query param, nên một đường dẫn đã lọc có thể
 * bookmark và gửi cho người khác — và tải lại trang không mất bộ lọc. Điều
 * kiện đang áp hiện thành chip ngay trên bảng: bộ lọc giấu sau một nút là cách
 * nhanh nhất khiến người dùng quên mình đang xem tập con rồi kết luận sai.
 */
@Component({
  selector: 'app-admin-bookings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiDataTable, UiFilterChips, UiPagination, UiSelect, UiInput, UiButton],
  template: `
    <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
      <h1 class="text-xl font-bold text-text">Đơn đặt phòng</h1>
      <ui-button variant="secondary" (pressed)="downloadCsv()">Xuất CSV</ui-button>
    </div>

    <div class="mb-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <ui-select label="Trạng thái" [options]="statusOptions" placeholder="Tất cả"
        [(value)]="status" />
      <ui-input label="Nhận phòng từ" type="text" placeholder="2026-01-01" [(value)]="from" />
      <ui-input label="Nhận phòng đến" type="text" placeholder="2026-12-31" [(value)]="to" />
      <ui-input label="Tìm mã / tên / SĐT" [(value)]="q" />
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-2">
      <ui-button (pressed)="applyFilters()">Áp dụng</ui-button>
    </div>

    <div class="mb-3">
      <ui-filter-chips
        [chips]="chips()"
        (removed)="removeFilter($event)"
        (cleared)="clearFilters()" />
    </div>

    <ui-data-table
      caption="Danh sách đơn đặt phòng"
      density="admin"
      [columns]="columns"
      [rows]="rows()"
      [loading]="loading()"
      [error]="error()"
      [emptyTitle]="emptyTitle()"
      [emptyDescription]="emptyDescription()"
      [emptyActionLabel]="hasFilters() ? 'Xoá tất cả bộ lọc' : null"
      (emptyAction)="clearFilters()" />

    @if (page(); as current) {
      @if (current.totalPages > 1) {
        <div class="mt-4">
          <!-- ui-pagination đếm từ 1, Spring Data đếm từ 0. Quy đổi ở đúng
               ranh giới này, không để lệch một trang lan vào phần còn lại. -->
          <ui-pagination
            [page]="current.number + 1"
            [totalPages]="current.totalPages"
            (pageChange)="goToPage($event - 1)" />
        </div>
      }
    }

    <p class="mt-3 text-sm text-text-muted">
      Bấm vào mã đơn để mở chi tiết.
      @if (rows().length) {
        <a
          [routerLink]="['/admin/bookings', rows()[0].id]"
          class="ml-1 text-focus underline">Ví dụ: {{ rows()[0].code }}</a>
      }
    </p>
  `,
})
export class AdminBookings {
  private readonly bookings = inject(AdminBookingService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly status = signal('');
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly q = signal('');

  protected readonly page = signal<Page<AdminBookingRow> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly rows = computed(() => this.page()?.content ?? []);

  protected readonly statusOptions: SelectOption[] = Object.entries(STATUS_LABELS).map(
    ([value, label]) => ({ value, label }),
  );

  protected readonly columns: TableColumn<AdminBookingRow>[] = [
    { key: 'code', header: 'Mã đơn', value: (row) => row.code },
    { key: 'guest', header: 'Khách', value: (row) => `${row.guestName} · ${row.guestPhone}` },
    { key: 'roomType', header: 'Loại phòng', value: (row) => row.roomTypeName },
    { key: 'stay', header: 'Lưu trú', value: (row) => `${row.checkIn} → ${row.checkOut}` },
    { key: 'rooms', header: 'Số phòng', value: (row) => String(row.roomQuantity), numeric: true },
    {
      key: 'total',
      header: 'Tổng tiền',
      value: (row) => MONEY.format(row.totalAmount) + ' đ',
      numeric: true,
    },
    { key: 'status', header: 'Trạng thái', value: (row) => STATUS_LABELS[row.status] ?? row.status },
  ];

  protected readonly chips = computed<FilterChip[]>(() => {
    const chips: FilterChip[] = [];
    if (this.status()) {
      chips.push({
        key: 'status',
        label: 'Trạng thái: ' + (STATUS_LABELS[this.status()] ?? this.status()),
      });
    }
    if (this.from()) chips.push({ key: 'from', label: 'Từ ' + this.from() });
    if (this.to()) chips.push({ key: 'to', label: 'Đến ' + this.to() });
    if (this.q()) chips.push({ key: 'q', label: 'Tìm: ' + this.q() });
    return chips;
  });

  protected readonly hasFilters = computed(() => this.chips().length > 0);

  protected readonly emptyTitle = computed(() =>
    this.hasFilters() ? 'Không có đơn nào khớp bộ lọc' : 'Chưa có đơn đặt phòng nào',
  );

  protected readonly emptyDescription = computed(() =>
    this.hasFilters()
      ? 'Bộ lọc hiện tại đang thu hẹp danh sách. Xoá bớt điều kiện để xem thêm đơn.'
      : 'Đơn đặt từ trang chủ sẽ xuất hiện ở đây ngay khi khách gửi.',
  );

  constructor() {
    // Đọc bộ lọc TỪ URL khi vào trang: đó là điều kiện để đường dẫn đã lọc
    // chia sẻ được, và để tải lại trang không mất bộ lọc.
    const params = this.route.snapshot.queryParamMap;
    this.status.set(params.get('status') ?? '');
    this.from.set(params.get('from') ?? '');
    this.to.set(params.get('to') ?? '');
    this.q.set(params.get('q') ?? '');
    this.load(Number(params.get('page') ?? 0));
  }

  protected applyFilters(): void {
    this.syncUrl(0);
    this.load(0);
  }

  protected removeFilter(key: string): void {
    if (key === 'status') this.status.set('');
    if (key === 'from') this.from.set('');
    if (key === 'to') this.to.set('');
    if (key === 'q') this.q.set('');
    this.applyFilters();
  }

  protected clearFilters(): void {
    this.status.set('');
    this.from.set('');
    this.to.set('');
    this.q.set('');
    this.applyFilters();
  }

  protected goToPage(page: number): void {
    this.syncUrl(page);
    this.load(page);
  }

  protected downloadCsv(): void {
    this.bookings.exportCsv(this.filter(0)).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'bookings.csv';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('Không xuất được tệp CSV.'),
    });
  }

  private filter(page: number): BookingFilter {
    return {
      status: this.status() || null,
      from: this.from() || null,
      to: this.to() || null,
      q: this.q() || null,
      page,
    };
  }

  private syncUrl(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        status: this.status() || null,
        from: this.from() || null,
        to: this.to() || null,
        q: this.q() || null,
        page: page || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.bookings.list(this.filter(page)).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách đơn.');
        this.loading.set(false);
      },
    });
  }
}
