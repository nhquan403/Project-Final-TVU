package com.tvh.homestay.booking.entity;

import com.tvh.homestay.common.BaseCreatedEntity;
import com.tvh.homestay.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Ghi chú nội bộ của quản trị viên trên một đơn.
 *
 * <p>Tách khỏi {@link BookingStatusHistory} vì mỗi dòng lịch sử bắt buộc kèm
 * một lần chuyển trạng thái thật; nhét ghi chú vào đó là bịa ra một lần chuyển
 * trạng thái không xảy ra. Xem javadoc trong migration V7.
 *
 * <p>Đây là dữ liệu NỘI BỘ — không endpoint công khai nào đọc bảng này.
 */
@Entity
@Table(name = "booking_notes")
@Getter
@Setter
public class BookingNote extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** NULL khi tài khoản viết ghi chú về sau bị xoá. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String content;
}
