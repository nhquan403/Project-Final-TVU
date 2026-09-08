package com.tvh.homestay.booking.entity;

/** Khớp CHECK ck_bsh_actor trong V3. */
public enum HistoryActor {
    GUEST,
    CUSTOMER,
    ADMIN,
    /** Bộ quét tự động: hết hạn giữ chỗ, webhook thanh toán. */
    SYSTEM
}
