import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Đường duy nhất được vào khi tài khoản đang bị buộc đổi mật khẩu. */
export const ADMIN_CHANGE_PASSWORD_PATH = '/admin/doi-mat-khau';

/**
 * Chặn khu quản trị theo VAI TRÒ, không chỉ theo "có token hay không".
 *
 * <p>Kiểm "đã đăng nhập" là chưa đủ: mọi khách hàng đều có token hợp lệ. Guard
 * này là lớp trải nghiệm — nó giấu màn hình khỏi người không có quyền. Lớp bảo
 * vệ thật vẫn là {@code hasRole('ADMIN')} ở máy chủ, vì mọi thứ chạy trên trình
 * duyệt đều sửa được.
 *
 * <p>Guard cũng đẩy người đang bị buộc đổi mật khẩu sang màn hình đổi mật khẩu.
 * Backend đã chặn bằng {@code MustChangePasswordFilter} (403 cho mọi đường trừ
 * ba đường tối thiểu), nhưng chặn ở tầng dữ liệu mà không dẫn đường ở tầng giao
 * diện thì người dùng chỉ thấy một dashboard trống không giải thích gì.
 */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const decide = () => {
    if (!auth.isAdmin()) {
      return router.createUrlTree(['/']);
    }
    // Chính màn hình đổi mật khẩu phải được miễn, nếu không guard đẩy nó về
    // chính nó và trình duyệt treo trong vòng chuyển hướng.
    if (auth.mustChangePassword() && !state.url.startsWith(ADMIN_CHANGE_PASSWORD_PATH)) {
      return router.createUrlTree([ADMIN_CHANGE_PASSWORD_PATH]);
    }
    return true;
  };

  // Khu quản trị có trang đăng nhập RIÊNG. Đẩy về `/dang-nhap` (trang của khách)
  // là đẩy quản trị viên tới đúng chỗ sai: đăng nhập xong họ đứng ở trang khách.

  if (auth.isLoggedIn()) {
    return decide();
  }

  return auth.restoreSession().pipe(
    map(() => decide()),
    catchError(() => of(router.createUrlTree(['/admin/login'])))
  );
};
