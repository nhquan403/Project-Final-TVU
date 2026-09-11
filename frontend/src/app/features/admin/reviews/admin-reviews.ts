import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import type { ReviewView } from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiFilterChips,
  UiInput,
  UiSelect,
  UiSkeleton,
  UiStarRating,
  type FilterChip,
  type SelectOption,
} from '../../../shared/ui';

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

/**
 * Duyệt đánh giá của khách.
 *
 * Nội dung đánh giá hiển thị bằng TEXT BINDING của Angular (`{{ }}`), không
 * `innerHTML`. Đây là nội dung do người ẩn danh gửi, và màn hình duyệt chính là
 * nơi quản trị viên BẮT BUỘC phải mở nó ra đọc — nên cũng là nơi một lỗ XSS
 * lưu trữ chắc chắn trúng đích. Backend đã lưu dạng văn bản thuần; đây là lớp
 * thứ hai, độc lập.
 */
@Component({
  selector: 'app-admin-reviews',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiButton,
    UiSelect,
    UiInput,
    UiEmptyState,
    UiSkeleton,
    UiFilterChips,
    UiStarRating,
    UiConfirmDialog,
  ],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Đánh giá</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    <div class="mb-3 max-w-xs">
      <ui-select label="Trạng thái" [options]="statusOptions" placeholder="Tất cả"
        [(value)]="status" />
    </div>
    <div class="mb-3 flex gap-2">
      <ui-button (pressed)="load()">Áp dụng</ui-button>
    </div>
    <div class="mb-4">
      <ui-filter-chips [chips]="chips()" (removed)="clearFilter()" (cleared)="clearFilter()" />
    </div>

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else if (reviews().length === 0) {
      <ui-empty-state
        [title]="status() ? 'Không có đánh giá nào ở trạng thái này' : 'Chưa có đánh giá nào'"
        [description]="
          status()
            ? 'Bỏ bộ lọc để xem toàn bộ đánh giá.'
            : 'Đánh giá của khách sau khi trả phòng sẽ xuất hiện ở đây chờ duyệt.'
        "
        [actionLabel]="status() ? 'Xoá bộ lọc' : null"
        (action)="clearFilter()" />
    } @else {
      <ul class="space-y-3">
        @for (review of reviews(); track review.id) {
          <li class="rounded-md border border-border bg-surface p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-text">
                  {{ review.guestName }} · đơn {{ review.bookingCode }}
                </p>
                <ui-star-rating [value]="review.rating" [readonly]="true" />
              </div>
              <span class="text-xs font-semibold uppercase text-text-muted">
                {{ label(review.status) }}
              </span>
            </div>

            @if (review.title) {
              <p class="mt-2 font-semibold text-text">{{ review.title }}</p>
            }
            <!-- Text binding: nội dung do người ẩn danh gửi không bao giờ được
                 diễn giải thành HTML. -->
            <p class="mt-1 whitespace-pre-line text-sm text-text">{{ review.content }}</p>

            @if (review.adminReply) {
              <p class="mt-2 rounded-sm bg-surface-2 p-2 text-sm text-text">
                <span class="font-semibold">Phản hồi:</span> {{ review.adminReply }}
              </p>
            }

            <div class="mt-3 grid gap-2 sm:grid-cols-[1fr_auto_auto]">
              <ui-input label="Trả lời khách" [(value)]="reply" />
              <div class="flex items-end">
                <ui-button variant="secondary" (pressed)="sendReply(review)">Gửi trả lời</ui-button>
              </div>
              <div class="flex items-end gap-2">
                @if (review.status !== 'APPROVED') {
                  <ui-button (pressed)="approve(review)">Duyệt</ui-button>
                }
                @if (review.status !== 'REJECTED') {
                  <ui-button variant="danger" (pressed)="pendingReject.set(review)">
                    Từ chối
                  </ui-button>
                }
              </div>
            </div>
          </li>
        }
      </ul>
    }

    @if (pendingReject(); as review) {
      <ui-confirm-dialog
        [open]="true"
        title="Từ chối đánh giá của {{ review.guestName }}?"
        question="Đánh giá sẽ không hiển thị trên trang công khai."
        [consequences]="[
          'Nội dung vẫn được lưu để tra cứu, không bị xoá.',
          'Khách không nhận được thông báo nào về việc này.',
          'Có thể duyệt lại về sau nếu đổi ý.'
        ]"
        confirmLabel="Từ chối đánh giá"
        (confirmed)="reject(review)"
        (cancelled)="pendingReject.set(null)" />
    }
  `,
})
export class AdminReviews {
  private readonly catalog = inject(AdminCatalogService);

  protected readonly reviews = signal<ReviewView[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly status = signal('');
  protected readonly reply = signal('');
  protected readonly pendingReject = signal<ReviewView | null>(null);

  protected readonly statusOptions: SelectOption[] = Object.entries(STATUS_LABELS).map(
    ([value, label]) => ({ value, label }),
  );

  protected readonly chips = computed<FilterChip[]>(() =>
    this.status() ? [{ key: 'status', label: 'Trạng thái: ' + this.label(this.status()) }] : [],
  );

  constructor() {
    this.load();
  }

  protected label(status: string): string {
    return STATUS_LABELS[status] ?? status;
  }

  protected clearFilter(): void {
    this.status.set('');
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.catalog.reviews(this.status() || null).subscribe({
      next: (list) => {
        this.reviews.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách đánh giá.');
        this.loading.set(false);
      },
    });
  }

  protected approve(review: ReviewView): void {
    this.catalog.approveReview(review.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Không duyệt được đánh giá.'),
    });
  }

  protected reject(review: ReviewView): void {
    this.pendingReject.set(null);
    this.catalog.rejectReview(review.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Không từ chối được đánh giá.'),
    });
  }

  protected sendReply(review: ReviewView): void {
    if (!this.reply().trim()) {
      return;
    }
    this.catalog.replyReview(review.id, this.reply().trim()).subscribe({
      next: () => {
        this.reply.set('');
        this.load();
      },
      error: () => this.error.set('Không gửi được trả lời.'),
    });
  }
}
