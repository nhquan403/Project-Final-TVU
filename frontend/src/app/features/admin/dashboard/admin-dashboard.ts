import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../../core/services/dashboard.service';
import type { DashboardSummary, MonthlyValue } from '../../../core/services/admin.types';
import { UiSkeleton, VndCurrencyPipe } from '../../../shared/ui';
import { BarChart, type ChartPoint } from './bar-chart';
import { LineChart } from './line-chart';

const MONEY = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

/**
 * Trang tổng quan.
 *
 * Hai chỉ số tiền đứng TÁCH nhau và mang hai cái tên khác nhau. Hệ thống chỉ
 * thu 30% tiền cọc, nên gọi tổng `total_amount` là "doanh thu" sẽ sai ngay khi
 * có người hỏi "tiền này đã về tài khoản chưa?". Mỗi thẻ số liệu kèm một dòng
 * nói rõ nó tính theo trục thời gian nào — ba trục khác nhau nằm cạnh nhau mà
 * không chú thích thì mặc nhiên bị hiểu là cùng kỳ.
 */
@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiSkeleton, VndCurrencyPipe, BarChart, LineChart],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Tổng quan</h1>

    @if (loading()) {
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        @for (placeholder of [0, 1, 2, 3, 4, 5]; track placeholder) {
          <div class="rounded-md border border-border bg-surface p-4">
            <ui-skeleton shape="line" />
          </div>
        }
      </div>
    } @else if (error()) {
      <p class="rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ error() }}</p>
    } @else if (summary(); as data) {
      <p class="mb-4 text-sm text-text-muted">
        Kỳ báo cáo: {{ data.periodStart }} → {{ data.periodEnd }} (không tính ngày cuối)
      </p>

      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-md border border-border bg-surface p-4">
          <p class="text-xs uppercase tracking-wide text-text-muted">Giá trị booking</p>
          <p class="text-lg font-bold tabular-nums text-text">
            {{ data.bookingValueTotal | vndCurrency }}
          </p>
          <p class="mt-1 text-xs text-text-muted">{{ data.axes.bookingValue }}</p>
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <p class="text-xs uppercase tracking-wide text-text-muted">Tiền đã thu</p>
          <p class="text-lg font-bold tabular-nums text-price">
            {{ data.amountReceivedTotal | vndCurrency }}
          </p>
          <p class="mt-1 text-xs text-text-muted">{{ data.axes.amountReceived }}</p>
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <p class="text-xs uppercase tracking-wide text-text-muted">Tỉ lệ lấp đầy trung bình</p>
          <p class="text-lg font-bold tabular-nums text-text">{{ percent(data.occupancyRate) }}</p>
          <p class="mt-1 text-xs text-text-muted">{{ data.axes.occupancy }}</p>
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <p class="text-xs uppercase tracking-wide text-text-muted">Đơn mới</p>
          <p class="text-lg font-bold tabular-nums text-text">{{ data.newBookings }}</p>
          <p class="mt-1 text-xs text-text-muted">{{ data.axes.newBookings }}</p>
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <p class="text-xs uppercase tracking-wide text-text-muted">Tỉ lệ huỷ</p>
          <p class="text-lg font-bold tabular-nums text-text">
            {{ percent(data.cancellationRate) }}
          </p>
          <p class="mt-1 text-xs text-text-muted">
            {{ data.cancelledBookings }} đơn huỷ hoặc không đến, theo ngày tạo đơn
          </p>
        </div>

        <a
          routerLink="/admin/payments"
          class="block rounded-md border border-border bg-surface p-4
                 transition-colors duration-[var(--dur-fast)] hover:bg-surface-2">
          <p class="text-xs uppercase tracking-wide text-text-muted">Cần đối soát</p>
          <p
            class="text-lg font-bold tabular-nums"
            [class.text-warning]="data.reconcileCount > 0"
            [class.text-text]="data.reconcileCount === 0">
            {{ data.reconcileCount }}
          </p>
          <p class="mt-1 text-xs text-text-muted">Khoản tiền đang chờ người xử lý — bấm để mở</p>
        </a>
      </div>

      <div class="mt-6 space-y-6">
        <div class="rounded-md border border-border bg-surface p-4">
          <app-bar-chart
            title="Giá trị booking theo tháng"
            [axisNote]="data.axes.bookingValue"
            [points]="moneySeries(data.bookingValueByMonth)" />
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <app-line-chart
            title="Tỉ lệ lấp đầy theo tháng"
            [axisNote]="data.axes.occupancy"
            [points]="ratioSeries(data.occupancyByMonth)" />
        </div>

        <div class="rounded-md border border-border bg-surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-text">Loại phòng bán chạy</h2>
          @if (data.topRoomTypes.length === 0) {
            <p class="text-sm text-text-muted">Chưa có đơn đã chốt nào trong kỳ.</p>
          } @else {
            <table class="w-full text-sm">
              <caption class="sr-only">Loại phòng bán chạy trong kỳ</caption>
              <thead>
                <tr class="border-b border-border text-left text-text-muted">
                  <th class="py-3">Loại phòng</th>
                  <th class="py-3 text-right">Số đơn</th>
                  <th class="py-3 text-right">Giá trị</th>
                </tr>
              </thead>
              <tbody>
                @for (row of data.topRoomTypes; track row.roomTypeName) {
                  <tr class="border-b border-border">
                    <td class="py-3">{{ row.roomTypeName }}</td>
                    <td class="py-3 text-right tabular-nums">{{ row.bookings }}</td>
                    <td class="py-3 text-right tabular-nums">
                      {{ row.bookingValue | vndCurrency }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      </div>
    }
  `,
})
export class AdminDashboard {
  private readonly dashboard = inject(DashboardService);

  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.dashboard.summary().subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được số liệu tổng quan.');
        this.loading.set(false);
      },
    });
  }

  protected percent(ratio: number): string {
    return (ratio * 100).toFixed(1) + '%';
  }

  protected moneySeries(values: MonthlyValue[]): ChartPoint[] {
    return values.map((point) => ({
      label: monthLabel(point.month),
      value: point.value,
      display: MONEY.format(point.value),
    }));
  }

  protected ratioSeries(values: MonthlyValue[]): ChartPoint[] {
    return values.map((point) => ({
      label: monthLabel(point.month),
      value: point.value,
      display: (point.value * 100).toFixed(1) + '%',
    }));
  }
}

/** `2026-03-01` → `03/26`. Nhãn trục hoành phải ngắn để 12 tháng không chồng nhau. */
function monthLabel(isoDate: string): string {
  const [year, month] = isoDate.split('-');
  return `${month}/${year.slice(2)}`;
}
