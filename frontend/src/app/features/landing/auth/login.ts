import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiButton, UiInput } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';

type Mode = 'login' | 'register';

/**
 * Đăng nhập và đăng ký cho khách.
 *
 * <h2>Tham số `tiep`</h2>
 *
 * `auth.guard` chuyển hướng tới đây kèm `?tiep=<đường dẫn đang muốn vào>`. Sau
 * khi đăng nhập, trang quay lại đúng chỗ đó — bỏ qua tham số này nghĩa là khách
 * bấm "Đơn của tôi", đăng nhập xong lại thấy trang chủ và phải bấm lại lần nữa.
 *
 * <p>Chỉ nhận đường dẫn NỘI BỘ bắt đầu bằng một dấu `/` duy nhất. Không kiểm thì
 * `?tiep=https://trang-gia-mao` biến trang đăng nhập của homestay thành bàn đạp
 * chuyển hướng cho kẻ lừa đảo, và `?tiep=//trang-gia-mao` cũng ra ngoài dù nhìn
 * như đường dẫn nội bộ.
 *
 * <h2>Tài khoản là tuỳ chọn</h2>
 *
 * Khách vãng lai vẫn đặt được phòng và vẫn tra cứu được bằng mã đơn + số điện
 * thoại. Tài khoản chỉ thêm tiện lợi: xem lại mọi đơn ở một chỗ. Trang nói rõ
 * điều đó thay vì chặn đường người không muốn đăng ký.
 */
@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiInput, UiButton],
  template: `
    <div class="mx-auto max-w-md px-4 py-12">
      <h1 class="text-h1 font-bold text-text">
        {{ mode() === 'login' ? 'Đăng nhập' : 'Tạo tài khoản' }}
      </h1>
      <p class="mt-2 text-sm text-text-muted">
        Không bắt buộc có tài khoản để đặt phòng — bạn vẫn
        <a routerLink="/tra-cuu" class="font-semibold text-primary hover:underline">
          tra cứu đơn </a
        >bằng mã đơn và số điện thoại.
      </p>

      <div class="mt-6 flex gap-2" role="tablist" aria-label="Chọn đăng nhập hoặc đăng ký">
        @for (option of modes; track option.value) {
          <button
            type="button"
            role="tab"
            class="min-h-[var(--touch-min)] flex-1 rounded-md border px-3 text-sm font-semibold
                   transition-colors duration-[var(--dur-fast)]"
            [class.border-primary]="mode() === option.value"
            [class.bg-primary]="mode() === option.value"
            [class.text-text-invert]="mode() === option.value"
            [class.border-border]="mode() !== option.value"
            [class.text-text]="mode() !== option.value"
            [attr.aria-selected]="mode() === option.value"
            (click)="switchTo(option.value)">
            {{ option.label }}
          </button>
        }
      </div>

      <form class="mt-6 flex flex-col gap-3" (submit)="submit($event)">
        @if (mode() === 'register') {
          <ui-input
            label="Họ và tên"
            [required]="true"
            placeholder="Nguyễn Văn A"
            [error]="fullNameError()"
            [(value)]="fullName" />
          <ui-input
            label="Số điện thoại"
            type="tel"
            placeholder="09xxxxxxxx"
            hint="Không bắt buộc, nhưng giúp homestay liên hệ khi cần."
            [(value)]="phone" />
        }

        <ui-input
          label="Email"
          type="email"
          [required]="true"
          placeholder="ban@vidu.com"
          [error]="emailError()"
          [(value)]="email" />

        <ui-input
          label="Mật khẩu"
          type="password"
          [required]="true"
          [hint]="mode() === 'register' ? 'Ít nhất 8 ký tự.' : null"
          [error]="passwordError()"
          [(value)]="password" />

        @if (error()) {
          <p class="rounded-md bg-danger/10 p-3 text-sm font-semibold text-danger" role="alert">
            {{ error() }}
          </p>
        }

        <ui-button type="submit" [fullWidth]="true" [loading]="loading()">
          {{ mode() === 'login' ? 'Đăng nhập' : 'Tạo tài khoản' }}
        </ui-button>
      </form>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly modes = [
    { value: 'login' as const, label: 'Đăng nhập' },
    { value: 'register' as const, label: 'Đăng ký' },
  ];

  protected readonly mode = signal<Mode>('login');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly fullName = signal('');
  protected readonly phone = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly emailError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);
  protected readonly fullNameError = signal<string | null>(null);

  protected switchTo(mode: Mode): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  protected submit(event: Event): void {
    event.preventDefault();
    if (!this.validate()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const request =
      this.mode() === 'login'
        ? this.auth.login(this.email().trim(), this.password())
        : this.auth.register({
            email: this.email().trim(),
            password: this.password(),
            fullName: this.fullName().trim(),
            phone: this.phone().trim() || undefined,
          });

    request.subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl(this.safeRedirect());
      },
      error: (failure) => {
        this.loading.set(false);
        this.error.set(
          errorMessageOf(
            failure,
            this.mode() === 'login'
              ? 'Email hoặc mật khẩu không đúng.'
              : 'Không tạo được tài khoản. Email này có thể đã được dùng.',
          ),
        );
      },
    });
  }

  private validate(): boolean {
    const emailError = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email().trim())
      ? null
      : 'Email không hợp lệ.';
    const passwordError =
      this.password().length >= 8 ? null : 'Mật khẩu phải có ít nhất 8 ký tự.';
    const fullNameError =
      this.mode() === 'register' && !this.fullName().trim() ? 'Nhập họ và tên.' : null;

    this.emailError.set(emailError);
    this.passwordError.set(passwordError);
    this.fullNameError.set(fullNameError);
    return !emailError && !passwordError && !fullNameError;
  }

  /**
   * Đường dẫn quay lại, đã lọc.
   *
   * Chỉ chấp nhận một dấu `/` mở đầu và ký tự tiếp theo không phải `/` hay `\`.
   * `//kẻ-xấu.vn` và `/\kẻ-xấu.vn` đều được trình duyệt hiểu là địa chỉ tuyệt
   * đối, nên chúng phải rơi về trang chủ.
   */
  private safeRedirect(): string {
    const target = this.route.snapshot.queryParamMap.get('tiep') ?? '';
    return /^\/(?![/\\])/.test(target) ? target : '/';
  }
}
