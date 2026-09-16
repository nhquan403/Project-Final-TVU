import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiButton, UiInput } from '../../../shared/ui';

/**
 * Buộc đổi mật khẩu trước khi dùng khu quản trị.
 *
 * <h2>Vì sao màn hình này tồn tại</h2>
 *
 * Tài khoản quản trị dựng sẵn nhận một mật khẩu tạm đi qua tay người khác (log
 * container, tin nhắn). {@code MustChangePasswordFilter} ở backend trả 403 cho
 * MỌI đường trừ ba đường tối thiểu, nên không có màn hình này thì quản trị viên
 * đăng nhập được, nhìn thấy dashboard, và mọi ô dữ liệu đều trống — không lỗi
 * rõ ràng, không lối thoát.
 *
 * <h2>Vì sao phải đăng nhập lại sau khi đổi</h2>
 *
 * Backend thu hồi mọi phiên và tăng {@code token_version} khi mật khẩu đổi, nên
 * access token đang giữ trong bộ nhớ chết ngay lúc đó. Màn hình này tự đăng
 * nhập lại bằng mật khẩu VỪA NHẬP — người dùng không phải gõ lại — rồi mới đi
 * tiếp vào dashboard. Bỏ bước đó thì trang kế tiếp sẽ nhận 401 và đá người dùng
 * về màn hình đăng nhập ngay sau khi họ vừa làm đúng mọi thứ.
 */
@Component({
  selector: 'app-admin-change-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInput, UiButton],
  template: `
    <div class="mx-auto max-w-md px-4 py-10">
      <h1 class="text-xl font-bold text-text">Đổi mật khẩu</h1>
      <p class="mt-2 text-sm text-text-muted">
        Tài khoản này đang dùng mật khẩu tạm. Đổi mật khẩu để tiếp tục — mật khẩu
        tạm đã đi qua log và tin nhắn nên không còn là bí mật.
      </p>

      <form class="mt-6 flex flex-col gap-3" (submit)="submit($event)">
        <ui-input
          label="Mật khẩu hiện tại"
          type="password"
          [required]="true"
          [error]="currentError()"
          [(value)]="currentPassword" />

        <ui-input
          label="Mật khẩu mới"
          type="password"
          [required]="true"
          hint="Ít nhất 8 ký tự."
          [error]="newError()"
          [(value)]="newPassword" />

        <ui-input
          label="Nhập lại mật khẩu mới"
          type="password"
          [required]="true"
          [error]="confirmError()"
          [(value)]="confirmPassword" />

        @if (error()) {
          <p class="rounded-md bg-danger/10 p-3 text-sm font-semibold text-danger" role="alert">
            {{ error() }}
          </p>
        }

        <ui-button type="submit" [fullWidth]="true" [loading]="saving()">
          Đổi mật khẩu và vào quản trị
        </ui-button>
      </form>
    </div>
  `,
})
export class AdminChangePassword {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly currentPassword = signal('');
  protected readonly newPassword = signal('');
  protected readonly confirmPassword = signal('');
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly currentError = signal<string | null>(null);
  protected readonly newError = signal<string | null>(null);
  protected readonly confirmError = signal<string | null>(null);

  protected submit(event: Event): void {
    event.preventDefault();
    if (!this.validate()) {
      return;
    }

    // Email phải đọc TRƯỚC khi đổi: changePassword xoá trạng thái phiên phía
    // client, nên sau lời gọi đó currentUser() là null.
    const email = this.auth.currentUser()?.email ?? '';
    const password = this.newPassword();

    this.saving.set(true);
    this.error.set(null);

    this.auth.changePassword(this.currentPassword(), password).subscribe({
      next: () => {
        this.auth.login(email, password).subscribe({
          next: () => {
            this.saving.set(false);
            void this.router.navigate(['/admin']);
          },
          error: () => {
            // Mật khẩu ĐÃ đổi thành công, chỉ bước đăng nhập lại hỏng. Nói rõ
            // điều đó: bảo người dùng "thử lại" ở đây sẽ khiến họ nhập mật khẩu
            // cũ vào ô "hiện tại" và thất bại mãi.
            this.saving.set(false);
            void this.router.navigate(['/admin/login'], {
              queryParams: { doimatkhau: 'xong' },
            });
          },
        });
      },
      error: (failure: { status?: number }) => {
        this.saving.set(false);
        if (failure?.status === 400 || failure?.status === 401) {
          this.currentError.set('Mật khẩu hiện tại không đúng.');
        } else {
          this.error.set('Không đổi được mật khẩu. Thử lại sau giây lát.');
        }
      },
    });
  }

  private validate(): boolean {
    const current = this.currentPassword() ? null : 'Nhập mật khẩu hiện tại.';
    const next = this.newPassword().length >= 8 ? null : 'Mật khẩu mới phải có ít nhất 8 ký tự.';
    const confirm =
      this.newPassword() === this.confirmPassword() ? null : 'Hai lần nhập không khớp.';

    this.currentError.set(current);
    this.newError.set(next);
    this.confirmError.set(confirm);
    return !current && !next && !confirm;
  }
}
