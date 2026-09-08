package com.tvh.homestay.booking.entity;

import com.tvh.homestay.common.BaseAuditEntity;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.user.entity.User;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Đơn đặt phòng.
 *
 * <p>Khoảng ngày dùng quy ước NỬA MỞ {@code [checkIn, checkOut)} — cùng quy
 * ước với ràng buộc {@code EXCLUDE} trên {@code daterange} ở tầng cơ sở dữ
 * liệu và với {@code night-count} pipe ở frontend. Nhận 10/03 trả 12/03 là
 * <b>2 đêm</b>, và phòng trống lại từ sáng 12/03.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
public class Booking extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã hiển thị cho khách, đọc qua điện thoại được. Không cấp quyền gì. */
    @Column(nullable = false, length = 20)
    private String code;

    /**
     * Bí mật thao tác, 32 ký tự hex từ SecureRandom. Khách vãng lai không có
     * tài khoản vẫn xem và huỷ được đơn qua link chứa token này.
     *
     * <p>Tách khỏi {@code code} là chủ ý: {@code code} in trên email và đọc
     * qua điện thoại nên phải ngắn và đoán được — thứ đoán được thì không
     * được phép cấp quyền.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "access_token", nullable = false, length = 32)
    private String accessToken;

    /** NULL = khách vãng lai, không có tài khoản. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 255)
    private String guestEmail;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int adults;

    @Column(nullable = false)
    private int children = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    /** Ảnh chụp lúc đặt: loại phòng đổi tên hay đổi giá về sau không được làm đổi đơn đã chốt. */
    @Column(name = "room_type_name_snapshot", nullable = false, length = 150)
    private String roomTypeNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    /**
     * Phải luôn bằng số dòng {@code booking_rooms} có trạng thái ACTIVE.
     * Bất biến này được hai constraint trigger ép ở tầng cơ sở dữ liệu, một
     * cho mỗi phía — sửa cột này mà không gán đủ phòng sẽ hỏng lúc commit.
     */
    @Column(name = "room_quantity", nullable = false)
    private int roomQuantity = 1;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "special_request", columnDefinition = "text")
    private String specialRequest;

    /** Hết hạn mà chưa thanh toán thì đơn thành EXPIRED và phòng được trả lại. */
    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    /** Kiểu inet của Postgres, dùng để truy vết lạm dụng. */
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;
}
