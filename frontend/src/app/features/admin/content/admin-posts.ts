import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AdminContentService, type PostRequest } from '../../../core/services/admin-content.service';
import type { PostSummary } from '../../../core/services/content.service';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiInput,
  UiSkeleton,
  UiToast,
} from '../../../shared/ui';
import { ImagePicker, type PickedImage } from './image-picker';

function emptyDraft(): PostRequest {
  return {
    slug: '',
    title: '',
    excerpt: null,
    content: null,
    coverImageUrl: null,
    published: false,
  };
}

/**
 * Soạn tin tức.
 *
 * <h2>Đường dẫn tĩnh (slug)</h2>
 *
 * Slug là phần đuôi của địa chỉ bài viết, nên đổi slug của một bài ĐÃ ĐĂNG là
 * làm chết mọi link đã chia sẻ. Màn hình vì thế gợi slug từ tiêu đề khi tạo
 * mới, nhưng không tự đổi lại khi sửa tiêu đề của bài cũ.
 *
 * <h2>Thân bài là HTML đã lọc</h2>
 *
 * Giống khối nội dung trang chủ: backend gỡ thẻ ngoài danh sách cho phép khi
 * lưu, và màn hình hiện lại đúng thứ đã lưu.
 */
@Component({
  selector: 'app-admin-posts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    UiInput,
    UiButton,
    UiSkeleton,
    UiEmptyState,
    UiConfirmDialog,
    UiToast,
    ImagePicker,
  ],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Tin tức</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    <div class="mb-4">
      <ui-button (pressed)="startCreate()">Viết bài mới</ui-button>
    </div>

    @if (editing()) {
      <form class="mb-6 rounded-md border border-border bg-surface p-4" (submit)="save($event)">
        <h2 class="mb-3 font-semibold text-text">
          {{ editingId() === null ? 'Bài viết mới' : 'Sửa bài viết' }}
        </h2>

        <div class="flex flex-col gap-3">
          <ui-input
            label="Tiêu đề"
            [required]="true"
            [value]="draft().title"
            (valueChange)="onTitle($event)" />

          <ui-input
            label="Đường dẫn (slug)"
            [required]="true"
            placeholder="kinh-nghiem-di-tra-vinh"
            hint="Phần đuôi địa chỉ bài viết. Đổi slug của bài đã đăng sẽ làm hỏng link đã chia sẻ."
            [value]="draft().slug"
            (valueChange)="patch({ slug: $event })" />

          <ui-input
            label="Tóm tắt"
            hint="Hiện ở danh sách và trong khung xem trước khi chia sẻ link."
            [value]="draft().excerpt ?? ''"
            (valueChange)="patch({ excerpt: $event || null })" />

          <app-image-picker
            label="Ảnh bìa"
            folder="posts"
            [value]="draft().coverImageUrl ?? ''"
            (picked)="onImage($event)" />

          <label class="flex flex-col gap-1">
            <span class="text-sm font-medium text-text">Nội dung (HTML)</span>
            <textarea
              rows="12"
              class="rounded-md border border-border bg-surface px-3 py-2 text-sm text-text"
              [value]="draft().content ?? ''"
              (input)="patch({ content: asValue($event) })"></textarea>
            <span class="text-xs text-text-muted">
              Thẻ được giữ lại: &lt;p&gt; &lt;h2&gt; &lt;h3&gt; &lt;h4&gt; &lt;ul&gt; &lt;ol&gt;
              &lt;li&gt; &lt;strong&gt; &lt;em&gt; &lt;a&gt; &lt;img&gt; &lt;br&gt;
              &lt;blockquote&gt;. Mọi thẻ khác bị gỡ khi lưu — kể cả &lt;b&gt; và &lt;i&gt;, hãy
              dùng &lt;strong&gt; và &lt;em&gt;. Ảnh chỉ nhận nguồn đã khai trong cấu hình.
            </span>
          </label>

          <label class="flex min-h-[var(--touch-min)] items-center gap-2 text-sm text-text">
            <input
              type="checkbox"
              [checked]="draft().published"
              (change)="patch({ published: isChecked($event) })" />
            Đăng công khai
          </label>

          <div class="flex gap-2">
            <ui-button type="submit" [loading]="saving()">Lưu</ui-button>
            <ui-button variant="ghost" (pressed)="editing.set(false)">Huỷ</ui-button>
          </div>
        </div>
      </form>
    }

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else if (posts().length === 0) {
      <ui-empty-state
        title="Chưa có bài viết nào"
        description="Viết bài đầu tiên để khối tin tức xuất hiện trên trang chủ."
        actionLabel="Viết bài mới"
        (action)="startCreate()" />
    } @else {
      <ul class="space-y-3">
        @for (post of posts(); track post.id) {
          <li class="flex flex-wrap items-center gap-3 rounded-md border border-border bg-surface p-3">
            <div class="min-w-0 flex-1">
              <p class="font-semibold text-text">{{ post.title }}</p>
              <p class="truncate text-xs text-text-muted">
                /tin-tuc/{{ post.slug }}
                @if (post.published) {
                  · đã đăng
                  @if (post.publishedAt) {
                    {{ post.publishedAt | date: 'dd/MM/yyyy' }}
                  }
                } @else {
                  · bản nháp
                }
              </p>
            </div>
            <ui-button variant="secondary" [loading]="openingId() === post.id" (pressed)="startEdit(post)">
              Sửa
            </ui-button>
            <ui-button variant="danger" (pressed)="deleting.set(post)">Xoá</ui-button>
          </li>
        }
      </ul>
    }

    <ui-confirm-dialog
      [open]="deleting() !== null"
      title="Xoá bài viết"
      [question]="'Xoá bài “' + (deleting()?.title ?? '') + '”?'"
      [consequences]="[
        'Địa chỉ /tin-tuc/' + (deleting()?.slug ?? '') + ' sẽ trả về trang không tìm thấy.',
        'Mọi link đã chia sẻ tới bài này sẽ hỏng.',
        'Thao tác này không hoàn tác được.',
      ]"
      confirmLabel="Xoá bài viết"
      [loading]="saving()"
      (confirmed)="confirmDelete()"
      (cancelled)="deleting.set(null)" />

    @if (toast(); as message) {
      <ui-toast kind="success" [message]="message" (dismissed)="toast.set(null)" />
    }
  `,
})
export class AdminPosts {
  private readonly api = inject(AdminContentService);

  protected readonly posts = signal<PostSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editing = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly openingId = signal<number | null>(null);
  protected readonly draft = signal<PostRequest>(emptyDraft());
  protected readonly deleting = signal<PostSummary | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly toast = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected asValue(event: Event): string {
    return (event.target as HTMLTextAreaElement).value;
  }

  protected isChecked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected patch(patch: Partial<PostRequest>): void {
    this.draft.update((current) => ({ ...current, ...patch }));
  }

  protected onImage(image: PickedImage): void {
    this.patch({ coverImageUrl: image.url || null });
  }

  /** Bài MỚI thì slug bám theo tiêu đề; bài cũ giữ nguyên slug đã công bố. */
  protected onTitle(title: string): void {
    if (this.editingId() === null) {
      this.patch({ title, slug: slugify(title) });
    } else {
      this.patch({ title });
    }
  }

  protected startCreate(): void {
    this.draft.set(emptyDraft());
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(post: PostSummary): void {
    // Danh sách không kèm thân bài — tải bản đầy đủ trước khi mở form, nếu
    // không lưu lại sẽ ghi đè nội dung bằng một ô trống.
    this.openingId.set(post.id);
    this.api.post(post.id).subscribe({
      next: (full) => {
        this.draft.set({
          slug: full.slug,
          title: full.title,
          excerpt: full.excerpt,
          content: full.content,
          coverImageUrl: full.coverImageUrl,
          published: full.published,
        });
        this.editingId.set(full.id);
        this.editing.set(true);
        this.openingId.set(null);
      },
      error: () => {
        this.openingId.set(null);
        this.error.set('Không mở được bài viết. Thử lại sau giây lát.');
      },
    });
  }

  protected save(event: Event): void {
    event.preventDefault();
    const body = this.draft();
    if (!body.title.trim() || !body.slug.trim()) {
      this.error.set('Bài viết cần có tiêu đề và đường dẫn.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request = id === null ? this.api.createPost(body) : this.api.updatePost(id, body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        this.toast.set('Đã lưu bài viết.');
        this.load();
      },
      error: (failure: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.error.set(
          failure?.error?.detail ?? 'Không lưu được bài viết. Đường dẫn có thể đã được dùng.',
        );
      },
    });
  }

  protected confirmDelete(): void {
    const post = this.deleting();
    if (!post) {
      return;
    }
    this.saving.set(true);
    this.api.deletePost(post.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.toast.set('Đã xoá bài viết.');
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.error.set('Không xoá được bài viết. Thử lại sau giây lát.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.posts().subscribe({
      next: (data) => {
        this.posts.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách bài viết.');
        this.loading.set(false);
      },
    });
  }
}

/**
 * Tiêu đề tiếng Việt thành slug không dấu.
 *
 * `normalize('NFD')` tách dấu thành ký tự riêng để bỏ đi; `đ` không có dạng tách
 * nên phải xử lý riêng, nếu không "đặt phòng" ra "t-phong".
 */
function slugify(title: string): string {
  return title
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 120);
}
