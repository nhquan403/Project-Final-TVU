import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { UiStatusBadge, VndCurrencyPipe } from '../../../shared/ui';
import type { Booking } from '../../../core/services/booking.service';
import { BookingFlowStore } from './booking-flow.store';

/**
 * Trang xác nhận sau khi tiền cọc đã về.
 *
 * <p>Việc quan trọng nhất ở đây là ĐƯA MÃ ĐƠN ra thật to. Đó là thứ khách cần
 * để tra cứu, để huỷ, và để đọc cho nhân viên nghe qua điện thoại. Thư xác nhận
 * cũng có mã này, nhưng thư có thể vào hộp thư rác.
 *
 * <p>Trang đọc bản đã cất trong tab; mở từ máy khác thì chỉ còn mã đơn và một
 * đường dẫn sang trang tra cứu. Không hiện thông tin khách khi chưa xác minh
 * được danh tính — mã đơn có trên sao kê ngân hàng, nên bản thân nó không chứng
 * minh người mở là chủ đơn.
 */
@Component({
  selector: 'app-booking-complete',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiStatusBadge, VndCurrencyPipe],
  template: `
    <div class="mx-auto max-w-2xl px-4 py-12">
      <div class="rounded-lg border border-border bg-surface p-6 text-center">
        <p class="text-4xl" aria-hidden="true">✓</p>
        <h1 class="mt-2 text-h1 font-bold text-text">Đặt phòng thành công</h1>
        <p class="mt-2 text-text-muted">
          Thư xác nhận đã được gửi tới email của bạn. Nếu không thấy, hãy kiểm tra hộp thư rác.
        </p>

        <p class="mt-6 text-sm text-text-muted">Mã đơn của bạn</p>
        <p class="font-mono text-h1 font-bold tracking-widest text-primary">{{ code() }}</p>

        @if (booking(); as data) {
          <div class="mt-6 text-left">
            <div class="flex items-center justify-between gap-2">
              <span class="font-semibold text-text">{{ data.roomTypeName }}</span>
              <ui-status-badge [status]="data.status" />
            </div>
            <dl class="mt-3 flex flex-col gap-1 text-sm">
              <div class="flex justify-between">
                <dt class="text-text-muted">Nhận phòng</dt>
                <dd class="text-text">{{ displayDate(data.checkIn) }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-text-muted">Trả phòng</dt>
                <dd class="text-text">{{ displayDate(data.checkOut) }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-text-muted">Số đêm · số phòng</dt>
                <dd class="text-text">{{ data.nights }} đêm · {{ data.roomQuantity }} phòng</dd>
              </div>
              <div class="flex justify-between border-t border-border pt-2">
                <dt class="font-semibold text-text">Tổng cả kỳ</dt>
                <dd class="font-bold text-price">{{ data.totalAmount | vndCurrency }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-text-muted">Đã cọc</dt>
                <dd class="text-text">{{ data.depositAmount | vndCurrency }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-text-muted">Còn lại trả tại homestay</dt>
                <dd class="text-text">
                  {{ data.totalAmount - data.depositAmount | vndCurrency }}
                </dd>
              </div>
            </dl>
          </div>
        }

        <div class="mt-8 flex flex-col gap-2 sm:flex-row sm:justify-center">
          <a
            routerLink="/tra-cuu"
            [queryParams]="{ code: code() }"
            class="inline-flex min-h-[var(--touch-min)] items-center justify-center rounded-md
                   bg-primary px-5 text-sm font-semibold text-text-invert hover:bg-primary-hover">
            Tra cứu đơn này
          </a>
          <a
            routerLink="/"
            class="inline-flex min-h-[var(--touch-min)] items-center justify-center rounded-md
                   border border-border px-5 text-sm font-semibold text-text hover:bg-surface-2">
            Về trang chủ
          </a>
        </div>
      </div>
    </div>
  `,
})
export class BookingCompletePage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(BookingFlowStore);

  protected readonly code = signal('');
  protected readonly booking = signal<Booking | null>(null);

  constructor() {
    const code = this.route.snapshot.paramMap.get('code') ?? '';
    this.code.set(code);
    this.booking.set(this.store.bookingFor(code));
  }

  protected displayDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}
