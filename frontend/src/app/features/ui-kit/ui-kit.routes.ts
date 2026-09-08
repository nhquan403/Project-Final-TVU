import { Routes } from '@angular/router';

/**
 * Route của trang trình bày hệ thống thiết kế.
 *
 * File này bị THAY THẾ bằng `ui-kit.routes.prod.ts` ở bản production
 * (xem `fileReplacements` trong angular.json). Chỉ tắt bằng cờ `environment`
 * là chưa đủ: lệnh `import()` động vẫn nằm trong mã nguồn nên trình đóng gói
 * vẫn sinh ra chunk, và ai đoán đúng tên file vẫn tải về được. Thay cả file
 * thì lệnh import biến mất, chunk cũng không được sinh ra.
 */
export const uiKitRoutes: Routes = [
  {
    path: 'ui-kit',
    loadComponent: () => import('./ui-kit').then((m) => m.UiKitPage),
    title: 'Hệ thống thiết kế — Homestay TVH',
  },
];
