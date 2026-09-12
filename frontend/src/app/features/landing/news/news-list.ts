import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ContentService, type PostSummary } from '../../../core/services/content.service';
import { UiEmptyState, UiSkeleton } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';

/**
 * Danh sách bài viết đã đăng.
 *
 * <p>Bộ lọc "đã đăng" nằm ở truy vấn phía backend, không ở đây: lọc ở frontend
 * nghĩa là bản nháp vẫn được gửi qua mạng và ai mở công cụ nhà phát triển cũng
 * đọc được.
 *
 * <p>Tiêu đề và đoạn tóm tắt hiển thị bằng text binding — đó là hai trường văn
 * bản thuần. Chỉ thân bài ở trang chi tiết mới dùng `[innerHTML]`.
 */
@Component({
  selector: 'app-news-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, UiSkeleton, UiEmptyState],
  template: `
    <div class="mx-auto max-w-4xl px-4 py-8">
      <h1 class="text-h1 font-bold text-text">Tin tức &amp; cẩm nang</h1>
      <p class="mt-2 text-text-muted">Chuyện ở homestay và gợi ý cho chuyến đi Trà Vinh của bạn.</p>

      @if (loading()) {
        <div class="mt-6 flex flex-col gap-3">
          <ui-skeleton shape="row" />
          <ui-skeleton shape="row" />
          <ui-skeleton shape="row" />
        </div>
      } @else if (error()) {
        <div class="mt-6">
          <ui-empty-state
            title="Không tải được tin tức"
            [description]="error()!"
            actionLabel="Thử lại"
            (action)="load()" />
        </div>
      } @else if (posts().length === 0) {
        <div class="mt-6">
          <ui-empty-state
            title="Chưa có bài viết nào"
            description="Homestay chưa đăng bài nào. Quay lại sau nhé." />
        </div>
      } @else {
        <ul class="mt-6 grid gap-4 sm:grid-cols-2">
          @for (post of posts(); track post.id) {
            <li class="h-full overflow-hidden rounded-lg border border-border bg-surface">
              <a [routerLink]="['/tin-tuc', post.slug]" class="flex h-full flex-col">
                @if (post.coverImageUrl) {
                  <img
                    [src]="post.coverImageUrl"
                    alt=""
                    width="640"
                    height="360"
                    loading="lazy"
                    class="aspect-[16/9] w-full object-cover" />
                }
                <div class="flex flex-1 flex-col gap-2 p-4">
                  <h2 class="text-h3 font-semibold text-text">{{ post.title }}</h2>
                  @if (post.excerpt) {
                    <p class="line-clamp-3 text-sm text-text-muted">{{ post.excerpt }}</p>
                  }
                  @if (post.publishedAt) {
                    <p class="mt-auto pt-2 text-xs text-text-muted">
                      {{ post.publishedAt | date: 'dd/MM/yyyy' }}
                    </p>
                  }
                </div>
              </a>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class NewsListPage {
  private readonly content = inject(ContentService);

  protected readonly posts = signal<PostSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.content.posts().subscribe({
      next: (data) => {
        this.posts.set(data);
        this.loading.set(false);
      },
      error: (failure) => {
        this.error.set(errorMessageOf(failure, 'Máy chủ chưa trả lời. Thử lại sau giây lát.'));
        this.loading.set(false);
      },
    });
  }
}
