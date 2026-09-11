import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiButton, UiInput } from '../../../shared/ui';

/** Đăng nhập khu quản trị. */
@Component({
  selector: 'app-admin-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-bg px-4">
      <form
        class="w-full max-w-sm rounded-lg border border-border bg-surface p-6"
        (submit)="submit($event)">
        <h1 class="mb-1 text-lg font-bold text-text">Khu quản trị</h1>
        <p class="mb-5 text-sm text-text-muted">Homestay TVH</p>

        <div class="space-y-4">
          <ui-input label="Email" type="email" [(value)]="email" [required]="true" />
          <ui-input label="Mật khẩu" type="password" [(value)]="password" [required]="true" />
        </div>

        @if (error(); as message) {
          <p class="mt-4 rounded-sm bg-danger/10 p-2 text-sm text-danger" role="alert">
            {{ message }}
          </p>
        }

        <div class="mt-5">
          <ui-button type="submit" [fullWidth]="true" [loading]="loading()">Đăng nhập</ui-button>
        </div>
      </form>
    </div>
  `,
})
export class AdminLogin {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(event: Event): void {
    event.preventDefault();
    this.loading.set(true);
    this.error.set(null);

    this.auth.login(this.email(), this.password()).subscribe({
      next: (user) => {
        this.loading.set(false);
        if (user.role !== 'ADMIN') {
          // Đăng nhập ĐÚNG nhưng sai vai trò. Nói thẳng ra thay vì đẩy im lặng
          // về trang chủ — im lặng khiến người dùng tưởng mật khẩu sai và gõ lại.
          this.error.set('Tài khoản này không có quyền vào khu quản trị.');
          return;
        }
        void this.router.navigate(['/admin']);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Email hoặc mật khẩu không đúng.');
      },
    });
  }
}
