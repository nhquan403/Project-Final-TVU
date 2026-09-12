import {
  BOOKING_ERROR_MESSAGES,
  type BookingErrorCode,
} from '../../../core/services/booking.service';

/** Hình dạng lỗi backend trả về: RFC 7807 kèm trường `code` tự thêm. */
interface ProblemDetail {
  code?: string;
  detail?: string;
}

interface HttpFailure {
  status?: number;
  error?: ProblemDetail | null;
}

/** Mã lỗi ổn định của backend, hoặc `null` khi lỗi không đến từ ứng dụng. */
export function errorCodeOf(failure: unknown): string | null {
  return (failure as HttpFailure)?.error?.code ?? null;
}

/**
 * Câu thông báo cho khách.
 *
 * <p>Ưu tiên MÃ lỗi, không đọc chuỗi `detail`: đổi câu chữ ở backend không được
 * phép làm hỏng màn hình, và `detail` viết cho lập trình viên đọc log chứ không
 * viết cho khách đang bối rối.
 *
 * <p>Mất mạng (status 0) được tách riêng vì lời khuyên khác hẳn: kiểm tra wifi,
 * chứ không phải thử lại sau.
 */
export function errorMessageOf(failure: unknown, fallback: string): string {
  const code = errorCodeOf(failure);
  if (code && code in BOOKING_ERROR_MESSAGES) {
    return BOOKING_ERROR_MESSAGES[code as BookingErrorCode];
  }
  if ((failure as HttpFailure)?.status === 0) {
    return 'Mất kết nối tới máy chủ. Kiểm tra mạng rồi thử lại.';
  }
  return fallback;
}
