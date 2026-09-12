import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ContentService, type PostDetail } from '../../../core/services/content.service';
import { SeoService } from '../../../core/services/seo.service';
import { UiEmptyState, UiSkeleton } from '../../../shared/ui';

/**
 * Một bài viết.
 *
 * <p>Thân bài là chỗ DUY NHẤT của trang công khai dùng `[innerHTML]`, và chỉ vì
 * nó đã đi qua bộ lọc thẻ ở tầng vào của backend: danh sách thẻ cho phép được
 * khai tường minh, `javascript:` bị chặn, và mọi thẻ ngoài danh sách bị gỡ
 * trước khi lưu. Angular lọc thêm một lượt nữa khi hiển thị — hai lớp, vì lớp
 * đầu chỉ bảo vệ được dữ liệu ghi SAU khi nó tồn tại.
 */
@Component({
  selector: 'app-news-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, UiSkeleton, UiEmptyState],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-8">
      @if (loading()) {
        <ui-skeleton shape="card" />
        <div class="mt-4 flex flex-col gap-2">
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
        </div>
      } @else if (notFound()) {
        <ui-empty-state
          title="Không tìm thấy bài viết"
          description="Liên kết có thể đã cũ, hoặc bài viết đã được gỡ."
          actionLabel="Về danh sách tin tức"
          (action)="goToList()" />
      } @else if (error()) {
        <ui-empty-state
          title="Không tải được bài viết"
          description="Máy chủ chưa trả lời. Thử lại sau giây lát."
          actionLabel="Thử lại"
          (action)="load()" />
      } @else if (post(); as data) {
        <article>
          <nav aria-label="Đường dẫn" class="text-sm text-text-muted">
            <a
              routerLink="/tin-tuc"
              class="inline-flex min-h-[var(--touch-min)] items-center hover:underline">
              Tin tức
            </a>
            <span aria-hidden="true"> / </span>
            <span class="text-text">{{ data.title }}</span>
          </nav>

          <h1 class="mt-3 text-h1 font-bold text-text">{{ data.title }}</h1>

          @if (data.publishedAt) {
            <p class="mt-1 text-sm text-text-muted">
              Đăng ngày {{ data.publishedAt | date: 'dd/MM/yyyy' }}
            </p>
          }

          @if (data.coverImageUrl) {
            <img
              [src]="data.coverImageUrl"
              alt=""
              width="960"
              height="540"
              fetchpriority="high"
              class="mt-4 aspect-[16/9] w-full rounded-lg object-cover" />
          }

          @if (data.excerpt) {
            <p class="mt-4 text-lg text-text-muted">{{ data.excerpt }}</p>
          }

          @if (data.content) {
            <div class="prose-tvh mt-6 text-text" [innerHTML]="data.content"></div>
          }
        </article>
      }
    </div>
  `,
})
export class NewsDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly content = inject(ContentService);
  private readonly seo = inject(SeoService);

  protected readonly post = signal<PostDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly notFound = signal(false);

  constructor() {
    this.load();
  }

  protected load(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.loading.set(true);
    this.error.set(false);
    this.notFound.set(false);
    this.content.post(slug).subscribe({
      next: (data) => {
        this.post.set(data);
        this.loading.set(false);
        this.seo.apply({
          title: data.title,
          description: data.excerpt ?? `${data.title} — tin tức từ Homestay TVH.`,
          imageUrl: data.coverImageUrl,
          type: 'article',
        });
      },
      error: (failure: { status?: number }) => {
        this.loading.set(false);
        if (failure?.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }

  protected goToList(): void {
    void this.router.navigate(['/tin-tuc']);
  }
}
