package com.tvh.homestay.review.entity;

import com.tvh.homestay.booking.entity.Booking;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/** Một đơn một đánh giá — có UNIQUE trên booking_id ép điều đó. */
@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** NULL = khách vãng lai. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_name_snapshot", nullable = false, length = 150)
    private String guestNameSnapshot;

    @Column(nullable = false)
    private short rating;

    /** VĂN BẢN THUẦN, không HTML — nơi an toàn nhất để chặn XSS là không nhận HTML vào. */
    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "admin_reply", columnDefinition = "text")
    private String adminReply;

    @Column(name = "replied_at")
    private OffsetDateTime repliedAt;
}
