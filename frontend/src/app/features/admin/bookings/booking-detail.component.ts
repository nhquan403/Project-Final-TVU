import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminBookingService } from '../../../core/services/admin-booking.service';
import type { AdminBookingDetail } from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiInput,
  UiSelect,
  UiSkeleton,
  UiStatusBadge,
  VndCurrencyPipe,
  type SelectOption,
} from '../../../shared/ui';

const MONEY = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

/** Các trạng thái quản trị viên đổi tới được, kèm nhãn tiếng Việt. */
const TARGETS: SelectOption[] = [
  { value: 'CONFIRMED', label: 'Xác nhận đơn' },
  { value: 'AWAITING_REVIEW', label: 'Chuyển chờ đối soát' },
  { value: 'CHECKED_IN', label: 'Nhận phòng' },
  { value: 'CHECKED_OUT', label: 'Trả phòng' },
  { value: 'NO_SHOW', label: 'Khách không đến' },
  { value: 'CANCELLED', label: 'Huỷ đơn' },
];

/**
 * Chi tiết một đơn: mọi thứ cần để quyết định, trên một màn hình.
 *
 * Gồm cả dòng thời gian trạng thái (kèm ai làm) lẫn trạng thái từng lá thư
 * trong hộp thư đi — câu hỏi "khách bảo không nhận được thư xác nhận" phải trả
 * lời được ngay tại đây, không phải bằng cách mở cơ sở dữ liệu.
 */
@Component({
  selector: 'app-booking-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    UiStatusBadge,
    UiButton,
    UiSelect,
    UiInput,
    UiSkeleton,
    UiConfirmDialog,
    VndCurrencyPipe,
  ],
  template: `
    <a routerLink="/admin/bookings" class="text-sm text-focus underline">← Về danh sách đơn</a>

    @if (loading()) {
      <div class="mt-4 space-y-2">
        <ui-skeleton shape="line" />
        <ui-skeleton shape="line" />
      </div>
    } @else if (error()) {
      <p class="mt-4 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ error() }}</p>
    } @else if (booking(); as data) {
      <header class="mt-3 mb-5 flex flex-wrap items-center gap-3">
        <h1 class="text-xl font-bold text-text">{{ data.code }}</h1>
        <ui-status-badge [status]="data.status" />
        <span class="text-sm text-text-muted">Thanh toán: {{ data.paymentStatus }}</span>
      </header>

      <div class="grid gap-4 lg:grid-cols-2">
        <section class="rounded-md border border-border bg-surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-text">Thông tin khách</h2>
          <dl class="space-y-1 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Họ tên</dt>
              <dd class="text-text">{{ data.guestName }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Điện thoại</dt>
              <dd class="text-text">{{ data.guestPhone }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Email</dt>
              <dd class="text-text">{{ data.guestEmail }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Lưu trú</dt>
              <dd class="text-text">
                {{ data.checkIn }} → {{ data.checkOut }} ({{ data.nights }} đêm)
              </dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Khách</dt>
              <dd class="text-text">{{ data.adults }} người lớn, {{ data.children }} trẻ em</dd>
            </div>
            @if (data.promotionCode) {
              <div class="flex justify-between gap-3">
                <dt class="text-text-muted">Mã giảm giá</dt>
                <dd class="text-text">{{ data.promotionCode }}</dd>
              </div>
            }
          </dl>

          @if (data.specialRequest) {
            <p class="mt-3 rounded-sm bg-surface-2 p-2 text-sm text-text">
              <span class="font-semibold">Yêu cầu đặc biệt:</span> {{ data.specialRequest }}
            </p>
          }
        </section>

        <section class="rounded-md border border-border bg-surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-text">Tiền và phòng</h2>
          <dl class="space-y-1 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Tổng tiền</dt>
              <dd class="font-semibold tabular-nums text-text">
                {{ data.totalAmount | vndCurrency }}
              </dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Tiền cọc</dt>
              <dd class="tabular-nums text-text">{{ data.depositAmount | vndCurrency }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-text-muted">Giảm giá</dt>
              <dd class="tabular-nums text-text">{{ data.discountAmount | vndCurrency }}</dd>
            </div>
          </dl>

          <h3 class="mt-4 mb-2 text-sm font-semibold text-text">Phòng được gán</h3>
          @if (data.rooms.length === 0) {
            <p class="text-sm text-text-muted">Đơn hiện không giữ phòng nào.</p>
          } @else {
            <ul class="space-y-1 text-sm text-text">
              @for (room of data.rooms; track room.roomId) {
                <li>Phòng {{ room.roomNumber }} · tầng {{ room.floor ?? '—' }} · {{ room.status }}</li>
              }
            </ul>
          }
        </section>

        <section class="rounded-md border border-border bg-surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-text">Dòng thời gian trạng thái</h2>
          <ol class="space-y-2 text-sm">
            @for (entry of data.history; track entry.createdAt + entry.toStatus) {
              <li class="border-l-2 border-border pl-3">
                <p class="text-text">
                  {{ entry.fromStatus ?? 'Tạo đơn' }} → <strong>{{ entry.toStatus }}</strong>
                </p>
                <p class="text-xs text-text-muted">
                  {{ entry.createdAt }} · {{ entry.actor }}
                  @if (entry.changedBy) {
                    · {{ entry.changedBy }}
                  }
                </p>
                @if (entry.note) {
                  <p class="text-xs text-text-muted">{{ entry.note }}</p>
                }
              </li>
            }
          </ol>
        </section>

        <section class="rounded-md border border-border bg-surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-text">Thanh toán và thư</h2>
          <ul class="space-y-2 text-sm">
            @for (payment of data.payments; track payment.id) {
              <li class="rounded-sm bg-surface-2 p-2">
                <p class="text-text">
                  Lần {{ payment.attemptNo }} · {{ payment.transferContent }}
                </p>
                <p class="text-xs text-text-muted">
                  Đã nhận {{ MONEY.format(payment.amountReceived) }} /
                  {{ MONEY.format(payment.amountExpected) }} đ ·
                  {{ payment.status }} · đối soát {{ payment.reconcileStatus }}
                </p>
              </li>
            }
          </ul>

          <h3 class="mt-4 mb-2 text-sm font-semibold text-text">Hộp thư đi</h3>
          @if (data.emails.length === 0) {
            <p class="text-sm text-text-muted">Chưa có thư nào được xếp hàng cho đơn này.</p>
          } @else {
            <ul class="space-y-1 text-sm">
              @for (email of data.emails; track email.id) {
                <li class="text-text">
                  {{ email.template }} → {{ email.status }} ({{ email.attempts }} lần thử)
                  @if (email.lastError) {
                    <span class="text-danger">· {{ email.lastError }}</span>
                  }
                </li>
              }
            </ul>
            <div class="mt-2">
              <ui-button variant="secondary" [loading]="resending()" (pressed)="resendEmail()">
                Gửi lại thư gần nhất
              </ui-button>
            </div>
          }
        </section>
      </div>

      <section class="mt-4 rounded-md border border-border bg-surface p-4">
        <h2 class="mb-3 text-sm font-semibold text-text">Thao tác</h2>
        <div class="grid gap-3 sm:grid-cols-[1fr_2fr_auto]">
          <ui-select label="Chuyển sang" [options]="targets" placeholder="Chọn trạng thái"
            [(value)]="target" />
          <ui-input label="Ghi chú (đi vào nhật ký)" [(value)]="note" />
          <div class="flex items-end">
            <ui-button [loading]="working()" (pressed)="requestTransition()">Thực hiện</ui-button>
          </div>
        </div>

        @if (actionError(); as message) {
          <p class="mt-3 rounded-sm bg-danger/10 p-2 text-sm text-danger" role="alert">
            {{ message }}
          </p>
        }
      </section>

      <section class="mt-4 rounded-md border border-border bg-surface p-4">
        <h2 class="mb-3 text-sm font-semibold text-text">Ghi chú nội bộ</h2>
        @if (data.notes.length === 0) {
          <p class="mb-3 text-sm text-text-muted">Chưa có ghi chú nào.</p>
        } @else {
          <ul class="mb-3 space-y-2 text-sm">
            @for (entry of data.notes; track entry.id) {
              <li class="rounded-sm bg-surface-2 p-2">
                <p class="text-text">{{ entry.content }}</p>
                <p class="text-xs text-text-muted">{{ entry.author }} · {{ entry.createdAt }}</p>
              </li>
            }
          </ul>
        }
        <div class="grid gap-3 sm:grid-cols-[1fr_auto]">
          <ui-input label="Thêm ghi chú" [(value)]="newNote" />
          <div class="flex items-end">
            <ui-button variant="secondary" (pressed)="addNote()">Lưu ghi chú</ui-button>
          </div>
        </div>
      </section>

      <ui-confirm-dialog
        [open]="confirming()"
        title="Huỷ đơn {{ data.code }}?"
        question="Thao tác này không hoàn tác được."
        [consequences]="cancelConsequences()"
        confirmLabel="Huỷ đơn"
        [loading]="working()"
        (confirmed)="applyTransition()"
        (cancelled)="confirming.set(false)" />
    }
  `,
})
export class BookingDetailComponent {
  private readonly bookings = inject(AdminBookingService);
  private readonly route = inject(ActivatedRoute);

  protected readonly MONEY = MONEY;
  protected readonly targets = TARGETS;

  protected readonly booking = signal<AdminBookingDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly working = signal(false);
  protected readonly resending = signal(false);
  protected readonly confirming = signal(false);

  protected readonly target = signal('');
  protected readonly note = signal('');
  protected readonly newNote = signal('');

  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  /**
   * Hậu quả CỤ THỂ của việc huỷ, dựng từ dữ liệu đơn đang mở.
   *
   * Câu "Bạn có chắc không?" không nói gì; câu "Phòng 203 sẽ được trả về kho"
   * và "khách đã đặt cọc 300.000đ, khoản này cần hoàn thủ công" mới là thứ
   * khiến người dùng dừng lại đúng lúc cần dừng.
   */
  protected readonly cancelConsequences = computed(() => {
    const data = this.booking();
    if (!data) {
      return [];
    }
    const lines: string[] = [];
    if (data.rooms.length) {
      const numbers = data.rooms.map((room) => room.roomNumber).join(', ');
      lines.push(`Phòng ${numbers} sẽ được trả về kho và có thể bán cho khách khác ngay lập tức.`);
    } else {
      lines.push('Đơn hiện không giữ phòng nào, nên không có phòng nào được trả lại.');
    }
    const received = data.payments.reduce((sum, payment) => sum + payment.amountReceived, 0);
    if (received > 0) {
      lines.push(`Khách đã chuyển ${MONEY.format(received)}đ — khoản này cần hoàn thủ công.`);
    } else {
      lines.push('Khách chưa chuyển đồng nào, không phát sinh hoàn tiền.');
    }
    if (data.promotionCode) {
      lines.push(`Lượt dùng của mã ${data.promotionCode} được hoàn lại.`);
    }
    return lines;
  });

  constructor() {
    this.load();
  }

  protected requestTransition(): void {
    this.actionError.set(null);
    if (!this.target()) {
      this.actionError.set('Hãy chọn trạng thái muốn chuyển sang.');
      return;
    }
    // Chỉ hành động PHÁ HUỶ mới cần hộp thoại. Hỏi lại ở mọi thao tác sẽ khiến
    // người dùng bấm Đồng ý theo phản xạ, và hộp thoại mất luôn tác dụng ở đúng
    // lần nó cần có tác dụng.
    if (this.target() === 'CANCELLED') {
      this.confirming.set(true);
      return;
    }
    this.applyTransition();
  }

  protected applyTransition(): void {
    this.working.set(true);
    this.bookings.transition(this.id, this.target(), this.note() || undefined).subscribe({
      next: (data) => {
        this.booking.set(data);
        this.working.set(false);
        this.confirming.set(false);
        this.target.set('');
        this.note.set('');
      },
      error: (response) => {
        this.working.set(false);
        this.confirming.set(false);
        this.actionError.set(
          response?.error?.detail ?? 'Không thực hiện được thao tác này.',
        );
      },
    });
  }

  protected addNote(): void {
    if (!this.newNote().trim()) {
      return;
    }
    this.bookings.addNote(this.id, this.newNote().trim()).subscribe({
      next: (data) => {
        this.booking.set(data);
        this.newNote.set('');
      },
      error: () => this.actionError.set('Không lưu được ghi chú.'),
    });
  }

  protected resendEmail(): void {
    this.resending.set(true);
    this.bookings.resendEmail(this.id).subscribe({
      next: () => {
        this.resending.set(false);
        this.load();
      },
      error: (response) => {
        this.resending.set(false);
        this.actionError.set(response?.error?.detail ?? 'Không gửi lại được thư.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.bookings.detail(this.id).subscribe({
      next: (data) => {
        this.booking.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được chi tiết đơn.');
        this.loading.set(false);
      },
    });
  }
}
