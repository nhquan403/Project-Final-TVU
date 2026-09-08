import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { UiSkeleton } from '../skeleton/skeleton';
import { VndCurrencyPipe } from '../../pipes/vnd-currency.pipe';

/** Dữ liệu hiển thị của một loại phòng. Thẻ không tự gọi API. */
export interface RoomCardData {
  id: string;
  name: string;
  imageUrl: string;
  imageAlt: string;
  capacity: number;
  bedSummary: string;
  /** Chỉ ba tiện ích nổi bật. Liệt kê hết là không ai đọc. */
  topAmenities: readonly string[];
  pricePerNight: number;
  nights: number;
  /** Tổng cả kỳ nghỉ, đã tính sẵn ở tầng gọi. */
  totalPrice: number;
  /** Số phòng còn trống, lấy từ truy vấn có ràng buộc EXCLUDE bảo chứng. */
  availableCount: number;
}

/**
 * Thẻ loại phòng.
 *
 * Thứ tự thông tin cố định: ảnh → tên → sức chứa và giường → ba tiện ích →
 * giá mỗi đêm → **tổng cả kỳ** → số phòng còn lại → nút đặt.
 *
 * Tổng cả kỳ hiện ngay tại đây là chủ ý: chỉ hiện giá mỗi đêm rồi để tổng lộ
 * ra ở bước cuối là điểm các trang đặt phòng bị chê nhiều nhất — khách thấy
 * mình bị hớ đúng lúc sắp trả tiền.
 *
 * "Còn N phòng" chỉ hiện khi N ≤ 3 và luôn là con số thật trong cơ sở dữ liệu.
 * Đây là phiên bản trung thực của "chỉ còn 1 phòng!": vẫn tạo cảm giác khan
 * hiếm, nhưng truy vết được về một hàng dữ liệu khi bị hỏi.
 *
 * Ảnh có `width`/`height` và `loading="lazy"`: thiếu kích thước thì lúc ảnh
 * tải xong bố cục nhảy, và cú bấm đang nhắm vào nút đặt rơi sang thẻ khác.
 */
@Component({
  selector: 'ui-room-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSkeleton, VndCurrencyPipe],
  template: `
    @if (loading()) {
      <article class="overflow-hidden rounded-lg border border-border bg-surface shadow-1">
        <ui-skeleton shape="card" />
        <div class="flex flex-col gap-2 p-4">
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
        </div>
      </article>
    } @else if (room(); as data) {
      <article
        [class]="cardClasses()"
        [attr.aria-disabled]="soldOut() ? 'true' : null">
        <img
          [src]="data.imageUrl"
          [alt]="data.imageAlt"
          width="640"
          height="420"
          loading="lazy"
          decoding="async"
          class="aspect-[16/10] w-full object-cover" />

        <div class="flex flex-col gap-3 p-4">
          <h3 class="text-h3 font-semibold">{{ data.name }}</h3>

          <p class="text-sm text-text-muted">
            {{ data.capacity }} khách · {{ data.bedSummary }}
          </p>

          <ul class="flex flex-wrap gap-2">
            @for (amenity of data.topAmenities.slice(0, 3); track amenity) {
              <li class="rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-text-muted">
                {{ amenity }}
              </li>
            }
          </ul>

          <div class="mt-1">
            <p class="text-sm text-text-muted">
              {{ data.pricePerNight | vndCurrency }} / đêm
            </p>
            <p class="text-h3 font-bold text-price">
              {{ data.totalPrice | vndCurrency }}
              <span class="text-sm font-normal text-text-muted">
                cho {{ data.nights }} đêm
              </span>
            </p>
          </div>

          @if (soldOut()) {
            <p class="text-sm font-semibold text-danger">Hết phòng cho khoảng ngày này</p>
          } @else if (data.availableCount <= 3) {
            <p class="text-sm font-semibold text-warning">
              Còn {{ data.availableCount }} phòng cho khoảng ngày này
            </p>
          }

          <button
            type="button"
            class="mt-1 min-h-[var(--touch-min)] w-full rounded-md bg-primary px-5 text-sm
                   font-semibold text-text-invert transition-colors duration-[var(--dur-fast)]
                   hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50
                   disabled:hover:bg-primary"
            [disabled]="soldOut()"
            (click)="selected.emit(data.id)">
            {{ soldOut() ? 'Hết phòng' : 'Chọn phòng' }}
          </button>
        </div>
      </article>
    }
  `,
})
export class UiRoomCard {
  readonly room = input<RoomCardData | null>(null);
  readonly loading = input(false);
  readonly selected = output<string>();

  protected readonly soldOut = computed(() => (this.room()?.availableCount ?? 0) <= 0);

  protected readonly cardClasses = computed(() => {
    const base =
      'block overflow-hidden rounded-lg border border-border bg-surface shadow-1 ' +
      'transition-shadow duration-[var(--dur-base)]';
    return this.soldOut() ? `${base} opacity-60` : `${base} hover:shadow-2`;
  });
}
