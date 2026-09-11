package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.BookingStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký chuyển trạng thái đơn, chỉ ghi thêm. */
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingIdOrderByIdAsc(Long bookingId);

    /**
     * Nạp kèm người thực hiện.
     *
     * <p>{@code open-in-view} đã tắt, nên đọc {@code entry.getChangedBy()} sau
     * khi transaction đóng sẽ ném {@code LazyInitializationException}.
     */
    @org.springframework.data.jpa.repository.Query("""
            select h from BookingStatusHistory h
            left join fetch h.changedBy
            where h.booking.id = :bookingId
            order by h.id
            """)
    List<BookingStatusHistory> findWithActorByBookingId(
            @org.springframework.data.repository.query.Param("bookingId") Long bookingId);
}
