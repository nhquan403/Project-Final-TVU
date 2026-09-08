package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đơn đặt phòng. */
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
