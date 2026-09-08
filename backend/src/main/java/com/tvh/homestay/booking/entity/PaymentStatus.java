package com.tvh.homestay.booking.entity;

/** Khớp CHECK ck_bookings_pay_status trong V3. */
public enum PaymentStatus {
    UNPAID,
    PARTIAL,
    DEPOSIT_PAID,
    PAID,
    OVERPAID,
    REFUND_REQUIRED,
    REFUNDED
}
