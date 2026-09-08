package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

/** Gán phòng vật lý cho đơn, trong một khoảng ngày. */
public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
}
