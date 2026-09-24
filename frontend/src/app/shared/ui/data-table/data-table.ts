import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiSkeleton } from '../skeleton/skeleton';
import { UiEmptyState } from '../empty-state/empty-state';

export interface TableColumn<T> {
  key: string;
  header: string;
  /** Lấy giá trị hiển thị từ một hàng. */
  value: (row: T) => string | number;
  sortable?: boolean;
  /**
   * Cho phép ô xuống dòng.
   *
   * <p>Mặc định KHÔNG xuống dòng. Ở bảng quản trị, một ô như "3.000.000 đ" hay
   * "2026-10-04 → 2026-10-06" bị bẻ làm hai dòng khiến cả hàng cao gấp đôi mà
   * không thêm một thông tin nào; hai mươi hàng như vậy là màn hình cuộn dài
   * gấp đôi. Bật cờ này cho cột chứa câu dài thật sự — tên khách, ghi chú.
   */
  wrap?: boolean;
  /** Căn phải cho cột số — mắt so sánh số theo hàng đơn vị, không theo chữ đầu. */
  numeric?: boolean;
  /**
   * Biến ô thành liên kết tới tuyến trong ứng dụng.
   *
   * <p>Là một thẻ `<a>` thật chứ không phải ô bắt sự kiện bấm: người dùng bàn
   * phím tới được bằng Tab, trình đọc màn hình đọc ra là liên kết, và mở tab
   * mới bằng chuột giữa vẫn chạy. Ô nào không khai `link` giữ nguyên như cũ.
   */
  link?: (row: T) => readonly unknown[];
}

export interface SortState {
  key: string;
  direction: 'asc' | 'desc';
}

/**
 * Bảng dữ liệu cho trang quản trị.
 *
 * Trạng thái rỗng KHÔNG phải một dòng "không có dữ liệu": nó luôn kèm một lối
 * đi tiếp. Bảng trắng trơn khiến người dùng không phân biệt được "chưa có gì"
 * với "trang hỏng".
 *
 * Cột đang sắp xếp mang cả `aria-sort` lẫn dấu mũi tên nhìn thấy được — chỉ tô
 * đậm chữ thì người mù màu và người dùng trình đọc màn hình đều không biết.
 *
 * Bảng bọc trong khối cuộn ngang riêng: ở 360px một bảng 6 cột không thể vừa,
 * nhưng cả TRANG cuộn ngang thì mới là lỗi.
 */
@Component({
  selector: 'ui-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSkeleton, UiEmptyState, RouterLink],
  template: `
    @if (error(); as message) {
      <div class="rounded-md border border-danger bg-surface p-4" role="alert">
        <p class="text-body font-semibold text-danger">{{ message }}</p>
      </div>
    } @else if (!loading() && rows().length === 0) {
      <div class="border border-border bg-surface">
        <ui-empty-state
          [title]="emptyTitle()"
          [description]="emptyDescription()"
          [actionLabel]="emptyActionLabel()"
          (action)="emptyAction.emit()" />
      </div>
    } @else {
      <div class="overflow-x-auto border border-border bg-surface">
        <table class="w-full border-collapse text-sm">
          <caption class="sr-only">{{ caption() }}</caption>
          <thead>
            <tr class="border-b border-border-strong bg-surface-2">
              @for (column of columns(); track column.key) {
                <th
                  scope="col"
                  [attr.aria-sort]="ariaSort(column)"
                  [class]="cellClass() + ' whitespace-nowrap font-semibold ' +
                           (column.numeric ? 'text-right' : 'text-left')">
                  @if (column.sortable) {
                    <button
                      type="button"
                      class="inline-flex min-h-11 items-center gap-1 rounded-sm
                             transition-colors duration-[var(--dur-fast)] hover:text-primary"
                      [class.text-primary]="sort()?.key === column.key"
                      [class.font-bold]="sort()?.key === column.key"
                      (click)="toggleSort(column)">
                      {{ column.header }}
                      <span aria-hidden="true">{{ sortArrow(column) }}</span>
                    </button>
                  } @else {
                    {{ column.header }}
                  }
                </th>
              }
            </tr>
          </thead>

          <tbody>
            @if (loading()) {
              @for (placeholder of skeletonRows; track placeholder) {
                <tr class="border-b border-border">
                  @for (column of columns(); track column.key) {
                    <td [class]="cellClass()"><ui-skeleton shape="line" /></td>
                  }
                </tr>
              }
            } @else {
              @for (row of rows(); track $index) {
                <!--
                  Hàng lẻ tô nền phụ: khi bảng rộng tới 8 cột, mắt lần theo một
                  hàng ngang rất dễ trượt sang hàng kế. Nền xen kẽ rẻ hơn kẻ ô
                  và không làm bảng nhìn như bảng tính.
                -->
                <tr
                  class="border-b border-border transition-colors duration-[var(--dur-fast)]
                         odd:bg-surface-2/60 hover:bg-surface-2">
                  @for (column of columns(); track column.key) {
                    <td
                      [attr.tabindex]="column.link ? null : 0"
                      [class]="cellClass() + (column.wrap ? '' : ' whitespace-nowrap') +
                               (column.numeric ? ' text-right tabular-nums' : '')">
                      @if (column.link; as target) {
                        <a [routerLink]="target(row)" class="text-focus underline">
                          {{ column.value(row) }}
                        </a>
                      } @else {
                        {{ column.value(row) }}
                      }
                    </td>
                  }
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class UiDataTable<T> {
  protected readonly skeletonRows = [0, 1, 2, 3, 4];

  /**
   * Mật độ dòng.
   *
   * <p>`comfortable` (mặc định) giữ nguyên nhịp cũ cho trang khách. `admin` cho
   * dòng cao 48px theo quy tắc UX của khu quản trị — nơi người dùng quét mắt
   * theo cột dọc hàng ngày và cần khoảng thở giữa các dòng.
   *
   * <p>Thêm một input thay vì sửa thẳng `py-2`: đổi component dùng chung sẽ đổi
   * luôn mọi bảng của trang landing, và đó là quyết định của Phase 8 chứ không
   * phải hệ quả phụ của Phase 7.
   */
  readonly density = input<'comfortable' | 'admin'>('comfortable');

  readonly caption = input.required<string>();
  readonly columns = input.required<readonly TableColumn<T>[]>();
  readonly rows = input<readonly T[]>([]);
  readonly sort = input<SortState | null>(null);
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly emptyTitle = input('Chưa có dữ liệu');
  readonly emptyDescription = input('Khi có bản ghi đầu tiên, nó sẽ xuất hiện ở đây.');
  readonly emptyActionLabel = input<string | null>(null);

  readonly sortChange = output<SortState>();
  readonly emptyAction = output<void>();

  protected readonly cellClass = computed(() =>
    this.density() === 'admin' ? 'px-4 py-2.5 leading-6' : 'px-3 py-2',
  );

  protected ariaSort(column: TableColumn<T>): 'ascending' | 'descending' | 'none' | null {
    if (!column.sortable) {
      return null;
    }
    const current = this.sort();
    if (current?.key !== column.key) {
      return 'none';
    }
    return current.direction === 'asc' ? 'ascending' : 'descending';
  }

  protected sortArrow(column: TableColumn<T>): string {
    const current = this.sort();
    if (current?.key !== column.key) {
      return '↕';
    }
    return current.direction === 'asc' ? '↑' : '↓';
  }

  protected toggleSort(column: TableColumn<T>): void {
    const current = this.sort();
    const direction = current?.key === column.key && current.direction === 'asc' ? 'desc' : 'asc';
    this.sortChange.emit({ key: column.key, direction });
  }
}
