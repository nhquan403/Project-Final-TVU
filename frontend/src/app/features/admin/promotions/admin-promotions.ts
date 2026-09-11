import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import type { PromotionView } from '../../../core/services/admin.types';
import {
  UiButton,
  UiConfirmDialog,
  UiEmptyState,
  UiInput,
  UiSelect,
  UiSkeleton,
  type SelectOption,
} from '../../../shared/ui';

const DISCOUNT_TYPES: SelectOption[] = [
  { value: 'PERCENT', label: 'Giảm theo phần trăm' },
  { value: 'FIXED', label: 'Giảm số tiền cố định' },
];

/**
 * Quản lý mã khuyến mãi.
 *
 * `usageLimit = null` nghĩa là KHÔNG giới hạn lượt dùng, và màn hình phải nói
 * đúng như vậy. Hiển thị nó thành `0` hay `—` khiến người dùng tưởng mã đã hết
 * lượt và đi tạo mã mới cho một chiến dịch vẫn đang chạy.
 */
@Component({
  selector: 'app-admin-promotions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiInput, UiSelect, UiEmptyState, UiSkeleton, UiConfirmDialog],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Khuyến mãi</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    @if (loading()) {
      <ui-skeleton shape="line" />
    } @else if (promotions().length === 0) {
      <ui-empty-state
        title="Chưa có mã khuyến mãi nào"
        description="Tạo mã đầu tiên để khách nhập được ở bước đặt phòng."
        actionLabel="Tạo mã"
        (action)="startCreate()" />
    } @else {
      <div class="mb-4">
        <ui-button (pressed)="startCreate()">Thêm mã</ui-button>
      </div>

      <ul class="space-y-3">
        @for (promotion of promotions(); track promotion.id) {
          <li class="rounded-md border border-border bg-surface p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-text">
                  <span class="font-mono">{{ promotion.code }}</span> · {{ promotion.name }}
                  @if (!promotion.active) {
                    <span class="ml-2 text-xs font-normal text-text-muted">(đang tắt)</span>
                  }
                </p>
                <p class="text-sm text-text-muted">
                  {{ describe(promotion) }} · tối thiểu {{ promotion.minNights }} đêm
                </p>
                <p class="text-sm text-text-muted">
                  Hiệu lực: {{ promotion.startsAt }} → {{ promotion.endsAt }}
                </p>
                <p class="text-sm tabular-nums text-text-muted">
                  Đã dùng {{ promotion.usedCount }} / {{ limitLabel(promotion) }} ·
                  {{ promotion.bookingCount }} đơn đang gắn mã
                </p>
              </div>
              <div class="flex gap-2">
                <ui-button variant="secondary" (pressed)="startEdit(promotion)">Sửa</ui-button>
                <ui-button variant="danger" (pressed)="pendingDelete.set(promotion)">Xoá</ui-button>
              </div>
            </div>
          </li>
        }
      </ul>
    }

    @if (editing(); as draft) {
      <section class="mt-5 rounded-md border border-border bg-surface p-4">
        <h2 class="mb-3 text-sm font-semibold text-text">
          {{ draft.id ? 'Sửa mã ' + draft.code : 'Thêm mã mới' }}
        </h2>
        @if (draft.id && draft.bookingCount) {
          <p class="mb-3 rounded-sm bg-surface-2 p-2 text-sm text-text-muted">
            Mã này đã có {{ draft.bookingCount }} đơn sử dụng nên KHÔNG đổi được chuỗi mã — nó nằm
            trong dữ liệu đối soát và trong thư đã gửi cho khách.
          </p>
        }
        <div class="grid gap-3 sm:grid-cols-2">
          <ui-input label="Mã" [(value)]="code" [disabled]="!!draft.id && !!draft.bookingCount" />
          <ui-input label="Tên chiến dịch" [(value)]="name" />
          <ui-select label="Kiểu giảm" [options]="discountTypes" [(value)]="discountType" />
          <ui-input label="Giá trị giảm" type="number" [(value)]="discountValue" />
          <ui-input label="Trần giảm (để trống = không trần)" type="number"
            [(value)]="maxDiscountAmount" />
          <ui-input label="Số đêm tối thiểu" type="number" [(value)]="minNights" />
          <ui-input label="Tổng tiền tối thiểu" type="number" [(value)]="minTotalAmount" />
          <ui-input label="Giới hạn lượt (để trống = không giới hạn)" type="number"
            [(value)]="usageLimit" />
          <ui-input label="Bắt đầu (ISO)" [(value)]="startsAt" />
          <ui-input label="Kết thúc (ISO)" [(value)]="endsAt" />
          <label class="inline-flex min-h-[var(--touch-min)] items-center gap-2 text-sm text-text">
            <input type="checkbox" [checked]="active()" (change)="active.set(!active())" />
            Đang hoạt động
          </label>
        </div>
        <div class="mt-3 flex gap-2">
          <ui-button [loading]="saving()" (pressed)="save()">Lưu</ui-button>
          <ui-button variant="secondary" (pressed)="editing.set(null)">Thoát</ui-button>
        </div>
      </section>
    }

    @if (pendingDelete(); as promotion) {
      <ui-confirm-dialog
        [open]="true"
        title="Xoá mã {{ promotion.code }}?"
        question="Thao tác này không hoàn tác được."
        [consequences]="[
          'Khách nhập mã này ở bước đặt phòng sẽ nhận thông báo mã không hợp lệ.',
          promotion.bookingCount + ' đơn đang gắn mã — nếu lớn hơn 0, hệ thống sẽ từ chối xoá.',
          'Muốn ngừng chiến dịch mà giữ lịch sử: hãy TẮT mã thay vì xoá.'
        ]"
        confirmLabel="Xoá mã"
        (confirmed)="remove(promotion)"
        (cancelled)="pendingDelete.set(null)" />
    }
  `,
})
export class AdminPromotions {
  private readonly catalog = inject(AdminCatalogService);

  protected readonly promotions = signal<PromotionView[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly editing = signal<Partial<PromotionView> | null>(null);
  protected readonly pendingDelete = signal<PromotionView | null>(null);

  protected readonly discountTypes = DISCOUNT_TYPES;

  protected readonly code = signal('');
  protected readonly name = signal('');
  protected readonly discountType = signal('PERCENT');
  protected readonly discountValue = signal('10');
  protected readonly maxDiscountAmount = signal('');
  protected readonly minNights = signal('1');
  protected readonly minTotalAmount = signal('0');
  protected readonly usageLimit = signal('');
  protected readonly startsAt = signal('');
  protected readonly endsAt = signal('');
  protected readonly active = signal(true);

  constructor() {
    this.load();
  }

  protected describe(promotion: PromotionView): string {
    return promotion.discountType === 'PERCENT'
      ? `Giảm ${promotion.discountValue}%`
      : `Giảm ${promotion.discountValue.toLocaleString('vi-VN')} đ`;
  }

  /** `null` phải đọc thành "không giới hạn", không phải `0` hay dấu gạch. */
  protected limitLabel(promotion: PromotionView): string {
    return promotion.usageLimit === null ? 'không giới hạn' : String(promotion.usageLimit);
  }

  protected startCreate(): void {
    this.editing.set({});
    this.code.set('');
    this.name.set('');
    this.discountType.set('PERCENT');
    this.discountValue.set('10');
    this.maxDiscountAmount.set('');
    this.minNights.set('1');
    this.minTotalAmount.set('0');
    this.usageLimit.set('');
    this.startsAt.set(new Date().toISOString());
    this.endsAt.set(new Date(Date.now() + 30 * 86_400_000).toISOString());
    this.active.set(true);
  }

  protected startEdit(promotion: PromotionView): void {
    this.editing.set(promotion);
    this.code.set(promotion.code);
    this.name.set(promotion.name);
    this.discountType.set(promotion.discountType);
    this.discountValue.set(String(promotion.discountValue));
    this.maxDiscountAmount.set(
      promotion.maxDiscountAmount === null ? '' : String(promotion.maxDiscountAmount),
    );
    this.minNights.set(String(promotion.minNights));
    this.minTotalAmount.set(String(promotion.minTotalAmount));
    this.usageLimit.set(promotion.usageLimit === null ? '' : String(promotion.usageLimit));
    this.startsAt.set(promotion.startsAt);
    this.endsAt.set(promotion.endsAt);
    this.active.set(promotion.active);
  }

  protected save(): void {
    const draft = this.editing();
    if (!draft) {
      return;
    }
    const body = {
      code: this.code(),
      name: this.name(),
      description: draft.description ?? null,
      discountType: this.discountType(),
      discountValue: Number(this.discountValue()),
      maxDiscountAmount: this.maxDiscountAmount() ? Number(this.maxDiscountAmount()) : null,
      minNights: Number(this.minNights()),
      minTotalAmount: Number(this.minTotalAmount()),
      // Để trống = KHÔNG giới hạn. Gửi 0 sẽ bị backend từ chối, đúng ý đồ.
      usageLimit: this.usageLimit() ? Number(this.usageLimit()) : null,
      startsAt: this.startsAt(),
      endsAt: this.endsAt(),
      active: this.active(),
    };

    this.saving.set(true);
    const request = draft.id
      ? this.catalog.updatePromotion(draft.id, body)
      : this.catalog.createPromotion(body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(null);
        this.error.set(null);
        this.load();
      },
      error: (response) => {
        this.saving.set(false);
        this.error.set(response?.error?.detail ?? 'Không lưu được mã khuyến mãi.');
      },
    });
  }

  protected remove(promotion: PromotionView): void {
    this.pendingDelete.set(null);
    this.catalog.deletePromotion(promotion.id).subscribe({
      next: () => this.load(),
      error: (response) => this.error.set(response?.error?.detail ?? 'Không xoá được mã.'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.catalog.promotions().subscribe({
      next: (list) => {
        this.promotions.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách mã khuyến mãi.');
        this.loading.set(false);
      },
    });
  }
}
