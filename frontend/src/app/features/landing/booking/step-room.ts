import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  AvailabilityService,
  type RoomTypeAvailability,
} from '../../../core/services/availability.service';
import { UiEmptyState, UiRoomCard, UiSkeleton } from '../../../shared/ui';
import { toRoomCard } from '../shared/room-card.mapper';
import { errorMessageOf } from '../shared/api-error';
import { BookingFlowStore } from './booking-flow.store';

/**
 * Bước 2: chọn loại phòng.
 *
 * <p>Danh sách lấy từ `/api/availability` với đúng ngày và số khách đã chọn,
 * nên mọi con số trên thẻ — tổng cả kỳ, số phòng còn lại — đều là số thật của
 * lần hỏi này, không phải giá niêm yết.
 */
@Component({
  selector: 'app-step-room',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiRoomCard, UiSkeleton, UiEmptyState],
  template: `
    <section class="rounded-lg border border-border bg-surface p-4" aria-labelledby="buoc-2">
      <h2 id="buoc-2" class="text-h2 font-bold text-text">Chọn loại phòng</h2>
      <p class="mt-1 text-sm text-text-muted">
        Giá hiển thị là tổng cho cả {{ nights() }} đêm, đã tính {{ params().roomQuantity }} phòng.
      </p>

      @if (loading()) {
        <ul class="mt-4 grid gap-4 md:grid-cols-2">
          @for (placeholder of [1, 2]; track placeholder) {
            <li class="overflow-hidden rounded-lg border border-border">
              <ui-skeleton shape="card" />
              <div class="flex flex-col gap-2 p-4">
                <ui-skeleton shape="line" />
                <ui-skeleton shape="line" />
              </div>
            </li>
          }
        </ul>
      } @else if (error()) {
        <div class="mt-4">
          <ui-empty-state
            title="Không tải được danh sách phòng trống"
            [description]="error()!"
            actionLabel="Thử lại"
            (action)="load()" />
        </div>
      } @else if (rooms().length === 0) {
        <div class="mt-4">
          <ui-empty-state
            title="Không còn phòng trống cho lựa chọn này"
            description="Thử đổi ngày, giảm số phòng, hoặc giảm số khách mỗi phòng."
            actionLabel="Đổi ngày"
            (action)="backToDates()" />
        </div>
      } @else {
        <ul class="mt-4 grid gap-4 md:grid-cols-2">
          @for (room of rooms(); track room.roomTypeId) {
            <li>
              <ui-room-card [room]="card(room)" (selected)="choose(room)" />
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class StepRoom {
  private readonly store = inject(BookingFlowStore);
  private readonly availability = inject(AvailabilityService);

  protected readonly params = this.store.params;
  protected readonly nights = this.store.nights;

  protected readonly rooms = signal<RoomTypeAvailability[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /**
   * Số khách MỖI PHÒNG, không phải tổng số khách.
   *
   * Backend so sức chứa theo từng phòng. Gửi tổng 4 khách cho 2 phòng sẽ bị
   * loại hết các loại phòng chứa 2 người — tức là báo hết phòng trong khi còn.
   */
  private readonly perRoom = computed(() => {
    const { adults, children, roomQuantity } = this.params();
    const quantity = Math.max(1, roomQuantity);
    return {
      adults: Math.ceil(adults / quantity),
      children: Math.ceil(children / quantity),
    };
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    const { checkIn, checkOut, roomQuantity } = this.params();
    this.loading.set(true);
    this.error.set(null);
    this.availability
      .search({
        checkIn,
        checkOut,
        adults: this.perRoom().adults,
        children: this.perRoom().children,
        roomQuantity,
      })
      .subscribe({
        next: (response) => {
          this.rooms.set(response.roomTypes);
          this.loading.set(false);
        },
        error: (failure) => {
          this.error.set(errorMessageOf(failure, 'Máy chủ chưa trả lời. Thử lại sau giây lát.'));
          this.loading.set(false);
        },
      });
  }

  protected card(room: RoomTypeAvailability) {
    return toRoomCard(room, this.nights());
  }

  protected choose(room: RoomTypeAvailability): void {
    this.store.patchSearch({ roomTypeId: room.roomTypeId });
    this.store.goToStep(3);
  }

  protected backToDates(): void {
    this.store.goToStep(1);
  }
}
