import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { VndCurrencyPipe } from '../../../shared/ui';
import type { PublicRoomType } from '../../../core/services/room-type.service';
import { ROOM_PLACEHOLDER_IMAGE } from './room-card.mapper';

/**
 * Thẻ giới thiệu một loại phòng khi CHƯA biết ngày khách muốn ở.
 *
 * <p>Khác `ui-room-card` ở đúng một điểm quan trọng: không có nút "Chọn phòng"
 * và không có dòng "còn N phòng". `ui-room-card` coi `availableCount <= 0` là
 * hết phòng và khoá nút lại, nên truyền vào một con số bịa ra ở đây sẽ hoặc
 * đuổi khách đi ("Hết phòng" trong khi còn), hoặc hứa suông (nút mở nhưng bấm
 * vào thì hết). Chưa chọn ngày thì chưa ai biết còn bao nhiêu phòng — nên thẻ
 * này dẫn sang trang chi tiết để khách chọn ngày ở đó.
 *
 * <p>Giá ghi "từ X mỗi đêm": đó là `basePrice`, mức khởi điểm chưa gồm khuyến
 * mãi hay phụ thu. Bỏ chữ "từ" là hứa một con số mà hoá đơn không giữ.
 */
@Component({
  selector: 'app-room-catalog-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, VndCurrencyPipe],
  template: `
    <article
      class="flex h-full flex-col overflow-hidden rounded-lg border border-border bg-surface
             shadow-1 transition-shadow duration-[var(--dur-base)] hover:shadow-2">
      <img
        [src]="coverUrl()"
        [alt]="coverAlt()"
        width="640"
        height="420"
        loading="lazy"
        decoding="async"
        class="aspect-[16/10] w-full object-cover" />

      <div class="flex flex-1 flex-col gap-2 p-4">
        <h3 class="text-h3 font-semibold text-text">{{ room().name }}</h3>

        <p class="text-sm text-text-muted">
          {{ room().capacityAdults }} khách
          @if (room().bedInfo) {
            · {{ room().bedInfo }}
          }
          @if (room().areaSqm) {
            · {{ room().areaSqm }} m²
          }
        </p>

        @if (room().shortDescription) {
          <p class="line-clamp-2 text-sm text-text-muted">{{ room().shortDescription }}</p>
        }

        <p class="mt-auto pt-2 text-h3 font-bold text-price">
          <span class="text-sm font-normal text-text-muted">từ</span>
          {{ room().basePrice | vndCurrency }}
          <span class="text-sm font-normal text-text-muted">/ đêm</span>
        </p>

        <a
          [routerLink]="['/phong', room().slug]"
          class="mt-1 inline-flex min-h-[var(--touch-min)] items-center justify-center rounded-md
                 border border-primary px-5 text-sm font-semibold text-primary
                 transition-colors duration-[var(--dur-fast)] hover:bg-primary hover:text-text-invert">
          Xem chi tiết &amp; chọn ngày
        </a>
      </div>
    </article>
  `,
})
export class RoomCatalogCard {
  readonly room = input.required<PublicRoomType>();

  protected readonly cover = computed(
    () => this.room().images.find((image) => image.cover) ?? this.room().images[0] ?? null,
  );

  protected readonly coverUrl = computed(() => this.cover()?.url ?? ROOM_PLACEHOLDER_IMAGE);

  protected readonly coverAlt = computed(
    () => this.cover()?.altText ?? `Chưa có ảnh cho ${this.room().name}`,
  );
}
