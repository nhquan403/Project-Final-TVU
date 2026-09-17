import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import type {
  AffectedBooking,
  RoomClosureView,
  RoomView,
} from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiDateRangePicker,
  UiInput,
  UiSelect,
  type DateRange,
  type SelectOption,
} from '../../../shared/ui';

/** `dd/MM` từ khoá ngày `YYYY-MM-DD`. Không dựng `Date` — chuỗi đã đủ và không lệch múi giờ. */
function dayMonth(key: string): string {
  const [, month, day] = key.split('-');
  return `${day}/${month}`;
}

/**
 * Khoảng ngày một phòng không nhận khách.
 *
 * <p>Toàn bộ câu chữ ở đây phục vụ một việc: làm cho quy ước NỬA MỞ nhìn thấy
 * được. Backend chặn các đêm `[fromDate, toDate)`, nên hiện hai ô ngày trần
 * "20/10 – 25/10" sẽ khiến chủ homestay tin là đêm 25 cũng bị đóng. Dòng tóm
 * tắt luôn viết "N đêm: 20/10 – 24/10 (mở lại từ 25/10)" — đêm cuối và ngày mở
 * lại là hai thứ khác nhau và màn hình nói ra cả hai.
 */
@Component({
  selector: 'app-room-closure-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiSelect, UiInput, UiDateRangePicker, UiConfirmDialog],
  template: `
    <section class="mt-5 rounded-md border border-border bg-surface p-4">
      <h2 class="mb-1 text-sm font-semibold text-text">Ngày không nhận khách</h2>
      <p class="mb-3 text-xs text-text-muted">
        Đóng phòng theo khoảng ngày rồi hệ thống tự mở lại — không phải nhớ bật tắt bằng tay.
        Ngày mở bán lại là ngày phòng bán được trở lại, không phải đêm cuối bị đóng.
      </p>

      @if (error(); as message) {
        <p class="mb-3 rounded-sm bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
      }

      <ui-select label="Phòng" [options]="roomOptions()" placeholder="Chọn phòng"
        [value]="selectedRoom()" (valueChange)="pickRoom($event)" />

      @if (selectedRoom()) {
        <div class="mt-3">
          @if (closures().length) {
            <ul class="space-y-2">
              @for (closure of closures(); track closure.id) {
                <li class="flex flex-wrap items-center justify-between gap-2 rounded-sm bg-surface-muted p-3">
                  <div>
                    <p class="text-sm font-semibold text-text">{{ summary(closure) }}</p>
                    @if (closure.reason) {
                      <p class="text-xs text-text-muted">{{ closure.reason }}</p>
                    }
                  </div>
                  <ui-button variant="danger" (pressed)="askRemove(closure)">Xoá</ui-button>
                </li>
              }
            </ul>
          } @else {
            <p class="rounded-sm bg-surface-muted p-3 text-sm text-text-muted">
              Phòng này đang bán mọi ngày — chưa có khoảng đóng nào.
            </p>
          }

          <div class="mt-4 grid gap-3 sm:grid-cols-2">
            <ui-date-range-picker label="Đóng từ ngày – mở bán lại từ ngày" [(value)]="range" />
            <ui-input label="Lý do" placeholder="Sơn lại phòng, sửa điều hoà…" [(value)]="reason" />
          </div>
          @if (preview(); as text) {
            <p class="mt-2 text-sm text-text">Sẽ đóng {{ text }}</p>
          }
          <div class="mt-3">
            <ui-button [loading]="saving()" (pressed)="add()">Thêm khoảng đóng</ui-button>
          </div>

          @if (affected().length) {
            <div class="mt-3 rounded-sm bg-warning/10 p-3">
              <p class="text-sm font-semibold text-warning">
                {{ affected().length }} đơn nằm trong khoảng vừa đóng — hệ thống KHÔNG huỷ đơn nào.
              </p>
              <ul class="mt-1 space-y-1 text-sm text-text">
                @for (booking of affected(); track booking.code) {
                  <li>
                    {{ booking.code }} · {{ booking.guestName }} · {{ booking.checkIn }} →
                    {{ booking.checkOut }} · {{ booking.status }}
                  </li>
                }
              </ul>
              <p class="mt-1 text-xs text-text-muted">
                Hãy đổi phòng cho những khách này, hoặc dời lịch sửa chữa.
              </p>
            </div>
          }
        </div>
      }
    </section>

    <ui-confirm-dialog
      [open]="pendingRemoval() !== null"
      title="Xoá khoảng đóng phòng?"
      [question]="removalQuestion()"
      [consequences]="[
        'Phòng bán lại được ngay cho những đêm đó.',
        'Khách có thể đặt trúng khoảng ngày đang sửa chữa.',
      ]"
      confirmLabel="Xoá, mở bán lại"
      (confirmed)="confirmRemove()"
      (cancelled)="pendingRemoval.set(null)" />
  `,
})
export class RoomClosurePanel {
  private readonly catalog = inject(AdminCatalogService);

  readonly rooms = input.required<RoomView[]>();

  protected readonly closures = signal<RoomClosureView[]>([]);
  protected readonly affected = signal<AffectedBooking[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly selectedRoom = signal('');
  protected readonly reason = signal('');
  protected readonly range = signal<DateRange>({ checkIn: null, checkOut: null });
  protected readonly pendingRemoval = signal<RoomClosureView | null>(null);

  protected readonly roomOptions = computed<SelectOption[]>(() =>
    this.rooms().map((room) => ({
      value: String(room.id),
      label: `${room.roomNumber} — ${room.roomTypeName}`,
    })),
  );

  /** Bản xem trước của khoảng đang chọn, dùng đúng câu chữ với dòng đã lưu. */
  protected readonly preview = computed(() => {
    const { checkIn, checkOut } = this.range();
    if (!checkIn || !checkOut) {
      return null;
    }
    const nights = Math.round(
      (Date.parse(`${checkOut}T00:00:00Z`) - Date.parse(`${checkIn}T00:00:00Z`)) / 86_400_000,
    );
    return nights > 0 ? this.phrase(nights, checkIn, checkOut) : null;
  });

  protected summary(closure: RoomClosureView): string {
    return this.phrase(closure.nights, closure.fromDate, closure.toDate);
  }

  protected removalQuestion(): string {
    const closure = this.pendingRemoval();
    return closure ? `Phòng ${closure.roomNumber}, ${this.summary(closure)}.` : '';
  }

  /**
   * Đêm cuối bị đóng là `toDate - 1`, không phải `toDate`.
   *
   * Đây là chỗ duy nhất trong màn hình biết điều đó, nên nó chỉ có một cách sai
   * thay vì ba.
   */
  private phrase(nights: number, fromDate: string, toDate: string): string {
    const lastNight = new Date(Date.parse(`${toDate}T00:00:00Z`) - 86_400_000)
      .toISOString()
      .slice(0, 10);
    return `${nights} đêm: ${dayMonth(fromDate)} – ${dayMonth(lastNight)} (mở lại từ ${dayMonth(toDate)})`;
  }

  /** Đổi phòng thì xoá luôn cảnh báo của phòng trước — nó không nói về phòng này. */
  protected pickRoom(roomId: string): void {
    this.selectedRoom.set(roomId);
    this.affected.set([]);
    this.loadClosures();
  }

  protected loadClosures(): void {
    const roomId = this.selectedRoom();
    if (!roomId) {
      this.closures.set([]);
      return;
    }
    this.catalog.closures(Number(roomId)).subscribe({
      next: (list) => {
        this.closures.set(list);
        this.error.set(null);
      },
      error: () => this.error.set('Không tải được danh sách khoảng đóng.'),
    });
  }

  protected add(): void {
    const { checkIn, checkOut } = this.range();
    if (!this.selectedRoom() || !checkIn || !checkOut) {
      this.error.set('Chọn phòng và khoảng ngày trước khi thêm.');
      return;
    }
    this.saving.set(true);
    this.catalog
      .addClosure(Number(this.selectedRoom()), {
        fromDate: checkIn,
        toDate: checkOut,
        reason: this.reason().trim() || null,
      })
      .subscribe({
        next: (result) => {
          this.affected.set(result.affectedBookings);
          this.range.set({ checkIn: null, checkOut: null });
          this.reason.set('');
          this.error.set(null);
          this.saving.set(false);
          this.loadClosures();
        },
        error: (response) => {
          this.saving.set(false);
          this.error.set(response?.error?.detail ?? 'Không thêm được khoảng đóng.');
        },
      });
  }

  protected askRemove(closure: RoomClosureView): void {
    this.pendingRemoval.set(closure);
  }

  protected confirmRemove(): void {
    const closure = this.pendingRemoval();
    if (!closure) {
      return;
    }
    this.catalog.removeClosure(closure.id).subscribe({
      next: () => {
        this.pendingRemoval.set(null);
        this.loadClosures();
      },
      error: () => {
        this.pendingRemoval.set(null);
        this.error.set('Không xoá được khoảng đóng.');
      },
    });
  }
}
