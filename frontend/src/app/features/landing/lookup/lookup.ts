import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BookingService, type Booking } from '../../../core/services/booking.service';
import { UiButton, UiConfirmDialog, UiInput, UiToast, VndCurrencyPipe } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';
import { BookingSummaryCard } from './booking-summary-card';

/**
 * Dùng cho chuỗi ghép trong TypeScript.
 *
 * Pipe chỉ chạy được trong template, nhưng câu cảnh báo huỷ đơn là một chuỗi
 * dựng trong lớp. Gọi lại chính pipe thay vì tự viết một cách định dạng thứ
 * hai: hai cách định dạng là hai cách sẽ lệch nhau.
 */
const MONEY = new VndCurrencyPipe();

/**
 * Tra cứu đơn bằng mã đơn và số điện thoại.
 *
 * <h2>Vì sao đòi cả hai</h2>
 *
 * Mã đơn đi vào nội dung chuyển khoản, nên nó nằm trên sao kê ngân hàng của
 * homestay và trong tin nhắn biến động số dư. Nếu chỉ cần mã là tra được thì
 * bất kỳ ai nhìn thấy sao kê cũng đọc được tên, số điện thoại và lịch nghỉ của
 * khách. Số điện thoại là thứ chỉ chủ đơn biết chắc.
 *
 * <h2>Sai thì nói một câu duy nhất</h2>
 *
 * Backend trả cùng một lỗi `BOOKING_NOT_FOUND` cho "không có mã này" và "có mã
 * nhưng sai số điện thoại". Giao diện giữ nguyên cách đó: phân biệt hai câu là
 * biến ô tra cứu thành công cụ dò xem mã nào có thật.
 */
@Component({
  selector: 'app-lookup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton, UiToast, UiConfirmDialog, BookingSummaryCard],
  template: `
    <div class="mx-auto max-w-2xl px-4 py-8">
      <h1 class="text-h1 font-bold text-text">Tra cứu đơn đặt phòng</h1>
      <p class="mt-2 text-text-muted">
        Nhập mã đơn trong thư xác nhận và số điện thoại bạn đã dùng khi đặt.
      </p>

      <form class="mt-6 flex flex-col gap-3" (submit)="submit($event)">
        <ui-input
          label="Mã đơn"
          [required]="true"
          placeholder="Ví dụ: TVH-2026-000123"
          [(value)]="code" />
        <ui-input
          label="Số điện thoại"
          type="tel"
          [required]="true"
          placeholder="09xxxxxxxx"
          [error]="error()"
          [(value)]="phone" />
        <ui-button type="submit" [fullWidth]="true" [loading]="loading()">Tra cứu</ui-button>
      </form>

      @if (booking(); as data) {
        <div class="mt-6">
          <app-booking-summary-card
            [booking]="data"
            [cancelling]="cancelling()"
            (cancelRequested)="confirmOpen.set(true)" />
        </div>
      }

      <ui-confirm-dialog
        [open]="confirmOpen()"
        title="Huỷ đơn đặt phòng"
        [question]="'Huỷ đơn ' + (booking()?.code ?? '') + '?'"
        [consequences]="cancelConsequences()"
        confirmLabel="Huỷ đơn"
        cancelLabel="Giữ đơn"
        [loading]="cancelling()"
        (confirmed)="cancel()"
        (cancelled)="confirmOpen.set(false)" />

      @if (toast(); as message) {
        <ui-toast kind="success" [message]="message" (dismissed)="toast.set(null)" />
      }
    </div>
  `,
})
export class LookupPage {
  private readonly bookings = inject(BookingService);
  private readonly route = inject(ActivatedRoute);

  protected readonly code = signal('');
  protected readonly phone = signal('');
  protected readonly booking = signal<Booking | null>(null);
  protected readonly loading = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly confirmOpen = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly toast = signal<string | null>(null);

  /**
   * Hậu quả cụ thể, không phải "Bạn có chắc không?".
   *
   * Số tiền cọc lấy từ chính đơn đang xem, nên khách thấy đúng con số của mình
   * chứ không phải một câu chung chung mà ai cũng bấm qua.
   */
  protected readonly cancelConsequences = computed(() => {
    const current = this.booking();
    const lines = ['Phòng được mở lại cho khách khác ngay lập tức, và có thể không còn nếu bạn đổi ý.'];
    if (current && current.depositAmount > 0) {
      lines.push(
        `Tiền cọc ${MONEY.transform(current.depositAmount)} đã trả được homestay hoàn thủ công — gọi 0294 3855 246 để được hướng dẫn.`,
      );
    }
    lines.push('Thao tác này không hoàn tác được.');
    return lines;
  });

  constructor() {
    // Trang "đặt phòng thành công" dẫn sang đây kèm sẵn mã đơn. Số điện thoại
    // thì không — nó là thứ chứng minh danh tính, nên phải do khách tự gõ.
    this.code.set(this.route.snapshot.queryParamMap.get('code') ?? '');
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const code = this.code().trim();
    const phone = this.phone().trim();
    if (!code || !phone) {
      this.error.set('Nhập cả mã đơn và số điện thoại.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.bookings.lookup(code, phone).subscribe({
      next: (found) => {
        this.booking.set(found);
        this.loading.set(false);
      },
      error: (failure) => {
        this.booking.set(null);
        this.loading.set(false);
        this.error.set(
          errorMessageOf(failure, 'Không tìm thấy đơn. Kiểm tra lại mã và số điện thoại.'),
        );
      },
    });
  }

  protected cancel(): void {
    const current = this.booking();
    if (!current) {
      return;
    }
    this.cancelling.set(true);
    this.bookings
      .cancel(current.code, {
        token: current.accessToken ?? undefined,
        phone: this.phone().trim(),
      })
      .subscribe({
        next: (updated) => {
          this.booking.set(updated);
          this.cancelling.set(false);
          this.confirmOpen.set(false);
          this.toast.set('Đã huỷ đơn. Phòng được mở lại cho khách khác.');
        },
        error: (failure) => {
          this.cancelling.set(false);
          this.confirmOpen.set(false);
          this.error.set(errorMessageOf(failure, 'Không huỷ được đơn. Thử lại sau giây lát.'));
        },
      });
  }
}
