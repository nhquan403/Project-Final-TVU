package com.tvh.homestay.payment.entity;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.common.BaseCreatedEntity;
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
 * Hộp thư đi.
 *
 * <p>Email được xếp hàng ở đây trong cùng transaction với nghiệp vụ, rồi một
 * bộ quét riêng mới gửi đi. Gửi thẳng trong transaction thì hoặc mất thư khi
 * rollback, hoặc gửi thư xác nhận cho một đơn rốt cuộc không tồn tại. Bảng này
 * đồng thời là bằng chứng đã gửi khi khách bảo không nhận được.
 */
@Entity
@Table(name = "outbound_emails")
@Getter
@Setter
public class OutboundEmail extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false, length = 100)
    private String template;

    @Column(name = "to_email", nullable = false, length = 255)
    private String toEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboundEmailStatus status = OutboundEmailStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
