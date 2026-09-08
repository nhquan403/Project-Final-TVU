package com.tvh.homestay.payment.entity;

/** Khớp CHECK ck_payments_status trong V4. Khác với trạng thái thanh toán của cả đơn. */
public enum PaymentAttemptStatus {
    PENDING,
    PARTIAL,
    SUCCEEDED,
    OVERPAID,
    EXPIRED,
    FAILED
}
