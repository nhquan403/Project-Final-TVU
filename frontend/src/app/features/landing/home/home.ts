import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ContentService, type HomeContent } from '../../../core/services/content.service';
import { ReviewService, type PublicReview } from '../../../core/services/review.service';
import {
  RoomTypeService,
  type PublicAmenity,
  type PublicRoomType,
} from '../../../core/services/room-type.service';
import { UiEmptyState, UiSkeleton } from '../../../shared/ui';
import { RoomCatalogCard } from '../shared/room-catalog-card';
import { GallerySection } from './gallery-section';
import { ReviewsSection } from './reviews-section';
import { SearchBar } from './search-bar';

/**
 * Trang chủ.
 *
 * <h2>Ba lượt gọi API rời nhau, không phải một</h2>
 *
 * Nội dung CMS, danh mục phòng và đánh giá được gọi ĐỘC LẬP. Gộp bằng
 * `forkJoin` thì bảng đánh giá trống — chuyện hoàn toàn bình thường ở một
 * homestay mới mở — sẽ kéo sập cả trang chủ. Mỗi khối vì thế tự giữ trạng thái
 * nạp/lỗi của mình và tự biến mất khi không có gì để khoe.
 *
 * <h2>Trang chủ khi CMS còn trống</h2>
 *
 * Backend trả sẵn nội dung mặc định cho bốn khối `hero`, `about`, `contact`,
 * `map`, nên trang không bao giờ hiện ra một vùng trắng không rõ là "chưa nhập"
 * hay "hỏng". Các khối phụ thuộc dữ liệu thật — thư viện ảnh, đánh giá, tin tức
 * — thì ẨN HẲN khi rỗng thay vì hiện một ô rỗng có tiêu đề.
 *
 * <h2>Thân bài viết bằng `[innerHTML]`</h2>
 *
 * Chỉ trường `body` của các khối CMS dùng `[innerHTML]`, và chỉ vì nó đã đi qua
 * bộ lọc thẻ ở tầng vào của backend (OWASP sanitizer, danh sách thẻ cho phép
 * tường minh). Angular còn lọc thêm một lượt nữa lúc hiển thị. Mọi trường do
 * KHÁCH nhập đều đi bằng text binding, không có ngoại lệ.
 */
@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    SearchBar,
    RoomCatalogCard,
    GallerySection,
    ReviewsSection,
    UiSkeleton,
    UiEmptyState,
  ],
  template: `
    <!-- HERO -->
    <section
      class="relative flex min-h-[70vh] items-center justify-center overflow-hidden bg-surface-2">
      @if (heroImage(); as image) {
        <img
          [src]="image"
          alt=""
          class="absolute inset-0 h-full w-full object-cover"
          fetchpriority="high"
          decoding="async" />
        <!--
          Lớp phủ tối: chữ trắng trên một tấm ảnh mà admin có thể thay bất cứ
          lúc nào là canh bạc về độ tương phản. Lớp phủ giữ tỉ lệ tương phản
          đạt chuẩn dù ảnh sáng hay tối.
        -->
        <div class="absolute inset-0 bg-text/50"></div>
      }

      <div class="relative mx-auto w-full max-w-4xl px-4 py-16 text-center">
        <h1 class="text-h1 font-bold" [class.text-text-invert]="heroImage()">
          {{ hero()?.title ?? 'Homestay TVH' }}
        </h1>

        @if (hero()?.subtitle) {
          <p class="mt-3 text-lg" [class.text-text-invert]="heroImage()">
            {{ hero()!.subtitle }}
          </p>
        }

        <div class="mx-auto mt-8 max-w-3xl text-left">
          <app-search-bar />
        </div>
      </div>
    </section>

    <!-- KHUYẾN MÃI ĐANG CHẠY -->
    @if (content()?.banners?.length) {
      <section class="mx-auto max-w-6xl px-4 py-8" aria-labelledby="tieu-de-khuyen-mai">
        <h2 id="tieu-de-khuyen-mai" class="text-h2 font-bold text-text">Ưu đãi đang có</h2>
        <ul class="mt-4 grid gap-4 md:grid-cols-2">
          @for (banner of content()!.banners; track banner.id) {
            <li class="overflow-hidden rounded-lg border border-border bg-surface">
              @if (banner.linkUrl) {
                <a [href]="banner.linkUrl" class="block">
                  <img
                    [src]="banner.imageUrl"
                    [alt]="banner.title"
                    width="960"
                    height="360"
                    loading="lazy"
                    class="aspect-[8/3] w-full object-cover" />
                  <p class="p-3 text-sm font-semibold text-text">{{ banner.title }}</p>
                </a>
              } @else {
                <img
                  [src]="banner.imageUrl"
                  [alt]="banner.title"
                  width="960"
                  height="360"
                  loading="lazy"
                  class="aspect-[8/3] w-full object-cover" />
                <p class="p-3 text-sm font-semibold text-text">{{ banner.title }}</p>
              }
            </li>
          }
        </ul>
      </section>
    }

    <!-- GIỚI THIỆU -->
    @if (about(); as section) {
      <section class="mx-auto max-w-6xl px-4 py-12" aria-labelledby="tieu-de-gioi-thieu">
        <div class="grid items-center gap-8 md:grid-cols-2">
          <div>
            <h2 id="tieu-de-gioi-thieu" class="text-h2 font-bold text-text">
              {{ section.title ?? 'Về Homestay TVH' }}
            </h2>
            @if (section.subtitle) {
              <p class="mt-2 text-lg text-text-muted">{{ section.subtitle }}</p>
            }
            @if (section.body) {
              <div class="prose-tvh mt-4 text-text-muted" [innerHTML]="section.body"></div>
            }
          </div>
          @if (section.imageUrl) {
            <img
              [src]="section.imageUrl"
              alt=""
              width="720"
              height="540"
              loading="lazy"
              class="aspect-[4/3] w-full rounded-lg object-cover" />
          }
        </div>
      </section>
    }

    <!-- LOẠI PHÒNG -->
    <section class="bg-surface-2 py-12" aria-labelledby="tieu-de-phong">
      <div class="mx-auto max-w-6xl px-4">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="tieu-de-phong" class="text-h2 font-bold text-text">Các loại phòng</h2>
            <p class="mt-1 text-text-muted">Chọn ngày để xem phòng còn trống và giá cả kỳ nghỉ.</p>
          </div>
          <a
            routerLink="/phong"
            class="inline-flex min-h-[var(--touch-min)] items-center text-sm font-semibold
                   text-primary hover:underline">
            Xem tất cả
          </a>
        </div>

        @if (roomsLoading()) {
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
        } @else if (roomsError()) {
          <div class="mt-6">
            <ui-empty-state
              title="Chưa tải được danh sách phòng"
              description="Kiểm tra kết nối mạng rồi thử lại. Bạn vẫn có thể tra cứu đơn đã đặt."
              actionLabel="Thử lại"
              (action)="loadRooms()" />
          </div>
        } @else if (rooms().length === 0) {
          <div class="mt-6">
            <ui-empty-state
              title="Chưa có loại phòng nào được mở bán"
              description="Quản trị viên chưa bật loại phòng nào. Liên hệ homestay để được hỗ trợ trực tiếp." />
          </div>
        } @else {
          <ul class="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            @for (room of rooms(); track room.id) {
              <li><app-room-catalog-card [room]="room" /></li>
            }
          </ul>
        }
      </div>
    </section>

    <!-- TIỆN ÍCH CHUNG -->
    @if (propertyAmenities().length) {
      <section class="mx-auto max-w-6xl px-4 py-12" aria-labelledby="tieu-de-tien-ich">
        <h2 id="tieu-de-tien-ich" class="text-h2 font-bold text-text">Tiện ích của homestay</h2>
        <ul class="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          @for (amenity of propertyAmenities(); track amenity.code) {
            <li
              class="flex items-center gap-2 rounded-md border border-border bg-surface px-3 py-2
                     text-sm text-text">
              <span aria-hidden="true">{{ amenity.icon ?? '•' }}</span>
              {{ amenity.name }}
            </li>
          }
        </ul>
      </section>
    }

    <!-- THƯ VIỆN ẢNH -->
    @if (content()?.gallery?.length) {
      <section class="bg-surface-2 py-12" aria-labelledby="tieu-de-thu-vien">
        <div class="mx-auto max-w-6xl px-4">
          <h2 id="tieu-de-thu-vien" class="text-h2 font-bold text-text">Không gian homestay</h2>
          <div class="mt-4">
            <app-gallery-section [images]="content()!.gallery" />
          </div>
        </div>
      </section>
    }

    <!-- ĐÁNH GIÁ -->
    @if (reviews().length) {
      <section class="mx-auto max-w-6xl px-4 py-12" aria-labelledby="tieu-de-danh-gia">
        <h2 id="tieu-de-danh-gia" class="text-h2 font-bold text-text">Khách đã ở nói gì</h2>
        <p class="mt-1 text-text-muted">
          Chỉ đánh giá của khách có đơn đã hoàn tất, được homestay duyệt trước khi đăng.
        </p>
        <div class="mt-4">
          <app-reviews-section [reviews]="reviews()" />
        </div>
      </section>
    }

    <!-- TIN TỨC -->
    @if (content()?.latestPosts?.length) {
      <section class="bg-surface-2 py-12" aria-labelledby="tieu-de-tin-tuc">
        <div class="mx-auto max-w-6xl px-4">
          <div class="flex flex-wrap items-end justify-between gap-3">
            <h2 id="tieu-de-tin-tuc" class="text-h2 font-bold text-text">Tin tức &amp; cẩm nang</h2>
            <a
              routerLink="/tin-tuc"
              class="inline-flex min-h-[var(--touch-min)] items-center text-sm font-semibold
                     text-primary hover:underline">
              Xem tất cả
            </a>
          </div>

          <ul class="mt-4 grid gap-4 md:grid-cols-3">
            @for (post of content()!.latestPosts; track post.id) {
              <li class="h-full overflow-hidden rounded-lg border border-border bg-surface">
                <a [routerLink]="['/tin-tuc', post.slug]" class="flex h-full flex-col">
                  @if (post.coverImageUrl) {
                    <img
                      [src]="post.coverImageUrl"
                      alt=""
                      width="640"
                      height="360"
                      loading="lazy"
                      class="aspect-[16/9] w-full object-cover" />
                  }
                  <div class="flex flex-1 flex-col gap-2 p-4">
                    <h3 class="font-semibold text-text">{{ post.title }}</h3>
                    @if (post.excerpt) {
                      <p class="line-clamp-3 text-sm text-text-muted">{{ post.excerpt }}</p>
                    }
                    @if (post.publishedAt) {
                      <p class="mt-auto pt-2 text-xs text-text-muted">
                        {{ post.publishedAt | date: 'dd/MM/yyyy' }}
                      </p>
                    }
                  </div>
                </a>
              </li>
            }
          </ul>
        </div>
      </section>
    }

    <!-- LIÊN HỆ + BẢN ĐỒ -->
    <section class="mx-auto max-w-6xl px-4 py-12" aria-labelledby="tieu-de-lien-he">
      <div class="grid gap-8 md:grid-cols-2">
        <div>
          <h2 id="tieu-de-lien-he" class="text-h2 font-bold text-text">
            {{ contact()?.title ?? 'Liên hệ' }}
          </h2>
          @if (contact()?.subtitle) {
            <p class="mt-2 text-text-muted">{{ contact()!.subtitle }}</p>
          }
          @if (contact()?.body) {
            <div class="prose-tvh mt-4 text-text-muted" [innerHTML]="contact()!.body"></div>
          }
        </div>

        <div>
          <h3 class="text-h3 font-semibold text-text">{{ map()?.title ?? 'Đường tới homestay' }}</h3>
          @if (map()?.subtitle) {
            <p class="mt-1 text-text-muted">{{ map()!.subtitle }}</p>
          }
          @if (map()?.imageUrl) {
            <img
              [src]="map()!.imageUrl"
              alt="Bản đồ đường tới Homestay TVH"
              width="720"
              height="480"
              loading="lazy"
              class="mt-3 aspect-[3/2] w-full rounded-lg border border-border object-cover" />
          }
          @if (map()?.body) {
            <div class="prose-tvh mt-3 text-text-muted" [innerHTML]="map()!.body"></div>
          }
        </div>
      </div>
    </section>
  `,
})
export class HomePage {
  private readonly contentApi = inject(ContentService);
  private readonly roomTypeApi = inject(RoomTypeService);
  private readonly reviewApi = inject(ReviewService);

  protected readonly content = signal<HomeContent | null>(null);
  protected readonly rooms = signal<PublicRoomType[]>([]);
  protected readonly roomsLoading = signal(true);
  protected readonly roomsError = signal(false);
  protected readonly reviews = signal<PublicReview[]>([]);

  protected readonly hero = computed(() => this.section('hero'));
  protected readonly about = computed(() => this.section('about'));
  protected readonly contact = computed(() => this.section('contact'));
  protected readonly map = computed(() => this.section('map'));
  protected readonly heroImage = computed(() => this.hero()?.imageUrl ?? null);

  /**
   * Tiện ích của CẢ homestay, gom từ mọi loại phòng và bỏ trùng.
   *
   * Không có bảng "tiện ích chung" riêng trong cơ sở dữ liệu; `PROPERTY` là
   * cách lược đồ đánh dấu điều đó, và cùng một tiện ích được gắn cho nhiều loại
   * phòng. Không gom trùng thì "Wifi miễn phí" hiện ra năm lần.
   */
  protected readonly propertyAmenities = computed<PublicAmenity[]>(() => {
    const byCode = new Map<string, PublicAmenity>();
    for (const room of this.rooms()) {
      for (const amenity of room.amenities) {
        if (amenity.category === 'PROPERTY' && !byCode.has(amenity.code)) {
          byCode.set(amenity.code, amenity);
        }
      }
    }
    return [...byCode.values()];
  });

  constructor() {
    // Nội dung CMS hỏng thì trang vẫn còn thanh tìm phòng và danh mục phòng —
    // tức là vẫn bán được hàng. Vì thế không có cờ lỗi cho khối này.
    this.contentApi.home().subscribe({ next: (data) => this.content.set(data) });
    this.reviewApi.published().subscribe({ next: (data) => this.reviews.set(data) });
    this.loadRooms();
  }

  protected loadRooms(): void {
    this.roomsLoading.set(true);
    this.roomsError.set(false);
    this.roomTypeApi.list().subscribe({
      next: (data) => {
        this.rooms.set(data);
        this.roomsLoading.set(false);
      },
      error: () => {
        this.roomsError.set(true);
        this.roomsLoading.set(false);
      },
    });
  }

  private section(key: string) {
    return this.content()?.sections.find((item) => item.key === key) ?? null;
  }
}
