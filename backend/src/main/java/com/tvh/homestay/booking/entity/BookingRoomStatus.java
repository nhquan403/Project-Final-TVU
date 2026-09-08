package com.tvh.homestay.booking.entity;

/**
 * Khớp CHECK ck_booking_rooms_status trong V3.
 *
 * <p>Ràng buộc EXCLUDE chống trùng lịch chỉ soi các dòng {@code ACTIVE}. Dùng
 * enum thay vì chuỗi thô ở tầng Java là lớp phòng thủ thứ hai cho đúng chỗ
 * nguy hiểm nhất của schema.
 */
public enum BookingRoomStatus {
    ACTIVE,
    /** Đơn huỷ hoặc hết hạn — phòng mở lại ngay, lịch sử vẫn còn. */
    RELEASED
}
