package com.tvh.homestay.booking.entity;

import com.tvh.homestay.common.BaseCreatedEntity;
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
import lombok.Getter;
import lombok.Setter;

/** Nhật ký chuyển trạng thái đơn. Chỉ ghi thêm, không bao giờ sửa. */
@Entity
@Table(name = "booking_status_history")
@Getter
@Setter
public class BookingStatusHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** NULL ở dòng đầu tiên: đơn vừa tạo, chưa có trạng thái trước đó. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BookingStatus toStatus;

    /** NULL khi người thực hiện là hệ thống. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HistoryActor actor;

    @Column(columnDefinition = "text")
    private String note;
}
