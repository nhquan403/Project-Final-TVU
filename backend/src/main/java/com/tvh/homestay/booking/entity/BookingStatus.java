package com.tvh.homestay.booking.entity;

/**
 * Tám trạng thái đơn đặt phòng. Tập này là HỢP ĐỒNG giữa ba nơi:
 * CHECK {@code ck_bookings_status} trong V3, enum này, và bảng màu trạng thái
 * của {@code ui-status-badge} ở frontend. Thêm một giá trị mà quên một trong
 * ba nơi là hiển thị ra ô trống, không lỗi, không ai biết.
 */
public enum BookingStatus {
    /** Đã giữ chỗ, đang chờ khách chuyển cọc. */
    PENDING_PAYMENT,
    CONFIRMED,
    /** Đã nhận tiền nhưng số tiền lệch — cần người đối soát. */
    AWAITING_REVIEW,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    /** Hết hạn giữ chỗ mà chưa thanh toán. */
    EXPIRED,
    NO_SHOW
}
