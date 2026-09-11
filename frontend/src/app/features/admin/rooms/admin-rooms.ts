import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminCatalogService } from '../../../core/services/admin-catalog.service';
import type {
  AffectedBooking,
  RoomTypeView,
  RoomView,
} from '../../../core/services/admin.types';
import {
  UiButton,
  UiDataTable,
  UiFilterChips,
  UiInput,
  UiSelect,
  type FilterChip,
  type SelectOption,
  type TableColumn,
} from '../../../shared/ui';

const ROOM_STATUS: Record<string, string> = {
  AVAILABLE: 'Đang khai thác',
  MAINTENANCE: 'Bảo trì',
  OUT_OF_SERVICE: 'Ngừng khai thác',
};

/**
 * Quản lý phòng vật lý.
 *
 * Đổi trạng thái phòng KHÔNG tự huỷ đơn nào. Backend trả về danh sách đơn bị
 * ảnh hưởng và màn hình hiện chúng ra: đổi phòng cho khách hay hoãn bảo trì là
 * quyết định kinh doanh, không phải hệ quả tự động của một cú bấm.
 */
@Component({
  selector: 'app-admin-rooms',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButton, UiSelect, UiInput, UiDataTable, UiFilterChips],
  template: `
    <h1 class="mb-4 text-xl font-bold text-text">Phòng</h1>

    @if (error(); as message) {
      <p class="mb-3 rounded-md bg-danger/10 p-3 text-sm text-danger" role="alert">{{ message }}</p>
    }

    <div class="mb-3 grid gap-3 sm:grid-cols-3">
      <ui-select label="Lọc theo loại phòng" [options]="roomTypeOptions()" placeholder="Tất cả"
        [(value)]="roomTypeFilter" />
      <div class="flex items-end">
        <ui-button (pressed)="load()">Áp dụng</ui-button>
      </div>
    </div>
    <div class="mb-3">
      <ui-filter-chips [chips]="chips()" (removed)="clearFilter()" (cleared)="clearFilter()" />
    </div>

    <ui-data-table
      caption="Danh sách phòng vật lý"
      density="admin"
      [columns]="columns"
      [rows]="rooms()"
      [loading]="loading()"
      [emptyTitle]="roomTypeFilter() ? 'Loại phòng này chưa có phòng nào' : 'Chưa có phòng nào'"
      emptyDescription="Thêm phòng vật lý để hệ thống có thứ để gán khi khách đặt."
      [emptyActionLabel]="roomTypeFilter() ? 'Xoá bộ lọc' : null"
      (emptyAction)="clearFilter()" />

    <section class="mt-5 rounded-md border border-border bg-surface p-4">
      <h2 class="mb-3 text-sm font-semibold text-text">Thêm phòng</h2>
      <div class="grid gap-3 sm:grid-cols-4">
        <ui-select label="Loại phòng" [options]="roomTypeOptions()" [(value)]="newRoomType" />
        <ui-input label="Số phòng" [(value)]="newRoomNumber" />
        <ui-input label="Tầng" type="number" [(value)]="newFloor" />
        <div class="flex items-end">
          <ui-button (pressed)="create()">Thêm</ui-button>
        </div>
      </div>
    </section>

    <section class="mt-5 rounded-md border border-border bg-surface p-4">
      <h2 class="mb-3 text-sm font-semibold text-text">Đổi trạng thái vận hành</h2>
      <div class="grid gap-3 sm:grid-cols-3">
        <ui-select label="Phòng" [options]="roomOptions()" [(value)]="targetRoom" />
        <ui-select label="Trạng thái mới" [options]="statusOptions" [(value)]="targetStatus" />
        <div class="flex items-end">
          <ui-button (pressed)="changeStatus()">Áp dụng</ui-button>
        </div>
      </div>

      @if (affected().length) {
        <div class="mt-3 rounded-sm bg-warning/10 p-3">
          <p class="text-sm font-semibold text-warning">
            {{ affected().length }} đơn đang giữ phòng này trong tương lai — hệ thống KHÔNG huỷ đơn nào.
          </p>
          <ul class="mt-1 space-y-1 text-sm text-text">
            @for (booking of affected(); track booking.code) {
              <li>
                {{ booking.code }} · {{ booking.guestName }} · {{ booking.checkIn }} →
                {{ booking.checkOut }} · {{ booking.status }}
              </li>
            }
          </ul>
          <p class="mt-1 text-xs text-text-muted">
            Hãy đổi phòng cho những khách này, hoặc hoãn việc đưa phòng ra khỏi vận hành.
          </p>
        </div>
      } @else if (statusChanged()) {
        <p class="mt-3 rounded-sm bg-success/10 p-3 text-sm text-success">
          Đã đổi trạng thái. Không đơn nào trong tương lai bị ảnh hưởng.
        </p>
      }
    </section>
  `,
})
export class AdminRooms {
  private readonly catalog = inject(AdminCatalogService);

  protected readonly rooms = signal<RoomView[]>([]);
  protected readonly roomTypes = signal<RoomTypeView[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly affected = signal<AffectedBooking[]>([]);
  protected readonly statusChanged = signal(false);

  protected readonly roomTypeFilter = signal('');
  protected readonly newRoomType = signal('');
  protected readonly newRoomNumber = signal('');
  protected readonly newFloor = signal('');
  protected readonly targetRoom = signal('');
  protected readonly targetStatus = signal('MAINTENANCE');

  protected readonly statusOptions: SelectOption[] = Object.entries(ROOM_STATUS).map(
    ([value, label]) => ({ value, label }),
  );

  protected readonly roomTypeOptions = computed<SelectOption[]>(() =>
    this.roomTypes().map((type) => ({ value: String(type.id), label: type.name })),
  );

  protected readonly roomOptions = computed<SelectOption[]>(() =>
    this.rooms().map((room) => ({
      value: String(room.id),
      label: `${room.roomNumber} (${ROOM_STATUS[room.status] ?? room.status})`,
    })),
  );

  protected readonly chips = computed<FilterChip[]>(() => {
    const id = this.roomTypeFilter();
    if (!id) {
      return [];
    }
    const type = this.roomTypes().find((item) => String(item.id) === id);
    return [{ key: 'roomType', label: 'Loại phòng: ' + (type?.name ?? id) }];
  });

  protected readonly columns: TableColumn<RoomView>[] = [
    { key: 'number', header: 'Số phòng', value: (row) => row.roomNumber },
    { key: 'type', header: 'Loại phòng', value: (row) => row.roomTypeName },
    { key: 'floor', header: 'Tầng', value: (row) => row.floor ?? '—', numeric: true },
    { key: 'status', header: 'Trạng thái', value: (row) => ROOM_STATUS[row.status] ?? row.status },
    { key: 'note', header: 'Ghi chú', value: (row) => row.note ?? '' },
  ];

  constructor() {
    this.catalog.roomTypes().subscribe({
      next: (list) => this.roomTypes.set(list),
      error: () => this.roomTypes.set([]),
    });
    this.load();
  }

  protected clearFilter(): void {
    this.roomTypeFilter.set('');
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.catalog.rooms(this.roomTypeFilter() ? Number(this.roomTypeFilter()) : null).subscribe({
      next: (list) => {
        this.rooms.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách phòng.');
        this.loading.set(false);
      },
    });
  }

  protected create(): void {
    if (!this.newRoomType() || !this.newRoomNumber()) {
      this.error.set('Chọn loại phòng và nhập số phòng trước khi thêm.');
      return;
    }
    this.catalog
      .createRoom({
        roomTypeId: Number(this.newRoomType()),
        roomNumber: this.newRoomNumber(),
        floor: this.newFloor() ? Number(this.newFloor()) : null,
        note: null,
      })
      .subscribe({
        next: () => {
          this.newRoomNumber.set('');
          this.newFloor.set('');
          this.error.set(null);
          this.load();
        },
        error: (response) => this.error.set(response?.error?.detail ?? 'Không thêm được phòng.'),
      });
  }

  protected changeStatus(): void {
    if (!this.targetRoom()) {
      this.error.set('Chọn phòng cần đổi trạng thái.');
      return;
    }
    this.statusChanged.set(false);
    this.catalog.changeRoomStatus(Number(this.targetRoom()), this.targetStatus()).subscribe({
      next: (result) => {
        this.affected.set(result.affectedBookings);
        this.statusChanged.set(true);
        this.error.set(null);
        this.load();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? 'Không đổi được trạng thái phòng.'),
    });
  }
}
