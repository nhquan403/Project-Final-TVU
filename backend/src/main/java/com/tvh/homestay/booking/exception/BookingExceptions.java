package com.tvh.homestay.booking.exception;

/**
 * Ngoại lệ nghiệp vụ của luồng đặt phòng.
 *
 * <p>Mỗi ngoại lệ mang một MÃ LỖI ỔN ĐỊNH. Frontend hiển thị thông báo tiếng
 * Việt theo mã, không đọc chuỗi mô tả — đổi câu chữ ở backend không được phép
 * làm hỏng giao diện.
 */
public final class BookingExceptions {

    private BookingExceptions() {}

    /** Gốc chung, mang mã lỗi ổn định. */
    public abstract static sealed class BookingException extends RuntimeException
            permits RoomNotAvailable, BookingNotFound, InvalidAccessToken,
                    InvalidStateTransition, InvalidPromotion, PromotionExhausted, InvalidBookingRequest {

        private final String code;

        protected BookingException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /** 409 — không đủ phòng trống cho khoảng ngày và số lượng yêu cầu. */
    public static final class RoomNotAvailable extends BookingException {
        public RoomNotAvailable() {
            super("ROOM_NOT_AVAILABLE", "Không còn đủ phòng trống cho khoảng ngày này.");
        }
    }

    /**
     * 404 — không tìm thấy đơn.
     *
     * <p>Dùng CHUNG cho cả "không có mã này" lẫn "sai số điện thoại". Phân biệt
     * hai trường hợp là cho người dò biết mã nào có thật, và từ đó dò tiếp số
     * điện thoại của khách.
     */
    public static final class BookingNotFound extends BookingException {
        public BookingNotFound() {
            super("BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt phòng.");
        }
    }

    /** 401 — thiếu hoặc sai access token của đơn. */
    public static final class InvalidAccessToken extends BookingException {
        public InvalidAccessToken() {
            super("INVALID_ACCESS_TOKEN", "Thiếu hoặc sai mã truy cập của đơn đặt phòng.");
        }
    }

    /** 409 — chuyển trạng thái không hợp lệ theo máy trạng thái. */
    public static final class InvalidStateTransition extends BookingException {
        public InvalidStateTransition(String from, String to) {
            super("INVALID_STATE_TRANSITION",
                    "Không thể chuyển đơn từ " + from + " sang " + to + ".");
        }
    }

    /** 400 — mã khuyến mãi không tồn tại, hết hiệu lực, hoặc chưa đủ điều kiện. */
    public static final class InvalidPromotion extends BookingException {
        public InvalidPromotion(String reason) {
            super("INVALID_PROMOTION", reason);
        }
    }

    /** 409 — mã khuyến mãi đã dùng hết lượt. */
    public static final class PromotionExhausted extends BookingException {
        public PromotionExhausted() {
            super("PROMOTION_EXHAUSTED", "Mã khuyến mãi đã hết lượt sử dụng.");
        }
    }

    /** 400 — dữ liệu đặt phòng không hợp lệ (ngày, sức chứa, số đêm). */
    public static final class InvalidBookingRequest extends BookingException {
        public InvalidBookingRequest(String reason) {
            super("INVALID_BOOKING_REQUEST", reason);
        }
    }
}
