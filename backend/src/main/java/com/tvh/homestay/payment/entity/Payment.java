package com.tvh.homestay.payment.entity;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.common.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Một lần thanh toán. Một đơn có thể có NHIỀU dòng ở đây: khách chuyển hai
 * lần, chuyển thiếu rồi bù, hoặc đặt lại sau khi hết hạn giữ chỗ.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "amount_expected", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountExpected;

    /** CỘNG DỒN qua nhiều lần chuyển khoản, không phải ghi đè. */
    @Column(name = "amount_received", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountReceived = BigDecimal.ZERO;

    @Column(name = "qr_content", columnDefinition = "text")
    private String qrContent;

    @Column(name = "qr_image_url", length = 500)
    private String qrImageUrl;

    /**
     * {@code code} của đơn cộng hai chữ số {@code attemptNo}, ví dụ
     * {@code TVH8F3K2Q01}. Nhờ hậu tố này mà QR của lần trước không khớp nhầm
     * vào lần sau — đó cũng là lý do cột này UNIQUE còn
     * {@code providerTxnId} thì không.
     */
    @Column(name = "transfer_content", nullable = false, length = 50)
    private String transferContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status = PaymentAttemptStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconcile_status", nullable = false, length = 20)
    private ReconcileStatus reconcileStatus = ReconcileStatus.NONE;

    @Column(name = "provider_txn_id", length = 100)
    private String providerTxnId;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
