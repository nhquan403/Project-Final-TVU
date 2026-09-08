package com.tvh.homestay.payment.entity;

/** Khớp CHECK ck_payments_provider trong V4. */
public enum PaymentProvider {
    SEPAY,
    /** Admin ghi nhận tay: khách trả tiền mặt hoặc chuyển khoản ngoài luồng. */
    MANUAL
}
