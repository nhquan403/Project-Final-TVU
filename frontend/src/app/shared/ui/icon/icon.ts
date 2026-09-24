import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

/**
 * Icon nội tuyến, vẽ bằng SVG.
 *
 * Trước đây tiện ích hiển thị bằng emoji lấy thẳng từ cột `amenities.icon`.
 * Emoji hỏng ở ba chỗ cùng lúc: mỗi hệ điều hành vẽ một kiểu (🅿️ trên Windows
 * là chữ P xanh, trên Android là biển báo), không nhuộm được theo token màu
 * nên nó luôn lạc khỏi bảng màu, và cỡ chữ không khớp với chữ bên cạnh.
 *
 * Nên cột `icon` giờ chứa TÊN icon, còn hình thì nằm ở đây. Dữ liệu không mô
 * tả hình dáng nữa, nó chỉ đặt tên cho ý nghĩa.
 *
 * Nét vẽ 1.75 và khung 24×24 là quy ước chung — trộn nét dày mỏng trong cùng
 * một hàng là lỗi dễ thấy nhất của một bộ icon chắp vá.
 */
export type IconName =
  // tiện ích trong phòng
  | 'wifi' | 'dieu-hoa' | 'nuoc-nong' | 'tv' | 'tu-lanh' | 'ban-cong' | 'ban-lam-viec'
  // tiện ích chung
  | 'bai-do-xe' | 'bua-sang' | 'san-vuon' | 'xe-dap' | 'le-tan'
  // trạng thái
  | 'thanh-cong' | 'loi' | 'canh-bao' | 'thong-tin'
  // giao diện
  | 'lich' | 'tim' | 'dong' | 'kiem' | 'cong' | 'tru' | 'mui-phai' | 'mui-trai'
  | 'mui-xuong' | 'ban-do' | 'dien-thoai' | 'thu' | 'nguoi' | 'giuong' | 'dien-tich'
  | 'menu' | 'sao';

/**
 * Hình của từng icon: phần nằm TRONG <svg>, khung 24×24, chỉ dùng nét (stroke)
 * để mọi icon nhận màu từ `currentColor`.
 */
const HINH: Record<IconName, string> = {
  wifi: '<path d="M5 12.5a10 10 0 0 1 14 0"/><path d="M2 8.8a15 15 0 0 1 20 0"/><path d="M8.5 16a5 5 0 0 1 7 0"/><circle cx="12" cy="19.5" r="1"/>',
  'dieu-hoa': '<path d="M12 3v18M4.2 7.5l15.6 9M19.8 7.5l-15.6 9"/><path d="M12 7l-2.2-2.2M12 7l2.2-2.2M12 17l-2.2 2.2M12 17l2.2 2.2"/>',
  'nuoc-nong': '<path d="M12 3s5 5.6 5 9.2A5 5 0 0 1 7 12.2C7 8.6 12 3 12 3z"/>',
  tv: '<rect x="2.5" y="6" width="19" height="12.5" rx="2"/><path d="M8 3l4 3 4-3"/>',
  'tu-lanh': '<rect x="5.5" y="2.5" width="13" height="19" rx="2"/><path d="M5.5 10h13M8.5 6v2M8.5 13.5v2.5"/>',
  'ban-cong': '<path d="M12 21c0-7 3.5-11 8-12-.5 6-4 9.5-8 10z"/><path d="M12 21c0-5-2.5-8-6-8.8.4 4.6 2.8 7.3 6 8z"/><path d="M12 21v-4"/>',
  'ban-lam-viec': '<path d="M3 8.5h18M4.5 8.5V6a1.5 1.5 0 0 1 1.5-1.5h12A1.5 1.5 0 0 1 19.5 6v2.5"/><path d="M6 8.5v11M18 8.5v11M6 14h12"/>',
  'bai-do-xe': '<rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9.5 17V7.5h3.2a2.9 2.9 0 0 1 0 5.8H9.5"/>',
  'bua-sang': '<path d="M3.5 11.5h17a8.5 8.5 0 0 1-17 0z"/><path d="M2.5 20.5h19"/><path d="M9 8c0-1.2-1-1.6-1-2.8S9 3.5 9 3.5M13 8c0-1.2-1-1.6-1-2.8s1-1.7 1-1.7"/>',
  'san-vuon': '<path d="M12 21v-6"/><path d="M12 15c-4.5 0-7-2.8-7-6.3C5 5.2 8 2.5 12 2.5s7 2.7 7 6.2c0 3.5-2.5 6.3-7 6.3z"/><path d="M12 15l3-3M12 12L9.5 9.5"/>',
  'xe-dap': '<circle cx="5.8" cy="17" r="3.4"/><circle cx="18.2" cy="17" r="3.4"/><path d="M5.8 17l4.4-8.2h4.6l3.4 8.2M9.8 8.8h5"/>',
  'le-tan': '<path d="M4 18h16"/><path d="M6 18a6 6 0 0 1 12 0"/><path d="M12 6V4.5M10.5 4.5h3"/>',

  'thanh-cong': '<circle cx="12" cy="12" r="9"/><path d="M8.2 12.3l2.6 2.6 5-5.4"/>',
  loi: '<circle cx="12" cy="12" r="9"/><path d="M12 7.5v5.2M12 16.3v.2"/>',
  'canh-bao': '<path d="M10.7 3.9L2.5 18a1.5 1.5 0 0 0 1.3 2.3h16.4A1.5 1.5 0 0 0 21.5 18L13.3 3.9a1.5 1.5 0 0 0-2.6 0z"/><path d="M12 9.5v4M12 17v.2"/>',
  'thong-tin': '<circle cx="12" cy="12" r="9"/><path d="M12 11.3v5M12 7.7v.2"/>',

  lich: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
  tim: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4.2-4.2"/>',
  dong: '<path d="M6 6l12 12M18 6L6 18"/>',
  kiem: '<path d="M4.5 12.5l5 5 10-11"/>',
  cong: '<path d="M12 5v14M5 12h14"/>',
  tru: '<path d="M5 12h14"/>',
  'mui-phai': '<path d="M5 12h14M13 6l6 6-6 6"/>',
  'mui-trai': '<path d="M19 12H5M11 6l-6 6 6 6"/>',
  'mui-xuong': '<path d="M6 9.5l6 6 6-6"/>',
  'ban-do': '<path d="M12 21s7-5.7 7-11a7 7 0 1 0-14 0c0 5.3 7 11 7 11z"/><circle cx="12" cy="10" r="2.6"/>',
  'dien-thoai': '<path d="M6.5 3h3l1.6 4-2 1.4a12 12 0 0 0 5.5 5.5l1.4-2 4 1.6v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.5 5.2 2 2 0 0 1 6.5 3z"/>',
  thu: '<rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M3.5 6.5l8.5 6.5 8.5-6.5"/>',
  nguoi: '<circle cx="12" cy="8" r="3.8"/><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0"/>',
  giuong: '<path d="M3 18V7M3 12h18v6M21 18v-4"/><circle cx="7.8" cy="10" r="2"/><path d="M11.5 12V9.5h6a3 3 0 0 1 3 2.5"/>',
  'dien-tich': '<rect x="3.5" y="3.5" width="17" height="17" rx="1.5"/><path d="M8 8h3M8 8v3M16 16h-3M16 16v-3"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  sao: '<path d="M12 3.5l2.7 5.6 6 .85-4.35 4.3 1.03 6.1L12 17.5l-5.38 2.85 1.03-6.1L3.3 9.95l6-.85z"/>',
};

/** Tên icon hợp lệ, dùng để kiểm dữ liệu đến từ CSDL. */
export function laTenIcon(gia_tri: string | null | undefined): gia_tri is IconName {
  return !!gia_tri && gia_tri in HINH;
}

/**
 * Lọc giá trị lấy từ CSDL về một tên icon dùng được, hoặc `null`.
 *
 * Quản trị viên thêm tiện ích mới có thể gõ vào cột `icon` một tên chưa có
 * hình. Khi đó hiện MỖI CHỮ, không hiện icon — một icon sai nghĩa gây hiểu lầm
 * hơn là không có icon nào. Đây cũng là chốt chặn không cho chuỗi bất kỳ từ
 * CSDL đi vào hằng `HINH`.
 */
export function tenIcon(gia_tri: string | null | undefined): IconName | null {
  return laTenIcon(gia_tri) ? gia_tri : null;
}

@Component({
  selector: 'ui-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      [attr.fill]="filled() ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      [attr.aria-hidden]="label() ? null : 'true'"
      [attr.role]="label() ? 'img' : null"
      [attr.aria-label]="label()"
      [innerHTML]="hinh()"></svg>
  `,
  styles: `
    :host { display: inline-flex; flex: none; }
  `,
})
export class UiIcon {
  readonly name = input.required<IconName>();
  /** Cạnh của khung vuông, tính bằng px. 20 hợp với chữ 17px. */
  readonly size = input(20);
  /**
   * Chỉ đặt khi icon là nguồn thông tin DUY NHẤT. Khi cạnh icon đã có chữ —
   * trường hợp thường gặp nhất — để trống, và icon sẽ mang aria-hidden để
   * trình đọc màn hình không đọc lặp.
   */
  readonly label = input<string | null>(null);
  /**
   * Tô đặc thay vì chỉ vẽ nét. Chỉ dùng khi ĐẦY và RỖNG là hai trạng thái của
   * cùng một icon — sao đánh giá là trường hợp duy nhất hiện nay. Đừng dùng nó
   * để "nhấn mạnh": nét dày mỏng lẫn lộn trong một hàng icon là lỗi dễ thấy
   * nhất của một bộ icon chắp vá.
   */
  readonly filled = input(false);

  private readonly sanitizer = inject(DomSanitizer);

  /**
   * `bypassSecurityTrustHtml` ở đây là an toàn, và lý do phải nói rõ vì gọi
   * hàm này thường là dấu hiệu của một lỗ XSS:
   *
   *   1. Chuỗi truyền vào LUÔN lấy từ hằng `HINH` ngay trong tệp này. Không
   *      có đường nào để dữ liệu ngoài đi vào — `name` có kiểu `IconName`,
   *      tức một trong các khoá của chính `HINH`.
   *   2. Giá trị đến từ CSDL (cột `amenities.icon`) phải qua `laTenIcon()`
   *      trước; tên lạ bị loại chứ không được dựng thành HTML.
   *
   * Không bypass thì bộ lọc của Angular gỡ sạch <path>, <circle>, <rect> —
   * SVG không nằm trong danh sách thẻ nó cho phép — và mọi icon ra rỗng.
   */
  protected readonly hinh = computed(() =>
    this.sanitizer.bypassSecurityTrustHtml(HINH[this.name()]),
  );
}
