import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AvailabilityService } from '../../../core/services/availability.service';
import {
  UiButton,
  UiDateRangePicker,
  UiGuestStepper,
  type AvailabilityMap,
  type DateRange,
} from '../../../shared/ui';
import { BookingFlowStore } from './booking-flow.store';

const CALENDAR_DAYS = 120;

/**
 * Bước 1: chọn ngày và số khách.
 *
 * <p>Lịch nạp bản đồ ngày của TOÀN homestay, nên ngày đã kín mọi loại phòng bị
 * chặn ngay trong lịch. Ngày quá khứ do `ui-date-range-picker` tự chặn theo giờ
 * máy khách — backend vẫn kiểm lại, vì giờ máy khách là thứ khách tự đặt được.
 */
@Component({
  selector: 'app-step-dates',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiDateRangePicker, UiGuestStepper, UiButton],
  template: `
    <section class="rounded-lg border border-border bg-surface p-4" aria-labelledby="buoc-1">
      <h2 id="buoc-1" class="text-h2 font-bold text-text">Chọn ngày và số khách</h2>

      <div class="mt-4 grid gap-4 md:grid-cols-2">
        <ui-date-range-picker
          [availability]="calendar()"
          [loading]="calendarLoading()"
          [error]="error()"
          [(value)]="dates" />

        <div class="flex flex-col gap-3">
          <ui-guest-stepper label="Người lớn" [min]="1" [max]="20" [(value)]="adults" />
          <ui-guest-stepper label="Trẻ em" [min]="0" [max]="20" [(value)]="children" />
          <ui-guest-stepper
            label="Số phòng"
            hint="Số khách ở trên được chia đều cho từng phòng."
            [min]="1"
            [max]="10"
            [(value)]="roomQuantity" />
        </div>
      </div>

      <p class="mt-3 text-sm text-text-muted" aria-live="polite">
        @if (nights() > 0) {
          {{ nights() }} đêm · {{ adults() + children() }} khách · {{ roomQuantity() }} phòng
        } @else {
          Chọn ngày nhận và ngày trả phòng để xem phòng còn trống.
        }
      </p>

      <div class="mt-4">
        <ui-button [fullWidth]="true" [disabled]="nights() === 0" (pressed)="next()">
          Xem phòng còn trống
        </ui-button>
      </div>
    </section>
  `,
})
export class StepDates {
  private readonly store = inject(BookingFlowStore);
  private readonly availability = inject(AvailabilityService);

  protected readonly dates = signal<DateRange>({ checkIn: null, checkOut: null });
  protected readonly adults = signal(2);
  protected readonly children = signal(0);
  protected readonly roomQuantity = signal(1);
  protected readonly error = signal<string | null>(null);
  protected readonly calendar = signal<AvailabilityMap>({});
  protected readonly calendarLoading = signal(true);

  protected readonly nights = computed(() => {
    const { checkIn, checkOut } = this.dates();
    if (!checkIn || !checkOut) {
      return 0;
    }
    return Math.max(0, Math.round((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86_400_000));
  });

  constructor() {
    const params = this.store.params();
    this.dates.set({ checkIn: params.checkIn || null, checkOut: params.checkOut || null });
    this.adults.set(params.adults);
    this.children.set(params.children);
    this.roomQuantity.set(params.roomQuantity);

    const today = new Date();
    this.availability
      .calendar(null, toIsoDate(today), toIsoDate(new Date(today.getTime() + CALENDAR_DAYS * 86_400_000)))
      .subscribe({
        next: (map) => {
          this.calendar.set(map);
          this.calendarLoading.set(false);
        },
        error: () => {
          this.calendar.set({});
          this.calendarLoading.set(false);
        },
      });
  }

  protected next(): void {
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
      roomQuantity: this.roomQuantity(),
      roomTypeId: null,
    });
    this.store.goToStep(2);
  }
}

/** `YYYY-MM-DD` theo giờ địa phương. `toISOString` đổi sang UTC và lệch một ngày. */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
