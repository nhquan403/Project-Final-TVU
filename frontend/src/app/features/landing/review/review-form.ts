import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReviewService } from '../../../core/services/review.service';
import { UiButton, UiInput, UiStarRating } from '../../../shared/ui';
import { errorMessageOf } from '../shared/api-error';
import { BookingFlowStore } from '../booking/booking-flow.store';

/**
 * Gửi đánh giá cho một đơn đã trả phòng.
 *
 * <h2>Phải chứng minh mình là chủ đơn</h2>
 *
 * Mã đơn nằm trên URL, nên bản thân nó không chứng minh gì — ai có link cũng
 * có mã. Vì thế form đòi thêm số điện thoại đã đặt, trừ khi tab này còn giữ mã
 * truy cập của chính đơn đó (vừa đặt xong ở đây). Đây đúng là mức xác thực mà
 * tra cứu và huỷ đơn đang dùng, không phải một lớp kiểm mới.
 *
 * <p>Tên người đánh giá KHÔNG có trong form: backend chụp tên từ đơn. Cho client
 * gửi tên nghĩa là ai gửi được đánh giá cũng ký tên người khác.
 *
 * <p>Ràng buộc UNIQUE (booking_id) ở cơ sở dữ liệu đảm bảo mỗi đơn một đánh giá,
 * nên gửi lần hai bị từ chối ở tầng dưới cùng chứ không phải bằng một cú kiểm
 * tra ở đây có thể bị hai tab qua mặt.
 */
@Component({
  selector: 'app-review-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiStarRating, UiInput, UiButton],
  template: `
    <div class="mx-auto max-w-xl px-4 py-8">
      @if (sent()) {
        <div class="rounded-lg border border-border bg-surface p-6 text-center">
          <p class="text-4xl" aria-hidden="true">✓</p>
          <h1 class="mt-2 text-h2 font-bold text-text">Cảm ơn bạn đã đánh giá</h1>
          <p class="mt-2 text-text-muted">
            Đánh giá được homestay xem qua trước khi đăng công khai, thường trong một ngày làm việc.
          </p>
          <a
            routerLink="/"
            class="mt-6 inline-flex min-h-[var(--touch-min)] items-center justify-center rounded-md
                   bg-primary px-5 text-sm font-semibold text-text-invert hover:bg-primary-hover">
            Về trang chủ
          </a>
        </div>
      } @else {
        <h1 class="text-h1 font-bold text-text">Đánh giá chuyến nghỉ của bạn</h1>
        <p class="mt-2 text-text-muted">
          Đơn <strong class="font-mono">{{ code() }}</strong>
        </p>

        <form class="mt-6 flex flex-col gap-4" (submit)="submit($event)">
          @if (!hasToken()) {
            <ui-input
              label="Số điện thoại đã đặt"
              type="tel"
              [required]="true"
              placeholder="09xxxxxxxx"
              hint="Dùng để xác nhận bạn là người đã ở. Không hiển thị công khai."
              [error]="phoneError()"
              [(value)]="phone" />
          }

          <ui-star-rating
            label="Bạn chấm bao nhiêu điểm?"
            [readonly]="false"
            [error]="ratingError()"
            [(value)]="rating" />

          <ui-input
            label="Tiêu đề"
            placeholder="Ví dụ: Yên tĩnh, chủ nhà thân thiện"
            [(value)]="title" />

          <ui-input
            label="Cảm nhận của bạn"
            placeholder="Điều gì làm bạn hài lòng, điều gì nên cải thiện?"
            [(value)]="content" />

          @if (error()) {
            <p class="rounded-md bg-danger/10 p-3 text-sm font-semibold text-danger" role="alert">
              {{ error() }}
            </p>
          }

          <ui-button type="submit" [fullWidth]="true" [loading]="sending()">
            Gửi đánh giá
          </ui-button>

          <p class="text-xs text-text-muted">
            Đánh giá hiển thị kèm tên trên đơn đặt phòng của bạn, sau khi homestay duyệt.
          </p>
        </form>
      }
    </div>
  `,
})
export class ReviewFormPage {
  private readonly route = inject(ActivatedRoute);
  private readonly reviews = inject(ReviewService);
  private readonly store = inject(BookingFlowStore);

  protected readonly code = signal('');
  protected readonly phone = signal('');
  protected readonly rating = signal(0);
  protected readonly title = signal('');
  protected readonly content = signal('');
  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly phoneError = signal<string | null>(null);
  protected readonly ratingError = signal<string | null>(null);
  protected readonly hasToken = signal(false);

  private token: string | null = null;

  constructor() {
    const code = this.route.snapshot.paramMap.get('code') ?? '';
    this.code.set(code);
    this.token = this.store.accessTokenFor(code);
    this.hasToken.set(this.token !== null);
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.phoneError.set(null);
    this.ratingError.set(null);
    this.error.set(null);

    if (this.rating() < 1) {
      this.ratingError.set('Chọn số sao trước khi gửi.');
      return;
    }
    if (!this.token && !this.phone().trim()) {
      this.phoneError.set('Nhập số điện thoại đã dùng khi đặt phòng.');
      return;
    }

    this.sending.set(true);
    this.reviews
      .submit(this.code(), {
        token: this.token,
        phone: this.token ? null : this.phone().trim(),
        rating: this.rating(),
        title: this.title().trim() || null,
        content: this.content().trim() || null,
      })
      .subscribe({
        next: () => {
          this.sending.set(false);
          this.sent.set(true);
        },
        error: (failure) => {
          this.sending.set(false);
          this.error.set(
            errorMessageOf(
              failure,
              'Không gửi được đánh giá. Có thể đơn này đã có đánh giá, hoặc chuyến đi chưa kết thúc.',
            ),
          );
        },
      });
  }
}
