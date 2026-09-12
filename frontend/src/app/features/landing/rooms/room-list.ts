import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AvailabilityService,
  type RoomTypeAvailability,
} from '../../../core/services/availability.service';
import { RoomTypeService, type PublicRoomType } from '../../../core/services/room-type.service';
import { UiEmptyState, UiRoomCard, UiSkeleton } from '../../../shared/ui';
import { RoomCatalogCard } from '../shared/room-catalog-card';
import { toRoomCard } from '../shared/room-card.mapper';

/**
 * Danh sách loại phòng.
 *
 * <h2>Hai chế độ hiển thị, và vì sao</h2>
 *
 * Không có ngày trên URL, trang hiện thẻ DANH MỤC: ảnh, tên, "từ X mỗi đêm",
 * dẫn sang trang chi tiết. Có ngày, trang hỏi API phòng trống và đổi sang
 * `ui-room-card` — lúc này mới có tổng cả kỳ và số phòng còn lại thật.
 *
 * Trộn hai chế độ làm một, bằng cách cho thẻ danh mục một `availableCount` bịa,
 * là cách nhanh nhất để nói dối khách: `ui-room-card` khoá nút khi con số đó
 * bằng 0, và mở nút khi nó lớn — cả hai đều sai khi chưa ai hỏi cơ sở dữ liệu.
 *
 * <h2>Ngày nằm trên URL</h2>
 *
 * Ngày là tham số tìm kiếm, không phải thông tin cá nhân, nên nó ĐƯỢC đưa lên
 * URL: đó là thứ khiến "gửi link phòng ngày này cho bạn" hoạt động, và khiến F5
 * không mất kết quả. Họ tên và số điện thoại thì không bao giờ.
 */
@Component({
  selector: 'app-room-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RoomCatalogCard, UiRoomCard, UiSkeleton, UiEmptyState],
  template: `
    <div class="mx-auto max-w-6xl px-4 py-8">
      <h1 class="text-h1 font-bold text-text">Các loại phòng</h1>

      @if (hasDates()) {
        <p class="mt-2 text-text-muted" aria-live="polite">
          Phòng còn trống từ {{ displayDate(checkIn()) }} đến {{ displayDate(checkOut()) }} ·
          {{ nights() }} đêm
          <a
            routerLink="/phong"
            class="ml-2 inline-flex min-h-[var(--touch-min)] items-center text-sm font-semibold
                   text-primary hover:underline">
            Bỏ lọc ngày
          </a>
        </p>
      } @else {
        <p class="mt-2 text-text-muted">
          Chọn một loại phòng để xem chi tiết và kiểm tra ngày còn trống.
        </p>
      }

      @if (loading()) {
        <ul class="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          @for (placeholder of [1, 2, 3]; track placeholder) {
            <li class="overflow-hidden rounded-lg border border-border bg-surface">
              <ui-skeleton shape="card" />
              <div class="flex flex-col gap-2 p-4">
                <ui-skeleton shape="line" />
                <ui-skeleton shape="line" />
              </div>
            </li>
          }
        </ul>
      } @else if (offline()) {
        <div class="mt-6">
          <ui-empty-state
            title="Bạn đang ngoại tuyến"
            description="Trang cần kết nối mạng để lấy giá và tình trạng phòng theo thời gian thực."
            actionLabel="Thử lại"
            (action)="load()" />
        </div>
      } @else if (error()) {
        <div class="mt-6">
          <ui-empty-state
            title="Không tải được danh sách phòng"
            description="Máy chủ chưa trả lời. Thử lại sau giây lát hoặc gọi 0294 3855 246."
            actionLabel="Thử lại"
            (action)="load()" />
        </div>
      } @else if (hasDates() && available().length === 0) {
        <div class="mt-6">
          <ui-empty-state
            title="Không còn phòng trống cho khoảng ngày này"
            description="Thử đổi ngày hoặc giảm số khách mỗi phòng. Homestay có thể còn phòng vào những ngày liền kề."
            actionLabel="Chọn ngày khác"
            (action)="clearDates()" />
        </div>
      } @else if (!hasDates() && rooms().length === 0) {
        <div class="mt-6">
          <ui-empty-state
            title="Chưa có loại phòng nào được mở bán"
            description="Quản trị viên chưa bật loại phòng nào. Liên hệ homestay để được hỗ trợ trực tiếp." />
        </div>
      } @else if (hasDates()) {
        <ul class="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          @for (room of available(); track room.roomTypeId) {
            <li>
              <ui-room-card
                [room]="toCard(room)"
                (selected)="chooseRoom(room.roomTypeId)" />
            </li>
          }
        </ul>
      } @else {
        <ul class="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          @for (room of rooms(); track room.id) {
            <li><app-room-catalog-card [room]="room" /></li>
          }
        </ul>
      }
    </div>
  `,
})
export class RoomListPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomTypeApi = inject(RoomTypeService);
  private readonly availabilityApi = inject(AvailabilityService);

  protected readonly rooms = signal<PublicRoomType[]>([]);
  protected readonly available = signal<RoomTypeAvailability[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly offline = signal(false);

  protected readonly checkIn = signal('');
  protected readonly checkOut = signal('');
  private readonly adults = signal(2);
  private readonly children = signal(0);

  protected readonly hasDates = computed(() => this.checkIn() !== '' && this.checkOut() !== '');

  protected readonly nights = computed(() => {
    if (!this.hasDates()) {
      return 0;
    }
    const ms = new Date(this.checkOut()).getTime() - new Date(this.checkIn()).getTime();
    return Math.max(0, Math.round(ms / 86_400_000));
  });

  constructor() {
    const query = this.route.snapshot.queryParamMap;
    this.checkIn.set(query.get('checkIn') ?? '');
    this.checkOut.set(query.get('checkOut') ?? '');
    this.adults.set(Number(query.get('adults')) || 2);
    this.children.set(Number(query.get('children')) || 0);
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.offline.set(false);

    if (this.hasDates()) {
      this.availabilityApi
        .search({
          checkIn: this.checkIn(),
          checkOut: this.checkOut(),
          adults: this.adults(),
          children: this.children(),
        })
        .subscribe({
          next: (response) => {
            this.available.set(response.roomTypes);
            this.loading.set(false);
          },
          error: () => this.fail(),
        });
      return;
    }

    this.roomTypeApi.list().subscribe({
      next: (data) => {
        this.rooms.set(data);
        this.loading.set(false);
      },
      error: () => this.fail(),
    });
  }

  protected toCard(room: RoomTypeAvailability) {
    return toRoomCard(room, this.nights());
  }

  protected chooseRoom(roomTypeId: number): void {
    void this.router.navigate(['/dat-phong'], {
      queryParams: {
        checkIn: this.checkIn(),
        checkOut: this.checkOut(),
        adults: this.adults(),
        children: this.children() || null,
        roomTypeId,
      },
    });
  }

  protected clearDates(): void {
    void this.router.navigate(['/phong']);
  }

  protected displayDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }

  /**
   * Phân biệt "máy không có mạng" với "máy chủ trả lỗi".
   *
   * Hai tình huống này cần hai câu khác nhau: một câu bảo khách kiểm tra wifi,
   * câu kia bảo khách thử lại sau. Gộp làm một thì nửa số người đọc được lời
   * khuyên vô dụng.
   */
  private fail(): void {
    this.offline.set(typeof navigator !== 'undefined' && navigator.onLine === false);
    this.error.set(!this.offline());
    this.loading.set(false);
  }
}
