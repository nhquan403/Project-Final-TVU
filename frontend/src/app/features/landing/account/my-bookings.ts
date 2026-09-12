import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BookingService, type Booking } from '../../../core/services/booking.service';
import { UiEmptyState, UiPagination, UiSkeleton } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';
import { BookingSummaryCard } from '../lookup/booking-summary-card';

const PAGE_SIZE = 5;

/**
 * Đơn của tài khoản đang đăng nhập.
 *
 * <p>Backend lọc theo token của phiên, không theo tham số nào trên URL — đổi số
 * trên thanh địa chỉ không xem được đơn của người khác.
 *
 * <p>Huỷ đơn ở đây KHÔNG kèm số điện thoại: `requireByCodeAndTokenOrPhone` nhận
 * mã truy cập của đơn, và đơn của chính mình thì đã có sẵn mã đó trong phản hồi.
 */
@Component({
  selector: 'app-my-bookings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BookingSummaryCard, UiPagination, UiSkeleton, UiEmptyState],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-8">
      <h1 class="text-h1 font-bold text-text">Đơn của tôi</h1>

      @if (loading()) {
        <div class="mt-6 flex flex-col gap-3">
          <ui-skeleton shape="row" />
          <ui-skeleton shape="row" />
          <ui-skeleton shape="row" />
        </div>
      } @else if (error()) {
        <div class="mt-6">
          <ui-empty-state
            title="Không tải được danh sách đơn"
            [description]="error()!"
            actionLabel="Thử lại"
            (action)="load(page())" />
        </div>
      } @else if (bookings().length === 0) {
        <div class="mt-6">
          <ui-empty-state
            title="Bạn chưa có đơn đặt phòng nào"
            description="Chọn ngày và loại phòng để đặt chuyến đầu tiên."
            actionLabel="Tìm phòng"
            (action)="goToBooking()" />
        </div>
      } @else {
        <ul class="mt-6 flex flex-col gap-4">
          @for (booking of bookings(); track booking.code) {
            <li>
              <app-booking-summary-card
                [booking]="booking"
                [cancelling]="cancellingCode() === booking.code"
                (cancelRequested)="cancel(booking)" />
            </li>
          }
        </ul>

        @if (totalPages() > 1) {
          <div class="mt-6">
            <!-- ui-pagination đếm từ 1, Spring Data đếm từ 0. Quy đổi ở đúng
                 ranh giới này, không để lệch một trang lan vào phần còn lại. -->
            <ui-pagination
              [page]="page() + 1"
              [totalPages]="totalPages()"
              [loading]="loading()"
              (pageChange)="load($event - 1)" />
          </div>
        }
      }
    </div>
  `,
})
export class MyBookingsPage {
  private readonly bookings_ = inject(BookingService);
  private readonly router = inject(Router);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly bookings = signal<Booking[]>([]);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly cancellingCode = signal<string | null>(null);

  constructor() {
    this.load(0);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.page.set(page);
    this.bookings_.myBookings(page, PAGE_SIZE).subscribe({
      next: (response) => {
        this.bookings.set(response.content);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (failure) => {
        this.error.set(errorMessageOf(failure, 'Máy chủ chưa trả lời. Thử lại sau giây lát.'));
        this.loading.set(false);
      },
    });
  }

  protected cancel(booking: Booking): void {
    this.cancellingCode.set(booking.code);
    this.bookings_
      .cancel(booking.code, { token: booking.accessToken ?? undefined })
      .subscribe({
        next: () => {
          this.cancellingCode.set(null);
          this.load(this.page());
        },
        error: (failure) => {
          this.cancellingCode.set(null);
          this.error.set(errorMessageOf(failure, 'Không huỷ được đơn. Thử lại sau giây lát.'));
        },
      });
  }

  protected goToBooking(): void {
    void this.router.navigate(['/dat-phong']);
  }
}
