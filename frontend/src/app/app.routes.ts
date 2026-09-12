import { Routes } from '@angular/router';
import { uiKitRoutes } from './features/ui-kit/ui-kit.routes';

export const routes: Routes = [
  {
    // Khu quản trị nạp lười: người vào trang đặt phòng không phải tải mã của
    // bảy màn hình quản trị mà họ không bao giờ mở.
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.adminRoutes),
  },
  // Rỗng ở bản production nhờ fileReplacements — xem ui-kit.routes.ts.
  ...uiKitRoutes,
  {
    // Khu công khai đứng CUỐI vì nó có route con `''`. Đặt trước `admin` thì
    // `''` khớp rỗng và nuốt luôn mọi đường dẫn phía sau.
    path: '',
    loadChildren: () => import('./features/landing/landing.routes').then((m) => m.landingRoutes),
  },
];
