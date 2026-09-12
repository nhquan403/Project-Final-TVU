import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { UiStarRating } from '../../../shared/ui';
import type { PublicReview } from '../../../core/services/review.service';

/**
 * Đánh giá của khách đã ở.
 *
 * <p>MỌI trường do khách nhập — tên, tiêu đề, nội dung — hiển thị bằng text
 * binding `{{ }}`. Đây là văn bản thuần của người lạ; `[innerHTML]` ở đây
 * nghĩa là bất kỳ ai đặt được một đêm phòng cũng chèn được mã vào trang chủ.
 * Backend cố tình KHÔNG lọc thẻ ở tầng vào cho những trường này, vì lọc rồi
 * lưu sẽ biến dấu &amp; trong tên khách thành chuỗi "&amp;amp;" trên màn hình.
 */
@Component({
  selector: 'app-reviews-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiStarRating, DatePipe],
  template: `
    <ul class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      @for (review of reviews(); track review.id) {
        <li class="flex h-full flex-col gap-2 rounded-lg border border-border bg-surface p-4">
          <ui-star-rating [value]="review.rating" [readonly]="true" />

          @if (review.title) {
            <p class="font-semibold text-text">{{ review.title }}</p>
          }

          @if (review.content) {
            <p class="text-sm text-text-muted">{{ review.content }}</p>
          }

          <p class="mt-auto pt-2 text-xs text-text-muted">
            {{ review.guestName }} · {{ review.roomTypeName }} ·
            {{ review.createdAt | date: 'dd/MM/yyyy' }}
          </p>

          @if (review.adminReply) {
            <div class="rounded-md bg-surface-2 p-3">
              <p class="text-xs font-semibold text-text">Phản hồi từ Homestay TVH</p>
              <p class="mt-1 text-sm text-text-muted">{{ review.adminReply }}</p>
            </div>
          }
        </li>
      }
    </ul>
  `,
})
export class ReviewsSection {
  readonly reviews = input.required<readonly PublicReview[]>();
}
