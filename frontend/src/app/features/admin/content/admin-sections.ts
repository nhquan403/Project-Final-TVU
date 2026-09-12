import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AdminContentService } from '../../../core/services/admin-content.service';
import type { SectionView } from '../../../core/services/content.service';
import { UiButton, UiInput, UiSkeleton, UiToast } from '../../../shared/ui';
import { ImagePicker, type PickedImage } from './image-picker';

/** Bốn khối trang chủ mà backend có nội dung mặc định. */
const KNOWN_KEYS: { key: string; label: string; note: string }[] = [
  { key: 'hero', label: 'Khối đầu trang', note: 'Ảnh nền và câu chào ngay dưới thanh điều hướng.' },
  { key: 'about', label: 'Giới thiệu', note: 'Đoạn kể về homestay, kèm một ảnh.' },
  { key: 'contact', label: 'Liên hệ', note: 'Số điện thoại, email, giờ làm việc.' },
  { key: 'map', label: 'Đường đi', note: 'Mô tả đường tới homestay, kèm ảnh bản đồ.' },
];

/**
 * Sửa nội dung các khối trang chủ.
 *
 * <h2>Thân khối là HTML, và nó được lọc ở backend</h2>
 *
 * Ô "nội dung" nhận HTML thô. Backend lọc theo DANH SÁCH THẺ CHO PHÉP khi lưu:
 * thẻ ngoài danh sách bị gỡ, `javascript:` trong liên kết bị chặn, ảnh chỉ được
 * trỏ tới nguồn đã khai. Vì thế dán một đoạn có `<script>` vào đây thì phần lưu
 * lại sẽ KHÔNG còn nó — màn hình hiện lại đúng thứ đã lưu để người soạn thấy
 * ngay điều đó, thay vì tưởng mình vừa nhúng được một đoạn mã.
 *
 * <h2>Xoá trắng không xoá khối</h2>
 *
 * Bỏ trống mọi ô thì trang công khai quay về nội dung mặc định của backend, chứ
 * không hiện một vùng trắng. Đó là lý do không có nút "xoá khối" ở đây.
 */
@Component({
  selector: 'app-admin-sections',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton, UiSkeleton, UiToast, ImagePicker],
  template: `
    <h1 class="mb-1 text-xl font-bold text-text">Nội dung trang chủ</h1>
    <p class="mb-4 text-sm text-text-muted">
      Để trống một khối thì trang chủ dùng lại nội dung mặc định của hệ thống.
    </p>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else {
      <ul class="space-y-4">
        @for (block of blocks; track block.key) {
          <li class="rounded-md border border-border bg-surface p-4">
            <h2 class="font-semibold text-text">{{ block.label }}</h2>
            <p class="mb-3 text-sm text-text-muted">{{ block.note }}</p>

            <div class="flex flex-col gap-3">
              <ui-input
                label="Tiêu đề"
                [value]="draftOf(block.key).title ?? ''"
                (valueChange)="patch(block.key, { title: $event })" />

              <ui-input
                label="Tiêu đề phụ"
                [value]="draftOf(block.key).subtitle ?? ''"
                (valueChange)="patch(block.key, { subtitle: $event })" />

              <label class="flex flex-col gap-1">
                <span class="text-sm font-medium text-text">Nội dung (HTML)</span>
                <textarea
                  rows="5"
                  class="min-h-[var(--touch-min)] rounded-md border border-border bg-surface px-3
                         py-2 text-sm text-text"
                  [value]="draftOf(block.key).body ?? ''"
                  (input)="patch(block.key, { body: asValue($event) })"></textarea>
                <span class="text-xs text-text-muted">
                  Thẻ được giữ lại: &lt;p&gt; &lt;h2&gt; &lt;h3&gt; &lt;h4&gt; &lt;ul&gt;
                  &lt;ol&gt; &lt;li&gt; &lt;strong&gt; &lt;em&gt; &lt;a&gt; &lt;img&gt;
                  &lt;br&gt; &lt;blockquote&gt;. Mọi thẻ khác bị gỡ khi lưu — kể cả
                  &lt;b&gt; và &lt;i&gt;, hãy dùng &lt;strong&gt; và &lt;em&gt;.
                </span>
              </label>

              <app-image-picker
                label="Ảnh của khối"
                folder="content"
                [value]="draftOf(block.key).imageUrl ?? ''"
                (picked)="onImage(block.key, $event)" />

              <div>
                <ui-button [loading]="savingKey() === block.key" (pressed)="save(block.key)">
                  Lưu khối này
                </ui-button>
              </div>
            </div>
          </li>
        }
      </ul>
    }

    @if (toast(); as message) {
      <ui-toast kind="success" [message]="message" (dismissed)="toast.set(null)" />
    }
  `,
})
export class AdminSections {
  private readonly api = inject(AdminContentService);

  protected readonly blocks = KNOWN_KEYS;
  protected readonly loading = signal(true);
  protected readonly savingKey = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly toast = signal<string | null>(null);

  private readonly drafts = signal<Record<string, Partial<SectionView>>>({});

  constructor() {
    this.load();
  }

  protected draftOf(key: string): Partial<SectionView> {
    return this.drafts()[key] ?? {};
  }

  protected asValue(event: Event): string {
    return (event.target as HTMLTextAreaElement).value;
  }

  protected patch(key: string, patch: Partial<SectionView>): void {
    this.drafts.update((current) => ({ ...current, [key]: { ...current[key], ...patch } }));
  }

  protected onImage(key: string, image: PickedImage): void {
    this.patch(key, { imageUrl: image.url || null });
  }

  protected save(key: string): void {
    const draft = this.draftOf(key);
    this.savingKey.set(key);
    this.error.set(null);
    this.api
      .saveSection(key, {
        title: blank(draft.title),
        subtitle: blank(draft.subtitle),
        body: blank(draft.body),
        imageUrl: blank(draft.imageUrl),
      })
      .subscribe({
        next: (saved) => {
          // Hiện lại ĐÚNG thứ backend đã lưu, không giữ bản nháp: đó là cách
          // người soạn thấy ngay thẻ nào vừa bị bộ lọc gỡ đi.
          this.drafts.update((current) => ({ ...current, [key]: saved }));
          this.savingKey.set(null);
          this.toast.set('Đã lưu. Mở trang chủ để xem kết quả.');
        },
        error: () => {
          this.savingKey.set(null);
          this.error.set('Không lưu được khối này. Thử lại sau giây lát.');
        },
      });
  }

  private load(): void {
    this.api.sections().subscribe({
      next: (sections) => {
        const byKey: Record<string, Partial<SectionView>> = {};
        for (const section of sections) {
          byKey[section.key] = section;
        }
        this.drafts.set(byKey);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được nội dung. Thử lại sau giây lát.');
        this.loading.set(false);
      },
    });
  }
}

/** Chuỗi rỗng và chuỗi toàn khoảng trắng đều là "không có nội dung". */
function blank(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed === '' ? null : trimmed;
}
