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
import { UiEmptyState, UiIcon, UiSkeleton, tenIcon } from '../../../shared/ui';
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
    UiIcon,
  ],
  template: `
    <!--
      HERO

      Bố cục tạp chí: ảnh tràn hết bề ngang, hộp chữ là một tấm giấy trắng đè
      lên mép dưới ảnh và lệch về bên trái. Bản trước đặt chữ GIỮA ảnh và phải
      phủ một lớp tối 50% lên toàn bộ tấm ảnh để chữ trắng đọc được — tức là
      làm mờ chính thứ đáng nhìn nhất trang. Ở đây chữ nằm trên nền trắng nên
      tỉ lệ tương phản không phụ thuộc vào tấm ảnh admin tải lên, và ảnh giữ
      nguyên độ sáng.
    -->
    <section class="relative bg-surface-2" aria-labelledby="tieu-de-hero">
      @if (heroImage(); as image) {
        <img
          [src]="image"
          alt=""
          class="h-[clamp(320px,46vw,560px)] w-full object-cover"
          fetchpriority="high"
          decoding="async" />
      } @else {
        <div class="h-[clamp(320px,46vw,560px)] w-full bg-surface-2"></div>
      }

      <div class="khung relative -mt-24 pb-4 md:-mt-32">
        <div class="max-w-[880px] bg-surface px-7 py-9 shadow-2 md:px-12 md:py-11">
          <span class="nhan">Ấp Long Trị · Trà Vinh</span>
          <h1 id="tieu-de-hero" class="mt-3 text-display">
            {{ hero()?.title ?? 'Homestay TVH' }}
          </h1>

          @if (hero()?.subtitle) {
            <p class="doc mt-4 text-body-lg text-text-muted">{{ hero()!.subtitle }}</p>
          }
        </div>

        <!--
          Thanh tìm phòng nằm NGOÀI hộp chữ và chiếm trọn bề ngang khung.
          Nhét nó vào trong hộp thì bốn ô (ngày, người lớn, trẻ em, nút) phải
          chia nhau khoảng 790px, và cả ba nhãn đều xuống dòng giữa chừng.
          Tách ra cũng đúng về mặt nội dung: hộp trên nói homestay là gì, thanh
          dưới là việc khách cần làm.
        -->
        <div class="mt-7">
          <app-search-bar />
        </div>
      </div>
    </section>

    <!--
      KHUYẾN MÃI ĐANG CHẠY

      Thuộc tính alt để rỗng là có chủ đích, không phải quên: tiêu đề đã hiện
      thành chữ thật ngay dưới ảnh. Đặt tiêu đề vào alt nữa thì trình đọc màn
      hình đọc hai lần cùng một câu. Ảnh ở đây đi kèm chú thích nhìn thấy
      được, nên nó là ảnh trang trí.
    -->
    @if (content()?.banners?.length) {
      <section class="khung sec-sm" aria-labelledby="tieu-de-khuyen-mai">
        <h2 id="tieu-de-khuyen-mai">Ưu đãi đang có</h2>
        <ul class="mt-8 grid gap-7 md:grid-cols-2">
          @for (banner of content()!.banners; track banner.id) {
            <li class="border border-border bg-surface">
              @if (banner.linkUrl) {
                <a [href]="banner.linkUrl" class="block">
                  <img
                    [src]="banner.imageUrl"
                    alt=""
                    width="960"
                    height="360"
                    loading="lazy"
                    class="aspect-[8/3] w-full object-cover" />
                  <p class="px-5 py-4 text-sm font-semibold text-text">{{ banner.title }}</p>
                </a>
              } @else {
                <img
                  [src]="banner.imageUrl"
                  alt=""
                  width="960"
                  height="360"
                  loading="lazy"
                  class="aspect-[8/3] w-full object-cover" />
                <p class="px-5 py-4 text-sm font-semibold text-text">{{ banner.title }}</p>
              }
            </li>
          }
        </ul>
      </section>
    }

    <!-- GIỚI THIỆU -->
    @if (about(); as section) {
      <section class="bg-surface-2 sec-lg" aria-labelledby="tieu-de-gioi-thieu">
      <div class="khung">
        <div class="grid items-center gap-12 md:grid-cols-[1.05fr_0.95fr] lg:gap-18">
          <div>
            <span class="nhan">Từ năm 2019</span>
            <h2 id="tieu-de-gioi-thieu" class="mt-3">
              {{ section.title ?? 'Về Homestay TVH' }}
            </h2>
            @if (section.subtitle) {
              <p class="doc mt-4 text-body-lg text-text-muted">{{ section.subtitle }}</p>
            }
            @if (section.body) {
              <div class="prose-tvh doc mt-5 text-text-muted" [innerHTML]="section.body"></div>
            }
          </div>
          @if (section.imageUrl) {
            <img
              [src]="section.imageUrl"
              alt=""
              width="720"
              height="540"
              loading="lazy"
              class="aspect-[5/4] w-full object-cover" />
          }
        </div>
      </div>
    </section>
    }

    <!-- LOẠI PHÒNG -->
    <section class="sec-md" aria-labelledby="tieu-de-phong">
      <div class="khung">
        <div class="flex flex-wrap items-end justify-between gap-6">
          <div>
            <span class="nhan">Bốn loại phòng</span>
            <h2 id="tieu-de-phong" class="mt-3">Chọn chỗ ngủ của bạn</h2>
            <p class="doc mt-3 text-text-muted">
              Giá hiển thị là giá thấp nhất cho một đêm. Chọn ngày để xem tổng tiền cả kỳ nghỉ.
            </p>
          </div>
          <a
            routerLink="/phong"
            class="inline-flex min-h-[var(--touch-min)] items-center border-b border-primary
                   text-sm font-semibold text-primary
                   transition-colors duration-[var(--dur-fast)] hover:text-primary-hover">
            Xem tất cả
          </a>
        </div>

        @if (roomsLoading()) {
          <ul class="mt-10 grid gap-7 sm:grid-cols-2 lg:grid-cols-4">
            @for (placeholder of [1, 2, 3, 4]; track placeholder) {
              <li class="border border-border bg-surface">
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
          <ul class="mt-10 grid gap-7 sm:grid-cols-2 lg:grid-cols-4">
            @for (room of rooms(); track room.id) {
              <li><app-room-catalog-card [room]="room" /></li>
            }
          </ul>
        }
      </div>
    </section>

    <!-- TIỆN ÍCH CHUNG -->
    @if (propertyAmenities().length) {
      <section class="bg-surface-2 sec-sm" aria-labelledby="tieu-de-tien-ich">
        <div class="khung">
        <h2 id="tieu-de-tien-ich">Tiện ích của homestay</h2>
        <ul class="mt-8 grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (amenity of propertyAmenities(); track amenity.code) {
            <li class="flex items-center gap-3 border-b border-border py-3 text-text">
              @if (tenIcon(amenity.icon); as icon) {
                <ui-icon [name]="icon" [size]="20" class="text-primary" />
              }
              {{ amenity.name }}
            </li>
          }
        </ul>
        </div>
      </section>
    }

    <!-- THƯ VIỆN ẢNH -->
    @if (content()?.gallery?.length) {
      <section class="sec-md" aria-labelledby="tieu-de-thu-vien">
        <div class="khung">
          <span class="nhan">Thư viện ảnh</span>
          <h2 id="tieu-de-thu-vien" class="mt-3">Không gian homestay</h2>
          <div class="mt-9">
            <app-gallery-section [images]="content()!.gallery" />
          </div>
        </div>
      </section>
    }

    <!-- ĐÁNH GIÁ -->
    @if (reviews().length) {
      <section class="bg-surface-2 sec-md" aria-labelledby="tieu-de-danh-gia">
        <div class="khung">
          <span class="nhan">Đánh giá đã xác minh</span>
          <h2 id="tieu-de-danh-gia" class="mt-3">Khách đã ở nói gì</h2>
          <p class="doc mt-3 text-text-muted">
            Chỉ đánh giá của khách có đơn đã hoàn tất, được homestay duyệt trước khi đăng.
          </p>
          <div class="mt-9">
            <app-reviews-section [reviews]="reviews()" />
          </div>
        </div>
      </section>
    }

    <!-- TIN TỨC -->
    @if (content()?.latestPosts?.length) {
      <section class="sec-md" aria-labelledby="tieu-de-tin-tuc">
        <div class="khung">
          <div class="flex flex-wrap items-end justify-between gap-6">
            <div>
              <span class="nhan">Cẩm nang</span>
              <h2 id="tieu-de-tin-tuc" class="mt-3">Tin tức &amp; cẩm nang</h2>
            </div>
            <a
              routerLink="/tin-tuc"
              class="inline-flex min-h-[var(--touch-min)] items-center border-b border-primary
                     text-sm font-semibold text-primary
                     transition-colors duration-[var(--dur-fast)] hover:text-primary-hover">
              Xem tất cả
            </a>
          </div>

          <ul class="mt-10 grid gap-7 md:grid-cols-3">
            @for (post of content()!.latestPosts; track post.id) {
              <li class="h-full border border-border bg-surface transition-shadow
                         duration-[var(--dur-base)] hover:shadow-2">
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
                  <div class="flex flex-1 flex-col gap-3 p-6">
                    <h3>{{ post.title }}</h3>
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
    <section class="bg-surface-2 sec-md" aria-labelledby="tieu-de-lien-he">
      <div class="khung grid gap-12 md:grid-cols-2 lg:gap-18">
        <div>
          <span class="nhan">Liên hệ</span>
          <h2 id="tieu-de-lien-he" class="mt-3">
            {{ contact()?.title ?? 'Liên hệ' }}
          </h2>
          @if (contact()?.subtitle) {
            <p class="mt-2 text-text-muted">{{ contact()!.subtitle }}</p>
          }
          @if (contact()?.body) {
            <div class="prose-tvh doc mt-5 text-text-muted" [innerHTML]="contact()!.body"></div>
          }
        </div>

        <div>
          <h3>{{ map()?.title ?? 'Đường tới homestay' }}</h3>
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
              class="mt-5 aspect-[3/2] w-full border border-border object-cover" />
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
  /** Lọc tên icon từ CSDL; template gọi trực tiếp. */
  protected readonly tenIcon = tenIcon;

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
