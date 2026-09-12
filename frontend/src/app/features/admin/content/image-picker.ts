import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import { UiButton, UiInput } from '../../../shared/ui';

/** Kết quả chọn ảnh: đường dẫn để hiển thị và id để xoá ở nhà cung cấp. */
export interface PickedImage {
  url: string;
  publicId: string | null;
}

/**
 * Chọn ảnh: tải tệp lên, hoặc dán sẵn một đường dẫn.
 *
 * <p>Dùng lại `POST /api/admin/images` của Phase 7 — backend ở đó đã kiểm định
 * dạng, giải mã lại ảnh và đổi tên tệp thành UUID. Viết một đường tải ảnh thứ
 * hai cho khu nội dung nghĩa là có một đường không qua những bước ấy.
 *
 * <p>Vẫn giữ ô dán đường dẫn: ảnh của homestay có thể đã nằm sẵn trên một dịch
 * vụ khác, và bắt tải lại chỉ để có bản sao thứ hai là việc vô ích.
 */
@Component({
  selector: 'app-image-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton],
  template: `
    <div class="flex flex-col gap-2">
      <ui-input
        [label]="label()"
        placeholder="https://… hoặc /uploads/…"
        [hint]="hint()"
        [error]="error()"
        [value]="value()"
        (valueChange)="onUrlTyped($event)" />

      <div class="flex flex-wrap items-center gap-2">
        <input
          #file
          type="file"
          accept="image/jpeg,image/png,image/webp"
          class="sr-only"
          (change)="upload($event)" />
        <ui-button variant="secondary" [loading]="uploading()" (pressed)="file.click()">
          Tải ảnh lên
        </ui-button>

        @if (value()) {
          <img
            [src]="value()"
            alt="Ảnh đã chọn"
            width="96"
            height="64"
            class="h-16 w-24 rounded-sm border border-border object-cover" />
          <ui-button variant="ghost" (pressed)="clear()">Bỏ ảnh</ui-button>
        }
      </div>
    </div>
  `,
})
export class ImagePicker {
  private readonly catalog = inject(AdminCatalogService);

  readonly label = input('Ảnh');
  readonly hint = input<string | null>(null);
  readonly folder = input('content');
  readonly value = input('');
  readonly picked = output<PickedImage>();

  protected readonly uploading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onUrlTyped(url: string): void {
    // Dán đường dẫn thì không có publicId — ảnh không do hệ thống này giữ, nên
    // nó cũng không có gì để xoá ở nhà cung cấp.
    this.picked.emit({ url, publicId: null });
  }

  protected clear(): void {
    this.picked.emit({ url: '', publicId: null });
  }

  protected upload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.error.set(null);
    this.catalog.uploadImage(file, this.folder()).subscribe({
      next: (uploaded) => {
        this.uploading.set(false);
        this.picked.emit({ url: uploaded.url, publicId: uploaded.publicId });
        // Xoá giá trị của ô tệp: chọn lại ĐÚNG tệp vừa rồi phải kích hoạt được
        // sự kiện change một lần nữa.
        input.value = '';
      },
      error: () => {
        this.uploading.set(false);
        this.error.set('Không tải được ảnh. Kiểm tra định dạng (JPEG/PNG/WebP) và dung lượng.');
        input.value = '';
      },
    });
  }
}
