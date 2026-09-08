package com.tvh.homestay.booking.entity;

import com.tvh.homestay.room.entity.Room;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Gán một phòng vật lý cho một đơn, trong một khoảng ngày.
 *
 * <p>Đây là bảng mang ràng buộc quan trọng nhất của cả hệ thống:
 * {@code EXCLUDE USING gist (room_id WITH =, stay WITH &&) WHERE (status = 'ACTIVE')}.
 *
 * <p><b>Cột {@code stay} cố ý KHÔNG có mặt trong entity này.</b> Nó là cột
 * sinh tự động từ {@code check_in} và {@code check_out}; ánh xạ vào đây sẽ
 * khiến Hibernate cố ghi vào nó và hỏng với lỗi "cannot insert into generated
 * column". Không ánh xạ cũng là cách nói rằng tầng Java không có quyền quyết
 * định giá trị của nó.
 */
@Entity
@Table(name = "booking_rooms")
@Getter
@Setter
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingRoomStatus status = BookingRoomStatus.ACTIVE;
}
