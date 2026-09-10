package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.BookingStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký chuyển trạng thái đơn, chỉ ghi thêm. */
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingIdOrderByIdAsc(Long bookingId);
}
