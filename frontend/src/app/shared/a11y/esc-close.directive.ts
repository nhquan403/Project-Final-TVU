import { Directive, output } from '@angular/core';

/**
 * Phát sự kiện khi nhấn `Esc`. Nghe ở `document` chứ không ở phần tử: lớp phủ
 * có thể chưa nhận focus vào trong tại thời điểm người dùng nhấn phím.
 */
@Directive({
  selector: '[uiEscClose]',
  host: { '(document:keydown.escape)': 'escape.emit()' },
})
export class EscCloseDirective {
  readonly escape = output<void>();
}
