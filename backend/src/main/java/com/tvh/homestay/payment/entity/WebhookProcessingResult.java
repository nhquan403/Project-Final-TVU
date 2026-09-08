package com.tvh.homestay.payment.entity;

/** Khớp CHECK ck_pwe_result trong V4. */
public enum WebhookProcessingResult {
    MATCHED,
    /** Không tìm được lần thanh toán nào khớp nội dung chuyển khoản. */
    UNMATCHED,
    /** Tiền về sau khi đơn đã hết hạn giữ chỗ — phải hoàn lại. */
    LATE,
    DUPLICATE,
    ERROR
}
