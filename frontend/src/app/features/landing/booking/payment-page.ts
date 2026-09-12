import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService, type Booking } from '../../../core/services/booking.service';
import { UiButton, UiInput } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';
import { BookingFlowStore } from './booking-flow.store';
import { PaymentQrComponent } from './payment-qr.component';

/**
 * Trang thanh toán giữ chỗ.
 *
 * <p>Bọc quanh `app-payment-qr` (Phase 6) và lo đúng một việc mà nó không lo:
 * tìm lại đơn khi trang được mở lại.
 *
 * <p>Ba nguồn, theo thứ tự: signal trong bộ nhớ (vừa đặt xong), bản cất trong
 * `sessionStorage` của tab này (đã F5, hoặc vừa quay về từ ứng dụng ngân hàng),
 * và cuối cùng là tra cứu bằng mã đơn + số điện thoại. Nguồn thứ ba tồn tại vì
 * link thanh toán mở ở máy khác hoặc tab khác thì hai nguồn trên đều trống —
 * và vì không có nguồn nào, khách sẽ gặp một trang trắng đúng lúc cầm tiền.
 *
 * <p>Tra cứu đòi số điện thoại chứ không chỉ mã đơn: mã đơn nằm trên sao kê
 * ngân hàng, nên nếu chỉ cần mã là mở được thì ai đọc được sao kê cũng xem được
 * thông tin khách.
 */
@Component({
  selector: 'app-payment-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PaymentQrComponent, UiInput, UiButton],
  template: `
    <div class="mx-auto max-w-2xl px-4 py-8">
      @if (booking(); as data) {
        <app-payment-qr
          [booking]="data"
          [accessToken]="accessToken()"
          (confirmed)="onConfirmed()"
          (rebook)="rebook()" />
      } @else {
        <section class="rounded-lg border border-border bg-surface p-5">
          <h1 class="text-h2 font-bold text-text">Mở lại đơn {{ code() }}</h1>
          <p class="mt-2 text-sm text-text-muted">
            Trình duyệt này chưa giữ thông tin thanh toán của đơn. Nhập số điện thoại đã dùng khi
            đặt để mở lại mã QR.
          </p>

          <form class="mt-4 flex flex-col gap-3" (submit)="lookup($event)">
            <ui-input
              label="Số điện thoại đã đặt"
              type="tel"
              [required]="true"
              placeholder="09xxxxxxxx"
              [error]="error()"
              [(value)]="phone" />
            <ui-button type="submit" [fullWidth]="true" [loading]="loading()">
              Mở lại đơn
            </ui-button>
          </form>
        </section>
      }
    </div>
  `,
})
export class PaymentPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject(BookingFlowStore);
  private readonly bookings = inject(BookingService);

  protected readonly code = signal('');
  protected readonly booking = signal<Booking | null>(null);
  protected readonly accessToken = signal('');
  protected readonly phone = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const code = this.route.snapshot.paramMap.get('code') ?? '';
    this.code.set(code);

    const cached = this.store.bookingFor(code);
    const token = cached?.accessToken ?? this.store.accessTokenFor(code);
    if (cached && token) {
      this.booking.set(cached);
      this.accessToken.set(token);
    }
  }

  protected lookup(event: Event): void {
    event.preventDefault();
    const phone = this.phone().trim();
    if (!phone) {
      this.error.set('Nhập số điện thoại đã dùng khi đặt phòng.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.bookings.lookup(this.code(), phone).subscribe({
      next: (found) => {
        this.loading.set(false);
        if (!found.accessToken) {
          this.error.set('Không mở được đơn này. Gọi 0294 3855 246 để được hỗ trợ.');
          return;
        }
        this.store.setCreatedBooking(found);
        this.accessToken.set(found.accessToken);
        this.booking.set(found);
      },
      error: (failure) => {
        this.loading.set(false);
        this.error.set(errorMessageOf(failure, 'Không tìm thấy đơn. Kiểm tra lại số điện thoại.'));
      },
    });
  }

  protected onConfirmed(): void {
    void this.router.navigate(['/dat-phong/hoan-tat', this.code()]);
  }

  protected rebook(): void {
    void this.router.navigate(['/dat-phong']);
  }
}
