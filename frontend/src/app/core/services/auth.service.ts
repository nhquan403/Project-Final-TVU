import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';

export type UserRole = 'CUSTOMER' | 'ADMIN';

export interface CurrentUser {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: UserRole;
  mustChangePassword: boolean;
}

interface TokenResponse {
  accessToken: string;
  expiresInSeconds: number;
}

/**
 * Trạng thái đăng nhập của phiên làm việc.
 *
 * <p>Access token giữ trong BỘ NHỚ, không đưa vào `localStorage`. Refresh token
 * nằm trong cookie `HttpOnly` do máy chủ đặt, nên mã JavaScript — kể cả mã do
 * kẻ tấn công chèn qua XSS — không đọc được nó. Hệ quả phải chấp nhận: tải lại
 * trang là mất access token, và ứng dụng phải gọi `restoreSession()` lúc khởi
 * động để lấy lại bằng cookie. Đó là cái giá đúng để đổi lấy việc một lỗ hổng
 * XSS không mang đi được phiên đăng nhập kéo dài bảy ngày.
 *
 * Mọi request đều `withCredentials: true` vì refresh token đi bằng cookie.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly accessToken = signal<string | null>(null);
  private readonly user = signal<CurrentUser | null>(null);

  readonly currentUser = this.user.asReadonly();
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  /** Đang bị buộc đổi mật khẩu: mọi màn hình khác phải chặn lại. */
  readonly mustChangePassword = computed(() => this.user()?.mustChangePassword === true);

  getAccessToken(): string | null {
    return this.accessToken();
  }

  login(email: string, password: string): Observable<CurrentUser> {
    return this.http
      .post<TokenResponse>('/api/auth/login', { email, password }, { withCredentials: true })
      .pipe(
        tap((response) => this.accessToken.set(response.accessToken)),
        switchMap(() => this.loadCurrentUser()),
      );
  }

  register(input: {
    email: string;
    password: string;
    fullName: string;
    phone?: string;
  }): Observable<CurrentUser> {
    return this.http
      .post<TokenResponse>('/api/auth/register', input, { withCredentials: true })
      .pipe(
        tap((response) => this.accessToken.set(response.accessToken)),
        switchMap(() => this.loadCurrentUser()),
      );
  }

  /** Đổi refresh token trong cookie lấy cặp token mới. Interceptor gọi hàm này. */
  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/refresh', null, { withCredentials: true })
      .pipe(tap((response) => this.accessToken.set(response.accessToken)));
  }

  /** Gọi lúc khởi động ứng dụng để khôi phục phiên từ cookie sau khi tải lại trang. */
  restoreSession(): Observable<CurrentUser> {
    return this.refresh().pipe(switchMap(() => this.loadCurrentUser()));
  }

  loadCurrentUser(): Observable<CurrentUser> {
    return this.http
      .get<CurrentUser>('/api/me', { withCredentials: true })
      .pipe(tap((me) => this.user.set(me)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>('/api/auth/logout', null, { withCredentials: true })
      .pipe(tap(() => this.clear()));
  }

  /** Xoá trạng thái phía client. Không tự gọi API — dùng khi phiên đã chết. */
  clear(): void {
    this.accessToken.set(null);
    this.user.set(null);
  }
}
