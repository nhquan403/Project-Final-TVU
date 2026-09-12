import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
}

const NAV: NavItem[] = [
  { path: '/', label: 'Trang chủ' },
  { path: '/phong', label: 'Phòng' },
  { path: '/tin-tuc', label: 'Tin tức' },
  { path: '/tra-cuu', label: 'Tra cứu đơn' },
];

/**
 * Khung trang công khai: header dính, menu mobile dạng trượt, footer.
 *
 * Header trong suốt khi ở đỉnh trang (để ảnh hero chiếm trọn màn hình) và
 * chuyển sang nền đặc khi cuộn xuống — nếu không, chữ trắng trên nền ảnh sáng
 * sẽ không đọc được ở nửa dưới trang.
 */
@Component({
  selector: 'app-public-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex min-h-screen flex-col bg-bg">
      <header
        class="sticky top-0 z-40 border-b transition-colors duration-[var(--dur-base)]"
        [class.bg-surface]="scrolled()"
        [class.border-border]="scrolled()"
        [class.bg-transparent]="!scrolled()"
        [class.border-transparent]="!scrolled()">
        <div class="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3">
          <a routerLink="/" class="flex min-h-[var(--touch-min)] items-center font-bold text-text">
            Homestay TVH
          </a>

          <nav class="hidden md:block" aria-label="Điều hướng chính">
            <ul class="flex items-center gap-1">
              @for (item of nav; track item.path) {
                <li>
                  <a
                    [routerLink]="item.path"
                    routerLinkActive="font-semibold text-primary"
                    [routerLinkActiveOptions]="{ exact: item.path === '/' }"
                    class="flex min-h-[var(--touch-min)] items-center rounded-sm px-3 text-sm
                           text-text transition-colors duration-[var(--dur-fast)]
                           hover:text-primary">
                    {{ item.label }}
                  </a>
                </li>
              }
            </ul>
          </nav>

          <div class="flex items-center gap-2">
            @if (auth.isLoggedIn()) {
              <a
                routerLink="/tai-khoan/dat-phong"
                class="hidden min-h-[var(--touch-min)] items-center rounded-sm px-3 text-sm
                       text-text hover:text-primary sm:flex">
                Đơn của tôi
              </a>
            } @else {
              <a
                routerLink="/dang-nhap"
                class="hidden min-h-[var(--touch-min)] items-center rounded-sm px-3 text-sm
                       text-text hover:text-primary sm:flex">
                Đăng nhập
              </a>
            }

            <a
              routerLink="/dat-phong"
              class="inline-flex min-h-[var(--touch-min)] items-center rounded-md bg-primary px-4
                     text-sm font-semibold text-text-invert
                     transition-colors duration-[var(--dur-fast)] hover:bg-primary-hover">
              Đặt phòng
            </a>

            <button
              type="button"
              class="inline-flex min-h-[var(--touch-min)] min-w-[var(--touch-min)] items-center
                     justify-center rounded-sm text-text md:hidden"
              [attr.aria-expanded]="menuOpen()"
              aria-controls="menu-mobile"
              aria-label="Mở menu"
              (click)="menuOpen.set(!menuOpen())">
              ☰
            </button>
          </div>
        </div>

        @if (menuOpen()) {
          <nav
            id="menu-mobile"
            class="border-t border-border bg-surface md:hidden"
            aria-label="Điều hướng chính (mobile)">
            <ul class="px-4 py-2">
              @for (item of nav; track item.path) {
                <li>
                  <a
                    [routerLink]="item.path"
                    class="flex min-h-[var(--touch-min)] items-center text-sm text-text"
                    (click)="menuOpen.set(false)">
                    {{ item.label }}
                  </a>
                </li>
              }
              <li>
                <a
                  [routerLink]="auth.isLoggedIn() ? '/tai-khoan/dat-phong' : '/dang-nhap'"
                  class="flex min-h-[var(--touch-min)] items-center text-sm text-text"
                  (click)="menuOpen.set(false)">
                  {{ auth.isLoggedIn() ? 'Đơn của tôi' : 'Đăng nhập' }}
                </a>
              </li>
            </ul>
          </nav>
        }
      </header>

      <main class="flex-1">
        <router-outlet />
      </main>

      <footer class="border-t border-border bg-surface">
        <div class="mx-auto max-w-6xl px-4 py-6 text-sm text-text-muted">
          <p class="font-semibold text-text">Homestay TVH</p>
          <p class="mt-1">Trà Vinh · 0294 3855 246 · lienhe&#64;homestaytvh.vn</p>
          <p class="mt-3 text-xs">
            Đồ án tốt nghiệp — Trường Đại học Trà Vinh. Giá và tình trạng phòng lấy trực tiếp từ
            hệ thống, không có số liệu trang trí.
          </p>
        </div>
      </footer>
    </div>
  `,
})
export class PublicLayout {
  protected readonly auth = inject(AuthService);
  protected readonly nav = NAV;
  protected readonly menuOpen = signal(false);
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  protected onScroll(): void {
    this.scrolled.set(window.scrollY > 24);
  }
}
