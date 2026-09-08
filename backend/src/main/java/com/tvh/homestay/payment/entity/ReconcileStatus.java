package com.tvh.homestay.payment.entity;

/** Khớp CHECK ck_payments_reconcile trong V4. */
public enum ReconcileStatus {
    /** Không cần ai xử lý. */
    NONE,
    NEEDS_REVIEW,
    REFUND_REQUIRED,
    RESOLVED
}
