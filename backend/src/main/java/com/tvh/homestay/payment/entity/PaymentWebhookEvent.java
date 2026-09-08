package com.tvh.homestay.payment.entity;

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
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Nhật ký mọi webhook nhận được, kể cả cái không khớp đơn nào.
 *
 * <p>Khoá chống xử lý trùng là {@code (provider, external_id)} chứ không phải
 * {@code providerTxnId}: nhà cung cấp có thể gửi cùng một mã tham chiếu cho
 * các sự kiện khác nhau, nên đặt UNIQUE ở đó sẽ làm rơi webhook hợp lệ.
 */
@Entity
@Table(name = "payment_webhook_events")
@Getter
@Setter
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    /** NULL khi webhook không khớp lần thanh toán nào. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /** Giữ nguyên nội dung gốc để đối soát lại được khi có tranh chấp. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_result", nullable = false, length = 20)
    private WebhookProcessingResult processingResult;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
}
