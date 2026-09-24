import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { UiIcon } from '../../shared/ui';
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
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UiIcon],
  template: `
    <div class="flex min-h-screen flex-col bg-bg">
      <header
        class="sticky top-0 z-40 border-b transition-colors duration-[var(--dur-base)]"
        [class.bg-surface]="scrolled()"
        [class.border-border]="scrolled()"
        [class.bg-transparent]="!scrolled()"
        [class.border-transparent]="!scrolled()">
        <div class="khung flex h-[76px] items-center justify-between gap-4">
          <a
            routerLink="/"
            class="flex min-h-[var(--touch-min)] items-center font-display text-[24px]
                   font-bold tracking-tight text-text">
            Homestay TVH
          </a>

          <nav class="hidden md:block" aria-label="Điều hướng chính">
            <ul class="flex items-center gap-7">
              @for (item of nav; track item.path) {
                <li>
                  <a
                    [routerLink]="item.path"
                    routerLinkActive="border-primary font-semibold text-primary"
                    [routerLinkActiveOptions]="{ exact: item.path === '/' }"
                    class="flex min-h-[var(--touch-min)] items-center border-b-2
                           border-transparent px-1 text-sm text-text
                           transition-colors duration-[var(--dur-fast)] hover:text-primary">
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
              <ui-icon name="menu" [size]="24" />
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

      <!--
        Chân trang ba cột trên màn rộng. Bản trước là ba dòng chữ 14px xếp dọc,
        nên trang kết thúc như bị cắt ngang giữa chừng. Nền mực thay vì nền
        trắng: nó đóng lại trang một cách dứt khoát, và tách hẳn khỏi dải nội
        dung cuối cùng dù dải đó nền gì.
      -->
      <footer class="mt-auto bg-text text-[color:var(--color-bg)]">
        <div class="khung py-14">
          <div class="grid gap-10 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr]">
            <div>
              <p class="font-display text-[26px] font-bold text-text-invert">Homestay TVH</p>
              <p class="mt-3 max-w-[38ch] text-sm opacity-80">
                Ấp Long Trị, xã Long Đức, TP. Trà Vinh. Nhận phòng từ 14:00, trả phòng trước
                12:00.
              </p>
            </div>

            <nav aria-label="Khám phá">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] opacity-70">Khám phá</p>
              <ul class="mt-4 space-y-1">
                @for (item of nav; track item.path) {
                  <li>
                    <a
                      [routerLink]="item.path"
                      class="flex min-h-[var(--touch-min)] items-center text-sm
                             transition-opacity duration-[var(--dur-fast)]
                             hover:opacity-70">
                      {{ item.label }}
                    </a>
                  </li>
                }
              </ul>
            </nav>

            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] opacity-70">Liên hệ</p>
              <ul class="mt-4 space-y-1">
                <li>
                  <a
                    href="tel:02943855246"
                    class="flex min-h-[var(--touch-min)] items-center gap-2 text-sm
                           transition-opacity duration-[var(--dur-fast)] hover:opacity-70">
                    <ui-icon name="dien-thoai" [size]="18" />
                    0294 3855 246
                  </a>
                </li>
                <li>
                  <a
                    href="mailto:lienhe@homestaytvh.vn"
                    class="flex min-h-[var(--touch-min)] items-center gap-2 text-sm
                           transition-opacity duration-[var(--dur-fast)] hover:opacity-70">
                    <ui-icon name="thu" [size]="18" />
                    lienhe&#64;homestaytvh.vn
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <p class="mt-12 border-t border-[color:var(--color-text-muted)] pt-6 text-xs opacity-60">
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
