import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import type { AmenityView, RoomTypeView } from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiInput,
  UiSkeleton,
  VndCurrencyPipe,
} from '../../../shared/ui';

/**
 * Quản lý loại phòng: thông tin, tiện ích, thư viện ảnh.
 *
 * Xoá loại phòng còn đơn bị backend chặn bằng 409 kèm lời chỉ đường (tắt trạng
 * thái hoạt động). Màn hình hiển thị nguyên câu đó thay vì dịch lại thành một
 * thông báo chung chung — người đọc cần biết việc tiếp theo là gì.
 */
@Component({
  selector: 'app-admin-room-types',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiInput, UiEmptyState, UiSkeleton, UiConfirmDialog, VndCurrencyPipe],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Loại phòng</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else if (roomTypes().length === 0) {
      <ui-empty-state
        title="Chưa có loại phòng nào"
        description="Tạo loại phòng đầu tiên để trang đặt phòng có thứ để bán."
        actionLabel="Tạo loại phòng"
        (action)="startCreate()" />
    } @else {
      <div class="mb-4">
        <ui-button (pressed)="startCreate()">Thêm loại phòng</ui-button>
      </div>

      <ul class="space-y-3">
        @for (type of roomTypes(); track type.id) {
          <li class="rounded-md border border-border bg-surface p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-text">
                  {{ type.name }}
                  @if (!type.active) {
                    <span class="ml-2 text-xs font-normal text-text-muted">(đang tắt)</span>
                  }
                </p>
                <p class="text-sm text-text-muted">
                  {{ type.code }} · {{ type.basePrice | vndCurrency }}/đêm ·
                  {{ type.capacityAdults }} người lớn + {{ type.capacityChildren }} trẻ em ·
                  {{ type.roomCount }} phòng vật lý
                </p>
                @if (type.bedInfo) {
                  <p class="text-sm text-text-muted">{{ type.bedInfo }} · {{ type.areaSqm ?? '—' }} m²</p>
                }
                <p class="mt-1 text-sm text-text-muted">
                  Tiện ích: {{ amenityNames(type) || 'chưa gắn tiện ích nào' }}
                </p>
              </div>
              <div class="flex gap-2">
                <ui-button variant="secondary" (pressed)="startEdit(type)">Sửa</ui-button>
                <ui-button variant="danger" (pressed)="pendingDelete.set(type)">Xoá</ui-button>
              </div>
            </div>

            <div class="mt-3">
              <p class="mb-1 text-sm font-semibold text-text">Ảnh ({{ type.images.length }})</p>
              <div class="flex flex-wrap gap-2">
                @for (image of type.images; track image.id) {
                  <figure class="w-28">
                    <img
                      [src]="image.url"
                      [alt]="image.altText ?? type.name"
                      width="112"
                      height="84"
                      loading="lazy"
                      class="h-21 w-28 rounded-sm border border-border object-cover" />
                    <figcaption class="mt-1 flex items-center justify-between text-xs text-text-muted">
                      <span>{{ image.cover ? 'Ảnh bìa' : '#' + image.displayOrder }}</span>
                      <button
                        type="button"
                        class="min-h-[var(--touch-min)] text-danger underline"
                        (click)="deleteImage(image.id)">
                        Xoá
                      </button>
                    </figcaption>
                  </figure>
                }
              </div>
              <label class="mt-2 inline-flex min-h-[var(--touch-min)] items-center text-sm text-focus underline">
                Tải ảnh lên
                <input
                  type="file"
                  class="sr-only"
                  accept="image/jpeg,image/png,image/webp"
                  (change)="uploadImage(type, $event)" />
              </label>
            </div>

            <details class="mt-3">
              <summary class="cursor-pointer text-sm text-focus underline">Gắn tiện ích</summary>
              <div class="mt-2 flex flex-wrap gap-2">
                @for (amenity of amenities(); track amenity.id) {
                  <label class="inline-flex min-h-[var(--touch-min)] items-center gap-1 text-sm text-text">
                    <input
                      type="checkbox"
                      [checked]="hasAmenity(type, amenity)"
                      (change)="toggleAmenity(type, amenity)" />
                    {{ amenity.name }}
                  </label>
                }
              </div>
            </details>
          </li>
        }
      </ul>
    }

    @if (editing(); as draft) {
      <section class="mt-5 rounded-md border border-border bg-surface p-4">
        <h2 class="mb-3 text-sm font-semibold text-text">
          {{ draft.id ? 'Sửa loại phòng' : 'Thêm loại phòng' }}
        </h2>
        <div class="grid gap-3 sm:grid-cols-2">
          <ui-input label="Mã" [(value)]="code" />
          <ui-input label="Đường dẫn (slug)" [(value)]="slug" />
          <ui-input label="Tên hiển thị" [(value)]="name" />
          <ui-input label="Giá mỗi đêm" type="number" [(value)]="basePrice" />
          <ui-input label="Sức chứa người lớn" type="number" [(value)]="capacityAdults" />
          <ui-input label="Sức chứa trẻ em" type="number" [(value)]="capacityChildren" />
          <ui-input label="Thông tin giường" [(value)]="bedInfo" />
          <ui-input label="Diện tích (m²)" type="number" [(value)]="areaSqm" />
          <ui-input label="Thứ tự hiển thị" type="number" [(value)]="displayOrder" />
          <label class="inline-flex min-h-[var(--touch-min)] items-center gap-2 text-sm text-text">
            <input type="checkbox" [checked]="active()" (change)="active.set(!active())" />
            Đang hoạt động (hiện trên trang đặt phòng)
          </label>
        </div>
        <div class="mt-3 flex gap-2">
          <ui-button [loading]="saving()" (pressed)="save()">Lưu</ui-button>
          <ui-button variant="secondary" (pressed)="editing.set(null)">Thoát</ui-button>
        </div>
      </section>
    }

    @if (pendingDelete(); as type) {
      <ui-confirm-dialog
        [open]="true"
        title="Xoá loại phòng {{ type.name }}?"
        question="Thao tác này không hoàn tác được."
        [consequences]="[
          type.roomCount + ' phòng vật lý thuộc loại này phải được chuyển hoặc xoá trước.',
          'Ảnh của loại phòng sẽ bị xoá khỏi nơi lưu trữ.',
          'Nếu loại phòng đã có đơn, hệ thống sẽ từ chối — khi đó hãy TẮT trạng thái hoạt động thay vì xoá.'
        ]"
        confirmLabel="Xoá loại phòng"
        (confirmed)="remove(type)"
        (cancelled)="pendingDelete.set(null)" />
    }
  `,
})
export class AdminRoomTypes {
  private readonly catalog = inject(AdminCatalogService);

  protected readonly roomTypes = signal<RoomTypeView[]>([]);
  protected readonly amenities = signal<AmenityView[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly editing = signal<Partial<RoomTypeView> | null>(null);
  protected readonly pendingDelete = signal<RoomTypeView | null>(null);

  protected readonly code = signal('');
  protected readonly slug = signal('');
  protected readonly name = signal('');
  protected readonly basePrice = signal('0');
  protected readonly capacityAdults = signal('2');
  protected readonly capacityChildren = signal('0');
  protected readonly bedInfo = signal('');
  protected readonly areaSqm = signal('');
  protected readonly displayOrder = signal('0');
  protected readonly active = signal(true);

  constructor() {
    this.load();
    this.catalog.amenities().subscribe({
      next: (list) => this.amenities.set(list),
      error: () => this.amenities.set([]),
    });
  }

  protected amenityNames(type: RoomTypeView): string {
    return type.amenities.map((amenity) => amenity.name).join(', ');
  }

  protected hasAmenity(type: RoomTypeView, amenity: AmenityView): boolean {
    return type.amenities.some((item) => item.id === amenity.id);
  }

  protected toggleAmenity(type: RoomTypeView, amenity: AmenityView): void {
    const ids = this.hasAmenity(type, amenity)
      ? type.amenities.filter((item) => item.id !== amenity.id).map((item) => item.id)
      : [...type.amenities.map((item) => item.id), amenity.id];
    this.catalog.setAmenities(type.id, ids).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Không cập nhật được tiện ích.'),
    });
  }

  protected startCreate(): void {
    this.editing.set({});
    this.code.set('');
    this.slug.set('');
    this.name.set('');
    this.basePrice.set('500000');
    this.capacityAdults.set('2');
    this.capacityChildren.set('0');
    this.bedInfo.set('');
    this.areaSqm.set('');
    this.displayOrder.set('0');
    this.active.set(true);
  }

  protected startEdit(type: RoomTypeView): void {
    this.editing.set(type);
    this.code.set(type.code);
    this.slug.set(type.slug);
    this.name.set(type.name);
    this.basePrice.set(String(type.basePrice));
    this.capacityAdults.set(String(type.capacityAdults));
    this.capacityChildren.set(String(type.capacityChildren));
    this.bedInfo.set(type.bedInfo ?? '');
    this.areaSqm.set(type.areaSqm === null ? '' : String(type.areaSqm));
    this.displayOrder.set(String(type.displayOrder));
    this.active.set(type.active);
  }

  protected save(): void {
    const draft = this.editing();
    if (!draft) {
      return;
    }
    const body = {
      code: this.code(),
      slug: this.slug(),
      name: this.name(),
      shortDescription: draft.shortDescription ?? null,
      description: draft.description ?? null,
      basePrice: Number(this.basePrice()),
      capacityAdults: Number(this.capacityAdults()),
      capacityChildren: Number(this.capacityChildren()),
      bedInfo: this.bedInfo() || null,
      areaSqm: this.areaSqm() ? Number(this.areaSqm()) : null,
      displayOrder: Number(this.displayOrder()),
      active: this.active(),
    };

    this.saving.set(true);
    const request = draft.id
      ? this.catalog.updateRoomType(draft.id, body)
      : this.catalog.createRoomType(body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(null);
        this.load();
      },
      error: (response) => {
        this.saving.set(false);
        this.error.set(response?.error?.detail ?? 'Không lưu được loại phòng.');
      },
    });
  }

  protected remove(type: RoomTypeView): void {
    this.pendingDelete.set(null);
    this.catalog.deleteRoomType(type.id).subscribe({
      next: () => this.load(),
      // Backend trả 409 kèm lời chỉ đường; hiện nguyên văn để người dùng biết
      // việc tiếp theo là tắt trạng thái hoạt động.
      error: (response) =>
        this.error.set(response?.error?.detail ?? 'Không xoá được loại phòng.'),
    });
  }

  protected uploadImage(type: RoomTypeView, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.catalog.uploadImage(file).subscribe({
      next: (uploaded) => {
        this.catalog.addImage(type.id, uploaded.url, uploaded.publicId, type.name).subscribe({
          next: () => this.load(),
          error: () => this.error.set('Tải ảnh lên được nhưng không gắn được vào loại phòng.'),
        });
        input.value = '';
      },
      error: (response) => {
        input.value = '';
        this.error.set(response?.error?.detail ?? 'Không tải được ảnh lên.');
      },
    });
  }

  protected deleteImage(imageId: number): void {
    this.catalog.deleteImage(imageId).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Không xoá được ảnh.'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.catalog.roomTypes().subscribe({
      next: (list) => {
        this.roomTypes.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách loại phòng.');
        this.loading.set(false);
      },
    });
  }
}
