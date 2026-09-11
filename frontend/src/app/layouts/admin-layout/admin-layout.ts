import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AdminBookingService } from '../../core/services/admin-booking.service';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
  /** Hiện số dòng cần đối soát ngay trên mục này. */
  badge?: boolean;
}

const NAV: NavItem[] = [
  { path: '/admin', label: 'Tổng quan' },
  { path: '/admin/bookings', label: 'Đơn đặt phòng' },
  { path: '/admin/payments', label: 'Đối soát thanh toán', badge: true },
  { path: '/admin/room-types', label: 'Loại phòng' },
  { path: '/admin/rooms', label: 'Phòng' },
  { path: '/admin/promotions', label: 'Khuyến mãi' },
  { path: '/admin/reviews', label: 'Đánh giá' },
];

/**
 * Khung của khu quản trị: sidebar, topbar, vùng nội dung.
 *
 * Số dòng cần đối soát hiện NGAY trên sidebar chứ không nằm trong một menu con.
 * Ba nhánh của Phase 6 kết thúc bằng "chuyển người xử lý", và một hàng đợi
 * không ai nhìn thấy là một hàng đợi không ai xử lý — tiền của khách nằm im
 * trong bảng đúng như khi chưa có màn hình này.
 */
@Component({
  selector: 'app-admin-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-bg md:grid md:grid-cols-[240px_1fr]">
      <aside class="border-b border-border bg-surface md:border-b-0 md:border-r">
        <div class="px-4 py-4">
          <p class="text-base font-bold text-text">Homestay TVH</p>
          <p class="text-xs text-text-muted">Khu quản trị</p>
        </div>

        <nav class="px-2 pb-4" aria-label="Điều hướng khu quản trị">
          <ul class="space-y-1">
            @for (item of nav; track item.path) {
              <li>
                <a
                  [routerLink]="item.path"
                  routerLinkActive="bg-surface-2 font-semibold text-primary"
                  [routerLinkActiveOptions]="{ exact: item.path === '/admin' }"
                  class="flex min-h-[var(--touch-min)] items-center justify-between gap-2 rounded-sm
                         px-3 text-sm text-text transition-colors duration-[var(--dur-fast)]
                         hover:bg-surface-2">
                  <span>{{ item.label }}</span>
                  @if (item.badge && reconcileCount() > 0) {
                    <span
                      class="inline-flex min-w-6 items-center justify-center rounded-sm bg-warning
                             px-1.5 text-xs font-bold text-text-invert"
                      [attr.aria-label]="reconcileCount() + ' khoản cần đối soát'">
                      {{ reconcileCount() }}
                    </span>
                  }
                </a>
              </li>
            }
          </ul>
        </nav>
      </aside>

      <div class="flex min-h-screen flex-col">
        <header class="flex flex-wrap items-center justify-between gap-3 border-b border-border
                       bg-surface px-4 py-3">
          <p class="text-sm text-text-muted">
            {{ auth.currentUser()?.email ?? 'Chưa đăng nhập' }}
          </p>
          <button
            type="button"
            class="min-h-[var(--touch-min)] rounded-sm px-3 text-sm font-semibold text-focus underline"
            (click)="signOut()">
            Đăng xuất
          </button>
        </header>

        <main class="flex-1 px-4 py-5">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class AdminLayout {
  protected readonly auth = inject(AuthService);
  private readonly bookings = inject(AdminBookingService);
  private readonly router = inject(Router);

  protected readonly nav = NAV;
  protected readonly reconcileCount = signal(0);

  constructor() {
    this.bookings.reconcileQueue().subscribe({
      next: (page) => this.reconcileCount.set(page.totalElements),
      // Sidebar không được vỡ vì một lần gọi hỏng: badge vắng mặt vẫn hơn cả
      // khung quản trị không hiện ra.
      error: () => this.reconcileCount.set(0),
    });
  }

  protected signOut(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigate(['/admin/login']),
      error: () => void this.router.navigate(['/admin/login']),
    });
  }
}
