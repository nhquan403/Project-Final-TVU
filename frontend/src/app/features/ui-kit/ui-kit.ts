import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import {
  UiButton,
  UiDataTable,
  UiDateRangePicker,
  UiEmptyState,
  UiGuestStepper,
  UiInput,
  UiLightbox,
  UiModal,
  UiPagination,
  UiRoomCard,
  UiSelect,
  UiSkeleton,
  UiStarRating,
  UiStatusBadge,
  UiToast,
  type BookingStatus,
  type TableColumn,
  type ToastKind,
} from '../../shared/ui';
import { VndCurrencyPipe } from '../../shared/pipes/vnd-currency.pipe';
import { NightCountPipe } from '../../shared/pipes/night-count.pipe';
import { UiKitContrastTable } from './contrast-table';
import { UiKitSection } from './ui-kit-section';
import { UiKitCase } from './ui-kit-case';
import {
  SAMPLE_BOOKINGS,
  SAMPLE_IMAGES,
  SAMPLE_OPTIONS,
  SAMPLE_ROOM,
  SOLD_OUT_ROOM,
  buildSampleAvailability,
  type DemoBooking,
} from './ui-kit.data';

const ALL_STATUSES: readonly BookingStatus[] = [
  'PENDING_PAYMENT',
  'CONFIRMED',
  'AWAITING_REVIEW',
  'CHECKED_IN',
  'CHECKED_OUT',
  'CANCELLED',
  'EXPIRED',
  'NO_SHOW',
];

const TOAST_KINDS: readonly ToastKind[] = ['success', 'error', 'warning', 'info'];

/**
 * Trang trình bày hệ thống thiết kế. Chỉ tồn tại ở bản `demo`
 * (`environment.uiKitEnabled`), không có trong bản production.
 *
 * Ba công dụng: sửa token thấy ngay ảnh hưởng lên tất cả; đi Tab một lượt là
 * kiểm được toàn bộ `focus-visible`; mở một trang là trình bày được cả hệ
 * thống thiết kế ở buổi bảo vệ.
 */
@Component({
  selector: 'app-ui-kit',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiButton,
    UiInput,
    UiSelect,
    UiDateRangePicker,
    UiGuestStepper,
    UiRoomCard,
    UiStatusBadge,
    UiModal,
    UiToast,
    UiSkeleton,
    UiPagination,
    UiDataTable,
    UiStarRating,
    UiLightbox,
    UiEmptyState,
    VndCurrencyPipe,
    NightCountPipe,
    UiKitContrastTable,
    UiKitSection,
    UiKitCase,
  ],
  templateUrl: './ui-kit.html',
  styles: `
    :host { display: block; }
  `,
})
export class UiKitPage {
  protected readonly sections = [
    ['mau', 'Bảng màu'],
    ['button', 'ui-button'],
    ['input', 'ui-input'],
    ['select', 'ui-select'],
    ['date-range-picker', 'ui-date-range-picker'],
    ['guest-stepper', 'ui-guest-stepper'],
    ['room-card', 'ui-room-card'],
    ['status-badge', 'ui-status-badge'],
    ['modal', 'ui-modal'],
    ['toast', 'ui-toast'],
    ['skeleton', 'ui-skeleton'],
    ['pagination', 'ui-pagination'],
    ['data-table', 'ui-data-table'],
    ['star-rating', 'ui-star-rating'],
    ['lightbox', 'ui-lightbox'],
    ['empty-state', 'ui-empty-state'],
    ['pipe', 'Pipe'],
  ] as const;

  protected readonly statuses = ALL_STATUSES;
  protected readonly toastKinds = TOAST_KINDS;
  protected readonly options = SAMPLE_OPTIONS;
  protected readonly room = SAMPLE_ROOM;
  protected readonly soldOutRoom = SOLD_OUT_ROOM;
  protected readonly images = SAMPLE_IMAGES;
  protected readonly bookings = SAMPLE_BOOKINGS;
  protected readonly availability = buildSampleAvailability();

  protected readonly bookingColumns: readonly TableColumn<DemoBooking>[] = [
    { key: 'code', header: 'Mã đặt phòng', value: (row) => row.code, sortable: true },
    { key: 'guest', header: 'Khách', value: (row) => row.guest, sortable: true },
    { key: 'nights', header: 'Số đêm', value: (row) => row.nights, numeric: true },
    {
      key: 'total',
      header: 'Tổng tiền',
      value: (row) => row.total.toLocaleString('vi-VN') + ' ₫',
      numeric: true,
      sortable: true,
    },
  ];

  // --- Trạng thái tương tác của trang trình bày ---------------------------
  protected readonly modalOpen = signal(false);
  protected readonly modalLoadingOpen = signal(false);
  protected readonly modalErrorOpen = signal(false);
  protected readonly lightboxOpen = signal(false);
  protected readonly lightboxLoadingOpen = signal(false);
  protected readonly lightboxBrokenOpen = signal(false);
  protected readonly brokenImages = [{ url: '/khong-ton-tai.png', alt: 'Ảnh hỏng' }];
  protected readonly page = signal(4);
  protected readonly sort = signal({ key: 'code', direction: 'asc' as const });
  protected readonly rating = signal(4);
  protected readonly ratingEmpty = signal(0);
  protected readonly guests = signal(2);
  protected readonly guestsOverCapacity = signal(5);
  protected readonly toastsVisible = signal(true);

  // Ví dụ cho pipe.
  protected readonly checkIn = '2026-03-10';
  protected readonly checkOut = '2026-03-12';
  protected readonly samplePrice = 1_700_000;
}
