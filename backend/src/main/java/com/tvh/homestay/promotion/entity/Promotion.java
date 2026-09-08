package com.tvh.homestay.promotion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** NULL = không có trần. Chỉ có ý nghĩa với {@link DiscountType#PERCENT}. */
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_nights", nullable = false)
    private int minNights = 1;

    @Column(name = "min_total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minTotalAmount = BigDecimal.ZERO;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    /** NULL = không giới hạn lượt dùng. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /**
     * Trần lượt dùng được ép bằng CHECK ở tầng cơ sở dữ liệu, không phó mặc
     * cho tầng service: hai yêu cầu song song cùng đọc thấy còn lượt thì cả
     * hai đều tăng, và mã bị dùng quá số lần cho phép.
     */
    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(nullable = false)
    private boolean active = true;
}
