import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { BookingStatus } from '../../shared/ui';

export interface CreateBookingRequest {
  roomTypeId: number;
  checkIn: string;
  checkOut: string;
  roomQuantity: number;
  adults: number;
  children: number;
  guestName: string;
  guestEmail: string;
  guestPhone: string;
  specialRequest?: string | null;
  promotionCode?: string | null;
}

export interface BookingRoomView {
  roomId: number;
  roomNumber: string;
  checkIn: string;
  checkOut: string;
}

/**
 * Thông tin để khách trả tiền.
 *
 * Backend gom thành một object thay vì rải phẳng ở gốc `Booking`: màn hình QR
 * cần cả bốn trường cùng lúc, và gom lại thì thêm trường mới (lần thử thứ hai,
 * hoàn tiền) không phải nong thêm gốc của phản hồi.
 *
 * `qrImageUrl` có thể là `null` khi bản triển khai chưa cấu hình tài khoản
 * nhận tiền — màn hình phải hiện phần chuyển khoản thủ công thay vì một ảnh
 * hỏng.
 */
export interface PaymentView {
  transferContent: string;
  qrImageUrl: string | null;
  accountNumber: string;
  bankCode: string;
  amount: number;
  expiresAt: string | null;
}

export interface Booking {
  code: string;
  /** Bí mật thao tác. Chỉ có mặt ngay sau khi tạo đơn hoặc tra cứu thành công. */
  accessToken: string | null;
  status: BookingStatus;
  paymentStatus: string;
  roomTypeName: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  roomQuantity: number;
  adults: number;
  children: number;
  guestName: string;
  guestPhone: string;
  pricePerNight: number;
  subtotalAmount: number;
  discountAmount: number;
  totalAmount: number;
  depositAmount: number;
  payment: PaymentView | null;
  holdExpiresAt: string | null;
  rooms: BookingRoomView[];
}

export interface PaymentStatus {
  code: string;
  status: BookingStatus;
  paymentStatus: string;
  amountExpected: number;
  amountReceived: number;
  holdExpiresAt: string | null;
}

/**
 * Mã lỗi ổn định của backend.
 *
 * <p>Giao diện hiển thị thông báo theo MÃ, không đọc chuỗi `detail`: đổi câu
 * chữ ở backend không được phép làm hỏng màn hình.
 */
export type BookingErrorCode =
  | 'ROOM_NOT_AVAILABLE'
  | 'INVALID_PROMOTION'
  | 'PROMOTION_EXHAUSTED'
  | 'BOOKING_NOT_FOUND'
  | 'INVALID_ACCESS_TOKEN'
  | 'INVALID_STATE_TRANSITION'
  | 'INVALID_BOOKING_REQUEST'
  | 'TOO_MANY_REQUESTS';

export const BOOKING_ERROR_MESSAGES: Record<BookingErrorCode, string> = {
  ROOM_NOT_AVAILABLE: 'Rất tiếc, phòng vừa được đặt hết cho khoảng ngày này.',
  INVALID_PROMOTION: 'Mã khuyến mãi không hợp lệ hoặc chưa đủ điều kiện.',
  PROMOTION_EXHAUSTED: 'Mã khuyến mãi đã hết lượt sử dụng.',
  BOOKING_NOT_FOUND: 'Không tìm thấy đơn đặt phòng. Kiểm tra lại mã và số điện thoại.',
  INVALID_ACCESS_TOKEN: 'Liên kết không hợp lệ. Hãy tra cứu lại đơn của bạn.',
  INVALID_STATE_TRANSITION: 'Không thể thực hiện thao tác này với trạng thái hiện tại của đơn.',
  INVALID_BOOKING_REQUEST: 'Thông tin đặt phòng chưa hợp lệ.',
  TOO_MANY_REQUESTS: 'Bạn thao tác hơi nhanh. Thử lại sau ít phút.',
};

/** Tạo, tra cứu, huỷ đơn và theo dõi thanh toán. */
@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);

  create(request: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings', request);
  }

  /** Tra cứu bằng mã và số điện thoại. Sai một trong hai đều trả BOOKING_NOT_FOUND. */
  lookup(code: string, phone: string): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings/lookup', { code, phone });
  }

  cancel(code: string, options: { token?: string; phone?: string; reason?: string }): Observable<Booking> {
    const params = options.token ? new HttpParams().set('token', options.token) : undefined;
    return this.http.post<Booking>(
      `/api/bookings/${encodeURIComponent(code)}/cancel`,
      { phone: options.phone ?? null, reason: options.reason ?? null },
      { params },
    );
  }

  /** Màn hình QR hỏi liên tục cho tới khi tiền về. Bắt buộc kèm access token. */
  paymentStatus(code: string, token: string): Observable<PaymentStatus> {
    return this.http.get<PaymentStatus>(
      `/api/bookings/${encodeURIComponent(code)}/payment-status`,
      { params: new HttpParams().set('token', token) },
    );
  }

  /** Đơn của tài khoản đang đăng nhập. Backend lọc theo token, không theo tham số. */
  myBookings(page = 0, size = 10): Observable<{ content: Booking[]; totalElements: number }> {
    return this.http.get<{ content: Booking[]; totalElements: number }>('/api/me/bookings', {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }
}
