import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { UiButton } from '../button/button';
import { UiModal } from '../modal/modal';

/**
 * Hộp thoại xác nhận cho hành động phá huỷ.
 *
 * Bắt buộc có `consequences`: đây là thứ phân biệt một hộp thoại hữu ích với
 * "Bạn có chắc không?" — câu hỏi mà ai cũng bấm Đồng ý mà không đọc. Nội dung
 * phải nói việc gì sẽ xảy ra và với CÁI GÌ ("Phòng 203 sẽ được trả về kho",
 * "Khách đã đặt cọc 300.000đ — khoản này cần hoàn thủ công").
 *
 * Nhãn nút xác nhận là một ĐỘNG TỪ THẬT ("Huỷ booking"), không phải "OK": người
 * đọc nhanh chỉ nhìn nút, và "OK" không nói nút đó làm gì.
 *
 * Nút an toàn đứng trước và mang `autofocus`, nên Enter là lựa chọn không phá
 * huỷ.
 */
@Component({
  selector: 'ui-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiModal, UiButton],
  template: `
    <ui-modal [open]="open()" [title]="title()" (closed)="cancelled.emit()">
      <p class="mb-3 text-sm text-text">{{ question() }}</p>

      <ul class="mb-4 list-disc space-y-1 pl-5 text-sm text-text-muted">
        @for (line of consequences(); track line) {
          <li>{{ line }}</li>
        }
      </ul>

      <div class="flex flex-wrap justify-end gap-2">
        <ui-button variant="secondary" (pressed)="cancelled.emit()">
          {{ cancelLabel() }}
        </ui-button>
        <ui-button variant="danger" [loading]="loading()" (pressed)="confirmed.emit()">
          {{ confirmLabel() }}
        </ui-button>
      </div>
    </ui-modal>
  `,
})
export class UiConfirmDialog {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly question = input('');
  /** Hậu quả cụ thể, mỗi dòng một việc. Rỗng nghĩa là hộp thoại này chưa dùng được. */
  readonly consequences = input.required<readonly string[]>();
  readonly confirmLabel = input.required<string>();
  readonly cancelLabel = input('Không, giữ nguyên');
  readonly loading = input(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
