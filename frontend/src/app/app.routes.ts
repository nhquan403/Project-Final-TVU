import { Routes } from '@angular/router';
import { uiKitRoutes } from './features/ui-kit/ui-kit.routes';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/health/health').then((m) => m.HealthPage),
    title: 'Homestay TVH',
  },
  {
    // Khu quản trị nạp lười: người vào trang đặt phòng không phải tải mã của
    // bảy màn hình quản trị mà họ không bao giờ mở.
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.adminRoutes),
  },
  // Rỗng ở bản production nhờ fileReplacements — xem ui-kit.routes.ts.
  ...uiKitRoutes,
];
