package com.tvh.homestay.promotion.entity;

/** Khớp CHECK ck_promotions_type trong V5. */
public enum DiscountType {
    /** Giảm theo phần trăm; có thể kèm trần qua maxDiscountAmount. */
    PERCENT,
    FIXED
}
