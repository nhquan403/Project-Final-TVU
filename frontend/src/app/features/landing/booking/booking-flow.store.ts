import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import type { Booking } from '../../../core/services/booking.service';

/** Tham số tìm kiếm — thứ DUY NHẤT được phép đi lên URL. */
export interface SearchParams {
  checkIn: string;
  checkOut: string;
  adults: number;
  children: number;
  roomTypeId: number | null;
  roomQuantity: number;
}

/** Thông tin khách — KHÔNG BAO GIỜ đi lên URL. */
export interface GuestDraft {
  guestName: string;
  guestEmail: string;
  guestPhone: string;
  specialRequest: string;
  promotionCode: string;
}

const GUEST_KEY = 'tvh.booking.guest';
const ACCESS_TOKEN_KEY = 'tvh.booking.tokens';

const EMPTY_GUEST: GuestDraft = {
  guestName: '',
  guestEmail: '',
  guestPhone: '',
  specialRequest: '',
  promotionCode: '',
};

/**
 * Trạng thái của luồng đặt phòng ba bước.
 *
 * <h2>Vì sao thông tin khách KHÔNG lên URL</h2>
 *
 * Đẩy họ tên, email và số điện thoại vào query param để "F5 không mất dữ liệu"
 * là đổi một tiện lợi nhỏ lấy một rò rỉ lớn. URL đi vào lịch sử trình duyệt
 * trên máy dùng chung, vào access log của nginx, và vào header `Referer` gửi
 * kèm mọi tài nguyên bên thứ ba mà trang nạp — kể cả bản đồ nhúng ở trang chủ.
 * Khách gửi cho bạn bè đường link "phòng này đẹp nè" là gửi luôn số điện thoại
 * của mình.
 *
 * `sessionStorage` giữ được ĐÚNG lợi ích đó (F5 không mất) mà không có nhược
 * điểm nào ở trên: nó không rời khỏi tab, không đi vào log, không gắn vào link
 * chia sẻ được.
 *
 * <h2>Mã truy cập đơn</h2>
 *
 * Sau khi tạo đơn, `accessToken` được cất theo mã đơn. Màn hình QR cần nó sau
 * khi khách chuyển sang ứng dụng ngân hàng rồi quay lại — F5 ở đó là chuyện
 * bình thường, không phải ngoại lệ.
 */
@Injectable({ providedIn: 'root' })
export class BookingFlowStore {
  private readonly router = inject(Router);

  private readonly search = signal<SearchParams>(defaultSearch());
  private readonly guest = signal<GuestDraft>(readGuest());
  private readonly created = signal<Booking | null>(null);

  readonly params = this.search.asReadonly();
  readonly guestDraft = this.guest.asReadonly();
  readonly createdBooking = this.created.asReadonly();

  readonly nights = computed(() => {
    const { checkIn, checkOut } = this.search();
    if (!checkIn || !checkOut) {
      return 0;
    }
    const ms = new Date(checkOut).getTime() - new Date(checkIn).getTime();
    return Math.max(0, Math.round(ms / 86_400_000));
  });

  readonly hasDates = computed(() => this.nights() > 0);

  /**
   * Bước XA NHẤT mà dữ liệu hiện có cho phép hiển thị.
   *
   * Chưa có ngày thì không có gì để hỏi phòng trống; chưa chọn loại phòng thì
   * màn hình xác nhận không có gì để xác nhận.
   */
  private readonly maxStep = computed(() => {
    if (!this.hasDates()) {
      return 1;
    }
    return this.search().roomTypeId === null ? 2 : 3;
  });

  private readonly requestedStep = signal(1);

  /**
   * Bước đang hiển thị.
   *
   * <p>Suy ra từ dữ liệu CHẶN TRÊN, còn khách chọn bước trong phạm vi đó. Chỉ
   * suy từ dữ liệu thì không quay lại được: đang ở bước 3 mà bấm "Chọn ngày"
   * sẽ không đi đâu cả, vì ngày vẫn còn đó nên công thức vẫn trả về 3. Chỉ giữ
   * một biến bước thì F5 giữa chừng làm biến đó lệch khỏi dữ liệu thật. Lấy
   * `min` của hai thứ giữ được cả hai tính chất.
   */
  readonly step = computed(() => Math.min(this.requestedStep(), this.maxStep()));

  patchSearch(patch: Partial<SearchParams>): void {
    this.search.update((current) => ({ ...current, ...patch }));
  }

  /** Đi tới một bước. Tiến chỉ được khi dữ liệu đủ; lùi thì luôn được. */
  goToStep(step: number): void {
    if (step <= 2) {
      // Quay lại chỗ chọn phòng nghĩa là chọn LẠI: bỏ loại phòng cũ, nếu không
      // màn hình xác nhận vẫn giữ lựa chọn mà khách vừa nói là muốn đổi.
      this.patchSearch({ roomTypeId: null });
    }
    // Ngày được giữ nguyên kể cả khi về bước 1: khách quay lại để ĐỔI ngày,
    // không phải để gõ lại từ đầu.
    this.requestedStep.set(Math.max(1, step));
    this.syncUrl();
  }

  patchGuest(patch: Partial<GuestDraft>): void {
    const next = { ...this.guest(), ...patch };
    this.guest.set(next);
    writeSession(GUEST_KEY, next);
  }

  clearGuest(): void {
    this.guest.set({ ...EMPTY_GUEST });
    removeSession(GUEST_KEY);
  }

  /**
   * Ghi nhớ đơn vừa tạo để màn hình thanh toán dựng lại được sau khi tải lại
   * trang.
   *
   * <p>Khách bấm sang ứng dụng ngân hàng rồi quay lại, hoặc bấm F5 vì sốt ruột
   * — cả hai đều làm mất signal trong bộ nhớ. Không cất lại thì màn hình QR
   * trắng trơn đúng lúc khách đang cầm tiền.
   *
   * <p>Cất trong `sessionStorage`, KHÔNG phải URL: đơn có tên và số điện thoại
   * của khách. `sessionStorage` không rời khỏi tab, không vào log máy chủ,
   * không dán được cho người khác. Backend vẫn là nơi giữ sự thật — màn hình QR
   * hỏi lại trạng thái thanh toán mỗi ba giây, nên bản cache này chỉ dùng để vẽ
   * lại thông tin chuyển khoản chứ không quyết định đơn đã trả tiền hay chưa.
   */
  setCreatedBooking(booking: Booking): void {
    this.created.set(booking);
    if (booking.accessToken) {
      this.rememberAccessToken(booking.code, booking.accessToken);
    }
    writeSession(bookingKey(booking.code), booking);
  }

  /** Đơn đã cất ở tab này, hoặc `null` khi mở từ tab/máy khác. */
  bookingFor(code: string): Booking | null {
    const cached = this.created();
    if (cached?.code === code) {
      return cached;
    }
    const stored = readSession<Partial<Booking>>(bookingKey(code));
    return stored.code === code ? (stored as Booking) : null;
  }

  /** Đọc tham số tìm kiếm TỪ URL khi vào trang — điều kiện để link chia sẻ được. */
  hydrateFromUrl(query: Record<string, string | undefined>): void {
    const current = this.search();
    this.search.set({
      checkIn: query['checkIn'] ?? current.checkIn,
      checkOut: query['checkOut'] ?? current.checkOut,
      adults: toInt(query['adults'], current.adults),
      children: toInt(query['children'], current.children),
      roomTypeId: query['roomTypeId'] ? Number(query['roomTypeId']) : null,
      roomQuantity: toInt(query['roomQuantity'], current.roomQuantity),
    });
    // Link chia sẻ đã kèm loại phòng thì mở thẳng bước xác nhận, đừng bắt khách
    // bấm lại đúng lựa chọn mà chính đường link đã nói ra.
    this.requestedStep.set(this.maxStep());
  }

  /** Đồng bộ CHỈ tham số tìm kiếm lên URL. */
  syncUrl(): void {
    const { checkIn, checkOut, adults, children, roomTypeId, roomQuantity } = this.search();
    void this.router.navigate([], {
      queryParams: {
        checkIn: checkIn || null,
        checkOut: checkOut || null,
        adults,
        children: children || null,
        roomTypeId,
        roomQuantity: roomQuantity > 1 ? roomQuantity : null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  rememberAccessToken(code: string, token: string): void {
    const tokens = readTokens();
    tokens[code] = token;
    writeSession(ACCESS_TOKEN_KEY, tokens);
  }

  accessTokenFor(code: string): string | null {
    return readTokens()[code] ?? null;
  }
}

function defaultSearch(): SearchParams {
  return {
    checkIn: '',
    checkOut: '',
    // Mặc định hai người lớn: cấu hình phổ biến nhất, và là mặc định của mọi
    // trang đặt phòng mà khách đã quen.
    adults: 2,
    children: 0,
    roomTypeId: null,
    roomQuantity: 1,
  };
}

function toInt(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.trunc(parsed) : fallback;
}

function readGuest(): GuestDraft {
  return { ...EMPTY_GUEST, ...readSession<Partial<GuestDraft>>(GUEST_KEY) };
}

function bookingKey(code: string): string {
  return `tvh.booking.order.${code}`;
}

function readTokens(): Record<string, string> {
  return readSession<Record<string, string>>(ACCESS_TOKEN_KEY);
}

/**
 * `sessionStorage` có thể ném: chế độ riêng tư của một số trình duyệt, hoặc
 * người dùng chặn lưu trữ. Luồng đặt phòng phải chạy được kể cả khi đó — mất
 * tiện lợi "F5 không mất dữ liệu" thì chấp nhận, chứ không được vỡ trang.
 */
function readSession<T>(key: string): T {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : ({} as T);
  } catch {
    return {} as T;
  }
}

function writeSession(key: string, value: unknown): void {
  try {
    sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Không làm gì: dữ liệu vẫn nằm trong signal cho tới khi rời tab.
  }
}

function removeSession(key: string): void {
  try {
    sessionStorage.removeItem(key);
  } catch {
    // như trên
  }
}
