import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  type OnInit,
  output,
  signal,
} from '@angular/core';
import { BookingService, type Booking, type PaymentStatus } from '../../../core/services/booking.service';
import { UiButton, UiStatusBadge, VndCurrencyPipe, type BookingStatus } from '../../../shared/ui';

/** Trạng thái mà đơn không đi tiếp được nữa — hỏi thêm cũng không đổi kết quả. */
const FINAL_STATUSES = new Set([
  'CONFIRMED',
  'CANCELLED',
  'EXPIRED',
  'CHECKED_IN',
  'CHECKED_OUT',
  'NO_SHOW',
]);

const POLL_INTERVAL_MS = 3_000;

/**
 * Trần thời gian hỏi trạng thái.
 *
 * <p>Đơn chuyển thiếu tiền được gia hạn giữ chỗ tới 24 giờ. Không có trần này,
 * một tab để quên sẽ gọi API mỗi 3 giây suốt một ngày — 28.800 request cho một
 * khách đã bỏ đi.
 */
const MAX_POLL_MS = 15 * 60_000;

@Component({
  selector: 'app-payment-qr',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiStatusBadge, VndCurrencyPipe],
  template: `
    <section class="mx-auto max-w-lg rounded-lg border border-border bg-surface p-5" aria-live="polite">
      <header class="mb-4 flex items-start justify-between gap-3">
        <div>
          <h2 class="text-lg font-bold text-text">Thanh toán giữ chỗ</h2>
          <p class="text-sm text-text-muted">
            Mã đơn <strong class="font-mono">{{ booking().code }}</strong>
          </p>
        </div>
        <ui-status-badge [status]="currentStatus()" />
      </header>

      @if (settled()) {
        <p class="rounded-md bg-success/10 p-3 text-sm font-semibold text-success">
          Đã nhận được tiền cọc. Thư xác nhận đang trên đường tới hộp thư của bạn.
        </p>
      } @else if (awaitingReview()) {
        <!--
          Đã trả một phần thì KHÔNG được báo hết hạn: phòng vẫn đang được giữ,
          và một dòng "hết hạn" ở đây sẽ khiến khách đã chuyển tiền tưởng mình
          mất tiền rồi chuyển thêm một lần nữa.
        -->
        <p class="rounded-md bg-warning/10 p-3 text-sm text-warning">
          Homestay đã nhận được
          <strong>{{ (status()?.amountReceived ?? 0) | vndCurrency }}</strong>
          trên tổng
          <strong>{{ (status()?.amountExpected ?? booking().depositAmount) | vndCurrency }}</strong>.
          Đơn của bạn đang chờ đối soát, phòng vẫn được giữ. Nhân viên sẽ liên hệ nếu cần.
        </p>
      } @else if (expired()) {
        <div class="rounded-md bg-danger/10 p-3">
          <p class="text-sm font-semibold text-danger">Đã hết hạn giữ chỗ.</p>
          <p class="mt-1 text-sm text-text-muted">
            Phòng đã được mở lại cho khách khác. Bạn có thể đặt lại nếu vẫn còn phòng trống.
          </p>
          <div class="mt-3">
            <ui-button variant="primary" (pressed)="rebook.emit()">Đặt lại</ui-button>
          </div>
        </div>
      } @else {
        <p class="mb-3 text-sm text-text-muted">
          Chuyển khoản đúng số tiền và đúng nội dung bên dưới. Màn hình tự chuyển khi tiền về —
          không cần bấm gì thêm.
        </p>

        @if (qrImageUrl(); as src) {
          <img
            [src]="src"
            width="280"
            height="280"
            class="mx-auto block rounded-md border border-border"
            alt="Mã QR chuyển khoản {{ transferContent() }}"
          />
        }

        <!--
          Số tài khoản và nội dung LUÔN hiện dạng chữ, kể cả khi có ảnh QR. Ảnh
          QR cần mạng để tải và cần một ứng dụng ngân hàng quét được; thiếu một
          trong hai thì khách vẫn phải gõ tay được.
        -->
        <dl class="mt-4 space-y-2 text-sm">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-text-muted">Số tiền</dt>
            <dd class="font-bold text-text">{{ amount() | vndCurrency }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-text-muted">Ngân hàng</dt>
            <dd class="text-text">{{ booking().payment?.bankCode }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-text-muted">Số tài khoản</dt>
            <dd class="flex items-center gap-2">
              <span class="font-mono text-text">{{ accountNumber() }}</span>
              <button
                type="button"
                class="min-h-[var(--touch-min)] rounded-sm px-2 text-xs font-semibold text-focus underline"
                (click)="copy(accountNumber(), 'account')"
              >
                {{ copied() === 'account' ? 'Đã sao chép' : 'Sao chép' }}
              </button>
            </dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-text-muted">Nội dung chuyển khoản</dt>
            <dd class="flex items-center gap-2">
              <span class="font-mono font-bold text-text">{{ transferContent() }}</span>
              <button
                type="button"
                class="min-h-[var(--touch-min)] rounded-sm px-2 text-xs font-semibold text-focus underline"
                (click)="copy(transferContent(), 'content')"
              >
                {{ copied() === 'content' ? 'Đã sao chép' : 'Sao chép' }}
              </button>
            </dd>
          </div>
        </dl>

        <p class="mt-4 text-center text-sm text-text-muted">
          Còn lại <strong class="font-mono text-text">{{ countdown() }}</strong> để hoàn tất
        </p>
      }
    </section>
  `,
})
export class PaymentQrComponent implements OnInit {
  private readonly bookings = inject(BookingService);

  readonly booking = input.required<Booking>();
  /** Bí mật thao tác của đơn. Không có nó thì backend trả 401, đúng như thiết kế. */
  readonly accessToken = input.required<string>();

  readonly confirmed = output<PaymentStatus>();
  readonly rebook = output<void>();

  protected readonly status = signal<PaymentStatus | null>(null);
  protected readonly copied = signal<'account' | 'content' | null>(null);

  private readonly now = signal(Date.now());
  private readonly startedAt = Date.now();
  private pollTimer?: ReturnType<typeof setInterval>;
  private tickTimer?: ReturnType<typeof setInterval>;

  constructor() {
    // Dọn dẹp khi component biến mất. Thiếu bước này, rời trang giữa chừng để
    // lại một bộ đếm gọi API mãi mãi — và mỗi lần khách quay lại là thêm một bộ.
    inject(DestroyRef).onDestroy(() => this.stopPolling());
  }

  /**
   * Vòng hỏi trạng thái bắt đầu ở {@link ngOnInit}, KHÔNG ở hàm dựng.
   *
   * <p>Angular gán giá trị cho input SAU khi dựng xong đối tượng. Đọc
   * `booking()` — một `input.required` — trong hàm dựng ném NG0950 và cả màn
   * hình thanh toán trắng trơn: khách vừa bấm đặt phòng xong nhìn thấy một
   * trang trống, đúng lúc cần mã QR nhất.
   */
  ngOnInit(): void {
    this.pollTimer = setInterval(() => this.poll(), POLL_INTERVAL_MS);
    this.tickTimer = setInterval(() => this.now.set(Date.now()), 1_000);
    this.poll();
  }

  protected readonly currentStatus = computed<BookingStatus>(
    () => this.status()?.status ?? this.booking().status,
  );

  protected readonly settled = computed(() => this.currentStatus() === 'CONFIRMED');

  protected readonly awaitingReview = computed(
    () => this.currentStatus() === 'AWAITING_REVIEW' || (this.status()?.amountReceived ?? 0) > 0,
  );

  protected readonly expired = computed(
    () => this.currentStatus() === 'EXPIRED' || this.remainingMs() <= 0,
  );

  protected readonly amount = computed(
    () => this.status()?.amountExpected ?? this.booking().payment?.amount ?? this.booking().depositAmount,
  );

  protected readonly qrImageUrl = computed(() => this.booking().payment?.qrImageUrl ?? null);
  protected readonly accountNumber = computed(() => this.booking().payment?.accountNumber ?? '');
  protected readonly transferContent = computed(() => this.booking().payment?.transferContent ?? '');

  private readonly deadline = computed(() => {
    const raw =
      this.status()?.holdExpiresAt ?? this.booking().payment?.expiresAt ?? this.booking().holdExpiresAt;
    return raw ? new Date(raw).getTime() : Number.POSITIVE_INFINITY;
  });

  protected readonly remainingMs = computed(() => this.deadline() - this.now());

  protected readonly countdown = computed(() => {
    const remaining = Math.max(0, this.remainingMs());
    if (!Number.isFinite(remaining)) {
      return '--:--';
    }
    const totalSeconds = Math.floor(remaining / 1000);
    const minutes = `${Math.floor(totalSeconds / 60)}`.padStart(2, '0');
    const seconds = `${totalSeconds % 60}`.padStart(2, '0');
    return `${minutes}:${seconds}`;
  });

  protected copy(value: string, which: 'account' | 'content'): void {
    navigator.clipboard?.writeText(value).then(
      () => this.copied.set(which),
      // Trình duyệt từ chối quyền clipboard là chuyện bình thường; giá trị vẫn
      // hiện ra dạng chữ nên khách gõ tay được, không cần báo lỗi.
      () => this.copied.set(null),
    );
  }

  private poll(): void {
    if (this.shouldStop()) {
      this.stopPolling();
      return;
    }
    this.bookings.paymentStatus(this.booking().code, this.accessToken()).subscribe({
      next: (status) => {
        this.status.set(status);
        if (status.status === 'CONFIRMED') {
          this.confirmed.emit(status);
        }
        if (this.shouldStop()) {
          this.stopPolling();
        }
      },
      // Một lần hỏng không dừng vòng hỏi: mất mạng chốc lát là chuyện thường,
      // và dừng hẳn nghĩa là màn hình đứng im kể cả khi tiền đã về.
      error: () => undefined,
    });
  }

  /** Ba điều kiện dừng: trạng thái cuối, hết hạn giữ chỗ, hoặc quá trần thời gian. */
  private shouldStop(): boolean {
    const status: string = this.status()?.status ?? this.booking().status;
    return (
      FINAL_STATUSES.has(status) ||
      Date.now() >= this.deadline() ||
      Date.now() - this.startedAt >= MAX_POLL_MS
    );
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = undefined;
    }
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = undefined;
    }
  }
}
