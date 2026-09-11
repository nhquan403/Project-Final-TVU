import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Chặn khu quản trị theo VAI TRÒ, không chỉ theo "có token hay không".
 *
 * <p>Kiểm "đã đăng nhập" là chưa đủ: mọi khách hàng đều có token hợp lệ. Guard
 * này là lớp trải nghiệm — nó giấu màn hình khỏi người không có quyền. Lớp bảo
 * vệ thật vẫn là {@code hasRole('ADMIN')} ở máy chủ, vì mọi thứ chạy trên trình
 * duyệt đều sửa được.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const decide = () => (auth.isAdmin() ? true : router.createUrlTree(['/']));

  // Khu quản trị có trang đăng nhập RIÊNG. Đẩy về `/dang-nhap` (trang của khách,
  // thuộc Phase 8) là đẩy quản trị viên tới một đường dẫn chưa tồn tại — và khi
  // nó tồn tại thì vẫn là trang sai: đăng nhập xong họ đứng ở trang khách.

  if (auth.isLoggedIn()) {
    return decide();
  }

  return auth.restoreSession().pipe(
    map(() => decide()),
    catchError(() => of(router.createUrlTree(['/admin/login'])))
  );
};
