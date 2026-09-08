import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';
import { EscCloseDirective } from '../../a11y/esc-close.directive';

/** Thông tin một đêm, lấy từ lịch giá của backend. */
export interface DayInfo {
  /** Giá của đêm đó. Bỏ trống thì ô ngày chỉ hiện số. */
  price?: number;
  /** Số phòng còn trống. `0` = hết phòng, ô ngày bị chặn sẵn. */
  availableCount: number;
}

/** Khoá là ngày dạng `YYYY-MM-DD` theo giờ địa phương. */
export type AvailabilityMap = Readonly<Record<string, DayInfo>>;

export interface DateRange {
  checkIn: string | null;
  checkOut: string | null;
}

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'] as const;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** `YYYY-MM-DD` theo giờ địa phương — không dùng toISOString(), nó đổi sang UTC. */
function toKey(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function fromKey(key: string): Date {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(year!, month! - 1, day!);
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

/** Số đêm giữa hai ngày, khoảng nửa mở `[nhận, trả)`. */
function nightsBetween(from: string, to: string): number {
  return Math.round(
    (Date.UTC(fromKey(to).getFullYear(), fromKey(to).getMonth(), fromKey(to).getDate()) -
      Date.UTC(fromKey(from).getFullYear(), fromKey(from).getMonth(), fromKey(from).getDate())) /
      MS_PER_DAY,
  );
}

/**
 * Giá rút gọn cho ô ngày: `850K`, `1,05Tr`.
 *
 * Ô ngày rộng 44px — vừa đúng ngưỡng vùng chạm tối thiểu, không nới thêm được
 * mà không phá lưới 7 cột trên mobile. Số tiền đầy đủ (`1.050.000 ₫`) không thể
 * vừa, và để nó tràn thì giá của hai ngày cạnh nhau đè lên nhau, đọc thành số
 * khác hẳn. Số đầy đủ vẫn nằm trong `aria-label` của ô.
 */
function compactPrice(price: number | undefined): string | null {
  if (price === undefined) {
    return null;
  }
  if (price < 1_000_000) {
    return `${Math.round(price / 1000)}K`;
  }
  const millions = (price / 1_000_000).toFixed(2).replace(/\.?0+$/, '');
  return `${millions.replace('.', ',')}Tr`;
}

interface DayCell {
  key: string;
  label: number;
  price: number | null;
  /** Giá rút gọn để vừa ô 44px: `850K`, `1,05Tr`. */
  priceLabel: string | null;
  disabled: boolean;
  isToday: boolean;
  isStart: boolean;
  isEnd: boolean;
  inRange: boolean;
}

/**
 * Chọn khoảng ngày nhận – trả phòng.
 *
 * **Chặn sẵn, không báo lỗi sau.** Ngày hết phòng và ngày quá khứ bị vô hiệu
 * ngay trên lịch. Cho chọn rồi mới báo "ngày này không đặt được" là một trong
 * những trải nghiệm khó chịu nhất trên các trang đặt phòng: người dùng đã đầu
 * tư công sức vào lựa chọn trước khi bị từ chối.
 *
 * **Khoảng nửa mở `[nhận, trả)`**, đúng quy ước của ràng buộc `EXCLUDE` trên
 * `daterange` ở tầng cơ sở dữ liệu. Hệ quả nhìn thấy được: ngày trả phòng
 * KHÔNG cần còn phòng — sáng hôm đó khách đã đi. Nên một ngày hết phòng vẫn
 * chọn được làm ngày trả, chỉ không chọn được làm đêm ở.
 *
 * Sau khi chọn ngày nhận, mọi ngày nằm sau đêm hết phòng gần nhất đều bị chặn:
 * một khoảng có lỗ hổng ở giữa thì không đặt được, chặn trước vẫn hơn.
 *
 * Ngày không có trong bản đồ lịch giá thì KHÔNG bị chặn — bản đồ chỉ phủ vài
 * tháng tới, chặn hết phần còn lại sẽ khoá cả những ngày còn trống. Ràng buộc
 * `EXCLUDE` ở cơ sở dữ liệu vẫn là chốt chặn cuối.
 */
@Component({
  selector: 'ui-date-range-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EscCloseDirective],
  template: `
    <div class="relative">
      <button
        type="button"
        [disabled]="disabled()"
        [attr.aria-expanded]="opened()"
        aria-haspopup="dialog"
        [class]="triggerClasses()"
        (click)="toggle()">
        <span class="flex flex-col items-start">
          <span class="text-xs font-medium uppercase tracking-wide text-text-muted">
            {{ label() }}
          </span>
          <span class="text-body">{{ summary() }}</span>
        </span>
        <span aria-hidden="true" class="text-text-muted">▾</span>
      </button>

      @if (error(); as message) {
        <p class="mt-1 text-xs font-semibold text-danger" aria-live="polite">{{ message }}</p>
      }

      @if (opened()) {
        <div
          role="dialog"
          [attr.aria-label]="label()"
          uiEscClose
          (escape)="close()"
          class="absolute left-0 z-40 mt-2 w-[min(92vw,44rem)] rounded-lg border border-border
                 bg-surface p-3 shadow-3">
          @if (loading()) {
            <p class="p-6 text-center text-sm text-text-muted" aria-live="polite">
              Đang tải lịch giá…
            </p>
          } @else {
            <div class="mb-2 flex items-center justify-between">
              <button
                type="button"
                [class]="navClasses"
                aria-label="Tháng trước"
                (click)="shiftMonth(-1)">‹</button>
              <p aria-live="polite" class="text-body font-semibold">{{ monthTitle() }}</p>
              <button
                type="button"
                [class]="navClasses"
                aria-label="Tháng sau"
                (click)="shiftMonth(1)">›</button>
            </div>

            <!-- Mobile một tháng, desktop hai tháng cạnh nhau. -->
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              @for (month of months(); track month.key) {
                <div [class]="$index === 1 ? 'hidden md:block' : ''">
                  <p class="mb-1 text-center text-sm font-semibold md:text-left">
                    {{ month.title }}
                  </p>
                  <div class="grid grid-cols-7" role="grid" [attr.aria-label]="month.title">
                    @for (weekday of weekdays; track weekday) {
                      <span class="py-1 text-center text-xs text-text-muted" aria-hidden="true">
                        {{ weekday }}
                      </span>
                    }
                    @for (cell of month.cells; track $index) {
                      @if (cell === null) {
                        <span></span>
                      } @else {
                        <button
                          type="button"
                          role="gridcell"
                          [attr.data-date]="cell.key"
                          [disabled]="cell.disabled"
                          [tabindex]="cell.key === focusedKey() ? 0 : -1"
                          [attr.aria-selected]="cell.isStart || cell.isEnd"
                          [attr.aria-current]="cell.isToday ? 'date' : null"
                          [attr.aria-label]="ariaLabel(cell)"
                          [class]="cellClasses(cell)"
                          (click)="pick(cell)"
                          (keydown)="onKeydown($event)">
                          <span class="text-sm leading-none">{{ cell.label }}</span>
                          @if (cell.priceLabel; as price) {
                            <span class="text-[0.625rem] leading-none opacity-80">{{ price }}</span>
                          }
                        </button>
                      }
                    }
                  </div>
                </div>
              }
            </div>

            <div class="mt-3 flex items-center justify-between border-t border-border pt-3">
              <p class="text-sm text-text-muted" aria-live="polite">{{ nightsLabel() }}</p>
              <button
                type="button"
                class="min-h-[var(--touch-min)] rounded-md px-4 text-sm font-semibold text-primary
                       transition-colors duration-[var(--dur-fast)] hover:bg-surface-2"
                (click)="clear()">Xoá</button>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class UiDateRangePicker {
  private readonly host = inject(ElementRef<HTMLElement>);

  protected readonly weekdays = WEEKDAYS;
  protected readonly navClasses =
    'flex h-[var(--touch-min)] w-[var(--touch-min)] items-center justify-center rounded-md ' +
    'text-text transition-colors duration-[var(--dur-fast)] hover:bg-surface-2';

  readonly label = input('Ngày nhận – trả phòng');
  readonly availability = input<AvailabilityMap>({});
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly value = model<DateRange>({ checkIn: null, checkOut: null });
  readonly rangeSelected = output<DateRange>();

  protected readonly opened = signal(false);
  /** Ngày đang mang vòng focus của lưới — điều hướng bằng mũi tên đổi giá trị này. */
  protected readonly focusedKey = signal(toKey(new Date()));
  private readonly viewMonth = signal(startOfMonth(new Date()));
  private readonly todayKey = toKey(new Date());

  constructor() {
    // Sau mỗi lần focusedKey đổi, đưa vòng focus thật sang ô ngày tương ứng.
    effect(() => {
      const key = this.focusedKey();
      if (!this.opened()) {
        return;
      }
      queueMicrotask(() => {
        const element = (this.host.nativeElement as HTMLElement).querySelector<HTMLElement>(
          `[data-date="${key}"]`,
        );
        element?.focus();
      });
    });
  }

  protected readonly summary = computed(() => {
    const { checkIn, checkOut } = this.value();
    if (!checkIn) {
      return 'Chọn ngày';
    }
    if (!checkOut) {
      return `${this.format(checkIn)} → chọn ngày trả`;
    }
    return `${this.format(checkIn)} → ${this.format(checkOut)}`;
  });

  protected readonly nightsLabel = computed(() => {
    const { checkIn, checkOut } = this.value();
    if (!checkIn || !checkOut) {
      return 'Chọn ngày nhận và ngày trả phòng';
    }
    return `${nightsBetween(checkIn, checkOut)} đêm`;
  });

  protected readonly monthTitle = computed(() => this.titleOf(this.viewMonth()));

  protected readonly months = computed(() => {
    const first = this.viewMonth();
    const second = new Date(first.getFullYear(), first.getMonth() + 1, 1);
    return [first, second].map((month) => ({
      key: toKey(month),
      title: this.titleOf(month),
      cells: this.buildCells(month),
    }));
  });

  /**
   * Đêm cuối cùng còn chọn được sau khi đã chọn ngày nhận: đêm hết phòng gần
   * nhất chặn mọi ngày phía sau nó.
   */
  private readonly maxCheckOut = computed<string | null>(() => {
    const { checkIn, checkOut } = this.value();
    if (!checkIn || checkOut) {
      return null;
    }
    const availability = this.availability();
    let cursor = fromKey(checkIn);
    // Quét tối đa một năm; xa hơn thế thì không cần chặn nữa.
    for (let i = 0; i < 366; i++) {
      const info = availability[toKey(cursor)];
      if (info && info.availableCount <= 0) {
        // Đêm này hết phòng ⇒ ngày trả muộn nhất chính là ngày đó.
        return toKey(cursor);
      }
      cursor = addDays(cursor, 1);
    }
    return null;
  });

  private buildCells(month: Date): (DayCell | null)[] {
    const availability = this.availability();
    const { checkIn, checkOut } = this.value();
    const maxCheckOut = this.maxCheckOut();
    const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    // getDay(): 0 = Chủ nhật. Lưới bắt đầu từ Thứ hai theo thói quen Việt Nam.
    const leading = (month.getDay() + 6) % 7;

    const cells: (DayCell | null)[] = Array.from({ length: leading }, () => null);

    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(month.getFullYear(), month.getMonth(), day);
      const key = toKey(date);
      const info = availability[key];
      const isPast = key < this.todayKey;
      const soldOut = info !== undefined && info.availableCount <= 0;

      let disabled: boolean;
      if (checkIn && !checkOut) {
        // Đang chọn ngày trả. Ngày trả không cần còn phòng (sáng đó khách đã
        // đi), nên chỉ chặn theo thứ tự và theo đêm hết phòng gần nhất.
        disabled = isPast || key <= checkIn || (maxCheckOut !== null && key > maxCheckOut);
      } else {
        disabled = isPast || soldOut;
      }

      cells.push({
        key,
        label: day,
        price: info?.price ?? null,
        priceLabel: compactPrice(info?.price),
        disabled,
        isToday: key === this.todayKey,
        isStart: key === checkIn,
        isEnd: key === checkOut,
        inRange: !!checkIn && !!checkOut && key > checkIn && key < checkOut,
      });
    }
    return cells;
  }

  protected cellClasses(cell: DayCell): string {
    // Ô ngày ≥ 44px kể cả trên mobile: 44px là ngưỡng vùng chạm tối thiểu.
    const base =
      'flex h-11 w-full min-w-11 flex-col items-center justify-center gap-0.5 ' +
      'overflow-hidden rounded-md ' +
      'transition-colors duration-[var(--dur-fast)] ' +
      'disabled:cursor-not-allowed disabled:text-text-muted disabled:opacity-40 ' +
      'disabled:line-through';
    if (cell.isStart || cell.isEnd) {
      return `${base} bg-primary font-bold text-text-invert`;
    }
    if (cell.inRange) {
      return `${base} bg-surface-2 text-text`;
    }
    if (cell.isToday) {
      // Hôm nay có viền để làm mốc tham chiếu, không tô nền để khỏi nhầm với
      // ngày đã chọn.
      return `${base} border border-border-strong font-semibold text-text hover:bg-surface-2`;
    }
    return `${base} text-text hover:bg-surface-2`;
  }

  protected ariaLabel(cell: DayCell): string {
    const parts = [this.format(cell.key)];
    if (cell.disabled) {
      parts.push('không chọn được');
    }
    if (cell.price !== null) {
      parts.push(`${cell.price.toLocaleString('vi-VN')} đồng`);
    }
    return parts.join(', ');
  }

  protected toggle(): void {
    const next = !this.opened();
    this.opened.set(next);
    if (next) {
      const anchor = this.value().checkIn ?? this.todayKey;
      this.focusedKey.set(anchor);
      this.viewMonth.set(startOfMonth(fromKey(anchor)));
    }
  }

  protected close(): void {
    this.opened.set(false);
  }

  protected clear(): void {
    this.value.set({ checkIn: null, checkOut: null });
  }

  protected shiftMonth(delta: number): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() + delta, 1));
  }

  protected pick(cell: DayCell): void {
    if (cell.disabled) {
      return;
    }
    const { checkIn, checkOut } = this.value();
    if (!checkIn || checkOut) {
      // Bắt đầu khoảng mới.
      this.value.set({ checkIn: cell.key, checkOut: null });
      return;
    }
    const range = { checkIn, checkOut: cell.key };
    this.value.set(range);
    this.rangeSelected.emit(range);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const current = fromKey(this.focusedKey());
    // Mọi nhánh không gán `next` đều thoát sớm, nên khai báo không cần giá trị đầu.
    let next: Date;

    switch (event.key) {
      case 'ArrowLeft':
        next = addDays(current, -1);
        break;
      case 'ArrowRight':
        next = addDays(current, 1);
        break;
      case 'ArrowUp':
        next = addDays(current, -7);
        break;
      case 'ArrowDown':
        next = addDays(current, 7);
        break;
      case 'PageUp':
        next = new Date(current.getFullYear(), current.getMonth() - 1, current.getDate());
        break;
      case 'PageDown':
        next = new Date(current.getFullYear(), current.getMonth() + 1, current.getDate());
        break;
      case 'Home':
        next = addDays(current, -((current.getDay() + 6) % 7));
        break;
      case 'End':
        next = addDays(current, 6 - ((current.getDay() + 6) % 7));
        break;
      case 'Enter':
      case ' ': {
        // Ô đang focus có thể nằm ngoài tháng đang hiển thị sau PageUp/PageDown,
        // nên tìm lại cell từ lưới hiện tại.
        const cell = this.months()
          .flatMap((month) => month.cells)
          .find((candidate) => candidate?.key === this.focusedKey());
        if (cell) {
          event.preventDefault();
          this.pick(cell);
        }
        return;
      }
      default:
        return;
    }

    event.preventDefault();
    this.focusedKey.set(toKey(next));
    // Kéo tháng hiển thị theo vòng focus, nếu không người dùng focus vào ô
    // không nhìn thấy.
    const monthStart = startOfMonth(next);
    const shown = this.months().map((month) => month.key);
    if (!shown.includes(toKey(monthStart))) {
      this.viewMonth.set(monthStart);
    }
  }

  private format(key: string): string {
    return fromKey(key).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
  }

  private titleOf(month: Date): string {
    return `Tháng ${month.getMonth() + 1}/${month.getFullYear()}`;
  }

  protected readonly triggerClasses = computed(() => {
    const base =
      'flex w-full items-center justify-between gap-3 rounded-md bg-surface px-3 ' +
      'min-h-[var(--touch-min)] text-left transition-colors duration-[var(--dur-fast)] ' +
      'disabled:cursor-not-allowed disabled:opacity-50';
    return this.error()
      ? `${base} border-2 border-danger`
      : `${base} border border-border-strong hover:border-text-muted`;
  });
}
