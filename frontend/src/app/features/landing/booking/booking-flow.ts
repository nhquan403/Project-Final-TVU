import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BookingFlowStore } from './booking-flow.store';
import { StepDates } from './step-dates';
import { StepGuest } from './step-guest';
import { StepRoom } from './step-room';

interface StepLabel {
  index: number;
  label: string;
}

const STEPS: StepLabel[] = [
  { index: 1, label: 'Chọn ngày' },
  { index: 2, label: 'Chọn phòng' },
  { index: 3, label: 'Xác nhận' },
];

/**
 * Luồng đặt phòng ba bước.
 *
 * <h2>Thanh tiến trình luôn hiện</h2>
 *
 * Ba bước luôn nhìn thấy, kể cả bước chưa tới. Khách biết mình đang ở đâu và
 * còn bao xa nữa mới xong — đó là khác biệt giữa "điền nốt cho rồi" và "không
 * biết còn bao nhiêu bước nữa, thôi bỏ". Bước đã qua bấm được để quay lại; bước
 * chưa tới thì không, vì nhảy cóc sang bước 3 khi chưa chọn phòng chỉ dẫn tới
 * một màn hình trống.
 *
 * <h2>Bước hiện tại suy ra từ dữ liệu</h2>
 *
 * Không có biến "bước hiện tại" riêng. Có ngày thì sang bước 2, có loại phòng
 * thì sang bước 3. Giữ một biến song song với dữ liệu là tạo ra hai nguồn sự
 * thật, và F5 giữa chừng sẽ làm chúng lệch nhau ngay.
 */
@Component({
  selector: 'app-booking-flow',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StepDates, StepRoom, StepGuest],
  template: `
    <div class="mx-auto max-w-5xl px-4 py-8">
      <h1 class="text-h1 font-bold text-text">Đặt phòng</h1>

      <!-- THANH TIẾN TRÌNH -->
      <nav class="mt-4" aria-label="Các bước đặt phòng">
        <ol class="flex items-center gap-2">
          @for (item of steps; track item.index) {
            <li class="flex flex-1 items-center gap-2">
              <button
                type="button"
                class="flex min-h-[var(--touch-min)] flex-1 flex-col items-start justify-center
                       rounded-md border px-3 py-1 text-left transition-colors
                       duration-[var(--dur-fast)] disabled:cursor-default"
                [class.border-primary]="item.index <= step()"
                [class.bg-primary]="item.index === step()"
                [class.text-text-invert]="item.index === step()"
                [class.border-border]="item.index > step()"
                [class.text-text-muted]="item.index > step()"
                [disabled]="item.index >= step()"
                [attr.aria-current]="item.index === step() ? 'step' : null"
                (click)="goTo(item.index)">
                <span class="text-xs">Bước {{ item.index }}</span>
                <span class="text-sm font-semibold">{{ item.label }}</span>
              </button>
            </li>
          }
        </ol>
      </nav>

      <div class="mt-6">
        @switch (step()) {
          @case (1) {
            <app-step-dates />
          }
          @case (2) {
            <app-step-room />
          }
          @default {
            <app-step-guest />
          }
        }
      </div>
    </div>
  `,
})
export class BookingFlowPage {
  private readonly store = inject(BookingFlowStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly steps = STEPS;
  protected readonly step = computed(() => this.store.step());

  constructor() {
    // Đọc tham số từ URL: đây là thứ khiến link chia sẻ và F5 giữ nguyên lựa
    // chọn. Chỉ tham số tìm kiếm — thông tin khách nằm trong sessionStorage.
    const query = this.route.snapshot.queryParamMap;
    this.store.hydrateFromUrl({
      checkIn: query.get('checkIn') ?? undefined,
      checkOut: query.get('checkOut') ?? undefined,
      adults: query.get('adults') ?? undefined,
      children: query.get('children') ?? undefined,
      roomTypeId: query.get('roomTypeId') ?? undefined,
      roomQuantity: query.get('roomQuantity') ?? undefined,
    });
  }

  protected goTo(index: number): void {
    this.store.goToStep(index);
  }
}
