import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { UiLightbox, type LightboxImage } from '../../../shared/ui';
import type { GalleryImageView } from '../../../core/services/content.service';

/**
 * Thư viện ảnh của homestay.
 *
 * <p>Chú thích ảnh do quản trị viên nhập được hiển thị bằng text binding, và
 * dùng luôn làm `alt`. Ảnh không có chú thích nhận `alt` rỗng thay vì một câu
 * bịa: trình đọc màn hình bỏ qua ảnh trang trí không có thông tin, còn "Ảnh
 * homestay" lặp lại mười hai lần chỉ làm người mù mất thời gian.
 */
@Component({
  selector: 'app-gallery-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiLightbox],
  template: `
    <ul class="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
      @for (image of images(); track image.id; let i = $index) {
        <li>
          <button
            type="button"
            class="group block w-full overflow-hidden rounded-md border border-border
                   focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2
                   focus-visible:outline-focus"
            [attr.aria-label]="'Xem ảnh lớn' + (image.caption ? ': ' + image.caption : '')"
            (click)="openAt(i)">
            <img
              [src]="image.url"
              [alt]="image.caption ?? ''"
              width="480"
              height="360"
              loading="lazy"
              decoding="async"
              class="aspect-[4/3] w-full object-cover transition-transform
                     duration-[var(--dur-base)] group-hover:scale-105" />
          </button>
          @if (image.caption) {
            <p class="mt-1 text-xs text-text-muted">{{ image.caption }}</p>
          }
        </li>
      }
    </ul>

    <ui-lightbox
      [open]="open()"
      [images]="lightboxImages()"
      [(index)]="index"
      (closed)="open.set(false)" />
  `,
})
export class GallerySection {
  readonly images = input.required<readonly GalleryImageView[]>();

  protected readonly open = signal(false);
  protected readonly index = signal(0);

  protected readonly lightboxImages = computed<LightboxImage[]>(() =>
    this.images().map((image) => ({
      url: image.url,
      alt: image.caption ?? '',
    })),
  );

  protected openAt(index: number): void {
    this.index.set(index);
    this.open.set(true);
  }
}
