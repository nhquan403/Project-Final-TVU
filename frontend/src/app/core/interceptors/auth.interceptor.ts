import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Đường không bao giờ được tự làm mới token — chính chúng là cơ chế làm mới. */
const NO_RETRY_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

/**
 * Trạng thái dùng chung giữa các lượt gọi interceptor.
 *
 * Để ngoài hàm là có chủ ý: interceptor kiểu hàm được gọi lại cho MỖI request,
 * nên biến khai bên trong sẽ được khởi tạo lại và cơ chế chống vòng lặp mất tác
 * dụng đúng lúc cần nhất — khi nhiều request cùng nhận 401 một lượt.
 */
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/**
 * Gắn Bearer token, và tự làm mới MỘT LẦN khi gặp 401.
 *
 * <p>Ba lớp chống vòng lặp vô hạn, thiếu lớp nào cũng đủ làm treo tab:
 * <ol>
 *   <li>Không bao giờ thử làm mới cho chính `/api/auth/refresh`.
 *   <li>Cờ `isRefreshing`: nhiều request cùng nhận 401 chỉ kích hoạt một lượt
 *       làm mới; những request còn lại xếp hàng chờ token mới.
 *   <li>Làm mới thất bại → xoá phiên và chuyển về trang đăng nhập, KHÔNG thử
 *       lại lần nữa.
 * </ol>
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const send = (req: HttpRequest<unknown>) => {
    const token = auth.getAccessToken();
    // withCredentials bật cho MỌI request: refresh token nằm trong cookie, và
    // trình duyệt chỉ gửi cookie kèm request khi cờ này bật.
    return next(
      req.clone({
        withCredentials: true,
        setHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      }),
    );
  };

  const isAuthEndpoint = NO_RETRY_PATHS.some((path) => request.url.startsWith(path));

  return send(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthEndpoint) {
        return throwError(() => error);
      }

      if (isRefreshing) {
        // Xếp hàng: chờ lượt làm mới đang chạy xong rồi phát lại request này.
        return refreshedToken$.pipe(
          filter((token): token is string => token !== null),
          take(1),
          switchMap(() => send(request)),
        );
      }

      isRefreshing = true;
      refreshedToken$.next(null);

      return auth.refresh().pipe(
        switchMap((response) => {
          isRefreshing = false;
          refreshedToken$.next(response.accessToken);
          return send(request);
        }),
        catchError((refreshError: unknown) => {
          // Làm mới cũng hỏng nghĩa là phiên đã chết thật. Dừng ở đây.
          isRefreshing = false;
          auth.clear();
          void router.navigate(['/dang-nhap']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
