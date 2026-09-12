import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AdminContentService,
  type GalleryImageRequest,
} from '../../../core/services/admin-content.service';
import type { GalleryImageView } from '../../../core/services/content.service';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiInput,
  UiSkeleton,
  UiToast,
} from '../../../shared/ui';
import { ImagePicker, type PickedImage } from './image-picker';

function emptyDraft(): GalleryImageRequest {
  return { url: '', publicId: null, caption: null, category: null, displayOrder: 0, active: true };
}

/**
 * Thư viện ảnh của homestay.
 *
 * <h2>Chú thích ảnh chính là chữ thay thế</h2>
 *
 * Trang công khai dùng `caption` làm `alt` của ảnh. Vì thế ô chú thích ở đây
 * không phải trang trí: bỏ trống thì người dùng trình đọc màn hình không biết
 * ảnh chụp gì. Ảnh thuần trang trí thì để trống là ĐÚNG — trình đọc màn hình bỏ
 * qua nó thay vì đọc một câu vô nghĩa.
 */
@Component({
  selector: 'app-admin-gallery',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton, UiSkeleton, UiEmptyState, UiConfirmDialog, UiToast, ImagePicker],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Thư viện ảnh</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    <div class="mb-4">
      <ui-button (pressed)="startCreate()">Thêm ảnh</ui-button>
    </div>

    @if (editing()) {
      <form class="mb-6 rounded-md border border-border bg-surface p-4" (submit)="save($event)">
        <h2 class="mb-3 font-semibold text-text">
          {{ editingId() === null ? 'Ảnh mới' : 'Sửa ảnh' }}
        </h2>

        <div class="flex flex-col gap-3">
          <app-image-picker
            label="Ảnh"
            folder="gallery"
            [value]="draft().url"
            (picked)="onImage($event)" />

          <ui-input
            label="Chú thích"
            hint="Cũng là chữ thay thế cho người dùng trình đọc màn hình. Để trống nếu ảnh chỉ để trang trí."
            [value]="draft().caption ?? ''"
            (valueChange)="patch({ caption: $event || null })" />

          <ui-input
            label="Nhóm"
            placeholder="Ví dụ: phòng, sân vườn, bữa sáng"
            [value]="draft().category ?? ''"
            (valueChange)="patch({ category: $event || null })" />

          <ui-input
            label="Thứ tự hiển thị"
            type="number"
            [value]="draft().displayOrder + ''"
            (valueChange)="patch({ displayOrder: toInt($event) })" />

          <label class="flex min-h-[var(--touch-min)] items-center gap-2 text-sm text-text">
            <input
              type="checkbox"
              [checked]="draft().active"
              (change)="patch({ active: isChecked($event) })" />
            Đang hiện trên trang chủ
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
    } @else if (images().length === 0) {
      <ui-empty-state
        title="Thư viện còn trống"
        description="Thêm ảnh để khối “Không gian homestay” xuất hiện trên trang chủ."
        actionLabel="Thêm ảnh"
        (action)="startCreate()" />
    } @else {
      <ul class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        @for (image of images(); track image.id) {
          <li class="rounded-md border border-border bg-surface p-2">
            <img
              [src]="image.url"
              [alt]="image.caption ?? ''"
              width="240"
              height="180"
              class="aspect-[4/3] w-full rounded-sm object-cover" />
            <p class="mt-1 truncate text-xs text-text">{{ image.caption ?? '(không chú thích)' }}</p>
            <p class="truncate text-xs text-text-muted">
              {{ image.category ?? 'chưa phân nhóm' }} · thứ tự {{ image.displayOrder }}
              @if (!image.active) {
                · đang ẩn
              }
            </p>
            <div class="mt-2 flex gap-1">
              <ui-button variant="secondary" (pressed)="startEdit(image)">Sửa</ui-button>
              <ui-button variant="danger" (pressed)="deleting.set(image)">Xoá</ui-button>
            </div>
          </li>
        }
      </ul>
    }

    <ui-confirm-dialog
      [open]="deleting() !== null"
      title="Xoá ảnh khỏi thư viện"
      question="Xoá ảnh này khỏi thư viện?"
      [consequences]="[
        'Ảnh biến mất khỏi trang chủ ngay lập tức.',
        'Tệp ảnh vẫn nằm trên máy chủ, không tự xoá theo.',
        'Thao tác này không hoàn tác được.',
      ]"
      confirmLabel="Xoá ảnh"
      [loading]="saving()"
      (confirmed)="confirmDelete()"
      (cancelled)="deleting.set(null)" />

    @if (toast(); as message) {
      <ui-toast kind="success" [message]="message" (dismissed)="toast.set(null)" />
    }
  `,
})
export class AdminGallery {
  private readonly api = inject(AdminContentService);

  protected readonly images = signal<GalleryImageView[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editing = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly draft = signal<GalleryImageRequest>(emptyDraft());
  protected readonly deleting = signal<GalleryImageView | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly toast = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected toInt(value: string): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? Math.trunc(parsed) : 0;
  }

  protected isChecked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected patch(patch: Partial<GalleryImageRequest>): void {
    this.draft.update((current) => ({ ...current, ...patch }));
  }

  protected onImage(image: PickedImage): void {
    this.patch({ url: image.url, publicId: image.publicId });
  }

  protected startCreate(): void {
    this.draft.set(emptyDraft());
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(image: GalleryImageView): void {
    this.draft.set({
      url: image.url,
      publicId: image.publicId,
      caption: image.caption,
      category: image.category,
      displayOrder: image.displayOrder,
      active: image.active,
    });
    this.editingId.set(image.id);
    this.editing.set(true);
  }

  protected save(event: Event): void {
    event.preventDefault();
    const body = this.draft();
    if (!body.url.trim()) {
      this.error.set('Chọn hoặc tải lên một ảnh trước khi lưu.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request =
      id === null ? this.api.createGalleryImage(body) : this.api.updateGalleryImage(id, body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        this.toast.set('Đã lưu ảnh.');
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Không lưu được ảnh. Thử lại sau giây lát.');
      },
    });
  }

  protected confirmDelete(): void {
    const image = this.deleting();
    if (!image) {
      return;
    }
    this.saving.set(true);
    this.api.deleteGalleryImage(image.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.toast.set('Đã xoá ảnh.');
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.error.set('Không xoá được ảnh. Thử lại sau giây lát.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.gallery().subscribe({
      next: (data) => {
        this.images.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được thư viện ảnh.');
        this.loading.set(false);
      },
    });
  }
}
