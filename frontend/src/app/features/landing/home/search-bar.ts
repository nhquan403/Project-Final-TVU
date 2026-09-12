import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AvailabilityService } from '../../../core/services/availability.service';
import type { AvailabilityMap } from '../../../shared/ui';
import { UiButton, UiDateRangePicker, UiGuestStepper, type DateRange } from '../../../shared/ui';
import { BookingFlowStore } from '../booking/booking-flow.store';

/** Bao nhiêu ngày tới được nạp vào lịch một lượt. Backend cho tối đa 120. */
const CALENDAR_DAYS = 120;

/**
 * Thanh tìm phòng: ngày, số khách, nút tìm.
 *
 * <p>Lịch được nạp bản đồ ngày của TOÀN homestay (không kèm `roomTypeId`), nên
 * ngày đã kín phòng bị chặn TRƯỚC khi khách bấm chọn — thay vì cho chọn rồi báo
 * lỗi ở bước sau.
 *
 * <p>Giá hiển thị trên ô ngày là mức THẤP NHẤT trong các loại còn chỗ đêm đó.
 * Hệ thống chưa có bảng giá theo đêm, nên phần lớn trường hợp con số này giống
 * nhau ở mọi ngày — giao diện vì thế không vẽ nó như một biểu đồ giá thay đổi.
 */
@Component({
  selector: 'app-search-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiDateRangePicker, UiGuestStepper, UiButton],
  template: `
    <form
      class="rounded-lg border border-border bg-surface p-4 shadow-sm"
      (submit)="submit($event)"
      (focusin)="focused.emit(true)"
      (focusout)="focused.emit(false)">
      <div class="grid gap-3 md:grid-cols-[2fr_1fr_1fr_auto]">
        <ui-date-range-picker
          [availability]="calendar()"
          [loading]="loadingCalendar()"
          [error]="error()"
          [(value)]="dates" />

        <ui-guest-stepper label="Người lớn" [min]="1" [max]="20" [(value)]="adults" />
        <ui-guest-stepper label="Trẻ em" [min]="0" [max]="20" [(value)]="children" />

        <div class="flex items-end">
          <ui-button type="submit" [fullWidth]="true">Tìm phòng</ui-button>
        </div>
      </div>
    </form>
  `,
})
export class SearchBar {
  private readonly availability = inject(AvailabilityService);
  private readonly store = inject(BookingFlowStore);
  private readonly router = inject(Router);

  /** Trang chủ dùng tín hiệu này để làm mờ nền khi thanh tìm kiếm được focus. */
  readonly focused = output<boolean>();

  protected readonly dates = signal<DateRange>({ checkIn: null, checkOut: null });
  protected readonly adults = signal(2);
  protected readonly children = signal(0);
  protected readonly calendar = signal<AvailabilityMap>({});
  protected readonly loadingCalendar = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const params = this.store.params();
    this.dates.set({ checkIn: params.checkIn || null, checkOut: params.checkOut || null });
    this.adults.set(params.adults);
    this.children.set(params.children);
    this.loadCalendar();
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const range = this.dates();
    if (!range.checkIn || !range.checkOut) {
      this.error.set('Hãy chọn ngày nhận và ngày trả phòng.');
      return;
    }
    this.error.set(null);
    this.store.patchSearch({
      checkIn: range.checkIn,
      checkOut: range.checkOut,
      adults: this.adults(),
      children: this.children(),
      roomTypeId: null,
    });
    void this.router.navigate(['/dat-phong'], {
      queryParams: {
        checkIn: range.checkIn,
        checkOut: range.checkOut,
        adults: this.adults(),
        children: this.children() || null,
      },
    });
  }

  private loadCalendar(): void {
    const today = new Date();
    const from = toIsoDate(today);
    const to = toIsoDate(new Date(today.getTime() + CALENDAR_DAYS * 86_400_000));

    // Không truyền roomTypeId: đây là lịch của cả homestay, vì lúc này khách
    // chưa chọn loại phòng nào.
    this.availability.calendar(null, from, to).subscribe({
      next: (map) => {
        this.calendar.set(map);
        this.loadingCalendar.set(false);
      },
      error: () => {
        // Lịch hỏng KHÔNG được chặn việc tìm phòng: khách vẫn chọn ngày được,
        // chỉ mất phần chặn trước. Backend vẫn từ chối ngày hết phòng.
        this.calendar.set({});
        this.loadingCalendar.set(false);
      },
    });
  }
}

/** `YYYY-MM-DD` theo giờ ĐỊA PHƯƠNG, không dùng toISOString (nó đổi sang UTC). */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
