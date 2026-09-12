import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AvailabilityService,
  type RoomTypeAvailability,
} from '../../../core/services/availability.service';
import { RoomTypeService, type PublicRoomType } from '../../../core/services/room-type.service';
import { SeoService } from '../../../core/services/seo.service';
import {
  UiButton,
  UiDateRangePicker,
  UiEmptyState,
  UiGuestStepper,
  UiLightbox,
  UiSkeleton,
  VndCurrencyPipe,
  type AvailabilityMap,
  type DateRange,
  type LightboxImage,
} from '../../../shared/ui';
import { ROOM_PLACEHOLDER_IMAGE } from '../shared/room-card.mapper';

/** Backend cho phép hỏi lịch tối đa 120 ngày một lượt. */
const CALENDAR_DAYS = 120;

/**
 * Trang chi tiết một loại phòng.
 *
 * <h2>Ô đặt phòng dính</h2>
 *
 * Trên máy tính, ô đặt phòng `sticky` ở cột phải; trên điện thoại nó là một
 * thanh dính ĐÁY màn hình. Lý do khác nhau: ở màn hình rộng khách vừa đọc mô
 * tả vừa nhìn giá, còn ở màn hình hẹp mọi thứ xếp dọc nên ô đặt phòng bị đẩy
 * xuống tận cuối — khách đọc xong phải cuộn ngược lên mới đặt được.
 *
 * <h2>Tiền luôn từ backend</h2>
 *
 * Tổng cả kỳ lấy từ `/api/availability`, không nhân `basePrice × số đêm` ở đây.
 * Hai công thức song song là hai công thức sẽ lệch nhau ngay lần đầu có khuyến
 * mãi hoặc làm tròn, và bên lệch là bên khách nhìn thấy.
 */
@Component({
  selector: 'app-room-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    UiButton,
    UiDateRangePicker,
    UiGuestStepper,
    UiLightbox,
    UiSkeleton,
    UiEmptyState,
    VndCurrencyPipe,
  ],
  template: `
    @if (loading()) {
      <div class="mx-auto max-w-6xl px-4 py-8">
        <ui-skeleton shape="card" />
        <div class="mt-4 flex flex-col gap-2">
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
          <ui-skeleton shape="line" />
        </div>
      </div>
    } @else if (notFound()) {
      <div class="mx-auto max-w-2xl px-4 py-16">
        <ui-empty-state
          title="Không tìm thấy loại phòng này"
          description="Liên kết có thể đã cũ, hoặc loại phòng đã ngừng bán. Xem các loại phòng đang mở."
          actionLabel="Về danh sách phòng"
          (action)="goToList()" />
      </div>
    } @else if (loadError()) {
      <div class="mx-auto max-w-2xl px-4 py-16">
        <ui-empty-state
          title="Không tải được thông tin phòng"
          description="Máy chủ chưa trả lời. Thử lại sau giây lát hoặc gọi 0294 3855 246."
          actionLabel="Thử lại"
          (action)="load()" />
      </div>
    } @else if (room(); as data) {
      <article class="mx-auto max-w-6xl px-4 py-8 pb-28 lg:pb-8">
        <nav aria-label="Đường dẫn" class="text-sm text-text-muted">
          <a
            routerLink="/phong"
            class="inline-flex min-h-[var(--touch-min)] items-center hover:underline">
            Các loại phòng
          </a>
          <span aria-hidden="true"> / </span>
          <span class="text-text">{{ data.name }}</span>
        </nav>

        <!-- THƯ VIỆN: 1 ảnh lớn + 2 ảnh nhỏ -->
        <div class="mt-4 grid gap-2 sm:grid-cols-3">
          <button
            type="button"
            class="overflow-hidden rounded-lg border border-border sm:col-span-2"
            [attr.aria-label]="'Xem ảnh lớn của ' + data.name"
            (click)="openLightbox(0)">
            <img
              [src]="images()[0]?.url ?? placeholder"
              [alt]="images()[0]?.altText ?? ('Chưa có ảnh cho ' + data.name)"
              width="960"
              height="640"
              fetchpriority="high"
              decoding="async"
              class="aspect-[3/2] w-full object-cover" />
          </button>

          <div class="grid grid-cols-2 gap-2 sm:grid-cols-1">
            @for (image of images().slice(1, 3); track image.url; let i = $index) {
              <button
                type="button"
                class="relative overflow-hidden rounded-lg border border-border"
                [attr.aria-label]="'Xem ảnh ' + (i + 2) + ' của ' + data.name"
                (click)="openLightbox(i + 1)">
                <img
                  [src]="image.url"
                  [alt]="image.altText"
                  width="480"
                  height="320"
                  loading="lazy"
                  decoding="async"
                  class="aspect-[3/2] w-full object-cover" />
                @if (i === 1 && images().length > 3) {
                  <span
                    class="absolute inset-0 flex items-center justify-center bg-text/50 text-sm
                           font-semibold text-text-invert">
                    +{{ images().length - 3 }} ảnh
                  </span>
                }
              </button>
            }
          </div>
        </div>

        <div class="mt-6 grid gap-8 lg:grid-cols-[1fr_360px]">
          <!-- CỘT NỘI DUNG -->
          <div>
            <h1 class="text-h1 font-bold text-text">{{ data.name }}</h1>
            <p class="mt-2 text-text-muted">
              {{ data.capacityAdults }} người lớn
              @if (data.capacityChildren > 0) {
                · tối đa {{ data.capacityChildren }} trẻ em
              }
              @if (data.bedInfo) {
                · {{ data.bedInfo }}
              }
              @if (data.areaSqm) {
                · {{ data.areaSqm }} m²
              }
            </p>

            @if (data.description) {
              <p class="mt-4 whitespace-pre-line text-text-muted">{{ data.description }}</p>
            } @else if (data.shortDescription) {
              <p class="mt-4 text-text-muted">{{ data.shortDescription }}</p>
            }

            @if (roomAmenities().length) {
              <section class="mt-8" aria-labelledby="tien-ich-phong">
                <h2 id="tien-ich-phong" class="text-h2 font-bold text-text">Tiện ích trong phòng</h2>
                <ul class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
                  @for (amenity of roomAmenities(); track amenity.code) {
                    <li class="flex items-center gap-2 text-sm text-text">
                      <span aria-hidden="true">{{ amenity.icon ?? '•' }}</span>
                      {{ amenity.name }}
                    </li>
                  }
                </ul>
              </section>
            }

            @if (propertyAmenities().length) {
              <section class="mt-8" aria-labelledby="tien-ich-chung">
                <h2 id="tien-ich-chung" class="text-h2 font-bold text-text">
                  Tiện ích chung của homestay
                </h2>
                <ul class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
                  @for (amenity of propertyAmenities(); track amenity.code) {
                    <li class="flex items-center gap-2 text-sm text-text">
                      <span aria-hidden="true">{{ amenity.icon ?? '•' }}</span>
                      {{ amenity.name }}
                    </li>
                  }
                </ul>
              </section>
            }

            <!--
              Chính sách huỷ hiện THẲNG, không gấp vào ô bấm-mới-mở. Giấu điều
              kiện huỷ sau một cú bấm là cách các trang đặt phòng bị phàn nàn
              nhiều nhất, và ở đây không có gì phải giấu: khách huỷ được bất cứ
              lúc nào trước khi nhận phòng.
            -->
            <section class="mt-8" aria-labelledby="chinh-sach">
              <h2 id="chinh-sach" class="text-h2 font-bold text-text">Chính sách đặt và huỷ</h2>
              <ul class="mt-3 flex flex-col gap-2 text-sm text-text-muted">
                <li>
                  Sau khi đặt, phòng được giữ <strong class="text-text">15 phút</strong> để bạn
                  chuyển khoản tiền cọc. Quá hạn, đơn tự huỷ và phòng mở lại cho khách khác.
                </li>
                <li>
                  Chỉ cần trả trước tiền cọc; phần còn lại thanh toán tại homestay khi nhận phòng.
                  Số tiền cọc do hệ thống tính và hiện ở bước xác nhận.
                </li>
                <li>
                  Bạn tự huỷ đơn được ở trang
                  <a routerLink="/tra-cuu" class="font-semibold text-primary hover:underline">
                    tra cứu đơn </a
                  >, bất cứ lúc nào trước khi nhận phòng.
                </li>
                <li>
                  Việc hoàn tiền cọc được homestay xử lý thủ công. Gọi 0294 3855 246 để được hướng
                  dẫn.
                </li>
              </ul>
            </section>
          </div>

          <!-- Ô ĐẶT PHÒNG -->
          <aside class="lg:sticky lg:top-24 lg:self-start">
            <div class="rounded-lg border border-border bg-surface p-4 shadow-1">
              <p class="text-h3 font-bold text-price">
                <span class="text-sm font-normal text-text-muted">từ</span>
                {{ data.basePrice | vndCurrency }}
                <span class="text-sm font-normal text-text-muted">/ đêm</span>
              </p>

              <div class="mt-4 flex flex-col gap-3">
                <ui-date-range-picker
                  [availability]="calendar()"
                  [loading]="calendarLoading()"
                  [error]="dateError()"
                  [(value)]="dates"
                  (rangeSelected)="onRangeSelected()" />

                <ui-guest-stepper
                  label="Người lớn"
                  [min]="1"
                  [max]="20"
                  [capacity]="data.capacityAdults"
                  [(value)]="adults" />

                @if (data.capacityChildren > 0) {
                  <ui-guest-stepper
                    label="Trẻ em"
                    [min]="0"
                    [max]="20"
                    [capacity]="data.capacityChildren"
                    [(value)]="children" />
                }
              </div>

              <div aria-live="polite" class="mt-4">
                @if (quoting()) {
                  <ui-skeleton shape="line" />
                } @else if (quote(); as found) {
                  <dl class="flex flex-col gap-1 text-sm">
                    <div class="flex justify-between">
                      <dt class="text-text-muted">
                        {{ found.pricePerNight | vndCurrency }} × {{ found.nights }} đêm
                      </dt>
                      <dd class="text-text">{{ found.totalPrice | vndCurrency }}</dd>
                    </div>
                    <div class="flex justify-between border-t border-border pt-1">
                      <dt class="font-semibold text-text">Tổng cả kỳ</dt>
                      <dd class="text-h3 font-bold text-price">
                        {{ found.totalPrice | vndCurrency }}
                      </dd>
                    </div>
                  </dl>
                  @if (found.availableCount <= 3) {
                    <p class="mt-2 text-sm font-semibold text-warning">
                      Chỉ còn {{ found.availableCount }} phòng cho khoảng ngày này
                    </p>
                  }
                } @else if (soldOut()) {
                  <p class="text-sm font-semibold text-danger">
                    Loại phòng này đã kín cho khoảng ngày bạn chọn. Thử đổi ngày.
                  </p>
                } @else {
                  <p class="text-sm text-text-muted">
                    Chọn ngày để xem tổng tiền cho cả kỳ nghỉ.
                  </p>
                }
              </div>

              <div class="mt-4">
                <ui-button
                  [fullWidth]="true"
                  [disabled]="!quote()"
                  [loading]="quoting()"
                  (pressed)="book()">
                  Đặt phòng
                </ui-button>
              </div>
            </div>
          </aside>
        </div>

        <!-- THANH ĐẶT PHÒNG DÍNH ĐÁY (chỉ điện thoại) -->
        <div
          class="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-surface p-3 shadow-2 lg:hidden">
          <div class="flex items-center gap-3">
            <div class="min-w-0 flex-1">
              @if (quote(); as found) {
                <p class="truncate text-sm text-text-muted">Tổng {{ found.nights }} đêm</p>
                <p class="truncate text-h3 font-bold text-price">
                  {{ found.totalPrice | vndCurrency }}
                </p>
              } @else {
                <p class="truncate text-sm text-text-muted">Giá từ</p>
                <p class="truncate text-h3 font-bold text-price">
                  {{ data.basePrice | vndCurrency }} / đêm
                </p>
              }
            </div>
            <ui-button [disabled]="!quote()" (pressed)="book()">
              {{ quote() ? 'Đặt phòng' : 'Chọn ngày' }}
            </ui-button>
          </div>
        </div>
      </article>

      <ui-lightbox
        [open]="lightboxOpen()"
        [images]="lightboxImages()"
        [(index)]="lightboxIndex"
        (closed)="lightboxOpen.set(false)" />
    }
  `,
})
export class RoomDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomTypeApi = inject(RoomTypeService);
  private readonly availabilityApi = inject(AvailabilityService);
  private readonly seo = inject(SeoService);

  protected readonly placeholder = ROOM_PLACEHOLDER_IMAGE;

  protected readonly room = signal<PublicRoomType | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly notFound = signal(false);

  protected readonly dates = signal<DateRange>({ checkIn: null, checkOut: null });
  protected readonly adults = signal(2);
  protected readonly children = signal(0);
  protected readonly dateError = signal<string | null>(null);

  protected readonly calendar = signal<AvailabilityMap>({});
  protected readonly calendarLoading = signal(true);

  protected readonly quote = signal<RoomTypeAvailability | null>(null);
  protected readonly quoting = signal(false);
  protected readonly soldOut = signal(false);

  protected readonly lightboxOpen = signal(false);
  protected readonly lightboxIndex = signal(0);

  protected readonly images = computed(() => {
    const all = this.room()?.images ?? [];
    // Ảnh bìa lên đầu để ô lớn luôn là ảnh admin chọn làm đại diện.
    return [...all].sort((a, b) => Number(b.cover) - Number(a.cover));
  });

  protected readonly lightboxImages = computed<LightboxImage[]>(() =>
    this.images().map((image) => ({ url: image.url, alt: image.altText })),
  );

  protected readonly roomAmenities = computed(
    () => this.room()?.amenities.filter((item) => item.category === 'ROOM') ?? [],
  );

  protected readonly propertyAmenities = computed(
    () => this.room()?.amenities.filter((item) => item.category === 'PROPERTY') ?? [],
  );

  constructor() {
    const query = this.route.snapshot.queryParamMap;
    this.dates.set({
      checkIn: query.get('checkIn'),
      checkOut: query.get('checkOut'),
    });
    this.adults.set(Number(query.get('adults')) || 2);
    this.children.set(Number(query.get('children')) || 0);
    this.load();
  }

  protected load(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.loading.set(true);
    this.loadError.set(false);
    this.notFound.set(false);

    this.roomTypeApi.bySlug(slug).subscribe({
      next: (data) => {
        this.room.set(data);
        this.loading.set(false);
        this.applySeo(data);
        this.loadCalendar(data.id);
        if (this.dates().checkIn && this.dates().checkOut) {
          this.refreshQuote();
        }
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err?.status === 404) {
          this.notFound.set(true);
        } else {
          this.loadError.set(true);
        }
      },
    });
  }

  protected onRangeSelected(): void {
    this.dateError.set(null);
    this.refreshQuote();
  }

  protected openLightbox(index: number): void {
    this.lightboxIndex.set(index);
    this.lightboxOpen.set(true);
  }

  protected goToList(): void {
    void this.router.navigate(['/phong']);
  }

  protected book(): void {
    const range = this.dates();
    const data = this.room();
    if (!data || !range.checkIn || !range.checkOut) {
      this.dateError.set('Hãy chọn ngày nhận và ngày trả phòng.');
      return;
    }
    void this.router.navigate(['/dat-phong'], {
      queryParams: {
        checkIn: range.checkIn,
        checkOut: range.checkOut,
        adults: this.adults(),
        children: this.children() || null,
        roomTypeId: data.id,
      },
    });
  }

  /**
   * Hỏi lại giá và số phòng còn trống mỗi lần khách đổi ngày.
   *
   * Kết quả CHỈ để hiển thị. Đơn đặt vẫn được kiểm tra lại ở backend trong cùng
   * giao dịch tạo đơn, nên khoảng trống giữa lúc xem và lúc đặt không thành
   * một chỗ bán trùng phòng.
   */
  private refreshQuote(): void {
    const range = this.dates();
    const data = this.room();
    if (!data || !range.checkIn || !range.checkOut) {
      return;
    }
    this.quoting.set(true);
    this.soldOut.set(false);
    this.availabilityApi
      .search({
        checkIn: range.checkIn,
        checkOut: range.checkOut,
        adults: this.adults(),
        children: this.children(),
      })
      .subscribe({
        next: (response) => {
          const match = response.roomTypes.find((item) => item.roomTypeId === data.id) ?? null;
          this.quote.set(match);
          this.soldOut.set(match === null);
          this.quoting.set(false);
        },
        error: () => {
          this.quote.set(null);
          this.quoting.set(false);
          this.dateError.set('Không kiểm tra được phòng trống. Thử lại sau giây lát.');
        },
      });
  }

  private loadCalendar(roomTypeId: number): void {
    const today = new Date();
    this.availabilityApi
      .calendar(roomTypeId, toIsoDate(today), toIsoDate(addDays(today, CALENDAR_DAYS)))
      .subscribe({
        next: (map) => {
          this.calendar.set(map);
          this.calendarLoading.set(false);
        },
        error: () => {
          // Mất phần chặn trước thì vẫn đặt được: backend từ chối ngày kín.
          this.calendar.set({});
          this.calendarLoading.set(false);
        },
      });
  }

  private applySeo(data: PublicRoomType): void {
    this.seo.apply({
      title: data.name,
      description:
        data.shortDescription ??
        `${data.name} tại Homestay TVH — ${data.capacityAdults} khách, đặt phòng trực tuyến.`,
      imageUrl: this.images()[0]?.url ?? null,
      type: 'article',
    });
  }
}

/** `YYYY-MM-DD` theo giờ ĐỊA PHƯƠNG; `toISOString` đổi sang UTC và lệch một ngày. */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86_400_000);
}
