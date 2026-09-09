import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Chặn các đường cần đăng nhập.
 *
 * <p>Khi chưa có thông tin người dùng trong bộ nhớ, guard thử khôi phục phiên
 * từ cookie trước khi kết luận. Bỏ bước này thì mỗi lần tải lại trang người
 * dùng đều bị đá ra đăng nhập lại, dù refresh token trong cookie vẫn còn hạn.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  return auth.restoreSession().pipe(
    map(() => true),
    catchError(() =>
      of(router.createUrlTree(['/dang-nhap'], { queryParams: { tiep: state.url } }))),
  );
};
