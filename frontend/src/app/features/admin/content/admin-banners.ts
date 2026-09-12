import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AdminContentService, type BannerRequest } from '../../../core/services/admin-content.service';
import type { BannerView } from '../../../core/services/content.service';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiInput,
  UiSkeleton,
  UiToast,
} from '../../../shared/ui';
import { ImagePicker, type PickedImage } from './image-picker';

function emptyDraft(): BannerRequest {
  return {
    title: '',
    imageUrl: '',
    publicId: null,
    linkUrl: null,
    displayOrder: 0,
    active: true,
    startsAt: null,
    endsAt: null,
  };
}

/**
 * Quản lý banner khuyến mãi trên trang chủ.
 *
 * <h2>Khoảng thời gian hiển thị</h2>
 *
 * `startsAt`/`endsAt` để trống nghĩa là "không giới hạn phía đó" — banner bật
 * ngay, hoặc chạy mãi. Trang công khai lọc theo thời gian ở TRUY VẤN, nên một
 * banner hết hạn lúc nửa đêm sẽ biến mất mà không cần ai vào tắt tay.
 *
 * <h2>Đường dẫn của banner</h2>
 *
 * Backend chỉ nhận `http`, `https`, `mailto` và đường dẫn nội bộ bắt đầu bằng
 * `/`. Dán `javascript:...` vào đây sẽ bị từ chối ngay khi lưu, không phải khi
 * có khách bấm vào — banner là thứ hiện trên trang chủ cho mọi người.
 */
@Component({
  selector: 'app-admin-banners',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton, UiSkeleton, UiEmptyState, UiConfirmDialog, UiToast, ImagePicker],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Banner khuyến mãi</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    <div class="mb-4">
      <ui-button (pressed)="startCreate()">Thêm banner</ui-button>
    </div>

    @if (editing()) {
      <form class="mb-6 rounded-md border border-border bg-surface p-4" (submit)="save($event)">
        <h2 class="mb-3 font-semibold text-text">
          {{ editingId() === null ? 'Banner mới' : 'Sửa banner' }}
        </h2>

        <div class="flex flex-col gap-3">
          <ui-input
            label="Tiêu đề"
            [required]="true"
            hint="Hiển thị dưới ảnh và dùng làm chữ thay thế cho ảnh."
            [value]="draft().title"
            (valueChange)="patch({ title: $event })" />

          <app-image-picker
            label="Ảnh banner"
            folder="banners"
            hint="Tỉ lệ ngang, khoảng 960×360."
            [value]="draft().imageUrl"
            (picked)="onImage($event)" />

          <ui-input
            label="Đường dẫn khi bấm vào"
            placeholder="/phong hoặc https://…"
            hint="Để trống nếu banner chỉ để xem. Chỉ nhận http, https, mailto hoặc đường dẫn nội bộ."
            [value]="draft().linkUrl ?? ''"
            (valueChange)="patch({ linkUrl: $event || null })" />

          <div class="grid gap-3 sm:grid-cols-2">
            <ui-input
              label="Bắt đầu hiện"
              type="text"
              placeholder="2026-10-01T00:00:00Z"
              hint="Để trống là hiện ngay."
              [value]="draft().startsAt ?? ''"
              (valueChange)="patch({ startsAt: $event || null })" />
            <ui-input
              label="Ngừng hiện"
              type="text"
              placeholder="2026-12-31T23:59:59Z"
              hint="Để trống là chạy mãi."
              [value]="draft().endsAt ?? ''"
              (valueChange)="patch({ endsAt: $event || null })" />
          </div>

          <ui-input
            label="Thứ tự hiển thị"
            type="number"
            hint="Số nhỏ hiện trước."
            [value]="draft().displayOrder + ''"
            (valueChange)="patch({ displayOrder: toInt($event) })" />

          <label class="flex min-h-[var(--touch-min)] items-center gap-2 text-sm text-text">
            <input
              type="checkbox"
              [checked]="draft().active"
              (change)="patch({ active: isChecked($event) })" />
            Đang bật
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
    } @else if (banners().length === 0) {
      <ui-empty-state
        title="Chưa có banner nào"
        description="Thêm banner để giới thiệu ưu đãi ngay trên trang chủ."
        actionLabel="Thêm banner"
        (action)="startCreate()" />
    } @else {
      <ul class="space-y-3">
        @for (banner of banners(); track banner.id) {
          <li class="flex flex-wrap items-center gap-3 rounded-md border border-border bg-surface p-3">
            <img
              [src]="banner.imageUrl"
              [alt]="banner.title"
              width="128"
              height="48"
              class="h-12 w-32 rounded-sm object-cover" />
            <div class="min-w-0 flex-1">
              <p class="font-semibold text-text">{{ banner.title }}</p>
              <p class="truncate text-xs text-text-muted">
                Thứ tự {{ banner.displayOrder }}
                @if (!banner.active) {
                  · đang tắt
                }
                @if (banner.linkUrl) {
                  · {{ banner.linkUrl }}
                }
              </p>
            </div>
            <ui-button variant="secondary" (pressed)="startEdit(banner)">Sửa</ui-button>
            <ui-button variant="danger" (pressed)="askDelete(banner)">Xoá</ui-button>
          </li>
        }
      </ul>
    }

    <ui-confirm-dialog
      [open]="deleting() !== null"
      title="Xoá banner"
      [question]="'Xoá banner “' + (deleting()?.title ?? '') + '”?'"
      [consequences]="[
        'Banner biến mất khỏi trang chủ ngay lập tức.',
        'Ảnh đã tải lên vẫn nằm trên máy chủ, không tự xoá theo.',
        'Thao tác này không hoàn tác được.',
      ]"
      confirmLabel="Xoá banner"
      [loading]="saving()"
      (confirmed)="confirmDelete()"
      (cancelled)="deleting.set(null)" />

    @if (toast(); as message) {
      <ui-toast kind="success" [message]="message" (dismissed)="toast.set(null)" />
    }
  `,
})
export class AdminBanners {
  private readonly api = inject(AdminContentService);

  protected readonly banners = signal<BannerView[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editing = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly draft = signal<BannerRequest>(emptyDraft());
  protected readonly deleting = signal<BannerView | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly toast = signal<string | null>(null);

  constructor() {
    this.load();
  }

  /** `Number` không có trong phạm vi template, nên phép đổi kiểu nằm ở đây. */
  protected toInt(value: string): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? Math.trunc(parsed) : 0;
  }

  protected isChecked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected patch(patch: Partial<BannerRequest>): void {
    this.draft.update((current) => ({ ...current, ...patch }));
  }

  protected onImage(image: PickedImage): void {
    this.patch({ imageUrl: image.url, publicId: image.publicId });
  }

  protected startCreate(): void {
    this.draft.set(emptyDraft());
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(banner: BannerView): void {
    this.draft.set({
      title: banner.title,
      imageUrl: banner.imageUrl,
      publicId: banner.publicId,
      linkUrl: banner.linkUrl,
      displayOrder: banner.displayOrder,
      active: banner.active,
      startsAt: banner.startsAt,
      endsAt: banner.endsAt,
    });
    this.editingId.set(banner.id);
    this.editing.set(true);
  }

  protected save(event: Event): void {
    event.preventDefault();
    const body = this.draft();
    if (!body.title.trim() || !body.imageUrl.trim()) {
      this.error.set('Banner cần có tiêu đề và ảnh.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request = id === null ? this.api.createBanner(body) : this.api.updateBanner(id, body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        this.toast.set('Đã lưu banner.');
        this.load();
      },
      error: (failure: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.error.set(
          failure?.error?.detail ?? 'Không lưu được banner. Kiểm tra lại đường dẫn và thời gian.',
        );
      },
    });
  }

  protected askDelete(banner: BannerView): void {
    this.deleting.set(banner);
  }

  protected confirmDelete(): void {
    const banner = this.deleting();
    if (!banner) {
      return;
    }
    this.saving.set(true);
    this.api.deleteBanner(banner.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.toast.set('Đã xoá banner.');
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.deleting.set(null);
        this.error.set('Không xoá được banner. Thử lại sau giây lát.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.banners().subscribe({
      next: (data) => {
        this.banners.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách banner.');
        this.loading.set(false);
      },
    });
  }
}
